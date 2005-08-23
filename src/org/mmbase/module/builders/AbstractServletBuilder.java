/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.servlet.MMBaseServlet;
import org.mmbase.servlet.BridgeServlet;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.functions.Parameter;


/**
 * Some builders are associated with a servlet. Think of images and attachments.
 *
 * There is some common functionality for those kind of builders, which is collected here.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: AbstractServletBuilder.java,v 1.20.2.3 2005-08-23 11:52:18 michiel Exp $
 * @since   MMBase-1.6
 */
public abstract class AbstractServletBuilder extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(AbstractServletBuilder.class);

    /**
     * Can be used to construct a List for executeFunction argument
     * (new Parameters(GUI_ARGUMENTS))
     */
    public final static Parameter[] GUI_PARAMETERS = {
        new Parameter.Wrapper(MMObjectBuilder.GUI_PARAMETERS) // example, does not make too much sense :-)
    };


    public final static Parameter[] SERVLETPATH_PARAMETERS = {
        new Parameter("session",  String.class), // For read-protection
        new Parameter("field",    String.class), // The field to use as argument, defaults to number unless 'argument' is specified.
        new Parameter("context",  String.class), // Path to the context root, defaults to "/" (but can specify something relative).
        new Parameter("argument", String.class) // Parameter to use for the argument, overrides 'field'
    };
    public final static Parameter[] FORMAT_PARAMETERS   = {};
    public final static Parameter[] MIMETYPE_PARAMETERS = {};




    /**
     * In this string the path to the servlet is stored.
     */
    private String servletPath = null;
    /**
     * Whether {@link #servletPath} represents an absolute URL (starting with http:)
     * @since MMBase-1.7.4
     */
    private boolean servletPathAbsolute;

    /**
     * If this builder is association with a bridge servlet. If not, it should not put the
     * 'session=' in the url to the servlet (because the serlvet probably is servdb, which does not
     * understand that).
     */
    protected boolean usesBridgeServlet = false;

    /**
     * -2: check init, base on existance of filename field.
     * -1: based on existance of filename field
     * 0 : no
     * 1 : yes
     * @since MMBase-1.7.4
     */
    protected int addsFileName = -2;
    

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
        if (MMBaseContext.isInitialized()) {
            javax.servlet.ServletContext sx = MMBaseContext.getServletContext();
            if (sx != null) {
                String res = sx.getInitParameter("mmbase.servlet." + association + ".url");
                if (res != null && ! res.equals("")) {
                    return res;
                }
            }
        }
        String result;
        List ls = MMBaseServlet.getServletMappingsByAssociation(association);
        log.info("Found " + ls);
        if (ls.size() > 0) {
            result = (String) ls.get(0);
            usesBridgeServlet = MMBaseServlet.getServletByMapping(result) instanceof BridgeServlet;
            // remove mask
            int pos = result.lastIndexOf("*");
            if (pos > 0) {
                result = result.substring(0, pos);
            }
            pos = result.indexOf("*");
            if (pos == 0) {
                result = result.substring(pos+1);
            }
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
     */

    protected String getServletPath(String root) {
        if (servletPath == null) {
            servletPath = getServletPathWithAssociation(getAssociation(), "");
            servletPathAbsolute = servletPath.startsWith("http:") || servletPath.startsWith("https");
            if (log.isServiceEnabled()) {
                log.service(getAssociation() + " are served on: " + servletPath + "  root: " + root);
            }
        }
        String result;
        if (servletPathAbsolute) {
            result = servletPath;
        } else if (root.endsWith("/") && servletPath.startsWith("/")) {
            result = root + servletPath.substring(1);
        } else {
            result = root + servletPath;
        }

        if (! MMBaseContext.isInitialized()) { servletPath = null; }
        // add '?' if it wasn't already there (only needed if not terminated with /)
        if (! result.endsWith("/")) result = result + "?";
        return result;
    }

    protected String getServletPath() {
        return getServletPath(MMBaseContext.getHtmlRootUrlPath());
    }

    /**
     * 'Servlet' builders need a way to transform security to the servlet, in the gui functions, so
     * they have to implement the 'SGUIIndicators'
     */

    abstract protected String getSGUIIndicator(MMObjectNode node,  Parameters a);


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
        return getSGUIIndicator(node, new Parameters(GUI_PARAMETERS));
    }
    /**
     * This is final, because getSGUIIndicator has to be overridden in stead
     */

    final public String getGUIIndicator(String field, MMObjectNode node) { // final, override getSGUIIndicator
        return getSGUIIndicator(node, new Parameters(GUI_PARAMETERS).set("field", field));
    }


    /**
     * {@inheritDoc}
     */
    public Parameter[] getParameterDefinition(String function) {
        return org.mmbase.util.functions.NodeFunction.getParametersByReflection(Attachments.class, function);
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
            List empty = new ArrayList();
            Map info = (Map) super.executeFunction(node, function, empty);
            info.put("servletpath", "" + SERVLETPATH_PARAMETERS + " Returns the path to a the servlet presenting this node. All arguments are optional");
            info.put("servletpathof", "(function) Returns the servletpath associated with a certain function");
            info.put("format", "bla bla");
            info.put("mimetype", "Returns the mimetype associated with this object");
            info.put("gui", "" + GUI_PARAMETERS + "Gui representation of this object.");

            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        } else if (function.equals("servletpath")) {
            if (log.isDebugEnabled()) {
                log.debug("getting servletpath with args " + args);
            }
            // wrap the argument List into an 'Parameters', which makes
            // it easier to deal with.
            Parameters a;
            if (args instanceof Parameters) {
                a = (Parameters) args;
            } else {
                a = new Parameters(SERVLETPATH_PARAMETERS, args);
            }
            // first argument, in which session variable the cloud is (optional, but needed for read-protected nodes)
            String session = a.getString("session");

            String argument = (String) a.get("argument");
            // argument represents the node-number

            if (argument == null) {
                // second argument, which field to use, can for example be 'number' (the default)
                String fieldName   = (String) a.get("field");
                if (fieldName == null) {
                    argument = node.getStringValue("number");
                } else {
                    if (log.isDebugEnabled()) log.debug("Getting 'field' '" + fieldName + "'");
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
                servlet.append(getServletPath(context));
            }
            String fileName = node.getStringValue("filename");
            if (addsFileName == -2) {
                javax.servlet.ServletContext sx = MMBaseContext.getServletContext();
                if (sx != null) {
                    String res = sx.getInitParameter("mmbase.servlet." + getAssociation() + ".addfilename").toLowerCase();
                    if ("no".equals(res)) {
                        addsFileName = 0;
                    } else if ("yes".equals(res)) {
                        addsFileName = 1;
                    } else {
                        log.debug("Found " + res + " for mmbase.servlet." + getAssociation() + ".addfilename");
                        addsFileName = -1;
                    }
                }
            }
            log.info("addsFileName " + addsFileName);

            boolean addFileName =  addsFileName > 0 ||  ( addsFileName < 0 && !servlet.toString().endsWith("?")) &&  (! "".equals(fileName));

            if (usesBridgeServlet && ! session.equals("")) {
                servlet.append("session=" + session + "+");
            }

            if (! addFileName) {
                log.debug("Not adding file-name");
                return servlet.append(argument).toString();
            } else {

                if (fileName.equals("")) {
                    fileName = "image." + node.getFunctionValue("format", null);
                }
                log.debug("Adding filename ");
                StringObject fn = new StringObject(fileName);
                fn.replace(" ", "_");
                servlet.append(argument).append('/').append(fn.toString());
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
                Parameters a;
                if (args instanceof Parameters) {
                    a = (Parameters) args;
                } else {
                    a = new Parameters(GUI_PARAMETERS, args);
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
