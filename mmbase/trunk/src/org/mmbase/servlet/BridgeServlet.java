/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import java.io.IOException;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.mmbase.bridge.*;
import org.mmbase.util.logging.*;

/**
 * BridgeServlet is an MMBaseServlet with a bridge Cloud in it. Extending from this makes it easy to
 * implement servlet implemented with the MMBase bridge interfaces.
 *
 * An advantage of this is that security is used, which means that you cannot unintentionly serve
 * content to the whole world which should actually be protected by the security mechanism.
 *
 * Another advantage is that implementation using the bridge is easier/clearer.
 *
 * The query of a bridge servlet can possible start with session=<session-variable-name> in which case the
 * cloud is taken from that session attribute with that name. Otherewise 'cloud_mmbase' is
 * supposed. All this is only done if there was a session active at all. If not, or the session
 * variable was not found, that an anonymous cloud is used.
 *
 * @version $Id: BridgeServlet.java,v 1.17 2004-10-08 17:37:53 michiel Exp $
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 */
public abstract class BridgeServlet extends  MMBaseServlet {


    /**
     * Pattern used for the 'filename' part of the request. The a node-identifying string may be
     * present in it, and it the one capturing group.
     * It is a digit optionially followed by +.* (used in ImageServlet for url-triggered icache production)
     */

    private static final Pattern FILE_PATTERN = Pattern.compile(".*?\\D((?:session=.*?\\+)?\\d+(?:\\+.+?)?)(?:/.*)?"); 
    // some example captured by this regexp:
    //   /mmbase/images/session=mmbasesession+1234+s(100)/image.jpg
    //   /mmbase/images/1234+s(100)/image.jpg
    //   /mmbase/images/1234/image.jpg
    //   /mmbase/images/1234
    //   /mmbase/images?1234  (1234 not captured by regexp, but is in query!)


    // may not be digits in servlet mapping itself!


    private static Logger log;

    /**
     * This is constant after init.
     */
    private static int contextPathLength = -1;


    private String lastModifiedField = null;

    /**
     * The name of the mmbase cloud which must be used. At the moment this is not supported (every
     * mmbase cloud is called 'mmbase').
     */
    protected String getCloudName() {
        return "mmbase";
    }

    

    /**
     * Creates a QueryParts object which wraps request and response and the parse result of them.
     * @return A QueryParts or <code>null</code> if something went wrong (in that case an error was sent, using the response).
     */
    protected QueryParts readQuery(HttpServletRequest req, HttpServletResponse res) throws IOException  {
        QueryParts qp = (QueryParts) req.getAttribute("org.mmbase.servlet.BridgeServlet$QueryParts");
        if (qp != null) {
            log.trace("no need parsing query");
            if (qp.getResponse() == null && res != null) {
                qp.setResponse(res);
            }
            return qp;
        }
        log.trace("parsing query");

        String q = req.getQueryString();
        
        String query;
        if (q == null) { 
            // also possible to use /attachments/[session=abc+]<number>/filename.pdf
            if (contextPathLength == -1) {
                contextPathLength = req.getContextPath().length(); 
            }
            String reqString = req.getRequestURI().substring(contextPathLength); // substring needed, otherwise there may not be digits in context path.
            Matcher m = FILE_PATTERN.matcher(reqString);
            if (! m.matches()) {
                if (res != null) {
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Malformed URL: '" + reqString + "' does not match '"  + FILE_PATTERN.pattern() + "'.");
                }
                return null;
            }
            query = m.group(1);
            
        } else {
            // attachment.db?[session=abc+]number
            query = q;
        }
        
        String sessionName = null; // "cloud_" + getCloudName();
        String nodeIdentifier;
        if (query.startsWith("session=")) { 
            // indicated the session name in the query: session=<sessionname>+<nodenumber>
            
            int plus = query.indexOf("+", 8);
            if (plus == -1) {
                if (res != null) {
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Malformed URL: No node number found after session.");
                }
                return null;
            }
            sessionName = query.substring(8, plus);
            nodeIdentifier  = query.substring(plus + 1);
        } else {
            nodeIdentifier  = query;
        }
        qp = new QueryParts(sessionName, nodeIdentifier, req, res);
        req.setAttribute("org.mmbase.servlet.BridgeServlet$QueryParts", qp);
        return qp;
    }


    /**
     * Obtains a cloud object, using a QueryParts object.
     * @return A Cloud or <code>null</code> if unsuccessful (this may not be fatal).
     */
    final protected Cloud getCloud(QueryParts qp) throws IOException {
        log.debug("getting a cloud");
        // trying to get a cloud from the session
        Cloud cloud = null;
         HttpSession session = qp.getRequest().getSession(false); // false: do not create a session, only use it
        if (session != null) { // there is a session
            log.debug("from session");
            String sessionName = qp.getSessionName();
            if (sessionName != null) {
                cloud = (Cloud) session.getAttribute(sessionName); 
            } else { // desperately searching for a cloud, perhaps someone forgot to specify 'session_name' to enforce using the session?
                cloud = (Cloud) session.getAttribute("cloud_" + getCloudName()); 
            }
        } 
        return cloud;
    }

    /**
     * Obtains an 'anonymous' cloud.
     */
    final protected Cloud getAnonymousCloud() {
        try {
            return ContextProvider.getDefaultCloudContext().getCloud(getCloudName());
        } catch (org.mmbase.security.SecurityException e) {
            log.debug("could not generate anonymous cloud");
            // give it up
            return null;
        }
    }

