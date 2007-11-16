/*

  This software is OSI Certified Open Source Software.
  OSI Certified is a certification mark of the Open Source Initiative.

  The license (Mozilla version 1.0) can be read at the MMBase site.
  See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;

import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.*;
import java.io.*;
import org.mmbase.bridge.NotFoundException;
import org.mmbase.util.functions.*;
import org.mmbase.util.transformers.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * If rendering of a block fails, for some reason, that this renderer can be used in stead, to
 * present the error.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ErrorRenderer.java,v 1.6 2007-11-16 10:11:20 michiel Exp $
 * @since MMBase-1.9
 */

public class ErrorRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(ErrorRenderer.class);

    protected final Error error;
    protected final String url;

    public ErrorRenderer(Type t, Block parent, String u, int status, String m) {
        super(t, parent);
        error = new Error(status, new Exception(m));
        url = u;
    }

    public ErrorRenderer(Type t, Block parent, String u, int status, Throwable e) {
        super(t, parent);
        error = new Error(status, e);
        url = u;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST, Parameter.LOCALE};
    }

    public void render(Parameters blockParameters, Parameters frameworkParameters, Writer w, Renderer.WindowState state) throws FrameworkException {
        switch(getType()) {
        case BODY:
            try {
                HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
                HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);
                Locale  locale = blockParameters.get(Parameter.LOCALE);
                w.write("<div id=\"" + request.getAttribute(Framework.COMPONENT_ID_KEY) + "\"");
                w.write(" class=\"error mm_c_");
                w.write(getBlock().getComponent().getName());
                w.write(" mm_c_b_");
                w.write(getBlock().getName());
                w.write(" " + request.getAttribute(Framework.COMPONENT_CLASS_KEY));
                w.write("\">");
                w.write("<h1>" + error.status );
                w.write(": ");
                w.write(error.exception.getMessage());
                w.write(url);
                w.write("</h1>");
                w.write("<pre>");
                Writer t = new TransformingWriter(w, new Xml());
                error.getErrorReport(t, request);
                t.flush();
                w.write("</pre>");
                w.write("</div>");

            } catch (IOException eio) {
                throw new FrameworkException(eio.getMessage(), eio);
            }
            break;
        default:
        }
    }
    public String toString() {
        return "ERROR " + error;
    }

    public java.net.URI getUri() {
        try {
            return new java.net.URL(url).toURI();
        } catch (Exception e) {
            return null;
        }
    }
    public static class Error {
        public int status;
        public final Throwable exception;
        public Error(int s, Throwable e) {
            status = s; exception = e;
        }
        public Writer getErrorReport(Writer msg, final HttpServletRequest request) throws IOException {
            String ticket = new Date().toString();
            Throwable e = exception;
            Stack stack = new Stack();
            while (e != null) {
                stack.push(e);
                if (e instanceof NotFoundException) {
                    status = HttpServletResponse.SC_NOT_FOUND;
                }
                if (e instanceof ServletException) {
                    Throwable t = ((ServletException) e).getRootCause();
                    if (t == null) t = e.getCause();
                    e = t;
                } else if (e instanceof javax.servlet.jsp.JspException) {
                    Throwable t = ((JspException) e).getRootCause();
                    if (t == null) t = e.getCause();
                    e = t;
                } else {
                    e = e.getCause();
                }
            }

            msg.append("Headers\n----------\n");
            // request properties
            Enumeration en = request.getHeaderNames();
            while (en.hasMoreElements()) {
                String name = (String) en.nextElement();
                msg.append(name+": "+request.getHeader(name)+"\n");
            }

            msg.append("\nAttributes\n----------\n");
            Enumeration en2 = request.getAttributeNames();
            while (en2.hasMoreElements()) {
                String name = (String) en2.nextElement();
                msg.append(name+": "+request.getAttribute(name)+"\n");
            }
            msg.append("\n");
            msg.append("Misc. properties\n----------\n");

            msg.append("method: ").append(request.getMethod()).append("\n");
            msg.append("querystring: ").append(request.getQueryString()).append("\n");
            msg.append("requesturl: ").append(request.getRequestURL()).append("\n");
            msg.append("mmbase version: ").append(org.mmbase.Version.get()).append("\n");
            msg.append("status: ").append("" + status).append("\n\n");


            msg.append("Parameters\n----------\n");
            // request parameters
            en = request.getParameterNames();
            while (en.hasMoreElements()) {
                String name = (String) en.nextElement();
                msg.append(name).append(": ").append(request.getParameter(name)).append("\n");
            }
            msg.append("\nException\n----------\n\n" + (exception != null ? (exception.getClass().getName()) : "NO EXCEPTION") + ": ");


            while (! stack.isEmpty()) {

                Throwable t = (Throwable) stack.pop();
                // add stack stacktraces
                if (t != null) {
                    String message = t.getMessage();
                    String title = message;
                    if (title == null) {
                        StackTraceElement el = t.getStackTrace()[0];
                        title = t.getClass().getName().substring(t.getClass().getPackage().getName().length() + 1) + " " + el.getFileName() + ":" + el.getLineNumber();
                    }
                    msg.append(message).append("\n");
                    msg.append(org.mmbase.util.logging.Logging.stackTrace(t));
                    if (! stack.isEmpty()) {
                        msg.append("\n-------caused:\n");
                    }
                }
            }
            // write errors to mmbase log
            if (status == 500) {
                log.error(ticket + ":\n" + msg);
            }
            return msg;
        }
    }

}
