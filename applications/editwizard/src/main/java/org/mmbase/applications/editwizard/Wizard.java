/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.editwizard;

import java.io.*;
import java.net.URL;

import java.util.*;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.mmbase.applications.dove.Dove;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.util.Queries;
import org.mmbase.cache.Cache;
import org.mmbase.framework.*;
import org.mmbase.storage.search.RelationStep; // just for the search-constants.
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.ResourceWatcher;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.*;
import org.w3c.dom.*;


/**
 * EditWizard
 * @javadoc
 * @author Kars Veling
 * @author Michiel Meeuwissen
 * @author Pierre van Rooden
 * @author Hillebrand Gelderblom
 * @since MMBase-1.6
 * @version $Id$
 *
 */
public class Wizard implements org.mmbase.util.SizeMeasurable, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logging.getLoggerInstance(Wizard.class);
    public static final String PUBLIC_ID_EDITWIZARD_1_0 = "-//MMBase//DTD editwizard 1.0//EN";
    public static final String PUBLIC_ID_EDITWIZARD_1_0_FAULT = "-//MMBase/DTD editwizard 1.0//EN";
    public static final String DTD_EDITWIZARD_1_0       = "wizard-schema_1_0.dtd";

    public static final String RESPONSE_KEY = "org.mmbase.editwizard.response";


    static {
        EntityResolver.registerPublicID(PUBLIC_ID_EDITWIZARD_1_0, DTD_EDITWIZARD_1_0, Wizard.class);
        EntityResolver.registerPublicID(PUBLIC_ID_EDITWIZARD_1_0_FAULT, DTD_EDITWIZARD_1_0, Wizard.class);
    }

    // File -> Document (resolved includes/shortcuts)
    private static WizardSchemaCache wizardSchemaCache;

    static {
        wizardSchemaCache = new WizardSchemaCache();
        wizardSchemaCache.putCache();
    }

    private Map<String, Node> nodePermissionCache = new HashMap<String, Node>();

    /**
     * The cloud used to connect to MMBase
     */
    private Cloud cloud;

    // This object will be used the revolve URI's, for example those of XSL's and XML's.
    private URIResolver uriResolver = null;

    // the editwizard context path
    private String context = null;

    // the result objectnumber (the number of the object after a commit)
    // this value is only assigned after COMMIT is called - otherwise it is null
    private String objectNumber;

    // the wizard (file) name. Eg.: samples/jumpers will choose the file $path/samples/jumpers.xml
    private String wizardName;

    // the (possibly temporary) number of the main object
    private String dataId;

    // stores the current formid
    private String currentFormId;

    // filename of the stylesheet which should be used to make the html form.
    private transient URL wizardStylesheetFile;
    private String sessionId;
    private String sessionKey = "editwizard";
    private String referrer = "";
    private String templatesDir = null;
    private String timezone;

    /**
     * xmldom's: the schema, the data and the originaldata is stored.
     */
    private transient Document schema;
    private transient Document data;
    private transient Document originalData;


    /**
     * @since MMBase 1.9.5
     */
    private transient Map<Document, Map<String, List<Element>>> indexes = new HashMap<Document, Map<String, List<Element>>>();

    /**
     *  document where loaded data will be stored in when added by wizard actions
     */
    private Document loadedData;

    // not yet committed uploads are stored in these hashmaps
    private Map<String, byte[]> binaries = new HashMap<String, byte[]>();
    private Map<String, String> binaryNames = new HashMap<String, String>();
    private Map<String, String> binaryPaths = new HashMap<String, String>();

    // in the wizards, variables can be used. Values of the variables are stored here.
    private Map<String, Object> variables = new HashMap<String, Object>();

    // the constraints received from mmbase are stored + cached in this xmldom
    private transient Document constraints;

    // Seconds.
    private long listQueryTimeOut = 60 * 60;

    // the database connector handles communition with mmbase. the instance is stored here.
    private WizardDatabaseConnector databaseConnector;

    /**
     * This boolean tells the jsp that the wizard may be closed, as far as he is concerned.
     */
    private boolean mayBeClosed = false;

    /**
     * This boolean tells the jsp that a new (sub) wizard should be started
     */
    private boolean startWizard = false;

    /**
     * The command to use dor starting a new (sub) wizard
     * Only set when startwizard is true
     */
    private WizardCommand startWizardCmd = null;

    /**
     * This boolean tells the jsp that the wizard was committed, and changes may have been made
     */
    private boolean committed = false;

    /**
     * Constructor. Setup initial variables and connects to mmbase to load the data structure.
     *
     * @deprecated use Wizard(String, URIResolver, Config.WizardConfig, Cloud)
     * @param request Associated servlet request, sometimes needed for URL generation
     * @param uri  the URIResolver with which the wizard schema's and the xsl's will be loaded
     * @param wizardname name of the wizard
     * @param dataid the objectnumber
     * @param cloud the Cloud to use
     * @throws WizardException when wizard creation failed
     */
    public Wizard(HttpServletRequest request, URIResolver uri, String wizardname, String dataid, Cloud cloud) throws WizardException {
        Config.WizardConfig wizardConfig = new Config.WizardConfig();
        wizardConfig.objectNumber = dataid;
        wizardConfig.wizard = wizardname;
        initialize(request, uri, wizardConfig, cloud);
    }

    /**
     * Constructor. Setup initial variables and connects to mmbase to load the data structure.
     *
     * @param request Associated servlet request, sometimes needed for URL generation
     * @param uri  the URIResolver with which the wizard schema's and the xsl's will be loaded
     * @param wizardConfig the class containing the configuration parameters (i.e. wizard name and objectnumber)
     * @param cloud the Cloud to use
     * @throws WizardException when wizard creation failed
     */
    public Wizard(HttpServletRequest request, URIResolver uri,
                  Config.WizardConfig wizardConfig,
                  Cloud cloud) throws WizardException {
        initialize(request, uri, wizardConfig, cloud);
    }

    public int getByteSize() {
        return getByteSize(new org.mmbase.util.SizeOf());
    }

    public int getByteSize(org.mmbase.util.SizeOf sizeof) {
        return sizeof.sizeof(cloud) + sizeof.sizeof(uriResolver) +
            sizeof.sizeof(schema) + sizeof.sizeof(data) +
            sizeof.sizeof(originalData) + sizeof.sizeof(binaries) +
            sizeof.sizeof(binaryNames) + sizeof.sizeof(binaryPaths) +
            sizeof.sizeof(constraints);
    }

    private void initialize(HttpServletRequest request, URIResolver uri, Config.WizardConfig wizardConfig, Cloud cloud) throws WizardException {
        context = request.getContextPath();
        uriResolver = uri;
        constraints = Utils.parseXML("<constraints/>");

        // initialize database connector
        databaseConnector = new WizardDatabaseConnector();
        databaseConnector.setUserInfo(cloud);

        // set cloud
        this.cloud = cloud;

        // add username to variables
        variables.put("username", cloud.getUser().getIdentifier());

        // actually load the wizard
        loadWizard(wizardConfig, request);
    }

    public void setSessionId(String s) {
        sessionId = s;
    }

    public void setSessionKey(String s) {
        sessionKey = s;
    }

    public void setReferrer(String s) {
        referrer = s;
    }

    public void setTemplatesDir(String f) {
        templatesDir = f;
    }

    public void setTimezone(String s) {
        timezone = s;
    }

    public String getObjectNumber() {
        return objectNumber;
    }

    public String getDataId() {
        return dataId;
    }

    public Document getData() {
        return data;
    }

    public Document getSchema() {
        return schema;
    }

    public Document getPreForm() throws WizardException {
        return getPreForm(wizardName);
    }

    public Document getPreForm(String instanceName) throws WizardException {
        Node datastart = Utils.selectSingleNode(data, "/data/*");

        return createPreHtml(schema.getDocumentElement(), currentFormId,
                             datastart, instanceName);
    }


    /**
     * Returns whether the wizard has committed the transaction
     * @return <code>true</code> when committed
     */
    public boolean committed() {
        return committed;
    }

    /**
     * Returns whether the wizard may be closed
     * @return <code>true</code> when allowed to close
     */
    public boolean mayBeClosed() {
        return mayBeClosed;
    }

    /**
     * Returns whether a sub wizard should be started
     * @return <code>true</code> when start a sub wizard
     */
    public boolean startWizard() {
        return startWizard;
    }

    /**
     * Returns true if the specified operation is valid for the node with the specified objectnumber.
     * The  operation is valid if the node has the given property set to true.
     * To maintain backwards compatible, if the property is not given, the default value is true.
     * @param objectNumber the number of the node to check
     * @param operation a valid operation, i.e. maywrite or maydelete
     * @return <code>true</code> when valid operation
     * @throws WizardException if the object cannot be retrieved
     */
    protected boolean checkNode(String objectNumber, String operation) throws WizardException {
        Node node = nodePermissionCache.get(objectNumber);

        if (node == null) {
            List<Element> nodes = getElementsByNumber(data, objectNumber);

            if (nodes.size() > 0) {
                node = nodes.get(0);
            } else {
                if (objectNumber == null || objectNumber.equals("")) {
                    log.debug("Checking security for objectNumber '" + objectNumber + "'  "); // + Logging.stackTrace(5));
                    // MM: This happened to me when using sub-wizards (mmexamples)
                    // And this code made it work at least, but still wondering why it came here in the first place.
                    return true;
                }
                // node is from outside the datacloud...
                // get it through dove... should we add it, and if so where?
                node = databaseConnector.getDataNode(null, objectNumber, null);
            }

            nodePermissionCache.put(objectNumber, node);
            log.debug("Node loaded: " + node);
        } else {
            log.debug("Node found in cache: " + node);
        }

        return (node != null) &&
            Utils.getAttribute(node, operation, "true").equals("true");
    }

    /**
     * Returns true if the node with the specified objectnumber can be edited
     * @param objectNumber number of the object to check
     * @return <code>true</code> when allowed to edit
     * @throws WizardException if the object cannot be retrieved
     */
    protected boolean mayEditNode(String objectNumber) throws WizardException {
        return checkNode(objectNumber, "maywrite");
    }

    /**
     * Returns true if the node with the specified objectnumber can be deleted
     * @param objectNumber number of the object to check
     * @return <code>true</code> when allowed to delete
     * @throws WizardException if the object cannot be retrieved
     */
    protected boolean mayDeleteNode(String objectNumber) throws WizardException {
        return checkNode(objectNumber, "maydelete");
    }

    /**
     * Returns the subwizard start command
     * @return wizard command
     */
    public WizardCommand getStartWizardCommand() {
        return startWizardCmd;
    }

    /**
     * Stores configuration variables as attributes in the variables set.
     * @param wizardConfig the config with the parameters
     */
    protected void storeConfigurationAttributes(Config.WizardConfig wizardConfig) {
        variables.put("wizardname", wizardName);

        // set attributes from config
        // this sets: origin, context, debug, objectnumber, and wizard
        variables.putAll(wizardConfig.getAttributes());
    }

    /**
     * Loads the wizard schema, and a work document, and fills it with initial data.
     *
     * @param wizardConfig the class containing the configuration parameters (i.e. wizard name and objectnumber)
     * @throws WizardException when wizard loading failed
     */
    protected void loadWizard(Config.WizardConfig wizardConfig, HttpServletRequest request) throws WizardException {
        if (wizardConfig.wizard == null) {
            throw new WizardException("Wizardname may not be null");
        }

        wizardName = wizardConfig.wizard;
        dataId = wizardConfig.objectNumber;

        URL wizardSchemaFile;
        try {
            wizardSchemaFile     = uriResolver.resolveToURL(wizardName + ".xml", null);
        } catch (Exception e) {
            throw new WizardException(e);
        }
        if (wizardSchemaFile == null) {
            throw new WizardException("Could not resolve wizard " + wizardName + ".xml  with "  + uriResolver);
        }
        try {
            wizardStylesheetFile = uriResolver.resolveToURL(Config.wizardStyleSheet, null);
        } catch (Exception e) {
            throw new WizardException(e);
        }

        if (wizardStylesheetFile == null) {
            throw new WizardException("Could not resolve XSL '" + Config.wizardStyleSheet + "'  with "  + uriResolver);
        }
        // store variables so that these can be used in the wizard schema
        storeConfigurationAttributes(wizardConfig);

        // load wizard schema
        loadSchema(wizardSchemaFile, request); // expanded filename of the wizard


        // If the given dataid=new, we have to create a new object first, given
        // by the object definition in the schema.
        // If dataid equals null, we don't need to do anything. Wizard will not be used to show or save data;
        // just to load schema information.
        if (dataId != null) {
            if (dataId.equals("new")) {
                log.debug("Creating new xml");

                // setup original data
                originalData = Utils.emptyDocument();
                loadedData = Utils.parseXML("<loadedData />");

                // Get the definition and create a copy of the object-definition.
                Node objectdef = Utils.selectSingleNode(schema, "./wizard-schema/action[@type='create']");

                if (objectdef == null) {
                    throw new WizardException("You tried to start a create action in the wizard, but no create action was defined in the wizard schema. Please supply a <action type='create' /> section in the wizard.");
                }

                objectdef = objectdef.cloneNode(true);
                log.debug("Going to creating a new object " + objectdef.getNodeName() + " type " + Utils.getAttribute(objectdef, "type"));

                // We have to add the object to the data, so first determine to which parent it belongs.
                data = Utils.parseXML("<data />");

                Node parent = data.getDocumentElement();

                // Ask the database to create that object, ultimately to get the new id.
                Node newobject = databaseConnector.createObject(data, parent, objectdef, variables, loadedData);

                if (newobject == null) {
                    throw new WizardException("Could not create new object. Did you forget to add an 'object' subtag?");
                }

                parent.appendChild(newobject);
                databaseConnector.tagDataNodesWithoutDataID(data);
                dataId = Utils.getAttribute(newobject, "number");

                if (log.isDebugEnabled()) {
                    log.debug("Created object " + newobject.getNodeName() + " type " + Utils.getAttribute(newobject, "type") + ", id " + dataId);
                }
            } else {
                loadData();
            }
        }

        // initialize an editor session
        if (currentFormId == null) {
            currentFormId = determineNextForm("first");
        }
    }

    /**
     * Load data from mmbase based on the wizard schema
     * @throws WizardException when data loading failed
     * @since MMBase-1.7
     */
    protected void loadData() throws WizardException {
        // - load data.
        // - tags the datanodes
        data = databaseConnector.load(schema.getDocumentElement(), dataId);

        if (data == null) {
            throw new WizardException("The requested object could not be loaded from MMBase. ObjectNumber:" + dataId +
                                      ". Does the object exists and do you have enough rights to load this object.");
        }
        // setup original data
        originalData = Utils.emptyDocument();
        loadedData = Utils.parseXML("<loadedData />");


        // store original data, so that the put routines will know what to save/change/add/delete
        originalData.appendChild(originalData.importNode(data.getDocumentElement().cloneNode(true), true));
    }

    /**
     * Processes an incoming request (usually passed on by a jsp code).
     * First, all given values are stored in the current datatree,
     * Second, all given commands are processed sequentially.
     *
     * @param req the ServletRequest contains the name-value pairs received through the http connection
     * @throws WizardException when request processing failed
     */
    public void processRequest(HttpServletRequest req) throws WizardException {
        String curform = req.getParameter("curform");

        if ((curform != null) && !curform.equals("")) {
            currentFormId = curform;
        }

        storeValues(req);
        processCommands(req);
    }

    /**
     * Constructs and writes final form-html to the given out writer.
     * You can specify an instancename, so that the wizard is able to start another wizard in the
     * <em>same</em> session. The jsp pages and in the html the instancenames are used to keep track
     * of one and another.
     *
     * @param out The writer where the output (html) should be written to.
     * @param instanceName name of the current instance
     * @throws WizardException when building the current state of the wizard xml failed
     * @throws TransformerException when transforming the wizard xml failed
     */
    public void writeHtmlForm(Writer out, String instanceName) throws WizardException, TransformerException {
        writeHtmlForm(out, instanceName, null);
    }

    /**
     * Constructs and writes final form-html to the given out writer.
     * You can specify an instancename, so that the wizard is able to start another wizard in the
     * <em>same</em> session. The jsp pages and in the html the instancenames are used to keep track
     * of one and another.
     *
     * @param out The writer where the output (html) should be written to.
     * @param instanceName name of the current instance
     * @param externParams sending parameters to the stylesheet which are not
     *    from the editwizards itself
     * @throws WizardException when building the current state of the wizard xml failed
     * @throws TransformerException when transforming the wizard xml failed
     */
    public void writeHtmlForm(Writer out, String instanceName, Map<String, Object> externParams)
        throws WizardException, TransformerException {
        if (log.isDebugEnabled()) {
            log.debug("writeHtmlForm for " + instanceName);
        }

        // Build the preHtml version of the form.
        Document preForm = getPreForm(instanceName);
        Validator.validate(preForm, schema);

        Map<String, Object> params = new HashMap<String, Object>(variables);
        params.put("ew_context", context);

        // params.put("ew_imgdb",   org.mmbase.module.builders.AbstractImages.getImageServletPath(context));
        params.put("sessionid", sessionId);
        params.put("sessionkey", sessionKey);
        params.put("referrer", referrer);
        params.put("language", cloud.getLocale().getLanguage());
        params.put("timezone", timezone);
        try {
            params.put("referrer_encoded", java.net.URLEncoder.encode(referrer, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.debug("" + e.getMessage(), e);
        }

        if (templatesDir != null) {
            params.put("templatedir", templatesDir);
        }

        if (externParams != null && !externParams.isEmpty()) {
            params.putAll(externParams);
        }

        Utils.transformNode(preForm, wizardStylesheetFile, uriResolver, out, params, cloud);
    }

    /**
     * Internal method which is used to store the passed values. this method is called by processRequest.
     * @param req http servlet requets containing new values
     * @throws WizardException when failed to store data in the wizard data structure
     *
     * @see #processRequest
     */
    private void storeValues(HttpServletRequest req) throws WizardException {
        Enumeration<String> list = req.getParameterNames();
        log.debug("Synchronizing editor data, using the request");

        String formEncoding = req.getCharacterEncoding();
        boolean hasEncoding = formEncoding != null;
        if (!hasEncoding) {
            log.debug("Request did not mention encoding, supposing UTF-8, as JSP's are");
            formEncoding = "UTF-8";
        } else {
            log.debug("found encoding in the request: " + formEncoding);
        }

        while (list.hasMoreElements()) {
            String name = list.nextElement();

            if (name.startsWith("internal_")) {
                log.debug("Ignoring parameter " + name);
            } else {
                log.debug("Processing parameter " + name);

                String[] ids = processFormName(name);
                if (log.isDebugEnabled()) {
                    log.debug("found ids: " + ((ids == null) ? "null" : (" " + java.util.Arrays.asList(ids))));
                }
                if (ids != null) {
                    String result;
                    if (!hasEncoding) {
                        try {
                           result = new String(req.getParameter(name).getBytes("ISO-8859-1"), formEncoding);
                           if (log.isDebugEnabled()) {
                               log.debug("Found in post '" + req.getParameter(name) +  "' -> '" + result + "'");
                           }
                        } catch (java.io.UnsupportedEncodingException e) {
                            log.warn(e.toString());
                            result = req.getParameter(name);
                        }
                    } else { // the request encoding was known, so, I think we can suppose that the Parameter value was interpreted correctly.
                        result = req.getParameter(name);
                    }
                    log.debug("parameter value " + result);

                    if (result.equals("date")) {
                        result = buildDate(req, name);
                    }
                    if (result.equals("time")) {
                        result = buildTime(req, name);
                    }
                    if (result.equals("datetime")) {
                        result = buildDateTime(req, name);
                    }
                    if (result.equals("duration")) {
                        result = buildDuration(req, name);
                    }

                    storeValue(ids[0], ids[1], result);
                }
            }
        }
    }

    /**
     * @return Calendar with timezone parameter
     */
    private Calendar getCalendar() {
        if (timezone != null) {
            TimeZone tz = TimeZone.getTimeZone(timezone);
            if (tz.getID().equals(timezone)) {
                return Calendar.getInstance(tz);
            }
            else {
                return Calendar.getInstance();
            }
        }
        else {
            return Calendar.getInstance();
        }
    }

    private String buildDate(HttpServletRequest req, String name) {
        try {
            int day = Integer.parseInt(req.getParameter("internal_" + name + "_day"));
            int month = Integer.parseInt(req.getParameter("internal_" + name + "_month"));
            int year = Integer.parseInt(req.getParameter("internal_" + name + "_year"));

            Calendar cal = getCalendar();
            cal.set(year, month - 1, day, 0, 0, 0);
            return "" + cal.getTimeInMillis() / 1000;
        } catch (RuntimeException e) { //NumberFormat NullPointer
            log.debug("Failed to parse date for " + name + " " + e.getMessage());
            return "";
        }
    }

    private String buildTime(HttpServletRequest req, String name) {
        try {
            int hours = Integer.parseInt(req.getParameter("internal_" + name + "_hours"));
            int minutes = Integer.parseInt(req.getParameter("internal_" + name + "_minutes"));

            Calendar cal = getCalendar();
            cal.set(1970, 0, 1, hours, minutes, 0);
            return "" + cal.getTimeInMillis() / 1000;
        } catch (RuntimeException e) { //NumberFormat NullPointer
            log.debug("Failed to parse time for " + name + " "
                    + e.getMessage());
            return "";
        }
    }

    private String buildDateTime(HttpServletRequest req, String name) {
        try {
            int day = Integer.parseInt(req.getParameter("internal_" + name + "_day"));
            int month = Integer.parseInt(req.getParameter("internal_" + name + "_month"));
            int year = Integer.parseInt(req.getParameter("internal_" + name + "_year"));
            int hours = Integer.parseInt(req.getParameter("internal_" + name + "_hours"));
            int minutes = Integer.parseInt(req.getParameter("internal_" + name + "_minutes"));
            String secParameter = req.getParameter("internal_" + name + "_seconds");
            int seconds = secParameter == null ? 0 : Integer.parseInt(secParameter);

            Calendar cal = getCalendar();
            cal.set(year, month - 1, day, hours, minutes, seconds);
            return "" + cal.getTimeInMillis() / 1000;
        } catch (RuntimeException e) { //NumberFormat NullPointer
            log.debug("Failed to parse datetime for " + name + " "
                    + e.getMessage());
            return "";
        }
    }

    private String buildDuration(HttpServletRequest req, String name) {
        try {
            int hours = Integer.parseInt(req.getParameter("internal_" + name + "_hours"));
            int minutes = Integer.parseInt(req.getParameter("internal_" + name + "_minutes"));
            int seconds = Integer.parseInt(req.getParameter("internal_" + name + "_seconds"));

            Calendar cal =  Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.set(1970, 0, 1, hours, minutes, seconds);
            return "" + cal.getTimeInMillis() / 1000;
        } catch (RuntimeException e) { //NumberFormat NullPointer
            log.debug("Failed to parse duration for " + name + " " + e.getMessage());
            return "";
        }
    }



    /**
     * This method is used to determine what form is the sequential next, previous, first etc.
     * You can use the parameter to indicate what you want to know:
     *
     * @param direction     indicates what you wanna know. Possibilities:
     * <code>
     * - first (default)
     * - last
     * - previous
     * - next
     * </code>
     * @return name of next form
     */
    public String determineNextForm(String direction) {
        String stepDirection = direction;

        if (stepDirection == null) {
            stepDirection = "first";
        }

        // Determine if there are steps defined.
        // If so, use the step elements to determine previous and next forms.
        // If not, use the form-schema elements to determine previous and next forms.
        // Assume that steps are defined for the moment.
        String nextformid = "FORM_NOT_FOUND";
        Node laststep = Utils.selectSingleNode(schema,
                                               "//steps/step[@form-schema='" + currentFormId + "']");
        Node nextstep = null;

        // If the last step doesn't exist, get the first step.
        // If the last step exists, determine the next step.
        if ((laststep == null) || stepDirection.equals("first")) {
            nextstep = Utils.selectSingleNode(schema, "//steps/step");
            nextformid = Utils.getAttribute(nextstep, "form-schema");
        } else {
            if (stepDirection.equals("previous")) {
                nextstep = Utils.selectSingleNode(laststep, "./preceding-sibling::step");
            } else if (stepDirection.equals("last")) {
                nextstep = Utils.selectSingleNode(laststep, "../step[position()=last()]");
            } else {
                nextstep = Utils.selectSingleNode(laststep, "./following-sibling::step");
            }

            if (nextstep == null) {
                nextformid = "WIZARD_OUT_OF_BOUNDS";
            } else {
                nextformid = Utils.getAttribute(nextstep, "form-schema");
            }
        }

        return nextformid;
    }

    /**
     * This method generates the pre-html. See the full-spec method for more details.
     * @param       instanceName    The instancename of this wizard
     * @return xml representation of the wizard data ready to be transformed
     * @throws WizardException thrown when something failed in generating the xml
     *
     * @see #createPreHtml(Node, String, Node, String)
     */
    public Document createPreHtml(String instanceName) throws WizardException {
        Node datastart = Utils.selectSingleNode(data, "/data/*");

        return createPreHtml(schema.getDocumentElement(), "1", datastart,
                             instanceName);
    }

    /**
     * This method generated the pre-html.
     *
     * Pre-html is a temporarily datatree (xml of course) which is used to generate the final html from.
     * Is uses the wizardschema, the current formid, the current datatree and the instancename to generate the pre-html.
     *
     * Mainly, the pre-html contains all data needed to make a nice htmlform. The XSL's use the pre-html to generate html (thats why pre-html)
     *
     * @param       wizardSchema    the main node of the schema to be used. Includes should already be resolved.
     * @param       formid          The id of the current form
     * @param       data            The main node of the data tree to be used
     * @param       instanceName    The instancename of this wizard
     * @return xml representation of the wizard data ready to be transformed
     * @throws WizardException thrown when something failed in generating the xml
     */
    public Document createPreHtml(Node wizardSchema, String formid, Node data,
                                  String instanceName) throws WizardException {
        if (log.isDebugEnabled()) {
            log.debug("Create preHTML of " + instanceName);
        }

        // intialize preHTML wizard
        Document preHtml = Utils.parseXML("<wizard instance=\"" + instanceName + "\" />");
        Node wizardnode = preHtml.getDocumentElement();

        // copy all global wizard nodes.
        NodeList globals = Utils.selectNodeList(wizardSchema, "title|subtitle|description");
        Utils.appendNodeList(globals, wizardnode);

        // find the current step and, if appliccable, the next and previous ones.
        Utils.createAndAppendNode(wizardnode, "curform", formid);

        Node step = Utils.selectSingleNode(wizardSchema, "./steps/step[@form-schema='" + formid + "']");

        if (step != null) {
            // Yes. we have step information. Let's add info about that.
            String otherformid = "";
            Node prevstep = Utils.selectSingleNode(step, "./preceding-sibling::step[1]");

            if (prevstep != null) {
                otherformid = Utils.getAttribute(prevstep, "form-schema");
            }

            Utils.createAndAppendNode(wizardnode, "prevform", otherformid);

            otherformid = "";

            Node nextstep = Utils.selectSingleNode(step, "./following-sibling::step[1]");

            if (nextstep != null) {
                otherformid = Utils.getAttribute(nextstep, "form-schema");
            }

            Utils.createAndAppendNode(wizardnode, "nextform", otherformid);
        }

        // process all forms
        NodeList formlist = Utils.selectNodeList(schema, "/*/form-schema");

        if (formlist.getLength() == 0) { // this can be done by dtd-checking!
            throw new WizardException("No form-schema was found in the xml. Make sure at least one form-schema node is present.");
        }

        for (int f = 0; f < formlist.getLength(); f++) {
            Node form = formlist.item(f);

            // Make prehtml form.
            Node prehtmlform = preHtml.createElement("form");
            Utils.copyAllAttributes(form, prehtmlform);
            wizardnode.appendChild(prehtmlform);

            // Add the title, description.
            NodeList props = Utils.selectNodeList(form, "title|subtitle|description");
            Utils.appendNodeList(props, prehtmlform);

            // check all fields and do the thingies
            createPreHtmlForm(prehtmlform, form, data);
        }

        // now, resolve optionlist values:
        // - The schema contains the list definitions, from which the values are copied.
        // - Each list may have a query attached, which is performed before the copying.
        NodeList optionlists = Utils.selectNodeList(wizardnode, ".//field/optionlist");

        for (int i = 0; i < optionlists.getLength(); i++) {
            Node optionlist = optionlists.item(i);

            String listname = Utils.getAttribute(optionlist, "select");

            // import and create list if select was set - otherwise, this optionlist is defined 'inline'
            if (listname != null && !listname.equals("")) {
                log.debug("Handling optionlist: " + i + ": " + listname);

                Node list = Utils.selectSingleNode(wizardSchema, "/*/lists/optionlist[@name='" + listname + "']");

                if (list == null) {
                    // Not found in definition. Put an error in the list and proceed with the
                    // next list.
                    log.debug("Not found! Proceeding with next list.");

                    Element option = optionlist.getOwnerDocument().createElement("option");
                    option.setAttribute("id", "-");
                    Utils.storeText(option,
                                    "Error: optionlist '" + listname + "' not found");
                    optionlist.appendChild(option);

                    continue;
                }

                // Test if this list has a query and get the time-out related values.
                Node query = Utils.selectSingleNode(list, "query");
                if (query != null) {
                    if (!runQuery(list, query)) continue;
                }

                // Now copy the items of the list definition to the preHTML list.
                NodeList items = Utils.selectNodeList(list, "option");
                Utils.appendNodeList(items, optionlist);

            } else {
                // test if query occurs inline and get the time-out related values.
                Node query = Utils.selectSingleNode(optionlist, "query");
                if (query != null) {
                    if (!runQuery(optionlist, query)) continue;
                }
            }

            // set selected=true for option which is currently selected
            String selectedValue = Utils.selectSingleNodeText(optionlist,
                                                              "../value/text()", ""); //.getNodeValue();
            log.debug("Trying to preselect the list at value: " + selectedValue);

            Node selectedoption = Utils.selectSingleNode(optionlist,
                                                         "option[@id='" + selectedValue + "']");

            if (selectedoption != null) {
                // found! Let's set it selected.
                Utils.setAttribute(selectedoption, "selected", "true");
            }
        }

        // Okee, we are ready. Let's return what we've been working on so hard.
        return preHtml;
    }

    /**
     * This method tests if a query in a nodelist needs to be run, and runs it if needed, adding the resulting options to the optionlist.
     *
     * @param list    the node of the optionlist in which the query is defined
     * @param query    the node of the query definition
     */
    boolean runQuery(Node list, Node query) {
        long currentTime = new Date().getTime();
        long queryTimeOut = 1000 * Long.parseLong(Utils.getAttribute(list,
                                                                     "query-timeout", String.valueOf(this.listQueryTimeOut)));
        long lastExecuted = currentTime - queryTimeOut - 1;

        if (query != null) {
            String lastExecutedString = Utils.getAttribute(query, "last-executed", "never");

            if (!lastExecutedString.equals("never")) {
                lastExecuted = Long.parseLong(lastExecutedString);
            }
        }

        // Execute the query if it's there and only if it has timed out.
        if ((query != null) && ((currentTime - lastExecuted) > queryTimeOut)) {
            if (log.isDebugEnabled()) {
              log.debug("Performing query for optionlist '" + Utils.getAttribute(list, "select") +
                      "'. Cur time " + currentTime + " last executed " + lastExecuted +
                      " timeout " + queryTimeOut + " > " + (currentTime - lastExecuted));
            }
            Node queryresult = null;

            try {
                // replace {$origin} and such
                String newWhere = Utils.fillInParams(Utils.getAttribute(query, "where"), variables);
                Utils.setAttribute(query, "where", newWhere);
                queryresult = databaseConnector.getList(query);
                queryresult = Utils.selectSingleNode(queryresult, "/getlist/query");
            } catch (Exception e) {
                // Bad luck, tell the user and try the next list.
                log.debug("Error during query, proceeding with next list: " + e.toString());

                Element option = list.getOwnerDocument().createElement("option");
                option.setAttribute("id", "-");
                Utils.storeText(option, "Error: query for '" + Utils.getAttribute(list, "select") + "' failed");
                list.appendChild(option);

                return false;
            }

            // Remind the current time.
            Utils.setAttribute(query, "last-executed", String.valueOf(currentTime));

            // Remove any already existing options.
            NodeList olditems = Utils.selectNodeList(list, "option");

            for (int itemindex = 0; itemindex < olditems.getLength();
                 itemindex++) {
                list.removeChild(olditems.item(itemindex));
            }

            // Loop through the queryresult and add the included objects by creating
            // an option element for each one. The id and content of the option
            // element are taken from the object by performing the xpaths on the object,
            // that are given by the list definition.
            NodeList items = Utils.selectNodeList(queryresult, "*");
            String idPath = Utils.getAttribute(list, "optionid", "@number");
            String contentPath = Utils.getAttribute(list, "optioncontent", "field");

            for (int itemindex = 0; itemindex < items.getLength();
                 itemindex++) {
                Node item = items.item(itemindex);
                String optionId = Utils.transformAttribute(item, idPath, true);
                String optionContent = Utils.transformAttribute(item,
                                                                contentPath, true);
                Element option = list.getOwnerDocument().createElement("option");
                option.setAttribute("id", optionId);
                Utils.storeText(option, optionContent);
                list.appendChild(option);
            }
        }
        return true;
    }

    /**
     * This method is used by the #createPreHtml method to generate a pre-html form.
     *
     * @param       form    The node of the pre-html form which is to be generated
     * @param       formdef the node of the wizardschema form definition
     * @param       dataContext   Points to the datacontext node which should be used for this form.
     * @throws WizardException when form generation failed
     */
    public void createPreHtmlForm(Node form, Node formdef, Node dataContext)
        throws WizardException {
        if (log.isDebugEnabled()) {
            log.trace("Creating preHTMLForm for form:" + form + " / formdef:" + formdef + " / data:" + dataContext);
        }

        // select all fields on first level
        NodeList fields = Utils.selectNodeList(formdef, "fieldset|field|listset|list|command");

        // process all possible fields
        // - Parse the fdatapath attribute to obtain the corresponding data fields.
        // - Create a form field for each found data field.
        for (int i = 0; i < fields.getLength(); i++) {
            Node field = fields.item(i);

            // let's see what we should do here
            String nodeName = field.getNodeName();

            // a field set
            if (nodeName.equals("fieldset")) {
                Node newfieldset = form.getOwnerDocument().createElement("fieldset");
                Utils.copyAllAttributes(field, newfieldset);

                NodeList itemprops = Utils.selectNodeList(field, "prompt");
                Utils.appendNodeList(itemprops, newfieldset);

                // place newfieldset in pre-html form
                form.appendChild(newfieldset);
                createPreHtmlForm(newfieldset, field, dataContext);
            } else if (nodeName.equals("listset")) {
                Node newfieldset = form.getOwnerDocument().createElement("listset");
                Utils.copyAllAttributes(field, newfieldset);

                // place newfieldset in pre-html form
                form.appendChild(newfieldset);
                createPreHtmlForm(newfieldset, field, dataContext);
            } else {
                Node fieldDataNode = null;
                String xpath = Utils.getAttribute(field, "fdatapath", null);

                if (xpath == null) {
                    String ftype = Utils.getAttribute(field, "ftype", null);

                    if (!("startwizard".equals(ftype) || "wizard".equals(ftype))) {
                        throw new WizardException("A field tag should contain one of the following attributes: fdatapath or name");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Doing '" + xpath + "'");
                        log.debug("on " + Utils.getXML(dataContext));
                    }

                    // A normal field.
                    if (nodeName.equals("field")) {
                        // xpath is found.
                        fieldDataNode = Utils.selectSingleNode(dataContext, xpath);

                        if (fieldDataNode != null) {


                            int endFieldContextXpath = xpath.lastIndexOf("/") > -1 ? xpath.lastIndexOf('/') : 0;
                            String fieldContextXPath = xpath.substring(0, endFieldContextXpath);
                            String mayWriteXPath = "".equals(fieldContextXPath) ? "@maywrite" : fieldContextXPath + "/@maywrite";
                            Utils.setAttribute(field, "maywrite", Utils.selectSingleNodeText(dataContext, mayWriteXPath, "true"));
                            mergeConstraints(field, fieldDataNode);
                            createFormField(form, field, fieldDataNode);
                        } else {
                            String ftype = Utils.getAttribute(field, "ftype");
                            if ("function".equals(ftype)) {
                                log.debug("Not an data node, setting number attribute, because it cannot be found with fdatapath");

                                //set number attribute in field, then you can use it in wizard.xsl
                                Utils.setAttribute(field, "number",   Utils.selectSingleNodeText(dataContext, "object/@number", ""));
                                Utils.setAttribute(field, "maywrite", Utils.selectSingleNodeText(dataContext, "object/@maywrite", "true"));

                                // create the formfield (should be using the current data node ???)
                                createFormField(form, field, fieldDataNode);
                            } else if ("startwizard".equals(ftype) || "wizard".equals(ftype)) {
                                log.debug("A startwizard!");
                                // create the formfield using the current data node
                                createFormField(form, field, dataContext);
                            } else {
                                // throw an exception, but ONLY if the datapath was created from a 'name' attribute
                                // (only in that case can we be sure that the path is faulty - in otehr cases
                                // the path can be valid but point to a related object that is not present)
                                String fname = Utils.getAttribute(field, "name", null);

                                if (fname != null) {
                                    throw new WizardException("Perhaps the field with name '" + fname + "' does not exist (no data node found with " + xpath + ")?");
                                }
                            }
                        }
                    } else
                    // A list "field". Needs special processing.
                    if (nodeName.equals("list")) {
                        NodeList fieldInstances = Utils.selectNodeList(dataContext, xpath);

                        if (fieldInstances == null) {
                            throw new WizardException("The xpath: " + xpath +
                                                      " is not valid. Note: this xpath maybe generated from a <field name='fieldname'> tag. Make sure you use simple valid fieldnames use valid xpath syntax.");
                        }
                        createFormList(form, field, fieldInstances, dataContext);
                    }
                }
            }
        }
    }

    /**
     * This method loads the schema using the properties of the wizard. It loads the wizard using #wizardSchemaFilename,
     * resolves the includes, and 'tags' all datanodes. (Places temp. ids in the schema).
     * @param wizardSchemaFile Url to schema file
     * @throws WizardException when schema loading failed
     */
    private void loadSchema(URL  wizardSchemaFile, HttpServletRequest request) throws WizardException {
        schema = wizardSchemaCache.getDocument(wizardSchemaFile);

        if (schema == null) {
            schema = Utils.loadXMLFile(wizardSchemaFile);

            List<URL> dependencies = resolveIncludes(schema.getDocumentElement(), request);
            resolveShortcuts(schema.getDocumentElement(), true);

            wizardSchemaCache.put(wizardSchemaFile, schema, dependencies);

            log.debug("Schema loaded (and resolved): " + wizardSchemaFile);
        } else {
            log.debug("Schema found in cache: " + wizardSchemaFile);
        }

        // tag schema nodes
        NodeList fields = Utils.selectNodeList(schema, "//field|//list|//item");
        Utils.tagNodeList(fields, "fid", "f", 1);
    }

    /**
     * This method resolves the includes (and extends) in a wizard. It searches for include="" attributes, and searches for extends="" attributes.
     * Include means: the file is loaded (uses the path and assumes it references from the basepath param, and the referenced node
     * is placed 'over' the existing node. Attributes are copied also. Any content in the original node is removed.
     * Extends means: same as include, but now, the original content is not thrown away, and the nodes are placed after the included node.<br />
     * Note: this does not work with teh wizard-schema, and only ADDS objects when overriding (it does not replace them)
     *
     * This method is a recursive one. Included files are also scanned again for includes.
     *
     * @param       node    The node from where to start searching for include and extends attributes.
     * @return list of urls which are included
     * @throws WizardException when included urls failed to load
     * @returns    A list of included files.
     */
    private List<URL> resolveIncludes(Node node, HttpServletRequest request) throws WizardException{
        List<URL> result = new ArrayList<URL>();

        Document targetdoc = node.getOwnerDocument();
        /// resolve blocks
        NodeList blocks= Utils.selectNodeList(node,
                                              "//blocks");
        if (blocks != null) {
            log.debug("" + Collections.list(request.getAttributeNames()));
            HttpServletResponse response = (HttpServletResponse) request.getAttribute(RESPONSE_KEY);

            for (int i = 0; i < blocks.getLength(); i++) {
                Element blockElement = (Element) blocks.item(i);
                Node parent = blockElement.getParentNode();
                log.debug("Resolving blocks " + XMLWriter.write(blockElement));

                for (Block.Type bt : ComponentRepository.getInstance().getBlockClassification(blockElement.getAttribute("classification"))) {
                    String render = blockElement.getAttribute("render").toUpperCase();
                    if ("".equals(render)) render = "BODY";

                    for (Block b : bt.getBlocks()) {
                        Renderer body = b.getRenderer(Renderer.Type.valueOf(render));
                        if (! body.equals(Renderer.Type.valueOf(render).getEmpty(b))) {
                            Parameters params = b.createParameters();
                            log.debug("Including " + b + " " + params);

                            params.setIfDefined(Parameter.REQUEST, request);
                            params.setIfDefined(Parameter.RESPONSE, response);

                            Framework fw = Framework.getInstance();
                            if (fw == null) throw new WizardException("No MMBase Framework found");
                            Parameters frameworkParams = fw.createParameters();
                            frameworkParams.setIfDefined(Parameter.REQUEST, request);
                            frameworkParams.setIfDefined(Parameter.RESPONSE, response);
                            try {
                                Document doc = org.mmbase.framework.Utils.renderToXml(fw, body, params, frameworkParams, WindowState.NORMAL, Wizard.class);
                                parent.insertBefore(targetdoc.importNode(doc.getDocumentElement(), true), blockElement);
                            } catch (FrameworkException fwe) {
                                throw new WizardException(fwe);
                            }
                        }
                    }
                }
                parent.removeChild(blockElement);
            }
        }

        // Resolve references to elements in other wizards. This can be by inclusion
        // or extension.
        NodeList externalReferences = Utils.selectNodeList(node,
                                                           "//*[@include or @extends]");

        if (externalReferences != null) {
            for (int i = 0; i < externalReferences.getLength(); i++) {
                Node referer = externalReferences.item(i);
                boolean inherits = !Utils.getAttribute(referer, "extends", "").equals("");
                String includeUrl = Utils.getAttribute(referer, "include");
                if (inherits) {
                    includeUrl = Utils.getAttribute(referer, "extends");
                }
                // transform attribute, but ignore xpaths
                includeUrl = Utils.transformAttribute(null,includeUrl,false,variables);

                try {
                    // Resolve the filename and form-schema id.
                    String url = includeUrl;
                    String externalId = "not applicable";
                    int hash = includeUrl.indexOf('#');

                    if (hash != -1) {
                        url = includeUrl.substring(0, includeUrl.indexOf('#'));
                        externalId = includeUrl.substring(includeUrl.indexOf('#') +
                                                          1);
                    }

                    URL file;
                    try {
                        file = uriResolver.resolveToURL(url, null);
                    } catch (Exception e) {
                        throw new WizardException(e);
                    }
                    result.add(file);

                    // Load the external file.
                    Document externalDocument = Utils.loadXMLFile(file);

                    if (externalDocument == null) {
                        throw new WizardException("Could not load and parse included file. Filename:" + file);
                    }

                    // Add a copy of the external part to our schema here, to replace the
                    // referer itself.
                    Node externalPart = null;

                    if (hash == -1) {
                        // Load the entire file.
                        externalPart = externalDocument.getDocumentElement();
                    } else if (externalId.startsWith("xpointer(")) {
                        // Load only part of the file, using an xpointer.
                        String xpath = externalId.substring(9, externalId.length() - 1);
                        externalPart = Utils.selectSingleNode(externalDocument, xpath);
                    } else {
                        // Load only the node with the given id.
                        externalPart = Utils.selectSingleNode(externalDocument,
                                                              "//node()[@id='" + externalId + "']");
                    }

                    // recurse!
                    result.addAll(resolveIncludes(externalPart, request));

                    // place loaded external part in parent...
                    Node parent = referer.getParentNode();
                    externalPart = parent.insertBefore(targetdoc.importNode(externalPart, true), referer);

                    // If the old node had some attributes, copy them to the included one.
                    Utils.copyAllAttributes(referer, externalPart);

                    //
                    if (inherits) {
                        NodeList overriders = Utils.selectNodeList(referer, "node()");

                        for (int k = 0; k < overriders.getLength(); k++) {
                            // 'inherit' old nodes. Do not clone, as we are essentially 'moving' these
                            externalPart.appendChild(overriders.item(k));
                        }
                    }

                    // Remove the refering node.
                    parent.removeChild(referer);
                } catch (RuntimeException e) {
                    throw new WizardException("Error resolving external part '" +
                                              includeUrl + "': " + e.getMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * Resolves shortcuts placed in the schema.
     * eg.: if a user just entered <field name="firstname" /> it will be replaced by <field fdatapath="field[@name='firstname']" />
     *
     * later, other simplifying code could be placed here, so that for more simple fdatapath's more simple commands can be used.
     * (maybe we should avoid using xpath in total for normal use of the editwizards?)
     *
     * @param   schemaNode  The schemanode from were to start searching
     * @param   recurse     Set to true if you want to let the process search in-depth through the entire tree, false if you just want it to search the first-level children
     */
    private void resolveShortcuts(Node schemaNode, boolean recurse) {
        String xpath;

        if (recurse) {
            xpath = ".//field|.//list";
        } else {
            xpath = "field|list";
        }

        NodeList children = Utils.selectNodeList(schemaNode, xpath);

        if (children == null) {
            throw new RuntimeException("could not perform xpath:" + xpath + " for schemanode:\n" + schemaNode);
        }

        for (int i = 0; i < children.getLength(); i++) {
            resolveShortcut(children.item(i));
        }

        // if no <steps /> node exist, a default node is created with all form schema's in found order
        if (Utils.selectSingleNode(schemaNode, "steps") == null) {
            Node stepsNode = schemaNode.getOwnerDocument().createElement("steps");
            NodeList forms = Utils.selectNodeList(schemaNode, "form-schema");

            for (int i = 0; i < forms.getLength(); i++) {
                Node formStep = schemaNode.getOwnerDocument().createElement("step");
                String formId = Utils.getAttribute(forms.item(i), "id", null);

                if (formId == null) {
                    formId = "tempformid_" + i;
                    Utils.setAttribute(forms.item(i), "id", formId);
                }

                Utils.setAttribute(formStep, "form-schema", formId);
                stepsNode.appendChild(formStep);
            }

            // TODO: according to the dtd, schemanode should be inserted before any form-schema nodes
            schemaNode.appendChild(stepsNode);
        }
    }

    /**
     * Resolves possible shortcut for this given single node. (@see #resolveShortcuts for more information)
     *
     * @param   singleNode    The node to resolve
     */
    private void resolveShortcut(Node singleNode) {
        // transforms <field name="firstname"/> into <field fdatapath="field[@name='firstname']" />
        String nodeName = singleNode.getNodeName();

        if (nodeName.equals("field")) {
            // field nodes
            String name = Utils.getAttribute(singleNode, "name", null);
            String fdatapath = Utils.getAttribute(singleNode, "fdatapath", null);
            String ftype = Utils.getAttribute(singleNode, "ftype", null);

            if (fdatapath == null) {
                // if no name, select the current node
                if (name == null) {
                    if ("startwizard".equals(ftype) || "wizard".equals(ftype)) {
                        fdatapath = ".";
                    }
                } else {
                    if ("number".equals(name)) {
                        Utils.setAttribute(singleNode, "ftype", "data");

                        // the number field may of course never be edited
                        fdatapath = "@number";
                    } else {
                        fdatapath = "field[@name='" + name + "']";
                    }

                    // normal field or a field inside a list node?
                    Node parentNode = singleNode.getParentNode();
                    String parentname = parentNode.getNodeName();

                    // skip fieldset
                    if (parentname.equals("fieldset")) {
                        parentname = parentNode.getParentNode().getNodeName();
                    }

                    if (parentname.equals("item")) {
                        fdatapath = "object/" + fdatapath;
                    }
                }

                Utils.setAttribute(singleNode, "fdatapath", fdatapath);
            }
        } else if (nodeName.equals("list")) {
            // List nodes
            String role = Utils.getAttribute(singleNode, "role", null); //"insrel");
            String destination = Utils.getAttribute(singleNode, "destinationtype", null);

            // legacy: use destination if destinationtype not given
            if (destination == null) {
                destination = Utils.getAttribute(singleNode, "destination", null);
            }

            String searchString = Utils.getAttribute(singleNode, "searchdir", null);

            StringBuilder fdatapath = null;
            String tmp = Utils.getAttribute(singleNode, "fdatapath", null);
            if (tmp != null) fdatapath = new StringBuilder(tmp);

            if (fdatapath != null) {
                if (searchString != null || role != null || destination != null) {
                    log.warn("When 'fdatapath' is given, it does not make sense to specify the 'searchdir', 'role' or 'destination' attributes. These attributes are ignored. Check wizard: " + wizardName);
                }
            } else {
                // determine role
                fdatapath = new StringBuilder();

                if (role != null) {
                    fdatapath.append("@role='").append(role).append('\'');
                }

                // determine destination type
                if (destination != null) {
                    if (fdatapath.length() != 0) {
                        fdatapath.append(" and ");
                    }
                    // should also include types that inherit...

                    Node con = getConstraints(destination);
                    // and then ???
                    NodeList descendants = null;
                    if (con!=null) descendants = Utils.selectNodeList(con,"descendants/descendant");

                    if (Utils.isEmptyNodeList(descendants)) {
                        fdatapath.append("object/@type='").append(destination).append('\'');
                    } else {

                        fdatapath.append("(object/@type='").append(destination).append('\'');

                        for (int desci = 0; desci < descendants.getLength();desci++) {
                            Node descendant = descendants.item(desci);
                            String descendantName = Utils.getAttribute(descendant, "type", null);
                            fdatapath.append(" or object/@type='").append(descendantName).append('\'');
                        }

                        fdatapath.append(')');

                    }
                }

                // determine searchdir
                int searchDir = RelationStep.DIRECTIONS_BOTH;

                if (searchString != null) {
                    searchDir = Queries.getRelationStepDirection(searchString);
                }

                if (searchDir == RelationStep.DIRECTIONS_SOURCE) {
                    if (fdatapath.length() != 0) {
                        fdatapath.append(" and ");
                    }

                    fdatapath.append("@source=object/@number");
                } else if (searchDir == RelationStep.DIRECTIONS_DESTINATION) {
                    if (fdatapath.length() != 0) {
                        fdatapath.append(" and ");
                    }

                    fdatapath.append("@destination=object/@number");
                }

                fdatapath.insert(0, "relation[");
                fdatapath.append(']');

                // normal list or a list inside a list?
                if (singleNode.getParentNode().getNodeName().equals("item")) {
                    fdatapath.insert(0, "object/");
                }
                Utils.setAttribute(singleNode, "fdatapath", fdatapath.toString());
            }
        }
    }

    private void expandAttribute(Node node, String name, String defaultvalue) {
        String value = Utils.transformAttribute(data.getDocumentElement(),
                                                Utils.getAttribute(node, name, null), false, variables);

        if (value == null) {
            value = defaultvalue;
        }

        if (value != null) {
            Utils.setAttribute(node, name, value);
        }
    }

    private final static Pattern NUMBER_ORDERTYPE = Pattern.compile("(?i).*\\bnumber\\b.*");
    private final static Pattern INVERSE_ORDERTYPE = Pattern.compile("(?i).*\\binverse\\b.*");

    /**
     * Creates a form item (each of which may consist of several single form fields)
     *  for each given datanode.
     * @param form data of form
     * @param fieldlist list of fields
     * @param datalist list of data nodes
     * @param parentdatanode node where data starts
     * @throws WizardException when form creation failed
     */
    private void createFormList(Node form, Node fieldlist, NodeList datalist,
                                Node parentdatanode) throws WizardException {
        // copy all attributes from fielddefinition to new pre-html field definition
        log.debug("creating form list");

        Node newlist = fieldlist.cloneNode(false);
        newlist = form.getOwnerDocument().importNode(newlist, false);
        Utils.copyAllAttributes(fieldlist, newlist);

        // place parent object number as attribute number
        if (parentdatanode != null) {
            Utils.setAttribute(newlist, "number", Utils.getAttribute(parentdatanode, "number"));
        }

        // Add the title, description.
        NodeList props = Utils.selectNodeList(fieldlist,
                                              "title|description|action|command");

        Utils.appendNodeList(props, newlist);

        // Add the title, description from the item as attributes
        NodeList itemTitle = Utils.selectNodeList(fieldlist, "item/title|item/description");

        for (int i=0; i<itemTitle.getLength(); i++) {
            Utils.setAttribute(newlist, "item" + itemTitle.item(i).getNodeName(), Utils.getText(itemTitle.item(i)));
        }

        // expand attribute 'startnodes' for search command
        Node command = Utils.selectSingleNode(newlist, "command[@name='search']");


        if (command != null) {
            expandAttribute(command, "startnodes", null);
            // expand constraints attribute on search action
            String cAttribute = Utils.getAttribute(command, "constraints");
            if(cAttribute != null && !cAttribute.equals("")){
                expandAttribute(command, "constraints", dataId);
            }
        }

        // expand attribute 'objectnumber' en 'origin' for editwizard command
        //command = Utils.selectSingleNode(newlist, "command[@name='startwizard']");
        NodeList commands = Utils.selectNodeList(newlist, "command[@name='startwizard']");
        if (commands != null) {
            for (int i=0; i<commands.getLength(); i++) {
                command = commands.item(i);
                if (command!=null) {
                    expandAttribute(command,"objectnumber","new");
                    expandAttribute(command,"origin",dataId);
                    expandAttribute(command,"wizardname",null);
                }
            }
        }

        String hiddenCommands = "|" +
            Utils.getAttribute(fieldlist, "hidecommand") + "|";

        // place newfield in pre-html form
        form.appendChild(newlist);

        // calculate minoccurs and maxoccurs
        int minoccurs = Integer.parseInt(Utils.getAttribute(fieldlist,
                                                            "minoccurs", "0"));
        int nrOfItems = datalist.getLength();

        int maxoccurs = -1;
        String maxstr = Utils.getAttribute(fieldlist, "maxoccurs", "*");

        if (!maxstr.equals("*")) {
            maxoccurs = Integer.parseInt(maxstr);
        }

        String orderby = Utils.getAttribute(fieldlist, "orderby", null);

        if ((orderby != null) && (orderby.indexOf("@") == -1)) {
            orderby = "object/field[@name='" + orderby + "']";
        }

        final String ordertype = Utils.getAttribute(fieldlist, "ordertype", "string");

        // set the orderby attribute for all the nodes
        List<Element> tempstorage = new ArrayList<Element>(datalist.getLength());

        for (int dataIndex = 0; dataIndex < datalist.getLength(); dataIndex++) {
            Element datacontext = (Element) datalist.item(dataIndex);

            if (orderby != null) {
                String orderByValue = Utils.selectSingleNodeText(datacontext,
                                                                 orderby, "");

                // make sure of type
                if (NUMBER_ORDERTYPE.matcher(ordertype).matches()) {
                    double orderDbl;

                    try {
                        orderDbl = Double.parseDouble(orderByValue);
                    } catch (Exception e) {
                        log.error("fieldvalue [" + orderByValue + "] is not numeric, wizard: " + wizardName);
                        orderDbl = -1;
                    }

                    orderByValue = "" + orderDbl;
                }

                // sets orderby
                datacontext.setAttribute("orderby", orderByValue);
            }

            // clears firstitem
            datacontext.setAttribute("firstitem", "false");

            // clears lastitem
            datacontext.setAttribute("lastitem", "false");
            tempstorage.add(datacontext);
        }

        // sort list
        if (orderby != null) {
            Comparator<Element> comp =  new OrderByComparator(NUMBER_ORDERTYPE.matcher(ordertype).matches(),
                                                     INVERSE_ORDERTYPE.matcher(ordertype).matches());
            Collections.sort(tempstorage, comp);
            if (log.isDebugEnabled()) {
                log.debug("ordered with '" + ordertype + "' " + comp + " " + tempstorage);
            }
        }

        // and make form
        int listsize = tempstorage.size();

        for (int dataindex = 0; dataindex < listsize; dataindex++) {
            Element datacontext = tempstorage.get(dataindex);

            // Select the form item
            Node item = Utils.selectSingleNode(fieldlist, "item");

            if (item == null) {
                item = Utils.selectSingleNode(fieldlist, "item");

                if (item == null) {
                    throw new WizardException("Could not find item in a list of " +
                                              wizardName);
                }

                if (log.isDebugEnabled()) {
                    log.debug("found an item " + item.toString());
                }
            }

            Node newitem = item.cloneNode(false);
            newitem = form.getOwnerDocument().importNode(newitem, false);
            newlist.appendChild(newitem);

            // Copy all attributes from data to new pre-html field def (mainly needed for the did).
            Utils.copyAllAttributes(datacontext, newitem);
            Utils.copyAllAttributes(item, newitem);

            // Add the title, description.
            NodeList itemprops = Utils.selectNodeList(item, "title|description");
            Utils.appendNodeList(itemprops, newitem);

            // and now, do the recursive trick! All our fields inside need to be processed.
            createPreHtmlForm(newitem, item, datacontext);

            // finally, see if we need to place some commands here
            if ( /* nrOfItems > minoccurs && you should be able to replace!*/
                hiddenCommands.indexOf("|delete-item|") == -1) {
                addSingleCommand(newitem, "delete-item", datacontext);
            }

            if (orderby != null) {
                if ((dataindex > 0) && (hiddenCommands.indexOf("|move-up|") == -1)) {
                    addSingleCommand(newitem, "move-up", datacontext,
                                     tempstorage.get(dataindex - 1));
                }

                if (((dataindex + 1) < listsize) &&
                    (hiddenCommands.indexOf("|move-down|") == -1)) {
                    addSingleCommand(newitem, "move-down", datacontext,
                                     tempstorage.get(dataindex + 1));
                }
            }

            if (dataindex == 0) {
                datacontext.setAttribute("firstitem", "true");
            }

            if (dataindex == (tempstorage.size() - 1)) {
                datacontext.setAttribute("lastitem", "true");
            }
        }

        // should the 'save' button be inactive because of this list?
        // works likes this:
        //  If the minoccurs or maxoccurs condiditions are not satisfied, in the 'wizard.xml'
        //  to the form the 'invalidlist' attribute is filled with the name of the guilty list. By wizard.xsl then this value
        //  is copied to the html.
        //
        //  validator.js/doValidateForm returns invalid as long as this invalid list attribute of the html form is not an
        // emptry string.
        if (log.isDebugEnabled()) {
            log.debug("minoccurs:" + minoccurs + " maxoccurs: " + maxoccurs +
                      " items: " + nrOfItems);
        }

        if (((nrOfItems > maxoccurs) && (maxoccurs != -1)) ||
            (nrOfItems < minoccurs)) { // form cannot be valid in that case
            ((Element) newlist).setAttribute("status", "invalid");

            // which list?
            String listTitle = Utils.selectSingleNodeText(fieldlist, "title",
                                                          "some list");
            ((Element) form).setAttribute("invalidlist", listTitle);
        } else {
            ((Element) newlist).setAttribute("status", "valid");
        }

        log.debug("can we place an add-button?");

        if ((hiddenCommands.indexOf("|add-item|") == -1) &&
            ((maxoccurs == -1) || (maxoccurs > nrOfItems)) &&
            ( Utils.selectSingleNode(fieldlist, "action[@type='create']") != null ||
              Utils.selectSingleNode(fieldlist, "action[@type='add']") != null )) {
            String defaultpath = ".";

            if (fieldlist.getParentNode().getNodeName().equals("item")) {
                // this is a list in a list.
                defaultpath = "object";
            }

            String fparentdatapath = Utils.getAttribute(fieldlist,
                                                        "fparentdatapath", defaultpath);
            Node chosenparent = Utils.selectSingleNode(parentdatanode,
                                                       fparentdatapath);

            // try to find out what datanode is the parent of inserts...
            if ((datalist.getLength() > 0) && fparentdatapath.equals(".")) {
                // we have an example and no fparentdatapath was given. So, create a 'brother'
                addSingleCommand(newlist, "add-item",
                                 datalist.item(0).getParentNode());
            } else {
                // no living examples exist. Use the chosenparent
                addSingleCommand(newlist, "add-item", chosenparent);
            }
        }

        log.debug("end");
    }

    /**
     * This method generates a form field node in the pre-html.
     *
     * @param       form        the pre-html form node
     * @param       field       the form definition field node
     * @param       dataNode     the current context data node. It might be 'null' if the field already contains the 'number' attribute.
     * @throws WizardException when form field creation failed
     */
    private void createFormField(Node form, Node field, Node dataNode)
        throws WizardException {
        if (log.isDebugEnabled()) {
            log.debug("Creating form field for " + field + " wizard for obj: " + objectNumber);
        }

        // copy all attributes from fielddefinition to new pre-html field definition
        Node newField = form.getOwnerDocument().createElement("field");

        Utils.copyAllAttributes(field, newField);

        // place newfield in pre-html form
        form.appendChild(newField);

        List<String> exceptAttrs = new ArrayList<String>(); // what is this?
        exceptAttrs.add("fid");

        // copy all attributes from data to new pre-html field def
        if ((dataNode != null) && (dataNode.getNodeType() != Node.ATTRIBUTE_NODE)) {
            Utils.copyAllAttributes(dataNode, newField, exceptAttrs);
        }

        String ftype  = Utils.getAttribute(newField, "ftype");
        String dttype = Utils.getAttribute(newField, "dttype");

        // place html form field name (so that we always know about which datanode and fieldnode we are talking)
        String htmlFieldName = calculateFormName(newField);
        Utils.setAttribute(newField, "fieldname", htmlFieldName);

        // place objectNumber as attribute number, if not already was placed there by the copyAllAttributes method.
        if ((dataNode != null) && (Utils.getAttribute(dataNode, "number", null) == null)) {
            Utils.setAttribute(newField, "number", Utils.getAttribute(dataNode.getParentNode(), "number"));
        }

        // resolve special attributes
        if (ftype.equals("startwizard")) {
            String wizardObjectNumber = Utils.getAttribute(newField, "objectnumber", null);

            // if no objectnumber is found, assign the number of the current field.
            // exception is when the direct parent is a form.
            // in that case, we are editting the current object, so instead assign new
            // note: this latter does not take into account fieldsets!
            if (wizardObjectNumber == null) {
                if (form.getNodeName().equals("form")) {
                    wizardObjectNumber = "new";
                } else {
                    wizardObjectNumber = "{object/@number}";
                }
            }

            // evaluate object number
            wizardObjectNumber = Utils.transformAttribute(dataNode, wizardObjectNumber);

            boolean mayEdit = true;

            if ("new".equals(wizardObjectNumber)) {
                // test whether this number may be created
                // we can't do this now, as we cannot determine the type of node
                // unless we load the wizard.
                // This may be added in a later stage, when loading of wizard templates is
                // moved to a seperate module
                mayEdit = true;
            } else {
                // test whether this number may be edited
                mayEdit = mayEditNode(wizardObjectNumber);
            }

            if (!mayEdit) {
                // remove this field from the form
                form.removeChild(newField);
            }

            Utils.setAttribute(newField, "objectnumber", wizardObjectNumber);

            String wizardPath = Utils.getAttribute(newField, "wizardname", null);
            if (wizardPath != null) {
                wizardPath = Utils.transformAttribute(dataNode, wizardPath);
                Utils.setAttribute(newField, "wizardname", wizardPath);
            }

            String wizardOrigin = Utils.getAttribute(newField, "origin", null);

            if (wizardOrigin == null) {
                wizardOrigin = dataId;
            } else {
                wizardOrigin = Utils.transformAttribute(dataNode, wizardOrigin);
            }

            Utils.setAttribute(newField, "origin", wizardOrigin);
        } else if (!ftype.equals("function")) {
            // check rights - if you can't edit, set ftype to data
            if (!mayEditNode(Utils.getAttribute(newField, "number"))) {
                ftype = "data";
                Utils.getAttribute(newField, "ftype", ftype);
            }
        }

        // binary type needs special processing
        if ("binary".equals(dttype)) {
            addBinaryData(newField);
        }

        NodeList list = Utils.selectNodeList(field, "optionlist|prompt|description|action|prefix|postfix");
        Utils.appendNodeList(list, newField);

        // place value
        // by default, theValue is the text of the node.
        String theValue = "";

        try {
            if (dataNode == null) {
                if (ftype.equals("function")) {
                    theValue = Utils.getAttribute(field, "name");
                    log.debug("Found a function field " + theValue);
                } else if (ftype.equals("startwizard") || ftype.equals("wizard")) {
                    log.debug("found a wizard field");
                } else {
                    log.debug("Probably a new node");
                    throw new WizardException("No datanode given for field " +
                                              theValue + " and ftype does not equal 'function' or 'startwizard'(but " + ftype + ")");
                }
            } else if (dataNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                theValue = dataNode.getNodeValue();
            } else {
                theValue = dataNode.getFirstChild().getNodeValue();
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        }

        // if this is a relation, we want the value of the dnumber field
        if (ftype.equals("relation")) {
            theValue = Utils.getAttribute(newField, "destination");
        }

        if (theValue == null) {
            theValue = "";
        }

        Node value = form.getOwnerDocument().createElement("value");
        Utils.storeText(value, theValue);
        newField.appendChild(value);
    }

    private void addSingleCommand(Node field, String commandname, Node datanode) {
        addSingleCommand(field, commandname, datanode, null);
    }

    private void addSingleCommand(Node field, String commandname, Node datanode,
                                  Node otherdatanode) {
        String otherdid = "";

        if (otherdatanode != null) {
            otherdid = Utils.getAttribute(otherdatanode, "did");
        }

        Element command = field.getOwnerDocument().createElement("command");
        command.setAttribute("name", commandname);
        command.setAttribute("cmd", "cmd/" + commandname + "/" + Utils.getAttribute(field, "fid") + "/" +
                             Utils.getAttribute(datanode, "did") + "/" + otherdid + "/");
        command.setAttribute("value", Utils.getAttribute(datanode, "did"));
        field.appendChild(command);
    }

    /**
     * Returns the proper form-name (for <input name="xx" />).
     *
     * @param       preHtmlFormField        This is the prehtml node where the field data should be
     * @return     A string with the proper html field-name.
     */
    private String calculateFormName(Node preHtmlFormField) {
        try {
            String fid = Utils.getAttribute(preHtmlFormField, "fid");
            String did = Utils.getAttribute(preHtmlFormField, "did");

            return "field/" + fid + "/" + did;
        } catch (RuntimeException e) {
            return "field/fid_or_did_missed_a_tag";
        }
    }

    /**
     * This method de-encodes a html field-name (@see #calculateFormName) and returns an Array with the decoded values.
     * @param formName name of form
     * @return     The array with id's. First id in the array is the data-id (did), which indicates what datanode is pointed to,
     *              second id is the fid (field-id) which points to the proper fieldnode in the wizarddefinition.
     */
    private String[] processFormName(String formName) {
        String[] res = { "", "" };

        boolean isafield = (formName.indexOf("field/") > -1);
        int nr1 = formName.indexOf("/") + 1;
        int nr2 = formName.indexOf("/", nr1) + 1;

        if ((nr1 < 1) || (nr2 < 1) || !isafield) {
            // not good. no 2 slashes found
            return null;
        }

        String fid = formName.substring(nr1, nr2 - 1);
        String did = formName.substring(nr2);
        res[0] = did;
        res[1] = fid;

        return res;
    }

    /**
     * Puts the given value in the right datanode (given by did), depending on the type
     * of the form field.
     *
     * - text,line: the value is stored as text in the datanode.
     * - relation: the value is assumed to be the destination number (dnumber) of the relation.
     *
     * @param  did     The data id where the value should be stored
     * @param  fid     The wizarddefinition field id what applies to this data
     * @param  value   The (String) value what should be stored in the data.
     * @throws WizardException when failed to store data
     */
    private void storeValue(String did, String fid, String value) throws WizardException {
        if (log.isDebugEnabled()) {
            log.debug("String value " + value + " in " + did + " for  field " + fid);
            log.trace("Using data: " +
                      Utils.getSerializedXML(Utils.selectSingleNode(schema,
                                                                    ".//*[@fid='" + fid + "']")));
        }

        String xpath = ".//*[@fid='" + fid + "']/@dttype";
        Node dttypeNode = Utils.selectSingleNode(schema, xpath);

        if (dttypeNode == null) {
            String msg = "No node with fid=" + fid + " could be found";

            if (schema != null) {
                msg += "\nxpath was:" + xpath + " on:\n" + schema.getDocumentElement();
            }

            throw new WizardException(msg);
        }

        String dttype = dttypeNode.getNodeValue();
        xpath = ".//*[@did='" + did + "']";

        Node datanode = Utils.selectSingleNode(data, xpath);

        if (datanode == null) {
            String msg = "Unable to store value for field with dttype " + dttype + ". fid=" + fid + ", did=" + did + ", value=" + value + ", wizard:" + wizardName;

            if (data != null) {
                msg += "\nxpath was:" + xpath + " on:\n" + data.getDocumentElement();
            }

            log.warn(msg);

            return;
        }

        // everything seems to be ok
        if (dttype.equals("binary")) {
            // binaries are stored differently
            if (getBinary(did) != null) {
                Utils.setAttribute(datanode, "href", did);
                Utils.storeText(datanode, getBinaryName(did));
            }
        } else { // default behavior: store content as text
            Utils.storeText(datanode, value);
        }
    }

    /**
     * Puts the given value in the right field (given by name) of the right node (given by did)
     * Assumes a text field.
     *
     * @param  did     The data id of the node
     * @param  fieldName   The name of the field
     * @param  value   The (String) value what should be stored in the data.
     * @throws WizardException when failed to store field data
     */
    public void storeFieldValue(String did, String fieldName, String value) throws WizardException {
        if (log.isDebugEnabled()) {
            log.debug("String value " + value + " in " + did + " for  field " + fieldName);
        }

        String xpath = ".//*[@did='" + did + "']";
        Node objectNode = Utils.selectSingleNode(data, xpath);

        xpath = "./field[@name='" + fieldName + "']";
        Node fieldNode = Utils. selectSingleNode(objectNode, xpath);

        if (fieldNode == null) {
            throw new WizardException("Unable to store value for field with name " + fieldName + " for node with did=" + did + ", value=" + value + ", wizard:" + wizardName);
        }
        Utils.storeText(fieldNode, value);
    }

    /**
     * Obtains the value form the right field (given by name) of the right node (given by did).
     * Assumes a text field.
     *
     * @param  did     The data id of the node
     * @param  fieldName   The name of the field
     * @return value of field
     * @throws WizardException when failed to read field
     */
    public String retrieveFieldValue(String did, String fieldName) throws WizardException {
        if (log.isDebugEnabled()) {
            log.debug("Get value in " + did + " for  field " + fieldName);
        }

        String xpath = ".//*[@did='" + did + "']";
        Node objectNode = Utils.selectSingleNode(data, xpath);

        xpath = "./field[@name='" + fieldName + "']";
        Node fieldNode = Utils. selectSingleNode(objectNode, xpath);

        if (fieldNode == null) {
            throw new WizardException("Unable to store value for field with name " + fieldName + " for node with did=" + did + ", wizard:" + wizardName);
        }

        return Utils.getText(fieldNode);
    }


    /**
     * This method processes the commands sent over http.
     *
     * @param req The ServletRequest where the commands (name/value pairs) reside.
     * @throws WizardException when failed to process command
     */
    private void processCommands(HttpServletRequest req) throws WizardException {
        log.debug("processing commands");
        mayBeClosed = false;
        startWizard = false;
        startWizardCmd = null;

        String commandName = "";

        Enumeration<String> list = req.getParameterNames();

        while (list.hasMoreElements()) {
            commandName = list.nextElement();

            if ((commandName.indexOf("cmd/") == 0) && !commandName.endsWith(".y")) {
                if (log.isDebugEnabled()) {
                    log.debug("found a command " + commandName);
                }

                // this is a command.
                String commandValue = req.getParameter(commandName);
                WizardCommand wc = new WizardCommand(commandName, commandValue);
                processCommand(wc);
            } else {
                if (log.isDebugEnabled()) {
                    log.trace("ignoring non-command " + commandName);
                }
            }
        }

    }

    /**
     * This method is usually called by #processCommands and processes one command.
     * Possible wizard commands are:
     * <ul>
     *   <li>delete-item</li>
     *   <li>update-item</li>
     *   <li>add-item</li>
     *   <li>move-up</li>
     *   <li>move-down</li>
     *   <li>start-wizard</li>
     *   <li>goto-form</li>
     *   <li>cancel</li>
     *   <li>commit</li>
     * </ul>
     * @param cmd The command to be processed
     * @throws WizardException when failed to process command
     *
     */
    public void processCommand(WizardCommand cmd) throws WizardException {
        log.debug("Processing command " + cmd);
        // processes the given command
        switch (cmd.getType()) {
        case WizardCommand.DELETE_ITEM: {
            // delete item!
            // The command parameters is the did of the node to delete.
            // note that a fid parameter is expected in the command syntax but ignored
            String did = cmd.getDid();
            Node dataNode = Utils.selectSingleNode(data, ".//*[@did='" + did + "']");

            if (dataNode != null) {
                // Step one: determine what should eb deleted
                // if an <action name="delete"> exists, and it has an <object> child,
                // the object of the relatiosn should be deleted along with the relation
                // if there is no delete action defined, or object is not a child,
                // only the relation is deleted
                String fid = cmd.getFid();
                Node itemNode = Utils.selectSingleNode(schema, ".//*[@fid='" + fid + "']");
                Node listNode = itemNode.getParentNode();
                Node objectDef = Utils.selectSingleNode(listNode, "action[@type='delete']/object");

                Node dataObjectNode = dataNode;

                if (objectDef != null) {
                    dataObjectNode = Utils.selectSingleNode(dataNode, "object");
                }

                // all child objects of the object to be deleted are added to a repository.
                // these objects are not accessed for editing purposes any more,
                // but do continue to exist in the tree
                // This prevents the included objects from being deleted (deletion of
                // ojects is detected by comparing objects that exist in the original data tree with those
                // in the result data tree)
                Node newRepos = data.createElement("repos");
                NodeList insideObjects = Utils.selectNodeList(dataObjectNode, "object|relation");
                Utils.appendNodeList(insideObjects, newRepos);

                //place repos
                dataNode.getParentNode().appendChild(newRepos);

                //remove relation and inside objects
                dataNode.getParentNode().removeChild(dataNode);
            }

            break;
        }

        case WizardCommand.UPDATE_ITEM: {
            // update an item - replaces all fields of the item with updated values
            // retrieved from MMbase
            // The command parameters is a value indicating the number of the node(s) to update.
            String value = cmd.getValue();
            List<Element> nodesToUpdate = getElementsByNumber(data, value);
            List<Element> originalNodesToUpdate = getElementsByNumber(originalData, value);
            if (originalNodesToUpdate.isEmpty()) {
                originalNodesToUpdate = getElementsByNumber(loadedData, value);
            }

            if (! nodesToUpdate.isEmpty() || ! originalNodesToUpdate.isEmpty()) {
                Node updatedNode = null;

                try {
                    updatedNode = databaseConnector.getDataNode(null, value, null);
                } catch (Exception e) {
                    // hm?
                    break;
                }

                NodeList updatedFields = Utils.selectNodeList(updatedNode, "./field");

                Map<String, String> fieldValues = new HashMap<String, String>();

                for (int j = 0; j < updatedFields.getLength(); j++) {
                    Node fieldNode = updatedFields.item(j);
                    String fieldName = Utils.getAttribute(fieldNode, "name");
                    String fieldValue = Utils.getText(fieldNode);
                    fieldValues.put(fieldName, fieldValue);
                }

                for (Element dataNode : nodesToUpdate) {
                    NodeList fieldsToUpdate = Utils.selectNodeList(dataNode, "./field");

                    for (int j = 0; j < fieldsToUpdate.getLength(); j++) {
                        Node fieldNode = fieldsToUpdate.item(j);
                        String fieldName = Utils.getAttribute(fieldNode, "name");
                        String fieldValue = fieldValues.get(fieldName);
                        Utils.storeText(fieldNode, fieldValue);
                    }
                }

                for (Element dataNode : originalNodesToUpdate) {
                    NodeList fieldsToUpdate = Utils.selectNodeList(dataNode, "./field");

                    for (int j = 0; j < fieldsToUpdate.getLength(); j++) {
                        Node fieldNode = fieldsToUpdate.item(j);
                        String fieldName = Utils.getAttribute(fieldNode, "name");
                        String fieldValue = fieldValues.get(fieldName);
                        Utils.storeText(fieldNode, fieldValue);
                    }
                }
            }

            break;
        }

        case WizardCommand.MOVE_UP:
        case WizardCommand.MOVE_DOWN: {
            // This is in fact a SWAP action (swapping the order-by fieldname), not really move up or down.
            // The command parameters are the fid of the list in which the item falls (determines order),
            // and the did's of the nodes that are to be swapped.
            String fid = cmd.getFid();
            String did = cmd.getDid();
            String otherdid = cmd.getParameter(2);

            // Step one: get the fieldname to swap
            // this fieldname is determined by checking the 'orderby' attribute in a list
            // If there is no orderby attribute, you can't swap (there is no order defined),
            // so nothing happens.
            Node parentnode = Utils.selectSingleNode(schema,
                                                     ".//*[@fid='" + fid + "']");
            String orderby = Utils.getAttribute(parentnode.getParentNode(),
                                                "orderby");

            // step 2: select the nodes and their fieldfs (provide dthey have them)
            // and swap the values.
            // when the list is sorted again the order of the nodes will be changed
            if (orderby != null) {
                // if orderby is only a field name, create an xpath
                if (orderby.indexOf('@') == -1) {
                    orderby = "object/field[@name='" + orderby + "']";
                }

                log.debug("swap " + did + " and " + otherdid + " on " + orderby);

                Node datanode = Utils.selectSingleNode(data,
                                                       ".//*[@did='" + did + "']/" + orderby);

                if (datanode != null) {
                    // find other datanode
                    Node othernode = Utils.selectSingleNode(data,
                                                            ".//*[@did='" + otherdid + "']/" + orderby);

                    // now we gotta swap the value of them nodes.. (must be strings).
                    if (othernode != null) {
                        String datavalue = Utils.getText(datanode);
                        String othervalue = Utils.getText(othernode);
                        Utils.storeText(othernode, datavalue);
                        Utils.storeText(datanode, othervalue);
                    }
                }
            }

            break;
        }

        case WizardCommand.START_WIZARD: {
            // this involves a redirect and is handled by the jsp pages
            startWizard = true;
            startWizardCmd = cmd;

            break;
        }

        case WizardCommand.GOTO_FORM: {
            // The command parameters is the did of the form to jump to.
            // note that a fid parameter is expected in the command syntax but ignored
            currentFormId = cmd.getDid();

            break;
        }

        case WizardCommand.ADD_ITEM: {
            // The command parameters are the fid of the list in which the item need be added,
            // the did of the object under which it should be added (the parent node),
            // and a second id, indicating the object id to add.
            // The second id can be passed either as a paremeter (the 'otherdid' parameter), in
            // which case it involves a newly created item, OR as a value, in which case it is an
            // enumerated list of did's, the result of a search.
            //
            String fid = cmd.getFid();
            String did = cmd.getDid();
            String value = cmd.getValue();

            if (log.isDebugEnabled()) {
                log.debug("Adding item fid: " + fid + " did: " + did + " value: " + value);
            }

            if ((value != null) && !value.equals("")) {
                log.debug("no value");

                int createOrder = 1;
                StringTokenizer ids = new StringTokenizer(value, "|");

                while (ids.hasMoreElements()) {
                    addListItem(fid, did, ids.nextToken(), false, createOrder);
                    createOrder++;
                }
            } else {
                String otherdid = cmd.getParameter(2);

                if (otherdid.equals("")) {
                    otherdid = null;
                }

                addListItem(fid, did, otherdid, true, 1);
            }

            break;
        }

        case WizardCommand.CANCEL: {
            // This command takes no parameters.
            mayBeClosed = true;

            break;
        }
        case WizardCommand.SAVE : {
            log.debug("Wizard " + objectNumber + " will be saved (but not closed)");
        }

        case WizardCommand.COMMIT: {
            log.debug("Committing wizard " + objectNumber);

            // This command takes no parameters.
            if (log.isDebugEnabled()) {
                log.debug("orig: " + Utils.stringFormatted(originalData));
                log.debug("new orig: " + Utils.stringFormatted(data));
            }

            Element results = databaseConnector.put(originalData, loadedData, data, binaries);

            // find the (new) objectNumber and store it.
            String oldNumber = Utils.selectSingleNodeText(data, ".//object/@number", null);

            // select the 'most outer' object.
            if (log.isDebugEnabled()) {
                log.trace("results : " + results);
                log.debug("found old number " + oldNumber);
            }

            // in the result set the new objects are just siblings, so the new 'wizard number' must be found with this
            // xpath
            String newNumber = Utils.selectSingleNodeText(results, ".//object[@oldnumber='" + oldNumber + "']/@number", null);

            if (log.isDebugEnabled()) {
                log.debug("found new wizard number " + newNumber);
            }

            if (newNumber != null) {
                objectNumber = newNumber;
            }

            committed = true;
            mayBeClosed = (cmd.getType() == WizardCommand.COMMIT);

            if (!mayBeClosed) {

                // if we continue editing the xml's documents should be fixed for the new situation.
                if (log.isDebugEnabled()) {
                    log.trace("Using data was " + Utils.getSerializedXML(originalData));
                    log.trace("is " + Utils.getSerializedXML(data));
                }

                // copy data to original data.
                // makes sense but we don't get the new object numbers like that.


                if (newNumber != null) {
                    dataId = newNumber;
                }

                // try reload:
                loadData();


            }

            break;
        }
        default: {
            log.warn("Received an unknown wizard command '" + cmd.getValue() + "'");
        }
        }
    }

    /**
     * This method adds a listitem. It is used by the #processCommand method to add new items to a list. (Usually when the
     * add-item command is fired.)
     * Note: this method can only add new relations and their destinations!.
     * For creating new objects, use WizardDatabaseConnector.createObject.
     *
     * @param listId  the id of the proper list definition node, the list that issued the add command
     * @param subDataId  The did (dataid) of the anchor (parent) where the new node should be created
     * @param destinationId   The new destination
     * @param isCreate is create action
     * @param createOrder ordernr under which this item is added ()i.e. when adding more than one item to a
     *                    list using one add-item command). The first ordernr in a list is 1
     * @return The new relation.
     * @throws WizardException when failed to add item
     */
    private Node addListItem(String listId, String subDataId, String destinationId,
                             boolean isCreate, int createOrder) throws WizardException {
        log.debug("Adding list item");

        // Determine which list issued the add-item command, so we can get the create code from there.
        Node listNode = Utils.selectSingleNode(schema, ".//list[@fid='" + listId + "']");
        Node relationDefinition = null;

        // action=add is for search command
        if (!isCreate) {
            relationDefinition = Utils.selectSingleNode(listNode, "action[@type='add']/relation");
        }

        // action=create is for create command
        // (this should be an 'else', but is supported for 'search' for old xsls)
        if (relationDefinition == null) {
            relationDefinition = Utils.selectSingleNode(listNode, "action[@type='create']/relation");
        }


        if (relationDefinition == null) { // still null?
            throw new WizardException("Could not find action (add or create) to add a item to list with id " + listId);
        }

        relationDefinition = relationDefinition.cloneNode(true); // why is this necessary?

        if (log.isDebugEnabled()) {
            log.debug("Creating object " + relationDefinition.getNodeName() + " type " + Utils.getAttribute(relationDefinition, "type"));
        }

        // Put the value from the command in that object-definition.
        if (destinationId != null) {
            Utils.setAttribute(relationDefinition, "destination", destinationId);
        }

        // We have to add the object to the data, so first determine to which parent it belongs.
        Node parent = Utils.selectSingleNode(data, ".//*[@did='" + subDataId + "']");



        // Ask the database to create that object, and return it.
        Node newRelation =  databaseConnector.createObject(data, parent, relationDefinition, variables, createOrder, loadedData);

        // reload the data, there may be sub-list-data to be reloaded.
        if (destinationId != null) {
            Node newRelatedNode = Utils.selectSingleNode(newRelation, "object");
            if (newRelatedNode != null) {
                String relatedType = Utils.selectSingleNodeText(newRelatedNode, "@type", null);

                Node loadAction = Utils.selectSingleNode(schema.getDocumentElement(), "action[@type='load']/relation[@destination='" + relatedType + "']/object");

                if (loadAction != null) {
                    Collection<Node> newSubRelations = databaseConnector.loadRelations(newRelatedNode, destinationId, loadAction);
                    // newly loaded objects must be marked.

                    Iterator<Node> i = newSubRelations.iterator();
                    while (i.hasNext()) {
                        Node newSubRelation = (Node) i.next();
                        loadedData.getDocumentElement().appendChild(loadedData.importNode(newSubRelation.cloneNode(true), true));
                    }

                } else {
                    log.debug("Nothing found to load");
                }
            } else {

                throw new WizardException("Could not find relatednode " + Utils.getXML(newRelation));
            }
        }
        return newRelation;

    }

    /**
     * With this method you can store a binary in the wizard.
     *
     * @param       did     This is the dataid what points to in what field the binary should be stored, once commited.
     * @param       bytes    This is a bytearray with the data to be stored.
     * @param       name    This is the name which will be used to show what file is uploaded.
     * @param       path    The (local) path of the file placed.
     */
    public void setBinary(String did, byte[] bytes, String name, String path) {
        setBinary(did, bytes, name, path, null);
    }

    /**
     * set binary data for uploaded bytes
     * @param       did     This is the dataid what points to in what field the binary should be stored, once commited.
     * @param       bytes    This is a bytearray with the data to be stored.
     * @param       name    This is the name which will be used to show what file is uploaded.
     * @param       path    The (local) path of the file placed.
     * @param type Content-type of the byte (or null). If not null, then the fields 'mimetype',
     *             'size' and 'filename' are filled as well.
     * @since MMBase-1.7.2
     */
    public void setBinary(String did, byte[] bytes, String name, String path, String type) {
        binaries.put(did, bytes);
        binaryNames.put(did, name);
        binaryPaths.put(did, path);

        if (type != null) {
            Node mimetypeField = Utils.selectSingleNode(data, "//object[field/@did = '" + did + "']/field[@name='mimetype']");
            if (mimetypeField != null) {
                Utils.storeText(mimetypeField, type);
            }
            Node sizeField = Utils.selectSingleNode(data, "//object[field/@did = '" + did + "']/field[@name='size']");
            if (sizeField != null && bytes != null) {
                Utils.storeText(sizeField, "" + bytes.length);
            }
            Node fileNameField = Utils.selectSingleNode(data, "//object[field/@did = '" + did + "']/field[@name='filename']");
            if (fileNameField != null && name != null) {
                Utils.storeText(fileNameField, name);
            }
        }

    }



    /**
     * This method allows you to retrieve the data of a temporarily stored binary.
     *
     * @param       did     The dataid of the binary you want.
     * @return     the binary data, if found.
     */
    public byte[] getBinary(String did) {
        return binaries.get(did);
    }

    /**
     * With this method you can retrieve the binaryname of a placed binary.
     *
     * @param       did     The dataid of the binary you want.
     * @return     The name as set when #setBinary was used.
     */
    public String getBinaryName(String did) {
        return binaryNames.get(did);
    }

    /**
     * With this method you can retrieve the binarypath of a placed binary.
     *
     * @param       did     The dataid of the binary you want.
     * @return     The path as set when #setBinary was used.
     */
    public String getBinaryPath(String did) {
        return binaryPaths.get(did);
    }

    /**
     * This method stores binary data in the data, so that the information can be used by the html.
     * This method is called from the form creating code.
     *
     * @param       fieldnode       the fieldnode where the binary data information should be stored.
     */
    public void addBinaryData(Node fieldnode) {
        // add's information about the possible placed binaries in the fieldnode.
        // assumes this field is an binary-field
        String did = Utils.getAttribute(fieldnode, "did", null);

        if (did != null) {
            byte[] binary = getBinary(did);

            if (binary != null) {
                // upload
                Node binarynode = fieldnode.getOwnerDocument().createElement("upload");
                Utils.setAttribute(binarynode, "uploaded", "true");
                Utils.setAttribute(binarynode, "size", binary.length + "");
                Utils.setAttribute(binarynode, "name", getBinaryName(did));

                String path = getBinaryPath(did);
                Utils.createAndAppendNode(binarynode, "path", path);
                fieldnode.appendChild(binarynode);
            }
        }
    }

    /**
     * This method is used to merge MMBase specific constraints with the values placed in the Wizard definition.
     * For now, it checks requiredness and datatypes.
     *
     * It connects to the Dove, gets the constraints (from cache or not) and merges the values.
     *
     * @param       fieldDef        the fielddefinition as placed in the wizardschema (==definition)
     * @param       fieldNode       The fieldnode points to the datanode. (This is needed to find out what datatype this field is about).
     */
    public void mergeConstraints(Node fieldDef, Node fieldNode) {
        // load all constraints + merge them with the settings in the schema definition
        //
        if (fieldNode == null) {
            log.warn("Tried mergeContraints on fieldNode which is null. FielDef: " +
                     Utils.getXML(fieldDef));
        }

        String objectType = Utils.getAttribute(fieldNode.getParentNode(), "type",
                                               null);
        String fieldName = Utils.getAttribute(fieldNode, "name", null);

        if ((objectType == null) || (fieldName == null)) {
            if (log.isDebugEnabled()) {
                log.debug("wizard.mergeConstraints: objecttype or fieldname could not be retrieved for this field. Field:");
                log.debug(Utils.getXML(fieldNode));
            }

            return;
        }

        Node con = getConstraints(objectType, fieldName);

        if (con == null) {
            return; // no constraints found. so forget it.
        }

        String xmlSchemaType = null;
        String guiType = Utils.selectSingleNodeText(con, Dove.GUITYPE, "string/line");
        int pos = guiType.indexOf("/");

        if (pos != -1) {
            xmlSchemaType = guiType.substring(0, pos);
            guiType = guiType.substring(pos + 1);
        }

        String required = Utils.selectSingleNodeText(con,    Dove.REQUIRED, "false");
        String guiName = Utils.selectSingleNodeText(con,     Dove.GUINAME, "");
        String description = Utils.selectSingleNodeText(con, Dove.DESCRIPTION, "");
        String maxLength = Utils.selectSingleNodeText(con,   Dove.MAXLENGTH, "-1");

        // dttype?
        String ftype = Utils.getAttribute(fieldDef, "ftype", null);
        String dttype = Utils.getAttribute(fieldDef, "dttype", null);
        Node prompt = Utils.selectSingleNode(fieldDef, "prompt");
        Node descriptionTag = Utils.selectSingleNode(fieldDef, "description");

        if (dttype == null) {
            // import xmlSchemaType (dttype)
            // note :
            // Dove currently returns the following XML Schema base types (and their possible constraints):
            // - string (minLength, maxLength)
            // - float
            // - double
            // - date
            // - time
            //
            // Dove returns the following XML Schema derived types:
            // - int
            // - long
            //
            // Dove returns the following Non-XML Schema conformant types:
            // - binary (minLength, maxLength)
            // - datetime
            // - boolean
            //
            // The 'binary' type can be defined as :
            //  <xsd:simpleType name="binary">
            //      <xsd:restriction base='anyURI' />
            //  </xsd:simpleType>
            //
            // The 'datetime' type can be defined as :
            //  <xsd:simpleType name="datetime">
            //      <xsd:restriction base='dateTime' />
            //  </xsd:simpleType>
            //
            // Finally, Dove may send other, non-standard, types, depending on the formation of guitype in the builder xml.
            //
            dttype = xmlSchemaType;

            if (log.isDebugEnabled()) {
                log.debug("dttype was null, setting to " + xmlSchemaType);
            }
        }

        if (ftype == null) {
            // import guitype or ftype
            // this is a qualifier, not a real type
            //
            ftype = guiType;
        }

        // backward compatibility.
        // switch old 'upload' to 'binary'
        // The old format used the following convention:
        //  ftype="upload" + dttype="image"  ->  upload an image
        //  ftype="upload" + dttype="upload"  -> upload a file
        //  ftype="image"  -> display an image
        // The new format usesd 'binary' a s a dftytype,a nd 'image' or 'file' as a ftype,
        // as follows:
        //  ftype="image" + dttype="binary"  ->  upload an image
        //  ftype="file" + dttype="binary"  -> upload a file
        //  ftype="image" + dttype="data" -> display an image
        //  ftype="file" + dttype="data" -> display a link to a file
        // code below changes old format wizards to the new format.
        if ("upload".equals(ftype)) {
            if ("image".equals(dttype)) {
                ftype = "image";
                dttype = "binary";
            } else {
                ftype = "file";
                dttype = "binary";
            }
        } else if ("image".equals(ftype)) {
            // check if dttype is binary, else set to data
            if (!"binary".equals(dttype)) {
                dttype = "data";
            }
        }

        // in the old format, ftype was date, while dttype was date,datetime, or time
        // In the new format, this is reversed (dttype contains the base datatype,
        // ftype the format in which to enter it)
        // since wizards only understand formats 'date', 'time', 'duartion' and 'datetime',
        // 'ftype' values of new date guitypes (such as new datatypes) need to be converted to
        // datetime
        if (!"data".equals(ftype)) {
            if ("date".equals(dttype) || "time".equals(dttype) || "duration".equals(dttype)) {
                ftype = dttype;
                dttype = "datetime";
            } else if ("datetime".equals(dttype) &&
                       (!"date".equals(ftype) && !"time".equals(ftype) && !"duration".equals(ftype))) {
                ftype = "datetime";
            }
        }

        if ("enum".equals(ftype)) {

            // create normal formfield.
            String nodeManager = Utils.getAttribute(fieldNode, "nodemanager");
            String xpath = "../optionlist[@name = '_" + nodeManager + "_" +   fieldName + "']";
            NodeList optionlist = Utils.selectNodeList(fieldNode, xpath);
            if (optionlist.getLength() > 0 && Utils.selectNodeList(fieldDef, "optionlist").getLength() == 0) {
                Utils.appendNodeList(optionlist, fieldDef);
                ((Element) optionlist.item(0)).removeAttribute("name");
                ((Element) optionlist.item(0)).removeAttribute("select");
            }
        }

        // in the old format, 'html' could also be assigned to dttype
        // in the new format this is an ftype (the dttype is string)
        if ("html".equals(dttype)) {
            ftype = "html";
            dttype = "string";
        }

        // add guiname as prompt
        if (prompt == null) {
            Utils.createAndAppendNode(fieldDef, "prompt", guiName);
        }

        // add description as helptext
        if (descriptionTag == null) {
            Utils.createAndAppendNode(fieldDef, "description", description);
        }

        // process requiredness
        //
        String dtrequired = Utils.getAttribute(fieldDef, "dtrequired", null);

        if (dtrequired == null) {
            // if unknown, determine requiredness according to MMBase
            dtrequired = required;
        }

        // fix for old format type 'wizard'
        if ("wizard".equals(ftype)) {
            ftype = "startwizard";
        }

        // store new attributes in fielddef
        Utils.setAttribute(fieldDef, "ftype", ftype);
        Utils.setAttribute(fieldDef, "dttype", dttype);

        if (dtrequired != null) {
            Utils.setAttribute(fieldDef, "dtrequired", dtrequired);
        }

        // process min/maxlength for strings
        if ("string".equals(dttype) || "html".equals(dttype)) {
            String dtminlength = Utils.getAttribute(fieldDef, "dtminlength", null);

            if (dtminlength == null) {
                // manually set minlength if required is true
                if ("true".equals(dtrequired)) {
                    Utils.setAttribute(fieldDef, "dtminlength", "1");
                }
            }

            String dtmaxlength = Utils.getAttribute(fieldDef, "dtmaxlength", null);

            if (dtmaxlength == null) {
                int maxlen = -1;

                try {
                    maxlen = Integer.parseInt(maxLength);
                } catch (NumberFormatException e) {
                }

                // manually set maxlength if given
                // ignore sizes smaller than 1
                if (maxlen > 0) {
                    Utils.setAttribute(fieldDef, "dtmaxlength", "" + maxlen);
                }
            }
        }
    }

    /**
     * This method gets the MMBase constraints.
     *
     * @param       objecttype      The name of the object, eg. images, jumpers, urls, news
     * @return constrainra from mmbase
     */
    public Node getConstraints(String objecttype) {
        return getConstraints(objecttype, null);
    }

    /**
     * This method gets the MMBase constraints. It also handles the internal constraints cache.
     *
     * @param       objecttype      The name of the object, eg. images, jumpers, urls, news
     * @param       fieldname       The name of the field, eg. title, body, start
     * @return constrainra from mmbase
     */
    public Node getConstraints(String objecttype, String fieldname) {
        // check if constraints are in repository, if so, return thatone,
        // otherwise, retrieve+store+and return the contraints received from the Dove.
        Node con = Utils.selectSingleNode(constraints,
                                          "/*/getconstraints[@type='" + objecttype + "']");

        if (con == null) {
            // objecttype not in repository. Load from MMBase.
            try {
                con = databaseConnector.getConstraints(objecttype);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }

            if (con == null) {
                // something is wrong.
                log.debug("wizard.getConstraints: Could not retrieve MMBase constraints for objecttype:" + objecttype);

                return null;
            }

            // store in repository.
            con = constraints.importNode(con.cloneNode(true), true);
            constraints.getDocumentElement().appendChild(con);
        }

        // no fieldname supplied? return total info node..
        if (fieldname == null) {
            return con;
        }

        // find field declaration
        Node fieldcon = Utils.selectSingleNode(con, "fields/field[@name='" + fieldname + "']");

        return fieldcon;
    }

    /**
     * @since MMBase 1.9.5
     */
    protected List<Element> getElementsByNumber(Document d, String number) {
        Map<String, List<Element>> index = indexes.get(d);
        if (index == null) {
            index = new HashMap<String, List<Element>>();
            NodeList nl = Utils.selectNodeList(d, "//object|//relation");
            for (int i = 0; i < nl.getLength(); i++) {
                Element n = (Element) nl.item(i);
                String atvalue = n.getAttribute("number");
                if (atvalue.length() > 0) {
                    List<Element> list = index.get(atvalue);
                    if (list == null) {
                        list = new ArrayList<Element>();
                        index.put(atvalue, list);
                    }
                    list.add(n);
                }
            }

            indexes.put(d, index);
        }
        List<Element> result = index.get(number);
        if (result == null) {
            // happens for new nodes.
            result = new ArrayList<Element>();
            NodeList nl = Utils.selectNodeList(d, "//object[@number='" + number + "']|//relation[@number='" + number + "']");
            for (int i = 0; i < nl.getLength(); i++) {
                result.add((Element)nl.item(i));
            }
            index.put(number, result);
        }
        return result;
    }


    class OrderByComparator implements Comparator<Element> {
        final boolean compareByNumber;
        final int inverse;

        OrderByComparator(boolean numeric, boolean i) {
            compareByNumber = numeric;
            inverse = i ? -1 : 1;
        }

        public String toString() {
            return (inverse == -1 ? "inverse " : "") + (compareByNumber ? "number" : "string");
        }

        public int compare(Element n1, Element n2) {
            // Determine the orderby values and compare
            // store it??
            String order1 = n1.getAttribute("orderby");
            String order2 = n2.getAttribute("orderby");

            //this means it we want evaludate the value as a number
            if (compareByNumber) {
                try {
                    return inverse * Double.valueOf(order1).compareTo(Double.valueOf(order2));
                } catch (Exception e) {
                    log.error("Invalid field values (" + order1 + "/" + order2 + "):" + e);

                    return 0;
                }
            } else {
                return inverse * order1.compareToIgnoreCase(order2);
            }
        }
    }

    /**
     * Caches File to  Editwizard schema Document.
     * @since MMBase-1.6.4
     */
    private static class WizardSchemaCache extends Cache<URL, Wizard.WizardSchemaCache.Entry> {
        WizardSchemaCache() {
            super(100);
        }

        public String getName() {
            return "Editwizard schemas";
        }

        public String getDescription() {
            return "File -> Editwizard schema Document (resolved includes/shortcuts)";
        }

        synchronized public Entry put(URL f, Document doc, List<URL> dependencies) {
            Entry retval = super.get(f);

            if (retval != null) {
                return retval;
            }

            return super.put(f, new Entry(f, doc, dependencies));
        }

        synchronized public Entry remove(URL file) {
            Entry entry = get(file);

            if ((entry != null) && (entry.fileWatcher != null)) {
                entry.fileWatcher.exit();
            } else {
                log.warn("entry: " + entry);
            }

            return super.remove(file);
        }

        synchronized public Document getDocument(URL key) {
            Entry entry = super.get(key);

            if (entry == null) {
                return null;
            }

            return entry.doc;
        }

        private class Entry {
            Document doc; // the document.
            URL file; //the file belonging to this document (key of cache)

            /**
             * Cache entries must be invalidated if (one of the) file(s) changes.
             */
            ResourceWatcher fileWatcher = new ResourceWatcher(ResourceLoader.getWebRoot()) {
                    public void onChange(String u) {
                        // invalidate this cache entry
                        WizardSchemaCache.this.remove(Entry.this.file);
                        // stop watching files
                    }
                };

            Entry(URL f, Document doc, List<URL> dependencies) {
                this.file = f;
                this.doc = doc;
                try {
                    fileWatcher.add(f);
                } catch (UnsupportedOperationException uoe) {
                    // never mind
                }

                for (URL ff : dependencies) {
                    fileWatcher.add(ff);
                }
                fileWatcher.start();
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        schema = ((DocumentSerializable) in.readObject()).getDocument();
        data = ((DocumentSerializable) in.readObject()).getDocument();
        originalData = ((DocumentSerializable) in.readObject()).getDocument();
        constraints = ((DocumentSerializable) in.readObject()).getDocument();
        String u = in.readUTF();
        wizardStylesheetFile = ResourceLoader.getWebRoot().getResource(u);
        log.service("Deserialized wizard " + this);
    }
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(new DocumentSerializable(schema));
        out.writeObject(new DocumentSerializable(data));
        out.writeObject(new DocumentSerializable(originalData));
        out.writeObject(new DocumentSerializable(constraints));
        out.writeUTF(wizardStylesheetFile.toString());

    }

}