    /**
     * Obtains a cloud using 'class' security. If e.g. you authorize org.mmbase.servlet.ImageServlet
     * by class-security for read all rights, it will be used.
     * @since MMBase-1.8
     */
    protected Cloud getClassCloud() {
        try {
            return ContextProvider.getDefaultCloudContext().getCloud(getCloudName(), "class", null); // testing Class Security
        } catch (org.mmbase.security.SecurityException e) {
            log.debug("could not generate class cloud");
            // give it up
            return null;
        }
    }
    


    /**
     * Tries to find a Cloud which can read the given node.
     * @since MMBase-1.8
     */
    protected Cloud findCloud(Cloud c, String nodeNumber, QueryParts query) throws IOException {

        if (c == null || ! (c.mayRead(nodeNumber))) {
            c = getClassCloud();
        }

        if (c == null || ! (c.mayRead(nodeNumber))) {
            c = getCloud(query);
        }
        if (c == null || ! (c.mayRead(nodeNumber)))  { // cannot find any cloud what-so-ever, 
            HttpServletResponse res = query.getResponse();
            if (res != null) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Permission denied to anonymous for node '" + nodeNumber + "'");
            }
            return null; 
        }
        return c;       
    }

    /**
     * Servlets would often need a node. This function provides it.
     * @param query A QueryParts object, which you must have obtained by {@link readQuery}
     */
     
    final protected Node getNode(QueryParts query)  throws IOException {
        try {
            if (log.isDebugEnabled()) { 
                log.debug("query : " + query);
            }

            if (query == null) {
                return null;
            } else {
                Node n = query.getNode();
                if (n != null) {
                    return n;
                }
            }

            
            Cloud c = getAnonymousCloud(); // first try anonymously always, because then session has not to be used

            String nodeNumber = query.getNodeNumber();
            
            if (c != null && ! c.hasNode(nodeNumber)) {
                HttpServletResponse res = query.getResponse();
                if (res != null) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "Node '" + nodeNumber + "' does not exist");
                }
                return null;
            }

            c = findCloud(c, nodeNumber, query);
            if (c == null) { 
                return null;
            }

            Node n = c.getNode(nodeNumber);
            query.setNode(n);
            return n;
        } catch (Exception e) {
            HttpServletResponse res = query.getResponse();
            if (res != null) {
                query.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());           
            }
            return null;
        }
    }

    /**
     * If the node associated with the resonse is another node then the node associated with the request.\
     * (E.g. a icache based on a url with an image node).
     * @param qp A QueryParts object, which you must have obtained by {@link readQuery}
     * @param node The node which is specified on the URL (obtained by {@link getNode}
     * @since MMBase-1.8
     */
    protected Node getServedNode(QueryParts qp, Node node) throws IOException {
        return node;
    }

    /**
     * The idea is that a 'bridge servlet' on default serves 'nodes', and that there could be
     * defined a 'last modified' time for nodes. This can't be determined right now, so 'now' is
     * returned.
     *
     * This function is defined in HttpServlet
     * {@inheritDoc}
     **/
    protected long getLastModified(HttpServletRequest req) {
        if (lastModifiedField == null) return -1;
        try {
            QueryParts query = readQuery(req, null);
            Node node = getServedNode(query, getNode(query));
            if (node != null) { // && node.getNodeManager().hasField(lastModifiedField)) {
                return node.getDateValue(lastModifiedField).getTime();
            } else {
                return -1;
            }
        } catch (IOException ieo) {
            return -1;
        }
    }

    /**
     * Inits lastmodifiedField.
     * {@inheritDoc}
     */

    public void init() throws ServletException {
        super.init();        
        lastModifiedField = getInitParameter("lastmodifiedfield");
        if ("".equals(lastModifiedField)) lastModifiedField = null;
        log = Logging.getLoggerInstance(BridgeServlet.class);
        if (lastModifiedField != null) {
            log.service("Field '" + lastModifiedField + "' will be used to calculate lastModified");
        }
    }

    /**
     * Keeps track of determined information, to avoid redetermining it.
     */
    final class QueryParts {
        private String sessionName;
        private String nodeIdentifier;
        private HttpServletRequest req;
        private HttpServletResponse res;
        private Node node;
        private Node servedNode;
        QueryParts(String sessionName, String nodeIdentifier, HttpServletRequest req, HttpServletResponse res) throws IOException {
            this.req = req;
            this.res = res;
            this.sessionName = sessionName;
            this.nodeIdentifier = nodeIdentifier;
            
        }
        void setNode(Node node) {
            this.node = node;
        }
        Node getNode() {
            return node;
        }
        void setServedNode(Node node) {
            this.servedNode = node;
        }
        Node getServedNode() {
            return servedNode;
        }
        String getSessionName() { return sessionName; }
        String getNodeNumber() { 
            int i = nodeIdentifier.indexOf('+');            
            if (i > 0) {
                return nodeIdentifier.substring(0, i);
            } else {
                return nodeIdentifier;
            }
        }
            
        HttpServletRequest getRequest() {
            return req;
        }
        HttpServletResponse getResponse() {
            return res;
        }
        void setResponse(HttpServletResponse r) {
            res = r;
        }
        /**           
         * @since MMBase-1.8
         */
        String getNodeIdentifier() { return nodeIdentifier; }

        public  String toString() {
            return sessionName == null ? nodeIdentifier : "session=" + sessionName + "+" + nodeIdentifier;
        }
                   
                   
    }

    /**
     * Just to test to damn regexp
     */
    public static void main(String[] argv) {
       
        Matcher m = FILE_PATTERN.matcher(argv[0]);
        if (! m.matches()) {
            System.out.println("Didn't match");
        } else {
            System.out.println("Found node " + m.group(1));
        }
    }

}
