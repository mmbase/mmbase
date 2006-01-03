/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import java.io.File;
import java.util.*;
import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;

import org.mmbase.bridge.*;
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
 * @version $Id: ImageTag.java,v 1.63 2006-01-03 16:12:36 nklasens Exp $
 */

public class ImageTag extends FieldTag {

    private static final Logger log = Logging.getLoggerInstance(ImageTag.class);

    private static final int MODE_URL = 0;
    private static final int MODE_HTML_ATTRIBUTES = 1;
    private static final int MODE_HTML_IMG = 2;

    private static final String CROP_BEGIN = "begin";
    private static final String CROP_MIDDLE = "middle";
    private static final String CROP_END = "end";


    private static Boolean makeRelative = null;

    /** Holds value of property template. */
    private Attribute template = Attribute.NULL;

    /** Holds value of property mode. */
    private Attribute mode = Attribute.NULL;

    /** Holds value of property width. */
    private Attribute width = Attribute.NULL;
    
    /** Holds value of property height. */
    private Attribute height = Attribute.NULL;
    
    /** Holds value of property crop. */
    private Attribute crop = Attribute.NULL;
    
    /** Holds value of property style. */
    private Attribute style = Attribute.NULL;
    
    /** Holds value of property clazz. */
    private Attribute styleClass = Attribute.NULL;
    
    /** Holds value of property align. */
    private Attribute align = Attribute.NULL;
    
    /** Holds value of property border. */
    private Attribute border = Attribute.NULL;
    
    /** Holds value of property hspace. */
    private Attribute hspace = Attribute.NULL;
    
    /** Holds value of property vspace. */
    private Attribute vspace = Attribute.NULL;

    
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

    private int getMode() throws JspTagException {
        String m = mode.getString(this).toLowerCase();
        if (m.equals("") || m.equals("url")) {
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
        if (m.equals("")) { 
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

    public int doStartTag() throws JspTagException {
        Node node = getNode();
        if (!node.getNodeManager().hasField("handle")) {
            throw new JspTagException("Found parent node '" + node.getNumber() + "' of type " + node.getNodeManager().getName() + " does not have 'handle' field, therefore cannot be a image. Perhaps you have the wrong node, perhaps you'd have to use the 'node' attribute?");
        }

        HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();

        String servletArgument; // can be the node-number or a template (if that is configured to be allowed).

        String t = template.getString(this);
        if (t.length() == 0) {
            t = getTemplateFromAttributes();
        }
        if ("true".equals(pageContext.getServletContext().getInitParameter("mmbase.taglib.image.format.asis"))) {
            if (t.length() > 0) {
                t = t + "+f(asis)";
            } else {
                t = "f(asis)";
            }
        }
        if ("".equals(t)) {
            // the node/image itself
            servletArgument = node.getStringValue("number");
        } else {
            if ("true".equals(pageContext.getServletContext().getInitParameter("mmbase.taglib.image.urlconvert"))) {
                try {                    
                    servletArgument = "" + node.getNumber() + "+" + java.net.URLEncoder.encode(t, "UTF-8");
                } catch (java.io.UnsupportedEncodingException uee) {
                    // cannot happen 'UTF-8' is supported.
                    servletArgument = "" + node.getNumber() + "+" + t;
                }
            } else {
                // the cached image
                servletArgument = node.getFunctionValue("cache", new Parameters(Images.CACHE_PARAMETERS).set("template", t)).toString();
            }
        }

        if (makeRelative == null) {            
            String setting = pageContext.getServletContext().getInitParameter("mmbase.taglib.url.makerelative");
            makeRelative = "true".equals(setting) ? Boolean.TRUE : Boolean.FALSE;
        }


        String servletPath;
        Function servletPathFunction = node.getFunction("servletpath");        
        Parameters args = servletPathFunction.createParameters();
        args.set("context",  makeRelative.booleanValue() ? UriParser.makeRelative(new File(req.getServletPath()).getParent(), "/") : req.getContextPath())
            .set("argument", servletArgument);
        fillStandardParameters(args);
        servletPath = servletPathFunction.getFunctionValue( args).toString();

        helper.useEscaper(false);
        prevDimension = pageContext.getAttribute("dimension");
        switch(getMode()) {
        case MODE_URL: 
            helper.setValue(((HttpServletResponse) pageContext.getResponse()).encodeURL(servletPath));
            pageContext.setAttribute("dimension", new LazyDimension(getNodeVar(), t));
            break;
        case MODE_HTML_ATTRIBUTES: {
            Dimension dim = getDimension(node, t);

            String url = ((HttpServletResponse) pageContext.getResponse()).encodeURL(servletPath);
            if (dim.getHeight() > 0 && dim.getWidth() > 0) {
                helper.setValue(getBaseAttributes(url, dim));
            } else {
                log.warn("Found odd dimension " + dim);
                helper.setValue(getSrcAttribute(url));
            }
            pageContext.setAttribute("dimension", dim);
            break;
        }
        case MODE_HTML_IMG: {
            Dimension dim = getDimension(node, t);
            String url = ((HttpServletResponse) pageContext.getResponse()).encodeURL(servletPath);
            if (dim.getHeight() > 0 && dim.getWidth() > 0) {
                helper.setValue("<img " + getBaseAttributes(url, dim) + getAltAttribute(node) + getOtherAttributes() + " />");
            } else {
                log.warn("Found odd dimension " + dim);
                helper.setValue("<img " + getSrcAttribute(url) + getAltAttribute(node) + getOtherAttributes() + " />");
            }
            pageContext.setAttribute("dimension", dim);
        }
        }
        
        if (getId() != null) {
            getContextProvider().getContextContainer().register(getId(), helper.getValue());
        }
        
        return EVAL_BODY_BUFFERED;
    }

    private String getSrcAttribute(String url) {
        return " src=\"" + url + "\"";
    }

    private String getBaseAttributes(String url, Dimension dim) {
        return getSrcAttribute(url) + " height=\"" + dim.getHeight() + "\" width=\"" + dim.getWidth() + "\"";
    }

    private String getAltAttribute(Node node) {
        String alt = null;
        if (node.getNodeManager().hasField("title")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("title"));
        } 
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("name")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("name"));
        }
        if ((alt == null || "".equals(alt)) && node.getNodeManager().hasField("description")) {
            alt = org.mmbase.util.transformers.Xml.XMLAttributeEscape(node.getStringValue("description"));
        }
        
        return (alt == null ? "" : " alt=\"" + alt + "\" title=\"" + alt + "\"");
    }

