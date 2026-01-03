/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.richtext.processors.xml;
import org.mmbase.bridge.*;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.xml.XMLWriter;
import org.mmbase.storage.search.*;
import org.mmbase.util.*;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.regex.*;
import java.net.URL;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import org.mmbase.util.logging.*;


/**
 * Utility functions, used by various classes in the package.
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public abstract class Util {
    private static final Logger log = Logging.getLoggerInstance(Util.class);

    /**
     * Used for generating unique id's
     */
    static long indexCounter = System.currentTimeMillis() / 1000;

    /**
     * Just parses String to Document
     */
    public static Document parse(Object value)  throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException,  java.io.IOException {
        if (value instanceof Document) return (Document) value;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Parsing " + value);
            }
            return parse(new java.io.ByteArrayInputStream(("" + value).getBytes("UTF-8")));
        } catch (java.io.UnsupportedEncodingException uee) {
            // cannot happen, UTF-8 is supported..
            return null;
        }

    }
    /**
     * Just parses InputStream  to Document (without validation).
     */
    public static  Document parse(java.io.InputStream value)  throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException,  java.io.IOException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setValidating(false);
        dfactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();
        // dont log errors, and try to process as much as possible...
        org.mmbase.util.xml.ErrorHandler errorHandler = new org.mmbase.util.xml.ErrorHandler(false, org.mmbase.util.xml.ErrorHandler.NEVER);
        documentBuilder.setErrorHandler(errorHandler);

        documentBuilder.setEntityResolver(new org.mmbase.util.xml.EntityResolver(false));
        Document doc = documentBuilder.parse(value);
        if (! errorHandler.foundNothing()) {
            throw new IllegalArgumentException("xml invalid:\n" + errorHandler.getMessageBuffer() + "for xml:\n" + value);
        }
        return doc;
    }


    public static void copyAttributes(Element source, Element destination) {
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            org.w3c.dom.Node n = attributes.item(i);
            destination.setAttribute(n.getNodeName(), n.getNodeValue());
        }
    }

    public static void copyChilds(Element source, Element destination) {
        org.w3c.dom.Node child = source.getFirstChild();
        while (child != null) {
            org.w3c.dom.Node copy = destination.getOwnerDocument().importNode(child, true);
            destination.appendChild(copy);
            child = child.getNextSibling();
        }
    }

    public static SortedSet<String> getCssClasses(String cl, Set<String> allowed) {
        SortedSet<String> classes = new TreeSet<String>();
        for (String c : cl.split("\\s+")) {
            if (allowed == null || allowed.contains(c)) {
                classes.add(c);
            }
        }
        return classes;
    }

    public static String getCssClass(Set<String> classes) {
        StringBuilder c = new StringBuilder();
        Iterator<String> i = classes.iterator();
        while (i.hasNext()) {
            c.append(i.next());
            if (i.hasNext()) {
                c.append(" ");
            }
        }
        return c.toString();
    }

    /**
     */
    public static String getCssClass(String cl, Set<String> allowed) {
        return getCssClass(getCssClasses(cl, allowed));
    }
    public static String getCssClass(String cl) {
        return getCssClass(cl, null);
    }

    private final static Pattern WHITESPACE = Pattern.compile("\\s");
    /**
     * @since MMBase-1.9
     */
    public static String normalizeWhiteSpace(String s) {
        return WHITESPACE.matcher(s).replaceAll(" ");
    }


    /**
     * Just searches the nodes in a NodeList for which a certain field has a certain value.
     */
    public static  NodeList get(Cloud cloud, NodeList list, String field, String value) {
        NodeList result = cloud.createNodeList();
        NodeIterator i = list.nodeIterator();
        while(i.hasNext()) {
            Node n = i.nextNode();
            String pref = "" + list.getProperty(NodeList.NODESTEP_PROPERTY);
            String fieldName = field;
            if (fieldName.indexOf(".") == -1 && pref != null) {
                fieldName = pref + "." + field;
            }
            if (n.getStringValue(fieldName).equals(value)) {
                result.add(n);
            }
        }
        return result;
    }



    public static Node getUrlNode(Cloud cloud, String href, Element a) {
        NodeManager urls = cloud.getNodeManager("urls");
        NodeQuery q = urls.createQuery();
        StepField urlStepField = q.getStepField(urls.getField("url"));
        Constraint c = q.createConstraint(urlStepField, href);
        q.setConstraint(c);
        NodeList ul = urls.getList(q);
        Node url;
        if (ul.size() > 0) {
            url = ul.getNode(0);
            log.service("linking to exsting URL from cloud " + url);
        } else {
            // not found, create it!
            url = cloud.getNodeManager("urls").createNode();
            url.setStringValue("url", href);
            if (urls.hasField("title")) {
                url.setStringValue("title", a.getAttribute("alt"));
            } else if (urls.hasField("name")) {
                url.setStringValue("name", a.getAttribute("alt"));
            }
            url.commit();
        }
        return url;
    }



    public static final Pattern ABSOLUTE_URL = Pattern.compile("(http[s]?://[^/]+)(.*)");

    /**
     * Normalizes URL to absolute on server
     */
    public static String normalizeURL(final HttpServletRequest request, final String url) {

        if (url.startsWith("/")) {
            return url;
        }
        String u = url;
        if (url.startsWith(".")) {
            if (request == null) {
                log.warn("Did not receive a request, don't know how to normalize '" + url + "'");
                return url;
            }


            try {
                // based on the request as viewed by the client.
                if (log.isDebugEnabled()) {
                    log.debug("Request of " + request.getAttribute("time") + " " + Collections.list(request.getAttributeNames()));
                }
                String requestURL = (String) request.getAttribute("javax.servlet.include.servlet_path");
                if (request.getScheme() == null) {
                    log.warn("How odd, we got a request with no scheme!!");
                }
                if (requestURL == null) {
                    requestURL = request.getRequestURL().toString();
                }
                u = new URL(new URL(requestURL), url).toString();
            } catch (java.net.MalformedURLException mfe) {
                log.warn("" + mfe, mfe); // should not happen
                return url;
            } catch (NullPointerException npe) {
                log.warn("NPE ", npe);
            }
        } else {
            u = url;
        }
        if (log.isDebugEnabled()) {
            log.debug("url " + url + " ->" + u);
        }
        Matcher matcher = ABSOLUTE_URL.matcher(u);
        if (matcher.matches()) {
            if (request == null) {
                log.warn("Did not receive request, can't check if this URL is local: '" + url + "'");
                return url;
            }
            try {
                URL hostPart = new URL(matcher.group(1));
                String scheme = HttpServletRequestUtils.getScheme(request);
                if (scheme == null) {
                    log.warn("Request " + request + " " + request.getRequestURI() + " gave 'null'  scheme" + request.getServerName() + ":" + request.getServerPort() + " " + request.getContextPath());
                }
                String host   = HttpServletRequestUtils.getServerName(request);
                int port      = HttpServletRequestUtils.getServerPort(request);
                URL foundHost = scheme != null ? new URL(scheme, host, port, "") : null;
                if (scheme != null && hostPart.sameFile(foundHost)) {
                    String result = matcher.group(2);
                    if (log.isDebugEnabled()) {
                        log.trace("Converted " + url + " -> " + result);
                    }
                    return result;
                } else {
                    if (log.isDebugEnabled()) {
                        log.trace("Not converting url, it is on a different server " + hostPart + " != " + foundHost);
                    }
                    return url;
                }
            } catch (java.net.MalformedURLException mfe) {
                log.warn("" + mfe); // client could have typed this.
                return url; // don't know anything better then this.
            }
        } else {
            log.debug("Could not normalize url " + url);
            return url;
        }

    }

    static final Pattern OK_URL = Pattern.compile("[a-z]+:.*");

    /**
     * Adds missing protocol
     */
    public static String normalizeURL(String url) {
        if (OK_URL.matcher(url).matches() || (url.length() > 0 && url.charAt(0) == '/')) {
            return url;
        } else {
            return "http://" + url;
        }
    }


    // list related withouth inheritance
    public static NodeList getRelatedNodes(Node editedNode, NodeManager dest) {
        NodeQuery q = Queries.createRelatedNodesQuery(editedNode, dest, "idrel", "destination");
        StepField stepField = q.createStepField(q.getNodeStep(), "otype");
        Constraint newConstraint = q.createConstraint(stepField, new Integer(dest.getNumber()));
        Queries.addConstraint(q, newConstraint);
        Queries.addRelationFields(q, "idrel", "id", null);
        return q.getCloud().getList(q);
    }


    public static  String toString(Object value) {
        if (value instanceof Document) {
            return XMLWriter.write((Document) value, false, true);
        } else {
            return "" + value;
        }
    }


}
