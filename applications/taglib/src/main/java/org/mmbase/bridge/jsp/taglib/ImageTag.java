/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import java.io.File;
import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspException;

import org.mmbase.bridge.*;
import org.mmbase.util.HttpServletRequestUtils;
import org.mmbase.util.functions.*;
import org.mmbase.util.images.*;
import org.mmbase.util.UriParser;
import org.mmbase.module.builders.Images;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Produces an url to the image servlet mapping. Using this tag makes
 * your pages more portable to other system, and hopefully less
 * sensitive for future changes in how the image servlet works.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public class ImageTag extends FieldTag {

    private static final Logger log = Logging.getLoggerInstance(ImageTag.class);

    public static final int MODE_URL = 0;
    public static final int MODE_HTML_ATTRIBUTES = 1;
    public static final int MODE_HTML_IMG = 2;

    public static final String CROP_BEGIN = "begin";
    public static final String CROP_MIDDLE = "middle";
    public static final String CROP_END = "end";


    private static Boolean makeRelative = null;
    private static Boolean urlConvert   = null;

    private Attribute template    = Attribute.NULL;
    private Attribute mode        = Attribute.NULL;
    private Attribute width       = Attribute.NULL;
    private Attribute height      = Attribute.NULL;
    private Attribute crop        = Attribute.NULL;
    private Attribute style       = Attribute.NULL;
    private Attribute styleClass  = Attribute.NULL;
    private Attribute align       = Attribute.NULL;
    private Attribute border      = Attribute.NULL;
    private Attribute hspace      = Attribute.NULL;
    private Attribute vspace      = Attribute.NULL;
    private Attribute xmlns       = Attribute.NULL;


    private Attribute altAttribute = Attribute.NULL;

    private Attribute absolute     = Attribute.NULL;
    private Attribute disposition  = Attribute.NULL;


    private Object prevDimension;

    /**
     * The transformation template
     */
    public void setTemplate(String t) throws JspTagException {
        template = getAttribute(t);
    }

    public void setMode(String m) throws JspTagException {
        mode = getAttribute(m);
    }

    public void setAlign(String align) throws JspTagException {
        this.align = getAttribute(align);
    }


    public void setBorder(String border) throws JspTagException {
        this.border = getAttribute(border);
    }


    public void setStyleClass(String styleClass) throws JspTagException {
        this.styleClass = getAttribute(styleClass);
    }


    public void setCrop(String crop) throws JspTagException {
        this.crop = getAttribute(crop);
    }


    public void setHeight(String height) throws JspTagException {
        this.height = getAttribute(height);
    }


    public void setHspace(String hspace) throws JspTagException {
        this.hspace = getAttribute(hspace);
    }

    public void setStyle(String style) throws JspTagException {
        this.style = getAttribute(style);
    }


    public void setVspace(String vspace) throws JspTagException {
        this.vspace = getAttribute(vspace);
    }


    public void setWidth(String width) throws JspTagException {
        this.width = getAttribute(width);
    }
    public void setAlt(String a) throws JspTagException {
        altAttribute = getAttribute(a);
    }

    /**
     * @since MMBase-1.9.1
     */
    public void setAbsolute(String a) throws JspTagException {
        absolute = getAttribute(a, true);
    }

    /**
     * @since MMBase-1.9.1
     */
    public void setDisposition(String d) throws JspTagException {
        disposition = getAttribute(d, true);
    }

    /**
     * @since MMBase-1.9.2
     */
    public void setXmlnamespace(String x) throws JspTagException {
        xmlns = getAttribute(x, true);
    }

    private int getMode() throws JspTagException {
        String m = mode.getString(this).toLowerCase();
        if (m.length() == 0 || m.equals("url")) {
            return MODE_URL;
        } else if (m.equals("attributes")) {
            return MODE_HTML_ATTRIBUTES;
        } else if (m.equals("img")) {
            return MODE_HTML_IMG;
        } else {
            throw new JspTagException("Value '" + m + "' not known for 'mode' attribute");
        }
    }


    private String getCrop() throws JspTagException {
        String m = crop.getString(this).toLowerCase();
        if (m.length() == 0) {
            return null;
        } else if (m.equals("middle")) {
            return CROP_MIDDLE;
        } else if (m.equals("begin")) {
            return CROP_BEGIN;
        } else if (m.equals("end")) {
            return CROP_END;
        } else {
            throw new JspTagException("Value '" + m + "' not known for 'crop' attribute");
        }
    }

    private boolean makeRelative() {
        if (makeRelative == null) {
            String setting = pageContext.getServletContext().getInitParameter("mmbase.taglib.url.makerelative");
            makeRelative = Boolean.valueOf("true".equals(setting));
        }
        return makeRelative.booleanValue();
    }

    protected Node getServletNode(Node node, String template) {
        if (urlConvert() || "".equals(template)) {
            return node;
        } else {
            // the cached image
            log.debug("Getting cached node for " + node.getNumber());
            return node.getFunctionValue("cachednode", new Parameters(Images.CACHE_PARAMETERS).set("template", template)).toNode();
        }

    }

    private boolean urlConvert() {
        if (urlConvert == null) {
            urlConvert = Boolean.valueOf("true".equals(pageContext.getServletContext().getInitParameter("mmbase.taglib.image.urlconvert")));
        }
        return urlConvert.booleanValue();
    }

    @Override
    public int doStartTag() throws JspException {
        initTag();
        Node originalNode = getNode();
        if (!originalNode.getNodeManager().hasField("handle")) {
            throw new JspTagException("Found parent node '" + originalNode.getNumber() + "' of type " +
                                      originalNode.getNodeManager().getName() +
                                      " does not have 'handle' field, therefore cannot be a image." +
                                      " Perhaps you have the wrong node, perhaps you'd have to use the 'node' attribute?");
        }

        helper.useEscaper(false);
        prevDimension = pageContext.getAttribute("dimension");

        String templateStr = getTemplate(originalNode, template.getString(this), width.getInt(this, 0), height.getInt(this, 0), getCrop());
        Dimension dim = getDimension(originalNode, templateStr);

        Node node = getServletNode(originalNode, templateStr);

        String servletArgument;
        if (node == null) {
            node = originalNode;
            log.warn("Found null from " + node + " with '" + templateStr + "'");
            servletArgument = node.getStringValue("number");
        } else {
            servletArgument = getServletArgument(node, templateStr);
        }

        String servletPath = getServletPath(node, servletArgument);


        String outputValue = getOutputValue(getMode(), originalNode, servletPath, dim);

        if (outputValue != null) {
            helper.setValue(outputValue);
        }
        pageContext.setAttribute("dimension", dim);

        if (getId() != null) {
            getContextProvider().getContextContainer().register(getId(), helper.getValue());
        }

        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspTagException {
        if (prevDimension == null) {
            pageContext.removeAttribute("dimension");
        } else {
            pageContext.setAttribute("dimension", prevDimension);
        }
        helper.doEndTag();
        return super.doEndTag();
    }

    public static final org.mmbase.util.transformers.UrlEscaper URLESCAPER= new org.mmbase.util.transformers.UrlEscaper();
    public String getServletArgument(Node node, String t) {
        String servletArgument; // can be the node-number or a template (if that is configured to be allowed).
        if ("".equals(t) || ! urlConvert()) {
            // the node/image itself
            servletArgument = node.getStringValue("number");
        } else {
            servletArgument = "" + node.getNumber() + "+" + URLESCAPER.transform(t);
        }
        return servletArgument;
    }

    protected String getServletPath(Node node, String servletArgument) throws JspTagException {
        Function servletPathFunction = getServletFunction(node);
        Parameters args = getServletArguments(servletArgument, servletPathFunction);
        fillStandardParameters(args);
        String url = servletPathFunction.getFunctionValue(args).toString();
        if (absolute != Attribute.NULL) {
            String a = absolute.getString(this);
            HttpServletRequest req = (HttpServletRequest) getPageContext().getRequest();
            if ("true".equals(a)) {
                StringBuilder show = new StringBuilder();
                HttpServletRequestUtils.appendAbsolute(req, show);
                show.append(url);
                url = show.toString();
            } else if ("context".equals(a)) {
                url = url.substring(req.getContextPath().length(), url.length());
            } else if ("server".equals(a)) {
                // Ok, that's it already.
            } else {
                throw new TaglibException("Invalid value for absolute attribute '" + a + "'");
            }
        }
        return url;
    }

    public Function getServletFunction(Node node) {
        Function servletPathFunction = node.getFunction("servletpath");
        return servletPathFunction;
    }

    public Parameters getServletArguments(String servletArgument, Function servletPathFunction) throws JspTagException {
        HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
        Parameters args = servletPathFunction.createParameters();
        args.set("context",  makeRelative() ? UriParser.makeRelative(new File(req.getServletPath()).getParent(), "/") : req.getContextPath())
            .set("argument", servletArgument);
        if (disposition != Attribute.NULL) {
            args.set("disposition", disposition.getString(this));
        }
        return args;
    }


    /**
     * @since MMBase-1.9.4
     */
    protected String encodeURL(Node node, String servletPath) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        Object encodeUrls = request.getAttribute(ContentTag.ENCODEURLS_KEY);
        if (Boolean.FALSE.equals(encodeUrls) || node.getCloud().getCloudContext().getCloud(node.getCloud().getName()).mayRead(node.getNumber())) {
            return servletPath;
        } else {
            return ((HttpServletResponse) pageContext.getResponse()).encodeURL(servletPath);
        }
    }

    protected String getOutputValue(int mode, Node node, String servletPath, Dimension dim) throws JspTagException {
        String outputValue = null;
        switch(mode) {
        case MODE_URL:
            outputValue = encodeURL(node, servletPath);
            break;
        case MODE_HTML_ATTRIBUTES: {
            String url = encodeURL(node, servletPath);
            if (dim.getHeight() > 0 && dim.getWidth() > 0) {
                outputValue = getBaseAttributes(url, dim);
            } else {
                log.warn("Found odd dimension " + dim);
                outputValue = getSrcAttribute(url);
            }
            break;
        }
        case MODE_HTML_IMG: {
            String url = encodeURL(node, servletPath);
            if (dim.getHeight() > 0 && dim.getWidth() > 0) {
                outputValue = "<img " + getBaseAttributes(url, dim) + getAltAttribute(node) + getOtherAttributes() + " />";
            } else {
                log.warn("Found odd dimension " + dim);
                outputValue = "<img " + getSrcAttribute(url) + getAltAttribute(node) + getOtherAttributes() + " />";
            }
        }
        }
        return outputValue;
    }

    protected String getSrcAttribute(String url) throws JspTagException {
        return " src=\"" + url + "\"";
    }

    protected String getBaseAttributes(String url, Dimension dim) throws JspTagException {
        return getSrcAttribute(url) + " height=\"" + dim.getHeight() + "\" width=\"" + dim.getWidth() + "\"";
    }

    protected String getAltAttribute(Node node) throws JspTagException {
        String alt = null;
        if (altAttribute != Attribute.NULL) {
            alt = altAttribute.getString(this);
        }
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("alt")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("alt"), '\"');
        }
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("title")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("title"), '\"');
        }
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("name")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("name"), '\"');
        }
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("description")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("description"), '\"');
        }


        return (alt == null ? " alt=\"\"" : " alt=\"" + alt + "\" title=\"" + alt + "\"");
    }

    protected String getOtherAttributes() throws JspTagException {
        StringBuilder attributes = new StringBuilder();
        attributes.append((styleClass != Attribute.NULL) ? (" class=\"" + styleClass.getString(this) + "\"") : "");
        attributes.append((style != Attribute.NULL) ? (" style=\"" + style.getString(this) + "\"") : "");
        attributes.append((align != Attribute.NULL) ? (" align=\"" + align.getString(this) + "\"") : "");
        attributes.append((border != Attribute.NULL) ? (" border=\"" + border.getString(this) + "\"") : "");
        attributes.append((hspace != Attribute.NULL) ? (" hspace=\"" + hspace.getString(this) + "\"") : "");
        attributes.append((vspace != Attribute.NULL) ? (" vspace=\"" + vspace.getString(this) + "\"") : "");
        attributes.append((xmlns != Attribute.NULL) ? (" xmlns=\"" + xmlns.getString(this) + "\"") : "");
        return attributes.toString();
    }

    public Dimension getDimension(Node node, String template) {
        return new LazyDimension(node, template);
    }

    /**
     * Get template to manipulate image
     * @param node - image node
     * @param t - custom template string
     * @param widthTemplate - width of image, which is used when custom is not provided
     * @param heightTemplate - height of image, which is used when custom is not provided
     * @param cropTemplate - crop the image. values are 'begin', 'middle' and 'end'.
     * @return template for image
     */
    public String getTemplate(Node node, String t, int widthTemplate, int heightTemplate, String cropTemplate) {
        String template = "";
        if ((widthTemplate > 0) || (heightTemplate > 0)) {
            if (cropTemplate != null && cropTemplate.length() != 0) {
                template = getCropTemplate(node, widthTemplate, heightTemplate, cropTemplate);
            } else {
                template = getResizeTemplate(node, widthTemplate, heightTemplate);
            }
        }
        if (t != null && t.length() != 0) {
           if (template.length() == 0) {
             template = t;
           } else {
             template = template + "+" + t;
           }
        }
        boolean asis = "true".equals(pageContext.getServletContext().getInitParameter("mmbase.taglib.image.format.asis")) &&
                       template.indexOf("+f(") == -1;
        if (asis) {
            if (template.length() > 0) {
                template = template + "+f(asis)";
            } else {
                template = "f(asis)";
            }
        }
        return template;
    }

    /**
     * Returns the crop template string to be used by img servlet
     * @param node - get template for this node
     * @param width - template width
     * @param height - template height
     * @return the crop template
     */
    protected String getCropTemplate(Node node, int width, int height, String cropTemplate) {
        Dimension imageDimension = getDimension(node, null);
        int imageWidth = imageDimension.getWidth();
        int imageHeight = imageDimension.getHeight();
        int newWidth = width > 0 ? width : imageWidth;
        int newHeight = height > 0 ? height : imageHeight;

        // define orientation of images
        StringBuilder template = new StringBuilder();
        float horizontalMultiplier = (float) newWidth / (float) imageWidth;
        float verticalMultiplier = (float) newHeight / (float) imageHeight;
        int tempWidth = (int) (imageWidth * verticalMultiplier);
        int tempHeight = (int) (imageHeight * horizontalMultiplier);
        int xOffset = 0;
        int yOffset = 0;

        if (horizontalMultiplier == verticalMultiplier) {
            // only scaling
            template.append("s(").append(width).append(")");
        } else {
            if (horizontalMultiplier > verticalMultiplier) {
                // scale horizontal, crop vertical
                if (cropTemplate.equals(CROP_END)) {
                    yOffset = 0;
                } else {
                    if (cropTemplate.equals(CROP_BEGIN)) {
                        yOffset = (tempHeight - newHeight);
                    } else {
                        // CROP_MIDDLE
                        yOffset = (tempHeight - newHeight) / 2;
                    }
                }
                template.append("s(").append(newWidth).append(")");
                template.append("+part(").append(xOffset).append(",").append(yOffset).append(",");
                template.append(xOffset + newWidth).append(",").append(yOffset + newHeight).append(")");
            } else {
                // scale vertical, crop horizontal
                if (cropTemplate.equals(CROP_END)) {
                    xOffset = 0;
                } else {
                    if (cropTemplate.equals(CROP_BEGIN)) {
                        xOffset = (tempWidth - newWidth);
                    } else {
                        // CROP_MIDDLE
                        xOffset = (tempWidth - newWidth) / 2;
                    }
                }
                template.append("s(x").append(newHeight).append(")");
                template.append("+part(").append(xOffset).append(",").append(yOffset).append(",");
                template.append(xOffset + newWidth).append(",").append(yOffset + newHeight).append(")");
            }
        }
        log.debug(template.toString());
        return template.toString();
    }

    /**
     * Returns the resize template string to be used by img servlet without cropping
     * @param node - get template for this node
     * @param width - template width
     * @param height - template height
     * @return the resize template
     */
    protected String getResizeTemplate(Node node, int width, int height) {
        Dimension imageDimension = getDimension(node, null);
        int imageWidth = imageDimension.getWidth();
        int imageHeight = imageDimension.getHeight();
        int newWidth = width > 0 ? width : imageWidth;
        int newHeight = height > 0 ? height : imageHeight;

        // define orientation of images
        StringBuilder template = new StringBuilder();
        float horizontalMultiplier = (float) newWidth / (float) imageWidth;
        float verticalMultiplier = (float) newHeight / (float) imageHeight;

        if (horizontalMultiplier <= verticalMultiplier) {
            // scale horizontal
            template.append("+s(").append(newWidth).append(")");
        }
        else {
            // scale vertical
            template.append("+s(x").append(newHeight).append(")");
        }

        if (log.isDebugEnabled()) {
            log.debug(template.toString());
        }
        return template.toString();
    }

}

