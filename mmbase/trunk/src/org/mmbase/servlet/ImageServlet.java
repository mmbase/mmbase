/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import javax.servlet.http.HttpServletResponse;

import java.util.Map;

import org.mmbase.bridge.Node;

/**
 * ImageServlet handles nodes as images. If you want to convert an image (resize it, turn it, change
 * its colors etc) then you want to serve an 'icaches' node ('icaches' are cached conversions of
 * images), which you have to create yourself before calling this servlet. The cache() function of
 * Images can be used for this. An URL can be gotten with cachepath().
 *
 * @version $Id: ImageServlet.java,v 1.12 2003-03-04 15:32:14 nico Exp $
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @see    org.mmbase.module.builders.AbstractImages
 * @see    org.mmbase.module.builders.Images#executeFunction
 * @see    AttachmentServlet
 */
public class ImageServlet extends HandleServlet {

    public String getServletInfo()  {
        return "Serves (cached) MMBase images";
    }

    protected Map getAssociations() {
        Map a = super.getAssociations();
        a.put("images",      new Integer(50));  // Is good in images (knows icaches)
        a.put("attachments", new Integer(5));   // Can do attachments a little
        a.put("downloads",   new Integer(-10)); // Can do downloads even worse.
        return a;
    }

    protected String getMimeType(Node node) {
        return node.getFunctionValue("mimetype", null).toString();
    }

    
    protected boolean setContent(HttpServletResponse res, Node node, String mimeType) throws java.io.IOException {
        String fileName; // will be based on the 'title' field, because images lack a special field for this now.
        if (node.getNodeManager().getName().equals("icaches")) {
            try {
                fileName = node.getCloud().getNode(node.getIntValue("id")).getStringValue("title");
            } catch (org.mmbase.security.SecurityException e) {
                res.sendError(res.SC_FORBIDDEN, "Permission denied on original image node: " + e.toString());
                return false;
            }
        } else { // 'images', but as you this is not explicit, so you can also name your image builder otherwise.
            fileName = node.getStringValue("title"); 
        }

        // still not found a sensible fileName? Give it up then.
        if (fileName == null || fileName.equals("")) fileName = "mmbase-image";

        res.setHeader("Content-Disposition", "inline; filename=\"" + fileName  + "." + node.getFunctionValue("format", null).toString() + "\"");
        return true;
    }

}
