/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import java.util.regex.*;

import org.mmbase.servlet.MMBaseServlet;
import org.mmbase.servlet.BridgeServlet;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.magicfile.MagicFile;
import org.mmbase.util.functions.*;
import org.mmbase.bridge.*;
import org.mmbase.security.Rank;

/**
 * Some builders are associated with a servlet. Think of images and attachments.
 *
 * There is some common functionality for those kind of builders, which is collected here.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since   MMBase-1.6
 */
public abstract class AbstractServletBuilder extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(AbstractServletBuilder.class);

    public static final String PROPERTY_EXTERNAL_URL_FIELD = "externalUrlField";

    private static final String TMP_FIELD_CHECKHANDLE = "_checkHandle";

    public static final String FIELD_MIMETYPE   = "mimetype";
    public static final String FIELD_FILENAME   = "filename";
    public static final String FIELD_HANDLE     = "handle";

    /**
     * Can be used to construct a List for executeFunction argument
     * (new Parameters(GUI_ARGUMENTS))
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


    private enum FileName {
        ADD,
        DONTADD,
        IFSENSIBLE,
        CHECKSETTING;
    }
    /**


     * @since MMBase-1.7.4
     */
    private FileName addsFileName = FileName.CHECKSETTING;


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
     * If set, this points out a field in the builder that optionally contains
     * a url to an external (binary) source, which is then used as the stored
     * attachment (instead of accessing the database).
     */
    protected String externalUrlField = null;

    /**
     * Read 'externalUrlField' property
     */
    @Override
    public boolean init() {
        String property = getInitParameter(PROPERTY_EXTERNAL_URL_FIELD);
        if (property != null) {
            externalUrlField = property;
        }
        checkAddTmpField(TMP_FIELD_CHECKHANDLE);
        return super.init();
    }

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
        String result = MMBaseServlet.getBasePath(association);
        if (result != null) {
            usesBridgeServlet = MMBaseServlet.getServletByMapping(result) instanceof BridgeServlet;
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
            if (log.isServiceEnabled()) {
                log.service("Mimetype of attachment '" + node.getNumber() + "' was not set. Using magic to determine it automaticly.");
            }
            byte[] handle = node.getByteValue(FIELD_HANDLE);

            String extension = null;
            if (hasField(FIELD_FILENAME)) {
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

    abstract protected Set<String> getHandleFields();

    @Override
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

    @Override
    public boolean commit(MMObjectNode node) {
        Collection<String> changed = node.getChanged();
        if (log.isDebugEnabled()) {
            log.debug("Committing node " + node.getNumber() + " memory: " + SizeOf.getByteSize(node) + " fields " + changed);
        }

        if (changed.contains(FIELD_HANDLE) ||
            (externalUrlField != null && changed.contains(externalUrlField))
           ) {
            // set those fields to null, which are not changed too:
            Collection<String> cp = new ArrayList<String>();
            cp.addAll(getHandleFields());
            cp.removeAll(changed);

            for (String f : cp) {
                if (node.getBuilder().hasField(f)) {
                    node.setValue(f, null);
                }
            }
        }
        if (node.getValue(TMP_FIELD_CHECKHANDLE) == null) {
            // Check handle itself may call commit, this arrangment with 'TMP_FILE_CHECKHANDLE' avoids infinite recursion
            node.setValue(TMP_FIELD_CHECKHANDLE, "checking handle now");
            checkHandle(node);
            node.setValue(TMP_FIELD_CHECKHANDLE, null);
        }
        boolean result = super.commit(node);
        if (log.isDebugEnabled()) {
            log.debug("After commit node " + node.getNumber() + " memory: " + SizeOf.getByteSize(node));
        }
        return result;
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

    @Override
    final public  String getGUIIndicator(MMObjectNode node, Parameters pars) {
        String field = (String) pars.get("field");
        if (field == null || "".equals(field) || FIELD_HANDLE.equals(field)) {
            return getSGUIIndicator(node, pars);
        } else {
            return super.getGUIIndicator(node, pars);
        }

    }
    /**
     * This is final, because getSGUIIndicator has to be overridden in stead
     */

    @Override
    final public String getGUIIndicator(String field, MMObjectNode node) { // final, override getSGUIIndicator
        return getSGUIIndicator(node, new Parameters(GUI_PARAMETERS).set("field", field));
    }

    protected static final Pattern legalizeFileName = Pattern.compile("[%\\/\\:\\;\\\\ \\?\\&]+");
    private   static final org.mmbase.util.transformers.CharTransformer urlEscaper = new org.mmbase.util.transformers.Url();

    /**
     * @since MMBase-1.8
     */
    protected String getDefaultFileName() {
        return getSingularName("en");
    }
    /**
     * @since MMBase-1.8
     */
    public StringBuilder getFileName(MMObjectNode node, StringBuilder buf) {
        Cloud anonymousCloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase");
        Node bridgeNode;
        if (anonymousCloud.hasNode(node.getNumber()) && anonymousCloud.mayRead(node.getNumber())) {
            bridgeNode = anonymousCloud.getNode(node.getNumber());
        } else {
            bridgeNode = new org.mmbase.bridge.util.MapNode(node.getValues());
        }
        return getFileName(bridgeNode, buf);
    }

    /**
     * @since MMBase-1.9.6
     */
    public StringBuilder getFileName(Node node, StringBuilder buf) {
        String fileName = node.getNodeManager().hasField(FIELD_FILENAME) ? node.getStringValue(FIELD_FILENAME) : "";
        if (fileName.equals("")) {
            String fileTitle;
            if (hasField("title")) {
                fileTitle = node.getStringValue("title");
            } else if (hasField("name")) {
                fileTitle = node.getStringValue("name");
            } else {
                fileTitle = "";
            }
            if (fileTitle.equals("")) {
                fileTitle = getDefaultFileName();
            }
            fileName = fileTitle  + "." + node.getFunctionValue("format", null);
        }
        int backSlash = fileName.lastIndexOf("\\");
        // if uploaded in MSIE, then the path may be in the fileName
        // this is also fixed in the set-processor, but if that is or was missing, be gracefull here.
        if (backSlash > -1)  {
            fileName = fileName.substring(backSlash + 1);
        }
        String fixedFileName = urlEscaper.transform(legalizeFileName.matcher(fileName).replaceAll("_"));
        int extensionIndex = fixedFileName.lastIndexOf(".");
        if (extensionIndex > 0) { //lowercase extensions (MMB-1957)
            fixedFileName =
                fixedFileName.substring(0, extensionIndex) +
                fixedFileName.substring(extensionIndex).toLowerCase();

        }

        buf.append(fixedFileName);
        return buf;
    }

    /**
     * Adds a filename to the path to a servlet, unless this does not make sense (not filename can
     * be determined) or it was explicitely set not to, using the servlet context init parameter
     * 'mmbase.servlet.&lt;association&gt;addfilename.
     * @since MMBase-1.8
     */
    protected boolean addFileName(MMObjectNode node, String servlet) {
        if (addsFileName == FileName.CHECKSETTING) {
            javax.servlet.ServletContext sx = MMBaseContext.getServletContext();
            if (sx != null) {
                String res = sx.getInitParameter("mmbase.servlet." + getAssociation() + ".addfilename");
                if (res == null) res = "";
                res = res.toLowerCase();
                log.trace("res " + res);
                if ("no".equals(res) || "false".equals(res)) {
                    addsFileName = FileName.DONTADD;
                } else if ("yes".equals(res) || "true".equals(res)) {
                    addsFileName = FileName.ADD;
                } else {
                    log.debug("Found " + res + " for mmbase.servlet." + getAssociation() + ".addfilename");
                    addsFileName = FileName.IFSENSIBLE;
                }
            }
        }
        log.debug("addsFileName " + addsFileName);

        String fileName = hasField(FIELD_FILENAME) ? node.getStringValue(FIELD_FILENAME) : "";
        return  addsFileName == FileName.ADD ||
            ( addsFileName == FileName.IFSENSIBLE && (!servlet.endsWith("?")) &&  (! "".equals(fileName)));

    }


    /**
     * @since MMBase-1.8.1
     */
    protected String getSession(Parameters a, int nodeNumber) {
        String session = a.getString("session");
        if (session == null) {
            Cloud cloud = a.get(Parameter.CLOUD);
            log.debug("No session given for " + cloud);
            if(cloud != null && ! cloud.getUser().getRank().equals(Rank.ANONYMOUS)) {
                log.debug("not anonymous");
                // the user is not anonymous!
                // Need to check if node is readable by anonymous.
                // in that case URLs can be simpler
                // two situations are anticipated:
                // - node not readable by anonymous
                // - no anonymous user defined
                try {
                    String cloudName;
                    if (cloud instanceof Transaction) {
                        cloudName = ((Transaction) cloud).getCloudName();
                    }
                    else {
                        cloudName = cloud.getName();
                    }
                    Cloud anonymousCloud = cloud.getCloudContext().getCloud(cloudName);
                    if (! anonymousCloud.mayRead(nodeNumber)) {
                        session = (String) cloud.getProperty(Cloud.PROP_SESSIONNAME);
                        log.debug("Anonymous may not read, setting session to " + session);

                    }
                } catch (org.mmbase.security.SecurityException se) {
                    log.debug(se.getMessage());
                    session = (String) cloud.getProperty(Cloud.PROP_SESSIONNAME);
                }
            }
            if ("".equals(session)) session = null;
        }

        return session;
    }

    {
        // you can of course even implement it anonymously.
        addFunction(new NodeFunction<String>("servletpath",
                                             new Parameter[] {
                                                 new Parameter<String>("session",  String.class), // For read-protection
                                                 new Parameter<String>("field",    String.class), // The field to use as argument, defaults to number unless 'argument' is specified.
                                                 new Parameter<String>("context",  String.class), // Path to the context root, defaults to "/" (but can specify something relative).
                                                 new Parameter<String>("argument", String.class), // Parameter to use for the argument, overrides 'field'
                                                 new Parameter<String>("disposition", String.class),
                                                 Parameter.REQUEST,
                                                 Parameter.CLOUD
                                             },
                                             ReturnType.STRING) {
                        {
                            setDescription("Returns the path associated with this builder or node.");
                        }

                        protected StringBuilder getServletPath(Parameters a) {
                            StringBuilder servlet = new StringBuilder();
                            // third argument, the servlet context, can use a relative path here, as an argument
                            String context             = (String) a.get("context");

                            if (context == null) {
                                // no path to context-root specified explitiely, try to determin:
                                HttpServletRequest request = a.get(Parameter.REQUEST);
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

                        @Override
                        public String getFunctionValue(Node node, Parameters a) {
                            // verify if the object is stored externally (in which case
                            // its url has been filled in)
                            // if so, return the url of the external source
                            if (AbstractServletBuilder.this.externalUrlField != null ) {
                                String url = node.getStringValue(externalUrlField);
                                if (url != null && !url.equals("")) {
                                    log.debug("Found url from exernal field " + url);
                                    return url;
                                }
                            }
                            StringBuilder servlet = getServletPath(a);

                            String session = getSession(a, node.getNumber());
                            String argument = (String) a.get("argument");
                            // argument representing the node-number

                            if (log.isDebugEnabled()) {
                                log.debug("Using parmaeters " + a.toMap());
                            }
                            if (argument == null) {
                                String fieldName   = (String) a.get("field");
                                if (fieldName == null || "".equals(fieldName)) {
                                    argument = node.getStringValue("number");
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Getting 'field' '" + fieldName + "'");
                                    }
                                    argument = node.getStringValue(fieldName);
                                }
                            } else {
                                log.debug("Argument was explicitely '" + argument + "'");
                            }

                            MMObjectNode mmnode = node.getNumber() > 0 ?
                                AbstractServletBuilder.this.getNode(node.getNumber()) :
                                new MMObjectNode(AbstractServletBuilder.this, new org.mmbase.bridge.util.NodeMap(node));
                            boolean addFileName = addFileName(mmnode, servlet.toString());

                            log.debug("Using session " + session);

                            if (usesBridgeServlet &&  session != null && ! "".equals(session)) {
                                servlet.append("session=" + session + "+");
                            }

                            servlet.append(argument);

                            String disposition = (String) a.get("disposition");
                            if (disposition != null) {
                                String defaultDisposition = node.getNodeManager().getProperty("Content-Disposition");
                                if (! disposition.equals(defaultDisposition)) {
                                    servlet.append('/');
                                    servlet.append(disposition);
                                    addFileName = true;
                                }
                            }
                            if (addFileName) {
                                servlet.append('/');
                                getFileName(mmnode, servlet);
                            }
                            return servlet.toString();

                        }

                        @Override
                        public String getFunctionValue(Parameters a) {
                            return getServletPath(a).toString();
                        }
                    });

        addFunction(new NodeFunction<String>("url", new Parameter[] { Parameter.REQUEST, Parameter.CLOUD }) {
                @Override
                public String getFunctionValue(Node node, Parameters a) {
                    Function spFunction = node.getFunction("servletpath");
                    Parameters p = spFunction.createParameters();
                    p.setAll(a);
                    return  node.getFunctionValue("servletpath", p).toString();
                }
            });


        /**
         * @since MMBase-1.8
         */
        addFunction(new MMObjectNodeFunction<String>("iconurl",
                                     new Parameter[] {
                                         Parameter.REQUEST,
                                         new Parameter<String>("iconroot", String.class, "/mmbase/style/icons/"),
                                         new Parameter<String>("absolute", String.class, "false")
                                     },
                                     ReturnType.STRING) {
                        {
                            setDescription("Returns an URL for an icon for this blob");
                        }
                        @Override
                        public String getFunctionValue(Node n, Parameters parameters) {
                            String mimeType = AbstractServletBuilder.this.getMimeType(getCoreNode(AbstractServletBuilder.this, n));
                            ResourceLoader webRoot = ResourceLoader.getWebRoot();
                            HttpServletRequest request = parameters.get(Parameter.REQUEST);
                            String absolute = parameters.getString("absolute");
                            String root;
                            if (request != null) {
                                root = request.getContextPath();
                            } else {
                                root = MMBaseContext.getHtmlRootUrlPath();
                            }

                            if ("true".equals(absolute) && request != null) {
                                int port = request.getServerPort();
                                root = request.getScheme() + "://" + request.getServerName() + (port == 80 ? "" : ":" + port) + root;
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

    @Override
    protected Object executeFunction(MMObjectNode node, String function, List<?> args) {
        if (log.isDebugEnabled()) {
            log.debug("executefunction of abstractservletbuilder for " + node.getNumber() + "." + function + " " + args);
        }
        if (function.equals("info")) {
            List<Object> empty = new ArrayList<Object>();
            Map<String,String> info = (Map<String,String>) super.executeFunction(node, function, empty);
            info.put("servletpathof", "(function) Returns the servletpath associated with a certain function");
            info.put("format", "bla bla");
            info.put("mimetype", "Returns the mimetype associated with this object");
            info.put("gui", "" + GUI_PARAMETERS + "Gui representation of this object.");

            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
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


    /**
     * @since MMBase-1.9.2
     */
    public java.io.InputStream getBinary(MMObjectNode node) {
        return node.getInputStreamValue(FIELD_HANDLE);
    }

}
