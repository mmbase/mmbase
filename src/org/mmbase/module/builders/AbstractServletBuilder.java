/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.servlet.MMBaseServlet;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.Arguments;
import org.mmbase.util.Argument;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Some builders are associated with a servlet. Think of images and attachments.
 *
 * There is some common functionality for those kind of builders, which is collected here.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: AbstractServletBuilder.java,v 1.15 2003-05-19 09:59:12 michiel Exp $
 * @since   MMBase-1.6
 */
public abstract class AbstractServletBuilder extends MMObjectBuilder {

    private static Logger log = Logging.getLoggerInstance(AbstractServletBuilder.class);

    /**
     * Can be used to construct a List for executeFunction argument
     * (new Arguments(GUI_ARGUMENTS))
     */
    public final static Argument[] GUI_ARGUMENTS = { 
        new Argument.Wrapper(MMObjectBuilder.GUI_ARGUMENTS) // example, does not make too much sense :-)
    };


    public final static Argument[] SERVLETPATH_ARGUMENTS = { 
        new Argument("session", String.class), // For read-protection
        new Argument("field",   String.class), // The field to use as argument, defaults to number unless 'argument' is specified.
        new Argument("context", String.class), // Path to the context root, defaults to "/" (but can specify something relative).
        new Argument("argument", String.class) // Argument to use for the argument, overrides 'field'
    };

    /**
     * In this string the path to the servlet is stored.
     */
    private String servletPath = null;

    /**
     * If this builder is association with a bridge servlet. If not, it should not put the
     * 'session=' in the url to the servlet (because the serlvet probably is servdb, which does not
     * understand that).
     */
    protected boolean usesBridgeServlet = false;

    /**
     * This functions should return a string identifying where it is
     * for. This is used when communicating with MMBaseServlet, to
     * find the right servlet.
     *
     * For example 'images' or 'attachments'.
     *
     */
    abstract protected String getAssociation();

    /**
     * If no servlet path can be found via the association (if the
     * servlet did not 'associate' itself with something, like
     * servdb), then the getServletPath function will fall back to
     * this.
     *
     * For example 'img.db' or 'attachment.db'.
     *
     */
    abstract protected String getDefaultPath();

    /**
     * @param association e.g. 'images' or 'attachments'
     * @param root        Path to root of appliciation (perhaps relative).
     */

    private String getServletPathWithAssociation(String association, String root) {
        String result;
        List ls = MMBaseServlet.getServletMappingsByAssociation(association);
        if (ls.size()>0) {
            result = (String) ls.get(0);
            // remove mask
            int pos = result.lastIndexOf("*");
            if (pos > 0) {
                result = result.substring(0, pos);
            }
            pos = result.indexOf("*");
            if (pos == 0) {
                result = result.substring(pos+1);
            }
            usesBridgeServlet = true;
        } else {
            result = getDefaultPath();
        }

        if (result.startsWith("/")) {
            // if it not starts with / then no use adding context.
            if (root != null) {
                if (root.endsWith("/")) {
                    result = root + result.substring(1);
                } else {
                    result = root + result;
                }
            }
        }
        return result;
    }

   /**
     * Get a servlet path. Takes away the ? and the * which possibly
     * are present in the servlet-mappings. You can put the argument(s)
     * directly after this string.
     *
     * @param root The path to the application's root.
     * @param fileName Optional fileName. Will be added to the url, but it will not influence the servlet.
     */

    protected String getServletPath(String root, String fileName) {
        if (servletPath == null) {
            servletPath = getServletPathWithAssociation(getAssociation(), "");
            if (log.isServiceEnabled()) {
                log.service(getAssociation() + " are served on: " + servletPath + "  root: " + root);
            }
        }
        String result;
        if (fileName == null) {
            result = root + servletPath;
        } else {
            if (servletPath.endsWith("/")) {
                result =  root + servletPath + fileName;
            } else {
                result =  root + servletPath;
            }
        }

        // add '?' if it wasn't already there (only needed if not terminated with /)
        if (! result.endsWith("/")) result = result + "?";
        return result;
    }


    /**
     * Returns the path to the  servlet.
     * @see #getServletPath(String, String)
     */
    protected String getServletPath(String fileName) {
        return getServletPath(MMBaseContext.getHtmlRootUrlPath(), fileName);
    }
    protected String getServletPath() {
        return getServletPath(MMBaseContext.getHtmlRootUrlPath(), null);
    }

    /**
     * 'Servlet' builders need a way to transform security to the servlet, in the gui functions, so
     * they have to implement the 'SGUIIndicators'
     */

