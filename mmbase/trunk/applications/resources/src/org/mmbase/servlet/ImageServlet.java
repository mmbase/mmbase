/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.util.Map;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.security.Rank;

import org.mmbase.module.builders.Images;
import org.mmbase.util.logging.*;
import org.mmbase.util.functions.*;

/**
 * ImageServlet handles nodes as images. If you want to convert an image (resize it, turn it, change
 * its colors etc) then you want to serve an 'icaches' node ('icaches' are cached conversions of
 * images), which you have to create yourself before calling this servlet. The cache() function of
 * Images can be used for this. An URL can be gotten with cachepath().
 *
 * @version $Id: ImageServlet.java,v 1.2 2007-02-12 17:22:45 michiel Exp $
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @see    org.mmbase.module.builders.AbstractImages
 * @see    org.mmbase.module.builders.Images#executeFunction
 * @see    AttachmentServlet
 */
public class ImageServlet extends HandleServlet {
    private static Logger log;

    /**
     * Wheter this servlet is capable of doing transformations by itself.
     * @since MMBase-1.7.4
     */
    private boolean convert = false;

    public void init() throws ServletException {
        super.init();
        String convertParameter = getInitParameter("convert");
        convert = "true".equals(convertParameter);
        log = Logging.getLoggerInstance(ImageServlet.class);
        if (convert) {
            log.service("Image servlet will accept image conversion templates");
        }
    }

    // just to get ImageServlet in the stacktrace.
    protected final Cloud getClassCloud() {
        return super.getClassCloud();
    }

    public String getServletInfo()  {
        return "Serves (cached) MMBase images";
    }

    protected Map<String, Integer> getAssociations() {
        Map<String, Integer> a = super.getAssociations();
        a.put("images",      50);  // Is good in images (knows icaches)
        a.put("attachments", 5);   // Can do attachments a little
        a.put("downloads",   -10); // Can do downloads even worse.
        return a;
    }

    protected String getMimeType(Node node) {
        return node.getFunctionValue("mimetype", null).toString();
    }
    

    /**
     * Content-Disposition header
     * {@inheritDoc}
     */

    protected boolean setContent(QueryParts query, Node node, String mimeType) throws java.io.IOException {
        Node originalNode;
        if (node.getNodeManager().getName().equals("icaches")) {
            Cloud c = node.getCloud();
            int originalNodeNumber = node.getIntValue("id");

            if (! c.mayRead(originalNodeNumber) && c.getUser().getRank().equals(Rank.ANONYMOUS)) {
                // try (again?) cloud from session
                c = getCloud(query);
            }

            if (c == null || ! c.mayRead(originalNodeNumber)) {
                query.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "Permission denied on original image node '" + originalNodeNumber + "'");
                return false;
            }
            originalNode = c.getNode(originalNodeNumber);
        } else { // 'images', but as you see this is not explicit, so you can also name your image builder otherwise.
            originalNode = node;
        }

        query.getResponse().setHeader("Content-Disposition", "inline; filename=\"" + getFileName(node, originalNode, "mmbase-image")+ "\"");
        return true;
    }

    /**
     * ImageServlet can serve a icache node in stead (using the 'extra parameters)'
     *
     * @since MMBase-1.7.4
     */
    protected Node getServedNode(QueryParts query, Node node) throws java.io.IOException {
        if (node == null) {
            return null;
        }
        Node n = query.getServedNode();
        if (n != null) {
            return n;
        }
        String nodeNumber     = query.getNodeNumber();
        String nodeIdentifier = query.getNodeIdentifier();
        if (node.getNodeManager().getName().equals("icaches")) {
            if (! nodeNumber.equals(nodeIdentifier)) {
                query.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot convert icache node");
                return null;
            } else {
                n = getNode(query);
                if (n.getFunctionValue("wait", null).toBoolean()) {
                    // node may be changed, and crrent instante may not be valid any more because disappeared from cache
                    query.setNode(null);
                    n = getNode(query);
                }

            }
        } else {
            // This _is_ an original node.
            if (! nodeNumber.equals(nodeIdentifier)) {
                if (convert) {
                    Parameters args = new Parameters(Images.CACHE_PARAMETERS);
                    String template = nodeIdentifier.substring(nodeNumber.length() + 1);
                    try {
                        template = java.net.URLDecoder.decode(template, "UTF-8");
                    } catch (IllegalArgumentException iae) {
                        // never mind
                    }
                    args.set("template", template);
                    int icacheNodeNumber = node.getFunctionValue("cache", args).toInt();
                    if (icacheNodeNumber < 0) {
                    	log.error("Invalid image cache entry for node " + nodeNumber + " : " + args);
                    	return null;
                    }
                    Cloud cloud = node.getCloud();
                    cloud = findCloud(cloud, "" + icacheNodeNumber, query);
                    if (cloud == null) {
                        return null;
                    }
                    Node icache = cloud.getNode(icacheNodeNumber);
                    icache.getFunctionValue("wait", null);
                    n = icache;
                } else {
                    query.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "This server does not allow you to convert an image in this way");
                    return null;
                }
            } else {
                n =  getNode(query);
            }
        }
        query.setServedNode(n);
        return n;
    }

    /**
     * Overridden to support 'title aliases'.
     * @since MMBase-1.7.5
     */
    protected Node desperatelyGetNode(Cloud cloud, String nodeIdentifier) {
        log.debug("Desperately searching node '" + nodeIdentifier + "'");
        NodeManager nm = cloud.getNodeManager("images");
        NodeQuery nq = nm.createQuery();
        Constraint c = nq.createConstraint(nq.createStepField("title"), nodeIdentifier);
        nq.setConstraint(c);
        nq.addSortOrder(nq.createStepField("number"), SortOrder.ORDER_DESCENDING);
        NodeList result = nm.getList(nq);
        if (result.size() == 0) return null;
        return result.getNode(0);        
    }


}