    private String getOtherAttributes() throws JspTagException {
        StringBuffer attributes = new StringBuffer();
        attributes.append((styleClass != Attribute.NULL) ? (" class=\"" + styleClass.getString(this) + "\"") : "");
        attributes.append((style != Attribute.NULL) ? (" style=\"" + style.getString(this) + "\"") : "");
        attributes.append((align != Attribute.NULL) ? (" align=\"" + align.getString(this) + "\"") : "");
        attributes.append((border != Attribute.NULL) ? (" border=\"" + border.getString(this) + "\"") : " border=\"0\"");
        attributes.append((hspace != Attribute.NULL) ? (" hspace=\"" + hspace.getString(this) + "\"") : "");
        attributes.append((vspace != Attribute.NULL) ? (" vspace=\"" + vspace.getString(this) + "\"") : "");
        return attributes.toString();
    }
    
    public int doEndTag() throws JspTagException {
        if (prevDimension == null) {
            pageContext.removeAttribute("dimension");
        } else {
            pageContext.setAttribute("dimension", prevDimension);
        }
        helper.doEndTag();
        return super.doEndTag();
        
    }

    private Dimension getDimension(Node node, String template) {
        List a = new ArrayList();
        if (template != null) {
            a.add(template);
        }
        return (Dimension) node.getFunctionValue("dimension", a).get();
    }
    
    private String getTemplateFromAttributes() throws JspTagException {
        int widthTemplate = width.getInt(this, 0);
        int heightTemplate = height.getInt(this, 0);
        
        if ((widthTemplate <= 0) && (heightTemplate <= 0)) {
            return "";
        }
        else {
            String cropTemplate = getCrop();
            if (cropTemplate != null) {
               String template = getCropTemplate(widthTemplate, heightTemplate, cropTemplate);
               return template;
            }
            else {
               String template = getResizeTemplate(widthTemplate, heightTemplate);
               return template;
            }
        }
    }

    /**
     * Returns the crop template string to be used by img servlet
     *
     * @param width - template width
     * @param height - template height
     * @return the template
     */
    public String getCropTemplate(int width, int height, String cropTemplate) throws JspTagException {
        Dimension imageDimension = getDimension(getNode(), null);
        int imageWidth = imageDimension.getWidth();
        int imageHeight = imageDimension.getHeight();
        int newWidth = width > 0 ? width : imageWidth;
        int newHeight = height > 0 ? height : imageHeight;

        // define orientation of images
        StringBuffer template = new StringBuffer();
        float horizontalMultiplier = (float) newWidth / (float) imageWidth;
        float verticalMultiplier = (float) newHeight / (float) imageHeight;
        int tempWidth = (int) (imageWidth * verticalMultiplier);
        int tempHeight = (int) (imageHeight * horizontalMultiplier);
        int xOffset = 0;
        int yOffset = 0;

        if (horizontalMultiplier == verticalMultiplier) {
            // only scaling
            template.append("s(").append(width).append(")");
        }
        else {
            if (horizontalMultiplier > verticalMultiplier) {
                // scale horizontal, crop vertical
                if (cropTemplate.equals(CROP_END)) {
                    yOffset = 0;
                }
                else {
                    if (cropTemplate.equals(CROP_BEGIN)) {
                        yOffset = (tempHeight - newHeight);
                    }
                    else {
                        // CROP_MIDDLE
                        yOffset = (tempHeight - newHeight) / 2;
                    }
                }
                template.append("s(").append(newWidth).append(")");
                template.append("+part(").append(xOffset).append(",").append(yOffset).append(",");
                template.append(xOffset + newWidth).append(",").append(yOffset + newHeight).append(")");
            }
            else {
                // scale vertical, crop horizontal
                if (cropTemplate.equals(CROP_END)) {
                    xOffset = 0;
                }
                else {
                    if (cropTemplate.equals(CROP_BEGIN)) {
                        xOffset = (tempWidth - newWidth);
                    }
                    else {
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
     *
     * @param width - template width
     * @param height - template height
     * @return the template
     */
    public String getResizeTemplate(int width, int height) throws JspTagException {
        Dimension imageDimension = getDimension(getNode(), null);
        int imageWidth = imageDimension.getWidth();
        int imageHeight = imageDimension.getHeight();
        int newWidth = width > 0 ? width : imageWidth;
        int newHeight = height > 0 ? height : imageHeight;

        // define orientation of images
        StringBuffer template = new StringBuffer();
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

        log.debug(template.toString());
        return template.toString();
    }

}