    abstract protected String getSGUIIndicator(MMObjectNode node,  Arguments a);


    /**
     * Gets the GUI indicator of the super class of this class, to avoid circular references in
     * descendants, which will occur if they want to call super.getGUIIndicator().
     */

    final protected String getSuperGUIIndicator(String field, MMObjectNode node) {
        return super.getGUIIndicator(field, node);
    }

    /**
     * This is final, because getSGUIIndicator has to be overridden in stead
     */
    final public String getGUIIndicator(MMObjectNode node) {
        return getSGUIIndicator(node, new Arguments(GUI_ARGUMENTS));
    }
    /**
     * This is final, because getSGUIIndicator has to be overridden in stead
     */

    final public String getGUIIndicator(String field, MMObjectNode node) { // final, override getSGUIIndicator
        return getSGUIIndicator(node, new Arguments(GUI_ARGUMENTS).set("field", field));
    }


    /**
     * Overrides the executeFunction of MMObjectBuilder with a function to get the servletpath
     * associated with this builder. The field can optionally be the number field to obtain a full
     * path to the served object.
     *
     *
     */

    protected Object executeFunction(MMObjectNode node, String function, List args) {
        log.debug("executefunction of abstractservletbuilder");
        if (function.equals("info")) {
            List empty = new Vector();
            java.util.Map info = (java.util.Map) super.executeFunction(node, function, empty);
            info.put("servletpath", "" + SERVLETPATH_ARGUMENTS + " Returns the path to a the servlet presenting this node. All arguments are optional");
            info.put("servletpathof", "(function) Returns the servletpath associated with a certain function");
            info.put("format", "bla bla");
            info.put("mimetype", "Returns the mimetype associated with this object");
            info.put("gui", "" + GUI_ARGUMENTS + "Gui representation of this object.");

            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        } else if (function.equals("servletpath")) {
            if (log.isDebugEnabled()) {
                log.debug("getting servletpath with args " + args);
            }
            // wrap the argument List into an 'Arguments', which makes
            // it easier to deal with.
            Arguments a;
            if (args instanceof Arguments) {
                a = (Arguments) args;
            } else {
                a = new Arguments(SERVLETPATH_ARGUMENTS, args);
            }
            // first argument, in which session variable the cloud is (optional, but needed for read-protected nodes)
            String session = a.getString("session");


            String argument = a.getString("argument");
           
            if (argument == null) {                                
                // second argument, which field to use, can for example be 'number' (the default)
                String fieldName   = (String) a.get("field");
                if (fieldName == null) {
                    argument = node.getStringValue("number");
                } else {
                    argument = node.getStringValue(fieldName);
                }
            }
            // third argument, the servlet context, can use a relative path here, as an argument
            String context = (String) a.get("context");

            // ok, make the path.
            StringBuffer servlet = new StringBuffer();
            if (context == null) { 
                servlet.append(getServletPath()); // use 'absolute' path (starting with /)
            } else {
                servlet.append(getServletPath(context, null));
            }
            if (usesBridgeServlet && ! session.equals("")) {
                servlet.append("session=" + session + "+");
            }

            String fileName = node.getStringValue("filename");            
            if (servlet.toString().endsWith("?") ||  "".equals(fileName)) {
                return servlet.append(argument).toString();
            } else {
                servlet.append(fileName).append('?').append(argument);
                return servlet.toString();
            }
        } else if (function.equals("servletpathof")) {
            // you should not need this very often, only when you want to serve a node with the 'wrong' servlet this can come in handy.
            return getServletPathWithAssociation((String) args.get(0), MMBaseContext.getHtmlRootUrlPath());
        } else if (function.equals("format")) { // don't issue a warning, builders can override this.
            // images e.g. return jpg or gif
        } else if (function.equals("mimetype")) { // don't issue a warning, builders can override this.
            // images, attachments and so on
        } else if (function.equals("gui")) {
            if (log.isDebugEnabled()) {
                log.debug("GUI of servlet builder with " + args);
            }
            if (args == null || args.size() == 0) {
                return getGUIIndicator(node);
            } else {
                Arguments a;
                if (args instanceof Arguments) {
                    a = (Arguments) args;
                } else {
                    a = new Arguments(GUI_ARGUMENTS, args);
                }
                
                String  rtn = getSGUIIndicator(node, a);
                if (rtn == null) return super.executeFunction(node, function, args);
                return rtn;                
            }            
        } else {
            return super.executeFunction(node, function, args);
        }
        return null;
    }

}
