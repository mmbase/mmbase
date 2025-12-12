/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.applications.dove.servlet;

import java.io.*;

import javax.servlet.http.*;
import javax.servlet.*;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.*;
import org.w3c.dom.*;

import org.mmbase.util.logging.*;
import org.mmbase.util.xml.*;
import org.mmbase.applications.dove.*;
import org.mmbase.servlet.MMBaseServlet;

/**
 * This servlet routes RPC requests (represented in xml) to the intended method of
 * the 'Dove' object.
 *
 * @author Pierre van Rooden
 * @since MMBase-1.5
 * @version $Id$
 */
public class DoveServlet extends MMBaseServlet { // MMBase, only to be able to use its logging

    private static final Logger log =  Logging.getLoggerInstance(DoveServlet.class);

    /**
     * Handles a request using the GET method.
     * No communication is handled through GET - this method is for testing whether the servlet is online.
     * @param req the HTTP Request object
     * @param res the HTTP Response object
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        PrintWriter out = res.getWriter();

        res.setContentType("text/html");
        out.println("<html><head><title>Dove</title></head>");
        out.println("<body><h1>Dove RPC Router</h1>");
        out.println("<p>The Dove servlet is active. use HTTP Post to send RPC commands.</p></body></html>");
    }

    /**
     * Handles a request using the POST method.
     * Retrieves the value of the 'xml' parameter, and parses the body of that
     * parameter as an xml text. The resulting DOM tree is then passed to the Dove
     * class, which runs the RPCs described in that tree.
     * The result of Dove (also a DOM tree) is returned as xml to the client.
     * The mime type of the result is 'text/xml', unless the 'plain' parameter
     * is set to 'yes', in which case the mime type is 'text/plain'.
     * Specifying a 'pretty' parameter with value 'yes' results in pretty printed xml.
     * Both these parameters are ment for debugging.
     * <br />
     * XXX: Possibly we want to use xml directly in the body, instead of parameters.
     * <br />
     * XXX: Daniel suggested using CRC to validate calls. This is not implemented yet.
     * <br />
     * XXX: We have not yet established how we will use session-info and usercontext.
     *
     * @param req the HTTP Request object
     * @param res the HTTP Response object
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String error="unknown error";
        String errortype="unknown";
        boolean pretty="yes".equals(req.getParameter("pretty"));
        boolean plain="yes".equals(req.getParameter("plain"));
        try {
            DoveErrorHandler errorhandler = new DoveErrorHandler();
            DocumentBuilder db = DocumentReader.getDocumentBuilder(false,errorhandler, null);

            // Right now we read content from parameters
            // Maybe we want the xml to be directly in the body?
            // Depends on how the editors will post.
            String s = req.getParameter("xml");
            if (log.isDebugEnabled()){ log.debug("received : "+s);};
            Document document = db.parse(new InputSource(new StringReader(s)));
            if (errorhandler.erroroccurred) {
                error=errorhandler.parsingerrors;
                errortype="parser";
            } else {
                Element rootin=document.getDocumentElement();
                if (rootin.getTagName().equals(AbstractDove.REQUEST)) {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = docBuilder.newDocument();
                    Element rootout =doc.createElement(AbstractDove.RESPONSE);
                    doc.appendChild(rootout);
                    Dove birdy = new Dove(doc);
                    birdy.doRequest(rootin, rootout);
                    res.setStatus(HttpServletResponse.SC_OK);
                    if (plain) {
                        res.setContentType("text/plain");
                    } else {
                        res.setContentType("text/xml");
                    }

                    BufferedWriter out=new BufferedWriter( new OutputStreamWriter(res.getOutputStream()));
                    String content = XMLWriter.write(doc,pretty);
                    if (log.isDebugEnabled()){log.debug("sending : " + content);}
                    out.write(content);
                    out.flush();
                    return;
                } else {
                    error="No request found.";
                    errortype="parser";
                }
            }
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
            error="System error: "+e;
            errortype="server";
        }
        ServletOutputStream out = res.getOutputStream();
        res.setStatus(HttpServletResponse.SC_OK);
        if (plain) {
            res.setContentType("text/plain");
        } else {
            res.setContentType("text/xml");
        }
        out.println("<response><error type=\"" + errortype + "\">" + error + "</error></response>");
        out.flush();
    }

    /**
     * Dove Error handler for catching and storing parsing exceptions.
     * The Error handler catches all exceptions and stores their descriptions.
     * These can then be retrieved by the Dove servlet, so it can generate an
     * error response.
     */
    public static class DoveErrorHandler implements org.xml.sax.ErrorHandler {

        /**
         * The errors that occurred during the parse.
         */
        public String parsingerrors="";

        /**
         * Indicates whether any errors occurred.
         */
        public boolean erroroccurred=false;

        /**
         * Logs an error and adds it to the list of parsing errors.
         * @param ex the parsing exception describing the error
         */
        public void error(SAXParseException ex) {
            erroroccurred=true;
            String s = getErrorString(ex);
            log.error(s);
            parsingerrors=parsingerrors+s+"\n";
        }

        /**
         * Logs a warning.
         * Warnings are not added to the list of parsing errors.
         * @param ex the parsing exception describig the error
         */
        public void warning(SAXParseException ex) {
            log.warn(getErrorString(ex));
        }

        /**
         * Logs a fatal error.
         * Fatal errors are not added to the list of parsing errors, they
         * throw an exception and abort parsing.
         * @param ex the parsing exception describing the error
         */
        public void fatalError(SAXParseException ex) throws SAXException {
            log.error("[Fatal Error] "+ getErrorString(ex));
            throw ex;
        }

        /**
         * Returns a string describing the error.
         * @param ex the parsing exception describing the error
         * @return the error string
         */
        private String getErrorString(SAXParseException ex) {
            String msg = "";
            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                msg=systemId.substring(index + 1)+":";
            }
            return msg +ex.getLineNumber()+":"+
            ex.getColumnNumber()+":"+
            ex.getMessage();
        }
    }

}
