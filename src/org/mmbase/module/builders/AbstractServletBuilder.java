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
import javax.servlet.http.HttpServletRequest;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.util.functions.*;
import org.mmbase.bridge.Cloud;
import org.mmbase.security.Rank;

/**
 * Some builders are associated with a servlet. Think of images and attachments.
 *
 * There is some common functionality for those kind of builders, which is collected here.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: AbstractServletBuilder.java,v 1.30 2005-10-04 09:16:21 michiel Exp $
 * @since   MMBase-1.6
 */
public abstract class AbstractServletBuilder extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(AbstractServletBuilder.class);

    public static final String FIELD_MIMETYPE   = "mimetype";
    public static final String FIELD_FILENAME   = "filename";
    public static final String FIELD_HANDLE     = "handle";

    /**
     * Can be used to construct a List for executeFunction argument
     * (new ParametersImpl(GUI_ARGUMENTS))
     */
    public final static Parameter[] GUI_PARAMETERS = {
        new Parameter.Wrapper(MMObjectBuilder.GUI_PARAMETERS) // example, does not make too much sense :-)
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


    private static final int FILENAME_ADD = 1;
    private static final int FILENAME_DONTADD = 0;
    private static final int FILENAME_IFSENSIBLE = -1;
    private static final int FILENAME_CHECKSETTING = -2;
    /**
     * -2: check init, based on existance of filename field.
     * -1: based on existance of filename field
     * 0 : no
     * 1 : yes
     * @since MMBase-1.7.4
     */
    protected int addsFileName = FILENAME_CHECKSETTING;


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
        if (ls.size()>0) {
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
            if (log.isServiceEnabled()) {
                log.service(getAssociation() + " are served on: " + servletPath + "  root: " + root);
            }
            servletPathAbsolute = servletPath.startsWith("http:") || servletPath.startsWith("https");                
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
        if (! result.endsWith("/")) result += "?";
        return result;
    }

    protected String getServletPath() {
        return getServletPath(MMBaseContext.getHtmlRootUrlPath());
    }


    /**
     * Returns the Mime-type associated with this node
     */
    protected String getMimeType(MMObjectNode node) {
        String mimeType = node.getStringValue(FIELD_MIMETYPE);
        if (mimeType == null || mimeType.equals("")) {
            log.service("Mimetype of attachment '" + node.getStringValue("title") + "' was not set. Using magic to determine it automaticly.");
            byte[] handle = node.getByteValue(FIELD_HANDLE);

            String extension = null;                
            if (getField(FIELD_FILENAME) != null) {
                String filename = node.getStringValue(FIELD_FILENAME);
                int dotIndex = filename.lastIndexOf("."); 
                if (dotIndex > -1) {
                    extension = filename.substring(dotIndex + 1);
                }
            }
            
            MagicFile magic = MagicFile.getInstance();
            try {
                if (extension == null) {
                    mimeType = magic.getMimeType(handle);
                } else {
                    mimeType = magic.getMimeType(handle, extension);
                }
                log.service("Found mime-type: " + mimeType);
                node.setValue(FIELD_MIMETYPE, mimeType);
            } catch (Throwable e) {
                log.warn("Exception in MagicFile  for " + node);
                mimeType = "application/octet-stream";
                node.setValue(FIELD_MIMETYPE, mimeType);
            }            

        }
        return mimeType;
    }

    /**
     * Tries to fill all fields which are dependend on the 'handle' field.
     * They will be filled automaticly if not still null.
     */
    protected void checkHandle(MMObjectNode node) {
        if (getField(FIELD_MIMETYPE) != null) {
            getMimeType(node);
        }
        
    }

    /** 
     * Returns the fields which tell something about the 'handle' field, and can be calculated from it.
     */

    abstract protected Set getHandleFields();

    public int insert(String owner, MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("Inserting node " + node.getNumber() + " memory: " + SizeOf.getByteSize(node));
        }
        checkHandle(node);
        int result = super.insert(owner, node);
        if (log.isDebugEnabled()) {
            log.debug("After handle unload, memory: " + SizeOf.getByteSize(node));
        }
        return result;
    }
    public boolean commit(MMObjectNode node) {
        Collection changed = node.getChanged();
        if (log.isDebugEnabled()) {
            log.debug("Committing node " + node.getNumber() + " memory: " + SizeOf.getByteSize(node) + " fields " + changed);
        }
        
        Object h;
        if (changed.contains(FIELD_HANDLE)) {
            // set those fields to null, which are not changed too:
            Collection cp = new ArrayList();
            cp.addAll(getHandleFields());
            cp.removeAll(changed);
            Iterator i = cp.iterator();
            while (i.hasNext()) {
                String f = (String) i.next();
                if (node.parent.getField(f) != null) {
                    node.setValue(f, null);
                }
            }
        }
        checkHandle(node);
        boolean result = super.commit(node);
        if (log.isDebugEnabled()) {
            log.debug("After commit node " + node.getNumber() + " memory: " + SizeOf.getByteSize(node));
        }
        return result;
    }



    protected String getFileSizeGUI(int size) {
        if (size < 1000) {
            return "" + size + " byte";
        } else if (size < 1000000) {
            return "" + size / 1000 + " kbyte";
        } else if (size < 10000000) {
            return "" + size / 1000000 + " Mbyte";
        } else {
            return "" + size / 1000000 + " Mbyte";
        }
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
        return getSGUIIndicator(node, new ParametersImpl(GUI_PARAMETERS));
    }
    /**
     * This is final, because getSGUIIndicator has to be overridden in stead
     */

    final public String getGUIIndicator(String field, MMObjectNode node) { // final, override getSGUIIndicator
        return getSGUIIndicator(node, new ParametersImpl(GUI_PARAMETERS).set("field", field));
    }

    {
        // you can of course even implement it anonymously.
        addFunction(new NodeFunction("servletpath", 
                                         new Parameter[] {
                                             new Parameter("session",  String.class), // For read-protection
                                             new Parameter("field",    String.class), // The field to use as argument, defaults to number unless 'argument' is specified.
                                             new Parameter("context",  String.class), // Path to the context root, defaults to "/" (but can specify something relative).
                                             new Parameter("argument", String.class), // Parameter to use for the argument, overrides 'field'
                                             Parameter.REQUEST,
                                             Parameter.CLOUD
                                         },
                                         ReturnType.STRING) {
                {
                    setDescription("Returns the path associated with this builder or node.");
                }

                protected StringBuffer getServletPath(Parameters a) {
                    StringBuffer servlet = new StringBuffer();
                    // third argument, the servlet context, can use a relative path here, as an argument
                    String context             = (String) a.get("context");

                    if (context == null) {
                        // no path to context-root specified explitiely, try to determin:
                        HttpServletRequest request = (HttpServletRequest) a.get(Parameter.REQUEST);
                        if (request == null) {
                            // no request object given as well, hopefully it worked on servlet's initalizations (it would, in most servlet containers, like tomcat)
                            servlet.append(AbstractServletBuilder.this.getServletPath()); // use 'absolute' path (starting with /)
                        } else {
                            servlet.append(AbstractServletBuilder.this.getServletPath(request.getContextPath()));
                        }
                    } else {
                        // explicitely specified the path!
                        servlet.append(AbstractServletBuilder.this.getServletPath(context));
                    }
                    return servlet;
                }

                public Object getFunctionValue(MMObjectNode node, Parameters a) {
                    StringBuffer servlet = getServletPath(a);

                    String session = (String) a.get("session");
                    if (session == null) {
                        Cloud cloud = (Cloud) a.get(Parameter.CLOUD);
                        
                        if(cloud != null && ! cloud.getUser().getRank().equals(Rank.ANONYMOUS)) {
                            // the user is not anonymous!
                            // Need to check if node is readable by anonymous.
                            // in that case URLs can be simpler
                            // two situations are anticipated:
                            // - node not readable by anonymous
                            // - no anonymous user defined
                            try{
                                Cloud anonymousCloud = cloud.getCloudContext().getCloud(cloud.getName());
                                if (! anonymousCloud.mayRead(node.getNumber())) {
                                    session = (String) cloud.getProperty(Cloud.PROP_SESSIONNAME);
                                }
                            } catch (org.mmbase.security.SecurityException se) {
                                log.debug(se.getMessage());
                                session = (String) cloud.getProperty(Cloud.PROP_SESSIONNAME);
                            }
                        }
                        if (session == null) session = "";
                    }
                    
                    String argument = (String) a.get("argument");
                    // argument representint the node-number
                    
                    if (argument == null) {
                        String fieldName   = (String) a.get("field");
                        if (fieldName == null) {
                            argument = node.getStringValue("number");
                        } else {
                            if (log.isDebugEnabled()) log.debug("Getting 'field' '" + fieldName + "'");
                            argument = node.getStringValue(fieldName);
                        }
                    }
                    
                    String fileName = getField(FIELD_FILENAME) != null ? node.getStringValue("filename") : "";
                    if (addsFileName == FILENAME_CHECKSETTING) {
                        javax.servlet.ServletContext sx = MMBaseContext.getServletContext();
                        if (sx != null) {                            
                            String res = sx.getInitParameter("mmbase.servlet." + getAssociation() + ".addfilename");
                            if (res == null) res = "";
                            res = res.toLowerCase();
                            log.trace("res " + res);
                            if ("no".equals(res) || "false".equals(res)) {
                                addsFileName = FILENAME_DONTADD;
                            } else if ("yes".equals(res) || "true".equals(res)) {
                                addsFileName = FILENAME_ADD;
                            } else {
                                log.debug("Found " + res + " for mmbase.servlet." + getAssociation() + ".addfilename");
                                addsFileName = FILENAME_IFSENSIBLE;
                            }
                        }
                    }
                    log.debug("addsFileName " + addsFileName);

                    boolean addFileName =  addsFileName == FILENAME_ADD ||  
                        ( addsFileName == FILENAME_IFSENSIBLE && (!servlet.toString().endsWith("?")) &&  (! "".equals(fileName)));
                    
                    if (usesBridgeServlet && ! session.equals("")) {
                        servlet.append("session=" + session + "+");
                    }
                    
                    if (! addFileName) {
                        return servlet.append(argument).toString();
                    } else {
                        if (fileName.equals("")) {
                            String fileTitle = node.getStringValue("title");
                            if (fileTitle.equals("")) fileTitle = getSingularName("en");
                            fileName = fileTitle  + "." + node.getFunctionValue("format", null);
                        }
                        servlet.append(argument).append('/').append(fileName.replace(' ', '_'));
                        return servlet.toString();
                    }
                }

                public Object getFunctionValue(Parameters a) {
                    return getServletPath(a).toString();
                }
            });    

    }



    {
        /**
         * @since MMBase-1.8
         */
        addFunction(new NodeFunction("iconurl",
                                     new Parameter[] {
                                         Parameter.REQUEST,
                                         new Parameter("iconroot", String.class, "/mmbase/style/icons/")
                                     },
                                     ReturnType.STRING) {
                {
                    setDescription("Returns an URL for an icon for this blob");
                }
                public Object getFunctionValue(MMObjectNode node, Parameters parameters) {
                    String mimeType = AbstractServletBuilder.this.getMimeType(node);
                    ResourceLoader webRoot = ResourceLoader.getWebRoot();
                    HttpServletRequest request = (HttpServletRequest) parameters.get(Parameter.REQUEST);
                    String root;
                    if (request != null) {
                        root = request.getContextPath();
                    } else {
                        root = MMBaseContext.getHtmlRootUrlPath();
                    }

                    String iconRoot = (String) parameters.get("iconroot");
                    if (root.endsWith("/") && iconRoot.startsWith("/")) iconRoot = iconRoot.substring(1);

                    if (! iconRoot.endsWith("/")) iconRoot = iconRoot + '/';
                    
                    String resource = iconRoot + mimeType + ".gif";
                    try {
                        if (! webRoot.getResource(resource).openConnection().getDoInput()) {
                            resource = iconRoot + "application/octet-stream.gif";
                        }
                    } catch (java.io.IOException ioe) {
                        log.warn(ioe.getMessage(), ioe);
                        resource = iconRoot + "application/octet-stream.gif";
                    }
                    return root + resource;
                }

            });
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
                    a = new ParametersImpl(GUI_PARAMETERS, args);
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
