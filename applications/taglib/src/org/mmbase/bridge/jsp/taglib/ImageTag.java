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

import org.mmbase.bridge.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.UriParser;
import org.mmbase.module.builders.AbstractServletBuilder;
import org.mmbase.module.builders.Images;

import org.mmbase.security.Rank;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Produces an url to the image servlet mapping. Using this tag makes
 * your pages more portable to other system, and hopefully less
 * sensitive for future changes in how the image servlet works.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ImageTag.java,v 1.51 2005-01-03 18:06:21 michiel Exp $
 */

public class ImageTag extends FieldTag {

    private static final Logger log = Logging.getLoggerInstance(ImageTag.class);

    private static Boolean makeRelative = null;
    private Attribute template = Attribute.NULL;

    /**
     * The transformation template
     */

    public void setTemplate(String t) throws JspTagException {
        template = getAttribute(t);
    }

    public int doStartTag() throws JspTagException {
        node = null;
        getNodeVar();
        if (!node.getNodeManager().hasField("handle")) {
            throw new JspTagException("Found parent node '" + node.getNumber() + "' of type " + node.getNodeManager().getName() + " does not have 'handle' field, therefore cannot be a image. Perhaps you have the wrong node, perhaps you'd have to use the 'node' attribute?");
        }

        // some servlet implementation's 'init' cannot determin this theirselves, help them a little:
        HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
        String context = req.getContextPath();

        /* perhaps 'getSessionName' should be added to CloudProvider
         */
        String sessionName = "";

        if(! getCloudVar().getUser().getRank().equals(Rank.ANONYMOUS.toString())) {
            // the user is not anonymous!
            // Need to check if node is readable by anonymous.
            // in that case URLs can be simpler
            CloudTag ct = null;
            ct = (CloudTag) findParentTag(CloudTag.class, null, false);
            if (ct != null) {
                CloudContext cc = ct.getDefaultCloudContext();
                try {
                    Cloud anonymousCloud = cc.getCloud(ct.getName());
                    anonymousCloud.getNode(node.getNumber());
                } catch (org.mmbase.security.SecurityException se) {
                    // two situations are anticipated:
                    // - node not readable by anonymous
                    // - no anonymous user defined
                    sessionName = ct.getSessionName();
                }
            } else {
                // how can this happen?
            }
        }

        String number;
        String t = template.getString(this);
        if ("".equals(t)) {
            // the node/image itself
            number = node.getStringValue("number");
        } else {
            if ("false".equals(pageContext.getServletContext().getInitParameter("mmbase.taglib.url.convert"))) {
                number = "" + node.getNumber() + "+" + t;
            } else {
                // the cached image
                number = node.getFunctionValue("cache", new ParametersImpl(Images.CACHE_PARAMETERS).set("template", t)).toString();
            }
        }

        if (makeRelative == null) {            
            String setting = pageContext.getServletContext().getInitParameter("mmbase.taglib.url.makerelative");
            makeRelative = "true".equals(setting) ? Boolean.TRUE : Boolean.FALSE;
        }


        String servletPath;
        {
            Parameters args = new ParametersImpl(AbstractServletBuilder.SERVLETPATH_PARAMETERS)
                .set("session",  sessionName)
                .set("context",  makeRelative.booleanValue() ?
                     UriParser.makeRelative(new File(req.getServletPath()).getParent(), "/") :
                     req.getContextPath()
                     )
                .set("argument", number)
                ;
            servletPath = node.getFunctionValue("servletpath", args).toString();
        }

        super.handleEditTag();

        helper.useEscaper(false);
        helper.setValue(((HttpServletResponse) pageContext.getResponse()).encodeURL(servletPath));
        if (getId() != null) {
            getContextProvider().getContextContainer().register(getId(), helper.getValue());
        }
        return EVAL_BODY_BUFFERED;
    }
}

