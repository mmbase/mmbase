/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.io.File;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;

import org.mmbase.bridge.*;

import org.mmbase.cache.*;

import org.mmbase.datatypes.DataTypeCollector;

import org.mmbase.module.corebuilders.*;

import org.mmbase.core.*;
import org.mmbase.core.event.*;
import org.mmbase.core.util.Fields;
import org.mmbase.core.util.StorageConnector;

import org.mmbase.datatypes.DataType;

import org.mmbase.storage.StorageException;
import org.mmbase.storage.StorageNotFoundException;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;

import org.mmbase.util.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This class is the base class for all builders.
 * It offers a list of routines which are useful in maintaining the nodes in the MMBase
 * object cloud.
 * <br />
 * Builders are the core of the MMBase system. They create, delete and search the MMObjectNodes.
 * Most manipulations concern nodes of that builders type. However, a number of retrieval routines extend
 * beyond a builders scope and work on the cloud in general, allowing some ease in retrieval of nodes.
 * The basic routines in this class can be extended to handle more specific demands for nodes.
 * Most of these 'extended builders' will be stored in mmbase.org.builders or mmbase.org.corebuilders.
 * Examples include relation builders or builders for handling binary data such as images.
 * The various builders are registered by the 'TypeDef' builder class (one of the core builders, itself
 * an extension of this class).
 *
 * @author Daniel Ockeloen
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @author Eduard Witteveen
 * @author Johannes Verelst
 * @author Rob van Maris
 * @author Michiel Meeuwissen
 * @author Ernst Bunders
 * @version $Id: MMObjectBuilder.java,v 1.381 2006-06-06 19:44:13 michiel Exp $
 */
public class MMObjectBuilder extends MMTable implements NodeEventListener, RelationEventListener {

    /**
     * Name of the field containing the object number, which uniquely identifies the node.
     * @since MMBase-1.8
     */
    public static final String FIELD_NUMBER = "number";

    /**
     * Name of the field containing the owner. The owner field is used for security implementations.
     * @since MMBase-1.8
     */
    public static final String FIELD_OWNER = "owner";

    /**
     * Name of the field containing the object type number. This refers to an entry in the 'typedef' builder table.
     * @since MMBase-1.8
     */
    public static final String FIELD_OBJECT_TYPE = "otype";

    /**
     * @since MMBase-1.8
     */
    public static final String TMP_FIELD_NUMBER = "_number";
    static final String TMP_FIELD_EXISTS = "_exists";

    /**
     * Default (system) owner name for the owner field.
     * @since MMBase-1.8
     */
    public static final String SYSTEM_OWNER = "system";

    /** Default size of the temporary node cache */
    public final static int TEMPNODE_DEFAULT_SIZE = 1024;

    /** Default replacements for method getHTML() */
    public final static String DEFAULT_ALINEA = "<br />&#160;<br />";
    public final static String DEFAULT_EOL = "<br />";

    public final static int EVENT_TYPE_LOCAL = 0;
    public final static int EVENT_TYPE_REMOTE = 1;

    /**
     * Parameters for the age function
     * @since MMBase-1.7
     */
    public final static Parameter[] AGE_PARAMETERS = {};

    /**
     * Collection for temporary nodes,
     * Used by the Temporarynodemanager when working with transactions
     * The default size is 1024.
     * @duplicate use Cache object instead
     * @scope protected
     */
    public static Map temporaryNodes = new Hashtable(TEMPNODE_DEFAULT_SIZE);


    /**
     * The cache for all blobs.
     * @since 1.8.0
     */
    protected static BlobCache genericBlobCache = new BlobCache(200) {
            public String getName()        { return "GenericBlobCache"; }
        };

    static {
        genericBlobCache.putCache();
    }

    /**
     * The cache that contains the X last requested nodes
     */
    protected static org.mmbase.cache.NodeCache nodeCache = org.mmbase.cache.NodeCache.getCache();
    /**
     * Default output when no data is available to determine a node's GUI description
     */
    static String GUI_INDICATOR = "no info";

    /**
     * Determines whether the cache is locked.
     * A locked cache can be read, and nodes can be removed from it (allowing it to
     * clean invalid nodes), but nodes cannot be added.
     * Needed for committing nodes from transactions.
     */
    private static int cacheLocked = 0;

    private static final Logger log = Logging.getLoggerInstance(MMObjectBuilder.class);

    /**
     * The string that can be used inside the builder.xml as property,
     * to define the maximum number of nodes to return.
     */
    private static String MAX_NODES_FROM_QUERY_PROPERTY = "max-nodes-from-query";

    /**
     * The string that can be used inside the builder.xml as property,
     * to set whether the builder broadcasts changes to nodes to eventlisteners.
     */
    private static String BROADCAST_CHANGES_PROPERTY = "broadcast-changes";

    /**
     * Description of the builder in the currently selected language
     * Not that the first time the builder is created, this value is what is stored in the TypeDef table.
     * @scope protected
     */
    public String description="Base Object";

    /**
     * Descriptions of the builder per language
     * Can be set with the &lt;descriptions&gt; tag in the xml builder file.
     * @scope protected
     */
    public Hashtable descriptions;

    /**
     * The default search age for this builder.
     * Used for intializing editor search forms (see HtmlBase)
     * Default value is 31. Can be changed with the &lt;searchage&gt; tag in the xml builder file.
     * @scope protected
     */
    public String searchAge="31";

    /**
     * Determines whether changes to this builder need be broadcasted to other known mmbase servers.
     */
    protected boolean broadCastChanges = true;

    /**
     * Internal (instance) version number of this builder.
     */
    protected long internalVersion = -1;

    /**
     * The current builder's object type
     * Retrieved from the TypeDef builder.
     */
    protected int oType = -1;

    /**
     *  Maintainer information for builder registration
     *  Set with &lt;builder maintainer="mmbase.org" version="0"&gt; in the xml builder file
     */
    String maintainer = "mmbase.org";

    /** Collections of (GUI) names (singular) for the builder's objects, divided by language
     */
    Hashtable singularNames;

    /** Collections of (GUI) names (plural) for the builder's objects, divided by language
     */
    Hashtable pluralNames;

    /**
     * Full filename (path + buildername + ".xml") where we loaded the builder from
     * It is relative from the '/builders/' subdir
     */
    String xmlPath = "";

    /**
     * Parameters for the GUI function
     * @since MMBase-1.7
     */
    public final static Parameter[] GUI_PARAMETERS = {
        Parameter.FIELD,
        Parameter.LANGUAGE,
        new Parameter("session", String.class),
        Parameter.RESPONSE,
        Parameter.REQUEST,
        Parameter.LOCALE,
        new Parameter("stringvalue", String.class)
        //new Parameter("length", Integer.class),
        //       field, language, session, response, request) Returns a (XHTML) gui representation of the node (if field is '') or of a certain field. It can take into consideration a http session variable name with loging information and a language");

    };
    /**
     * The famous GUI function as a function object.
     * @since MMBase-1.8
     */
    protected Function guiFunction = new NodeFunction("gui", GUI_PARAMETERS, ReturnType.STRING) {
            protected Object getFunctionValue(Node node, Parameters parameters) {
                if (log.isDebugEnabled()) {
                    log.debug("GUI of builder with " + parameters);
                }
                String fieldName = (String) parameters.get(Parameter.FIELD);
                if (fieldName != null && (! fieldName.equals("")) && parameters.get("stringvalue") == null) {
                    if (node.getSize(fieldName) < 2000) {
                        parameters.set("stringvalue", node.getStringValue(fieldName));
                    }
                }
                MMObjectNode n = (MMObjectNode) parameters.get(Parameter.CORENODE);
                return MMObjectBuilder.this.getGUIIndicator(n, parameters);
            }
        };
    {
        addFunction(guiFunction);
    }
    /**
     * Parameters constants for the NodeFunction {@link #wrapFunction}.
     * @since MMBase-1.8
     */
    protected final static Parameter[] WRAP_PARAMETERS = {
        new Parameter(Parameter.FIELD, true),
        new Parameter("length", Number.class, new Integer(20))
    };

    /**
     * This function wraps the text of a node's field and returns the result as a String.
     * It takes as parameters a fieldname, the line length to wrap, and the Node containing the data.
     * This function can be called through the function framework.
     * @since MMBase-1.8
     */
    protected Function wrapFunction = new NodeFunction("wrap", WRAP_PARAMETERS, ReturnType.STRING) {
            {
                setDescription("This function wraps a field, word-by-word. You can use this, e.g. in <pre>-tags. This functionality should be available as an 'escaper', and this version should now be considered an example.");
            }
            public Object getFunctionValue(Node node, Parameters parameters) {
                String val = node.getStringValue(parameters.getString(Parameter.FIELD));
                Number wrappos = (Number) parameters.get("length");
                return wrap(val, wrappos.intValue());
            }
        };
    {
        addFunction(wrapFunction);
    }

    /**
     * Every Function Provider provides least the 'getFunctions' function, which returns a Set of all functions which it provides.
     * This is overridden from FunctionProvider, because this one needs to be (also) a NodeFunction
     * @since MMBase-1.8
     */
    protected Function getFunctions = new NodeFunction("getFunctions", Parameter.EMPTY, ReturnType.SET) {
            {
                setDescription("The 'getFunctions' returns a Map of al Function object which are available on this FunctionProvider");
            }
            public Object getFunctionValue(Node node, Parameters parameters) {
                return MMObjectBuilder.this.getFunctions(getCoreNode(MMObjectBuilder.this, node));
            }
            public Object getFunctionValue(Parameters parameters) {
                Node node = (Node) parameters.get(Parameter.NODE);
                if (node == null) {
                    return MMObjectBuilder.this.getFunctions();
                } else {
                    return MMObjectBuilder.this.getFunctions(getCoreNode(MMObjectBuilder.this, node));
                }
            }
        };
    {
        addFunction(getFunctions);
    }

    /**
     * The info-function is a node-function and a builder-function. Therefore it is defined as a node-function, but also overidesd getFunctionValue(Parameters).
     * @since MMBase-1.8
     */
    protected Function infoFunction = new NodeFunction("info", new Parameter[] { new Parameter("function", String.class) }, ReturnType.UNKNOWN) {
            {
                setDescription("Returns information about available functions");
            }
            protected Object getFunctionValue(Collection functions, Parameters parameters) {
                String function = (String) parameters.get("function");
                if (function == null || function.equals("")) {
                    Map info = new HashMap();
                    Iterator i = functions.iterator();
                    while (i.hasNext()) {
                        Function f = (Function) i.next();
                        info.put(f.getName(), f.getDescription());
                    }
                    return info;
                } else {
                    Function func = getFunction(function);
                    if (func == null) return "No such function " + function;
                    return func.getDescription();
                }
            }
            public Object getFunctionValue(Node node, Parameters parameters) {
                return getFunctionValue(MMObjectBuilder.this.getFunctions(getCoreNode(MMObjectBuilder.this, node)), parameters);
            }
            public Object getFunctionValue(Parameters parameters) {
                MMObjectNode node = (MMObjectNode) parameters.get(Parameter.NODE);
                if (node == null) {
                    return getFunctionValue(MMObjectBuilder.this.getFunctions(), parameters);
                } else {
                    return getFunctionValue(MMObjectBuilder.this.getFunctions(node), parameters);
                }
            }
        };
    {
        addFunction(infoFunction);
    }

    // contains the builder's field definitions
    protected final Map fields = new HashMap();

    /**
     * Determines whether a builder is virtual (data is not stored in the storage layer).
     */
    protected boolean virtual=false;

    /**
     *  Set of remote observers, which are notified when a node of this type changes
     */
    private Set remoteObservers = Collections.synchronizedSet(new HashSet());

    /**
     * Set of local observers, which are notified when a node of this type changes
     */
    private Set localObservers = Collections.synchronizedSet(new HashSet());

    /**
     * Reference to the builders that this builder extends.
     * @since MMBase-1.6.2 (parentBuilder in 1.6.0)
     */
    private Stack ancestors = new Stack();

    /**
     * Version information for builder registration
     * Set with &lt;builder maintainer="mmbase.org" version="0"&gt; in the xml
     * builder file
     */
    private int version=0;

    /**
     * Contains lists of builder fields in specified order
     * (ORDER_CREATE, ORDER_EDIT, ORDER_LIST, ORDER_SEARCH)
     */
    private Map sortedFieldLists = new HashMap();

    /** Properties of a specific Builder.
     * Specified in the xml builder file with the <properties> tag.
     * The use of properties is determined by builder
     */
    private Hashtable properties = null;

    /**
     * The datatype collector for this builder
     */
    private DataTypeCollector dataTypeCollector = null;

    /**
     * Constructor.
     */
    public MMObjectBuilder() {
        storageConnector = new StorageConnector(this);
    }

    private void initAncestors() {
        if (! ancestors.empty()) {
            ((MMObjectBuilder) ancestors.peek()).init();
        }
    }

    /**
     * Initializes this builder
     * The property 'mmb' needs to be set for the builder before this method can be called.
     * The method retrieves data from the TypeDef builder, or adds data to that builder if the
     * current builder is not yet registered.
     * @return true if init was completed, false if uncompleted.
     * @see #create
     */
    public boolean init() {
        synchronized(mmb) { // synchronized on mmb because can only init builder if mmb is inited completely

            // skip initialisation if oType has been set (happend at end of init)
            // note that init can be called twice
            if (oType != -1) return true;

            log.debug("Init of builder " + getTableName());

            // first make sure parent builder is initalized
            initAncestors();

            String broadCastChangesProperty = getInitParameter(BROADCAST_CHANGES_PROPERTY);
            if (broadCastChangesProperty != null) {
                broadCastChanges = broadCastChangesProperty.equals("true");
            }

            if (!created()) {
                log.info("Creating table for builder " + tableName);
                if (!create() ) {
                    // can't create buildertable.
                    // Throw an exception
                    throw new BuilderConfigurationException("Cannot create table for "+getTableName()+".");
                };
            }
            TypeDef typeDef = mmb.getTypeDef();
            // only deteremine otype if typedef is available,
            // or this is typedef itself (have to start somewhere)
            if (((typeDef != null)  && (typeDef.getObjectType()!=-1)) || (this == typeDef)) {
                oType = typeDef.getIntValue(tableName);
                if (oType == -1) { // no object type number defined yet
                    if (log.isDebugEnabled()) log.debug("Creating typedef entry for " + tableName);
                    MMObjectNode node = typeDef.getNewNode(SYSTEM_OWNER);
                    node.storeValue("name", tableName);

                    // This sucks:
                    if (description == null) description = "not defined in this language";

                    node.storeValue("description", description);

                    try {
                        oType = mmb.getStorageManager().createKey();
                    } catch (StorageException se) {
                        log.error(se.getMessage() + Logging.stackTrace(se));
                        return false;
                    }

                    log.debug("Got key " + oType);
                    node.storeValue(FIELD_NUMBER, new Integer(oType));
                    // for typedef, set otype explictly, as it wasn't set in getNewNode()
                    if (this == typeDef) {
                        node.storeValue(FIELD_OBJECT_TYPE, new Integer(oType));
                    }
                    typeDef.insert(SYSTEM_OWNER, node, false);
                    // for typedef, call it's parents init again, as otype is only now set
                    if (this == typeDef) {
                        initAncestors();
                    }
                }
            } else {
                // warn if typedef was not created
                // except for the 'object' and 'typedef' basic builders
                if(!tableName.equals("typedef") && !tableName.equals("object")) {
                    log.warn("init(): for tablename(" + tableName + ") -> can't get to typeDef");
                    return false;
                }
            }
            // XXX: wtf
            // add temporary fields
            checkAddTmpField(TMP_FIELD_NUMBER);
            checkAddTmpField(TMP_FIELD_EXISTS);

            // get property of maximum number of queries..
            String property = getInitParameter(MAX_NODES_FROM_QUERY_PROPERTY);
            if(property != null) {
                try {
                    maxNodesFromQuery = Integer.parseInt(property);
                    log.debug(getTableName() + " returns no more than " + maxNodesFromQuery + " records from a query.");
                } catch(NumberFormatException nfe) {
                    log.warn("property:" + MAX_NODES_FROM_QUERY_PROPERTY + " contained an invalid integer value:'" + property +"'(" + nfe + ")");
                }
            }
        }
        update();

        //now register it as a listener for events of it's own type
        //this is only for backwards compatibility, to notify the MMBaseObserver's
        MMBase.getMMBase().addNodeRelatedEventsListener(getTableName(), this);


        return true;
    }

    /**
     * Returns the builder object number, which also functions as the objecttype.
     * This is the same value as the value of the 'otype' field of objects created by this builder
     * (rather than created by its descendants).
     * @return the builder number
     * @since MMBase-1.8
     */
    public int getNumber() {
        return oType;
    }

    /**
     * Returns the objecttype (otype).
     * By preference, use {@link #getNumber()} for future compatibility with the bridge NodeManager methods.
     * @return the objecttype
     */
    public int getObjectType() {
        return getNumber();
    }

    /**
     * Updates the internal version number of this buidler;
     */
    protected void update() {
        internalVersion = System.currentTimeMillis();
    }

    /**
     * Returns the builder's internal version number.
     * This number can be used to sync wrapper classes. I.e. to make sure that a
     * nodemanager's fieldlist is the same as that of the wrapped builder.
     */
    public long getInternalVersion() {
        return internalVersion;
    }

    /**
     * Creates a new builder table in the storage layer.
     */
    public boolean create() {
        log.debug(tableName);
        try {
            mmb.getStorageManager().create(this);
            return true;
        } catch (StorageException se) {
            log.error(se.getMessage() + Logging.stackTrace(se));
            return false;
        }
    }

    /**
     * Removes the builder from the storage.
     * @since MMBase-1.7
     */
    public void delete() {
        log.service("trying to drop table of builder: '"+tableName+"'");
        mmb.getStorageManager().delete(this);
    }

    /**
     * Tests whether the data in a node is valid (throws an exception if this is not the case).
     * @param node The node whose data to check
     * @throws org.mmbase.module.core.InvalidDataException
     *   If the data was unrecoverably invalid (the references did not point to existing objects)
     */
    public void testValidData(MMObjectNode node) throws InvalidDataException {
        return;
    };

    /**
     * Insert a new, empty, object of a certain type.
     * @param oType The type of object to create
     * @param owner The administrator creating the node
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     *        The basic routine does not create any nodes this way and always fails.
     */
    public int insert(int oType, String owner) {
        return -1;
    }

    /**
     * Insert a new object (content provided) in the cloud, including an entry for the object alias (if provided).
     * This method indirectly calls {@link #preCommit}.
     * @param owner The administrator creating the node
     * @param node The object to insert. The object need be of the same type as the current builder.
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     */
    public int insert(String owner, MMObjectNode node) {
        int n = mmb.getStorageManager().create(node);
        if (n >= 0) {
            node.isNew = false;
        }

        node.useAliases();

        // it is in the storage now, all caches can allready be invalidated, this makes sure
        // that imediate 'select' after 'insert' will be correct'.
        //xxx: this is bad.let's kill it!
        //QueryResultCache.invalidateAll(node, NodeEvent.TYPE_NEW);
        if (n <= 0) {
            log.warn("Did not get valid nodeNumber of storage " + n);
        }
        Integer nodeNumber = new Integer(n);
        if (isNodeCached(nodeNumber)) {
            // it seems that something put the node in the cache already.
            // This is usually because the ChangeManager indirectly called 'getNode'
            // This should in the new event-mechanism not be needed, because the NodeEvent
            // contains the node.
            //log.warn("New node '" + n + "' of type " + node.parent.getTableName() + " is already in node-cache!" + Logging.stackTrace());
        } else {
            safeCache(nodeNumber, node);
        }
        return n;
    }

    /**
     * This method is called before an actual write to the storage layer is performed.
     * @param node The node to be committed.
     * @return the node to be committed (possibly after changes have been made).
     */
    public MMObjectNode preCommit(MMObjectNode node) {
        return node;
    }

    /**
     * Commit changes to this node to the storage layer. This method indirectly calls {@link #preCommit}.
     * Use only to commit changes - for adding node, use {@link #insert}.
     * @param node The node to be committed
     * @return true if commit successful
     */
    public boolean commit(MMObjectNode node) {
        mmb.getStorageManager().change(node);
        return true;
    }

    /**
     * Determines whether changes to this builder need be broadcasted to other known mmbase servers.
     * This setting also governs whether the cache for relation builders is emptied when a relation changes.
     * Actual broadcasting (and cache emptying) is initiated in the storage layer, when
     * changes are commited.
     * By default, all builders broadcast their changes, with the exception of the TypeDef builder.
     *
     * MM: Can somebody please explain _why_ typedef node changes, like e.g. creating a new node type are _not_ broadcasted.
     * @since MMBase-1.8
     */
    public boolean broadcastChanges() {
        return broadCastChanges;
    }

    /**
     *  Creates an alias for a node, provided the OAlias builder is loaded.
     *  @param number the to-be-aliased node's unique number
     *  @param alias the aliasname to associate with the object
     *  @param owner the owner of the alias
     *  @since MMBase-1.8
     *  @return if the alias could be created
     */
    public boolean createAlias(int number, String alias, String owner) {
        if (mmb.getOAlias() != null) {
            if (getNode(alias) != null ) {  // this alias already exists! Don't add a new one!
                return false;
            }
            mmb.getOAlias().createAlias(alias, number, owner);
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Creates an alias for a node, provided the OAlias builder is loaded.
     *  @param number the to-be-aliased node's unique number
     *  @param alias the aliasname to associate with the object
     *  @return if the alias could be created
     */
    public boolean createAlias(int number, String alias) {
        return createAlias(number, alias, "system");
    }

    /**
     * Returns the builder that this builder extends.
     *
     * @since MMBase-1.6
     * @return the extended (parent) builder, or null if not available
     */
    public MMObjectBuilder getParentBuilder() {
        if (ancestors.empty()) return null;
        return (MMObjectBuilder) ancestors.peek();
    }
    /**
     * Gives the list of parent-builders.
     *
     * @since MMBase-1.6.2

     */
    public List  getAncestors() {
        return Collections.unmodifiableList(ancestors);
    }

    /**
     * Creates list of descendant-builders.
     *
     * @since MMBase-1.6.2
     */
    public List getDescendants() {
        ArrayList result = new ArrayList();
        Enumeration e = mmb.getMMObjects();
        while(e.hasMoreElements()) {
            MMObjectBuilder builder = (MMObjectBuilder) e.nextElement();
            if (builder.isExtensionOf(this)) {
                result.add(builder);
            }
        }
        return result;
    }


    /**
     * Sets the builder that this builder extends, and registers it in the storage layer.
     * @param parent the extended (parent) builder, or null if not available
     *
     * @since MMBase-1.6
     */
    public void setParentBuilder(MMObjectBuilder parent) {
        ancestors.addAll(parent.getAncestors());
        ancestors.push(parent);
        getDataTypeCollector().addCollector(parent.getDataTypeCollector());
    }

    /**
     * Returns the datatype collector belonging to this buidler.
     * A datatype collector contains the datatypes that are local to this builder.
     * @since MMBase-1.8
     */
    public DataTypeCollector getDataTypeCollector() {
        if (dataTypeCollector == null) {
            Object signature = new String(getTableName()+ "_" + System.currentTimeMillis());
            dataTypeCollector = new DataTypeCollector(signature);
        }
        return dataTypeCollector;
    }

    /**
     * Checks wether this builder is an extension of the argument builder
     *
     * @since MMBase-1.6.2
     */
    public boolean isExtensionOf(MMObjectBuilder o) {
        return ancestors.contains(o);
    }

    /**
     * Get a new node, using this builder as its parent. The new node is not a part of the cloud
     * yet, and thus has the value -1 as a number. (Call {@link #insert} to add the node to the
     * cloud).
     * @param owner The administrator creating the new node.
     * @return A newly initialized <code>MMObjectNode</code>.
     */
    public MMObjectNode getNewNode(String owner) {
        MMObjectNode node = getEmptyNode(owner);
        node.isNew = true;
        return node;
    }

    /**
     * Returns a new empty node object. This is used by Storage to create a non-new node object (isNew is false), which is then
     * be filled with actual values from storage.
     * @since MMBase-1.8.
     */
    public MMObjectNode getEmptyNode(String owner) {
        MMObjectNode node = new MMObjectNode(this, false);
        node.setValue(FIELD_NUMBER, -1);
        node.setValue(FIELD_OWNER, owner);
        node.setValue(FIELD_OBJECT_TYPE, oType);
        setDefaults(node);
        return node;
    }
    /**
     * Sets defaults for a node. Fields "number", "owner" and "otype" are not set by this method.
     * @param node The node to set the defaults of.
     */
    public void setDefaults(MMObjectNode node) {
        for (Iterator i = getFields().iterator(); i.hasNext(); ) {
            CoreField field = (CoreField) i.next();
            if (field.getName().equals(FIELD_NUMBER))      continue;
            if (field.getName().equals(FIELD_OWNER))       continue;
            if (field.getName().equals(FIELD_OBJECT_TYPE)) continue;
            if (field.getType() == Field.TYPE_NODE)        continue;

            Object defaultValue = field.getDataType().getDefaultValue();
            if ((defaultValue == null) && field.isNotNull()) {
                defaultValue = Casting.toType(Fields.typeToClass(field.getType()), null, "");
            }
            node.setValue(field.getName(), defaultValue);
        }
    }

    /**
     * In setDefault you could want to generate unique values for fields (if the field is 'unique').
     * @since MMBase-1.7
     */
    protected String setUniqueValue(MMObjectNode node, String field, String baseValue) {
        int seq = 0;
        boolean found = false;
        String value = baseValue;
        try {
            while (! found) {
                NodeSearchQuery query = new NodeSearchQuery(this);
                value = baseValue + seq;
                BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(query.getField(getField(field)), value);
                query.setConstraint(constraint);
                if (getNodes(query).size() == 0) {
                    found = true;
                    break;
                }
                seq++;
            }
        } catch (SearchQueryException e) {
            value =   baseValue + System.currentTimeMillis();
        }
        node.setValue(field, value);
        return value;
    }

    /**
     * In setDefault you could want to generate unique values for fields (if the field is 'unique').
     * @since MMBase-1.7
     */
    protected int setUniqueValue(MMObjectNode node, String field, int offset) {
        int seq = offset;
        boolean found = false;
        try {
            while (! found) {
                NodeSearchQuery query = new NodeSearchQuery(this);
                BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(query.getField(getField(field)), new Integer(seq));
                query.setConstraint(constraint);
                if (getNodes(query).size() == 0) {
                    found = true;
                    break;
                }
                seq++;
            }
        } catch (SearchQueryException e) {
            seq =  (int) System.currentTimeMillis() / 1000;
        }
        node.setValue(field, seq);
        return seq;
    }

    /**
     * Remove a node from the cloud.
     * @param node The node to remove.
     */
    public void removeNode(MMObjectNode node) {
        if (oType != node.getOType()) {
            // fixed comment's below..??
            // prevent from making storage inconsistent(say remove nodes from inactive builder)
            // the builder we are in is not the actual builder!!
            // ? why not an node.remove()
            throw new RuntimeException("Builder with name: " + getTableName() + "(otype " + oType + ") is not the actual builder of the node that is to be deleted: " +
                                       node.getNumber() + " (otype: " + node.getOType() + ")");
        }

        removeSyncNodes(node);

        clearBlobCache(node.getNumber());

        // removes the node FROM THIS BUILDER
        // seems not a very logical call, as node.parent is the node's actual builder,
        // which may - possibly - be very different from the current builder
        mmb.getStorageManager().delete(node);

        // change is in storage, caches can be invalidated immediately
        //really bad!!!
        //QueryResultCache.invalidateAll(node, NodeEvent.EVENT_TYPE_DELETE);
    }

    /**
     * Removes the syncnodes to this node. This is logical, but also needed to maintain storage
     * integrety.
     *
     * @since MMBase-1.7
     */
    protected void removeSyncNodes(MMObjectNode node) {
        try {
            MMObjectBuilder syncnodes = mmb.getBuilder("syncnodes");
            NodeSearchQuery query = new NodeSearchQuery(syncnodes);
            Object numericalValue = new Integer(node.getNumber());
            BasicStepField field = query.getField(syncnodes.getField("localnumber"));
            BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(field,
                                                                                 numericalValue);
            query.setConstraint(constraint);
            Iterator syncs = syncnodes.getNodes(query).iterator();
            while (syncs.hasNext()) {
                MMObjectNode syncnode = (MMObjectNode) syncs.next();
                syncnode.parent.removeNode(syncnode);
                if (log.isDebugEnabled()) {
                    log.debug("Removed syncnode " + syncnode);
                }
            }
        } catch (SearchQueryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove the relations of a node.
     * @param node The node whose relations to remove.
     */
    public void removeRelations(MMObjectNode node) {
        List relsv = getRelations_main(node.getNumber());
        if (relsv != null) {
            for(Iterator rels = relsv.iterator(); rels.hasNext(); ) {
                // get the relation node
                MMObjectNode relnode = (MMObjectNode)rels.next();
                // determine the true builder for this node
                // (node.parent is always InsRel, but otype
                //  indicates any derived builders, such as AuthRel)
                MMObjectBuilder bul = mmb.getMMObject(mmb.getTypeDef().getValue(relnode.getOType()));
                // remove the node using this builder
                // circumvent problem in storage layer (?)
                bul.removeNode(relnode);
            }
        }
    }

    /**
     * Is this node cached at this moment?
     * @param number The number of the node to check.
     * @return <code>true</code> if the node is in the cache, <code>false</code> otherwise
     */
    public boolean isNodeCached(Integer number) {
        return nodeCache.containsKey(number);
    }

    /**
     * Retrieves a node from the cache, or <code>null</code> if it doesn't exist.
     * @param number The number of the node to retrieve.
     * @return an MMObjectNode or <code>null</code> if the node is not in the cache
     * @todo This is a simple wrapper around node cache, why not expose node cache in stead?
     * @since MMBase-1.8
     */
    public MMObjectNode getNodeFromCache(Integer number) {
        return (MMObjectNode) nodeCache.get(number);
    }

    /**
     * Stores a node in the cache provided the cache is not write locked.
     * @return a valid node. If the node already was in the cache, the cached node is returned.
     * In that case the node given as parameter should become invalid
     */
    public MMObjectNode safeCache(Integer n, MMObjectNode node) {
        MMObjectNode retval = getNodeFromCache(n);
        if (retval != null) {
            return retval;
        } else {
            synchronized (nodeCache) {
                if (cacheLocked == 0) {
                    nodeCache.put(n, node);
                }
            }
            return node;
        }
    }

    /**
     * Locks the node cache during the commit of a node.  This prevents the cache from gaining an
     * invalid state during the commit.
     *
     * Basicly the goals is to ensure that nothing is put into the cache during a commit of a node,
     * because that may be the wrong node then.
     */
    boolean safeCommit(MMObjectNode node) {
        boolean res = false;
        try {
            synchronized(nodeCache) {
                cacheLocked++;
            }
            if (node.getNumber() > 0 ) {
                Integer number = new Integer(node.getNumber());
                nodeCache.remove(number);
            }

            res = node.commit();
        } finally {
            synchronized(nodeCache) {
                cacheLocked--;
            }
        }
        return res;
    }

    /**
     * Locks the node cache during the insert of a node.
     * This prevents the cache from adding the node, which
     * means that the next time the node is read it is 'refreshed'
     * from the storage
     */
    int safeInsert(MMObjectNode node, String userName) {
        int res = -1;
        try {
            synchronized(nodeCache) {
                cacheLocked++;
            }
            // determine valid username
            if ((userName == null) || (userName.length() <= 1 )) { // may not have owner of 1 char??
                userName = node.getStringValue(FIELD_OWNER);
                if (log.isDebugEnabled()) {
                    log.debug("Found username " + (userName == null ? "NULL" : userName));
                }
            }
            res = node.insert(userName);
            if (res > -1) {
                nodeCache.put(new Integer(res), node);
            }
        } finally {
            synchronized(nodeCache) {
                cacheLocked--;
            }
        }
        return res;
    }

    /**
     * Determine whether this builder is virtual.
     * A virtual builder represents nodes that are not stored or retrieved directly
     * from storage, but are created as needed.
     * @return <code>true</code> if the builder is virtual.
     */
    public boolean isVirtual() {
        return virtual;
    }

    /**
     * Retrieves a node based on a unique key. The key is either an entry from the OAlias table
     * or the string-form of an integer value (the number field of an object node).
     * Note that the OAlias builder needs to be active for the alias to be used
     * (otherwise using an alias is concidered invalid).
     * @param key The value to search for
     * @param useCache If true, the node is retrieved from the node cache if possible.
     * @return <code>null</code> if the node does not exist or the key is invalid, or a
     *       <code>MMObjectNode</code> containing the contents of the requested node.
     */
    public MMObjectNode getNode(String key, boolean useCache) {
        if( key == null ) {
            log.error("getNode(null) for builder '" + tableName + "': key is null!");
            // who is doing that?
            log.info(Logging.stackTrace(6));
            return null;
        }
        int nr =-1;
        // first look if we have a number...
        try {
            nr = Integer.parseInt(key);
        } catch (Exception e) {}
        if (nr!=-1) {
            // key passed was a number.
            // return node with this number
            return getNode(nr, useCache);
        } else {
            // key passed was an alias
            // return node with this alias
            log.debug("Getting node by alias");
            if (mmb.getOAlias() != null) {
                return mmb.getOAlias().getAliasedNode(key);
            } else {
                return null;
            }
        }
    }

    /**
     * Retrieves a node based on a unique key. The key is either an entry from the OAlias table
     * or the string-form of an integer value (the number field of an object node).
     * Retrieves a node from the node cache if possible.
     * @param key The value to search for
     * @return <code>null</code> if the node does not exist or the key is invalid, or a
     *       <code>MMObjectNode</code> containing the contents of the requested node.
     */
    public MMObjectNode getNode(String key) {
        return getNode(key, true);
    }

    /**
     * Retrieves a node based on a unique key. The key is either an entry from the OAlias table
     * or the string-form of an integer value (the number field of an object node).
     * Retrieves the node from directly the storage, not using the node cache.
     * @param key The value to search for
     * @return <code>null</code> if the node does not exist or the key is invalid, or a
     *       <code>MMObjectNode</code> containing the contents of the requested node.
     */
    public MMObjectNode getHardNode(String key) {
        return getNode(key, false);
    }

    /**
     * Retrieves a node based on it's number (a unique key), retrieving the node
     * from the node cache if possible.
     * @param number The number of the node to search for
     * @return <code>null</code> if the node does not exist or the key is invalid, or a
     *       <code>MMObjectNode</code> containign the contents of the requested node.
     */
    public MMObjectNode getNode(int number) {
        return getNode(number, true);
    }

    /**
     * Retrieves a node based on it's number (a unique key), directly from
     * the storage, not using the node cache.
     * @param number The number of the node to search for
     * @return <code>null</code> if the node does not exist or the key is invalid, or a
     *  <code>MMObjectNode</code> containign the contents of the requested node.
     */
    public MMObjectNode getHardNode(int number) {
        return getNode(number, false);
    }

    /**
     * Create a new temporary node and put it in the temporary _exist
     * node space
     */
    public MMObjectNode getNewTmpNode(String owner,String key) {
        MMObjectNode node = null;
        node = getNewNode(owner);
        node.setValue(TMP_FIELD_NUMBER, key);
        temporaryNodes.put(key, node);
        return node;
    }

    /**
     * Put a Node in the temporary node list
     * @param key  The (temporary) key under which to store the node
     * @param node The node to store
     */
    public void putTmpNode(String key, MMObjectNode node) {
        node.setValue(TMP_FIELD_NUMBER, key);
        temporaryNodes.put(key, node);
    }

    /**
     * Defines a virtual field to use for temporary nodes. If the given field-name does not start
     * with underscore ('_'), wich it usually does, then the field does also get a 'dbpos' (1000) as if it
     * was actually present in the builder's XML as a virtual field (this is accompanied with a log
     * message).
     *
     * Normally this is used to add 'tmp' fields like _number, _exists and _snumber which are system
     * fields which are normally invisible.
     *
     * @param field the name of the temporary field
     * @return true if the field was added, false if it already existed.
     */
    public boolean checkAddTmpField(String field) {
        if (getDBState(field) == Field.STATE_UNKNOWN) { // means that field is not yet defined.
            CoreField fd = Fields.createField(field, Field.TYPE_STRING, Field.TYPE_UNKNOWN, Field.STATE_VIRTUAL, null);
            if (! fd.isTemporary()) {
                fd.setStoragePosition(1000);
                log.service("Added a virtual field '" + field + "' to builder '" + getTableName() + "' because it was not defined in the builder's XML, but the implementation requires it to exist.");
            } else {
                log.debug("Adding tmp (virtual) field '" + field + "' to builder '" + getTableName() + "'");
            }

            fd.setParent(this);
            fd.finish();

            addField(fd);
            // added field, so update version
            update();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get nodes from the temporary node space
     * @param key  The (temporary) key to use under which the node is stored
     */
    public MMObjectNode getTmpNode(String key) {
        MMObjectNode node = null;
        node = (MMObjectNode) temporaryNodes.get(key);
        if (node == null && log.isDebugEnabled()) {
            log.trace("getTmpNode(): node not found " + key);
        }
        return node;
    }

    /**
     * Remove a node from the temporary node space
     * @param key  The (temporary) key under which the node is stored
     */
    public void removeTmpNode(String key) {
        MMObjectNode node;
        node=(MMObjectNode) temporaryNodes.remove(key);
        if (node==null) {
        log.debug("removeTmpNode: node with "+key+" didn't exists");
    }
    }

    /**
     * Return a copy of the list  of field definitions of this table.
     * @return An unmodifiable <code>Collection</code> with the tables fields
     */
    public Collection getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }


    /**
     * Return a list of field names of this table.
     * @return a unmodifiable <code>Set</code> with the tables field names
     * @todo return an unmodifiable Set.
     */
    public Set getFieldNames() {
        return Collections.unmodifiableSet(fields.keySet());
    }

    /**
     * Return a field's definition
     * @param fieldName the requested field's name
     * @return a <code>FieldDefs</code> belonging with the indicated field
     * @todo  Should return CoreField
     */
    public FieldDefs getField(String fieldName) {
        return (FieldDefs) fields.get(fieldName.toLowerCase());
    }


    /**
     * @since MMBase-1.8
     */
    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName.toLowerCase());
    }

    /**
     * Clears all field list caches, and recalculates the field list.
     */
    protected void updateFields() {
        sortedFieldLists = new HashMap();
        update();
    }

    /**
     * Add a field to this builder.
     * This does not affect the builder config file, nor the table used.
     * @param def the field definiton to add
     */
    public void addField(CoreField def) {
        Object oldField = fields.put(def.getName().toLowerCase(), def);
        if (oldField != null) {
            log.warn("Replaced " + oldField + " !!");
        }
        updateFields();
    }


    /**
     * Remove a field from this builder.
     * This does not affect the builder config file, nor the table used.
     * @param fieldName the name of the field to remove
     */
    public void removeField(String fieldName) {
        CoreField def = getField(fieldName);
        int dbpos = def.getStoragePosition();
        fields.remove(fieldName);
        // move them all up one place
        for (Iterator e = fields.values().iterator(); e.hasNext();) {
            def = (CoreField) e.next();
            int curpos = def.getStoragePosition();
            if (curpos >= dbpos) def.setStoragePosition(curpos - 1);
        }
        updateFields();
    }


    /**
     * Return a field's storage type. The returned value is one of the following values
     * declared in Field:
     * TYPE_STRING,
     * TYPE_INTEGER,
     * TYPE_BINARY,
     * TYPE_FLOAT,
     * TYPE_DOUBLE,
     * TYPE_LONG,
     * TYPE_NODE,
     * TYPE_UNKNOWN
     * @param fieldName the requested field's name
     * @return the field's type.
     */
    public int getDBType(String fieldName) {
        if (fields == null) {
            log.error("getDBType(): fields are null on object : "+tableName);
            return Field.TYPE_UNKNOWN;
        }
        Field field = getField(fieldName);
        if (field == null) {
            //perhaps prefixed with own tableName[0-9]? (allowed since MMBase-1.7)
            int dot = fieldName.indexOf('.');
            if (dot > 0) {
                if (fieldName.startsWith(tableName)) {
                    if (tableName.length() <= dot  ||
                        Character.isDigit(fieldName.charAt(dot - 1))) {
                        fieldName = fieldName.substring(dot + 1);
                        field = getField(fieldName);
                    }
                }
            }
        }

        if (field == null) {

            // log warning, except for virtual builders
            if (!virtual) { // should getDBType not be overridden in Virtual Builder then?
                log.warn("getDBType(): Can't find definition on field '" + fieldName + "' of builder " + tableName);
                log.debug(Logging.stackTrace());
            }
            return Field.TYPE_UNKNOWN;
        }
        return field.getType();
    }

    /**
     * Return a field's storage state. The returned value is one of the following values
     * declared in Field:
     * STATE_VIRTUAL,
     * STATE_PERSISTENT,
     * STATE_SYSTEM,
     * STATE_UNKNOWN
     * @param fieldName the requested field's name
     * @return the field's state.
     */
    public int getDBState(String fieldName) {
        if (fields == null) return Field.STATE_UNKNOWN;
        Field field = getField(fieldName);
        if (field == null) return Field.STATE_UNKNOWN;
        return field.getState();
    }

    /**
     * A complicated default implementation for GUI.
     * @since MMBase-1.8
     */
    protected String getGUIIndicator(MMObjectNode node, Parameters pars) {
        Locale locale = (Locale) pars.get(Parameter.LOCALE);
        String language = (String) pars.get(Parameter.LANGUAGE);
        if (locale == null) {
            if (language != null) {
                locale = new Locale(language, "");
            }
        } else {
            if (language != null && (! locale.getLanguage().equals(language))) { // odd, but well,
                locale = new Locale(language, locale.getCountry());
            }
        }
        if (locale == null) locale = mmb.getLocale();

        if (log.isDebugEnabled()) {
            log.debug("language " + locale.getLanguage() + " country " + locale.getCountry());
        }

        String rtn;
        String field = pars.getString("field");

        if (locale == null) {
            if ("".equals(field)) {
                rtn = getGUIIndicator(node);
                if (rtn == GUI_INDICATOR) { // not overridden
                    rtn = getNodeGUIIndicator(node, pars);
                }
            } else {
                rtn = getGUIIndicator(field, node);
            }
        } else {
            if ("".equals(field)) {
                rtn = getLocaleGUIIndicator(locale, node);
                if (rtn == GUI_INDICATOR) { // not overridden
                    rtn = getNodeGUIIndicator(node, pars);
                }
            } else {
                rtn = getLocaleGUIIndicator(locale, field, node);
            }
        }

        if (rtn == null) {
            CoreField fdef = getField(field);

            Object returnValue;
            if (fdef != null) {
                // test if the value can be derived from the enumerationlist of a datatype
                DataType dataType = fdef.getDataType();
                returnValue = dataType.getEnumerationValue(locale, (Cloud) pars.get(Parameter.CLOUD), (Node) pars.get(Parameter.NODE), fdef, node.getStringValue(field));
            } else {
                returnValue = null;
            }
            if (returnValue != null) {
                rtn = returnValue.toString();
            } else {
                if (fdef != null && ("eventtime".equals(fdef.getGUIType()) || fdef.getType() == Field.TYPE_DATETIME)) { // do something reasonable for this
                    Date date;
                    if (fdef.getType() == Field.TYPE_DATETIME) {
                        date = node.getDateValue(field);
                    } else {
                        date = new Date(node.getLongValue(field) * 1000);
                    }
                    rtn = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, locale).format(date);
                    Calendar calendar = new GregorianCalendar(locale);
                    calendar.setTime(date);
                    if (calendar.get(Calendar.ERA) == GregorianCalendar.BC) {
                        java.text.DateFormat df = new java.text.SimpleDateFormat(" G", locale);
                        rtn += df.format(date);
                    }
                } else {
                    rtn = (String) pars.get("stringvalue");
                    if (rtn == null) {
                        rtn = node.getStringValue(field);
                    }
                }
            }
            rtn = org.mmbase.util.transformers.Xml.XMLEscape(rtn);
        }
        return rtn;
    }

    /**
     * Returns a GUI-indicator for the node itself.
     * @since MMBase-1.8.1
     */
    protected String getNodeGUIIndicator(MMObjectNode node, Parameters params) {
        // do the best we can because this method was not implemented
        // we get the first field in the object and try to make it
        // to a string we can return
        List list = getFields(NodeManager.ORDER_LIST);
        if (list.size() > 0) {
            String fname = ((CoreField) list.get(0)).getName();
            String str = node.getStringValue( fname );
            if (str.length() > 128) {
                str =  str.substring(0, 128) + "...";
            }
            params.set("field", fname);
            params.set("stringvalue", str);
            return getGUIIndicator(node, params);
        } else {
            return GUI_INDICATOR;
        }
    }


    /**
     * What should a GUI display for this node.
     * Default is the first non system field (first field after owner).
     * Override this to display your own choice (see Images.java).
     * You may want to override {@link #getNodeGUIIndicator} for more flexibility.
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
    public  String getGUIIndicator(MMObjectNode node) {
        return GUI_INDICATOR;
    }


    /**
     * What should a GUI display for this node/field combo.
     * Default is null (indicating to display the field as is)
     * Override this to display your own choice.
     * @param node The node to display
     * @param fieldName the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    public String getGUIIndicator(String fieldName, MMObjectNode node) {
        CoreField field = getField(fieldName);

        if (field != null && field.getType() == Field.TYPE_NODE && ! fieldName.equals(FIELD_NUMBER)) {
            try {
                MMObjectNode otherNode = node.getNodeValue(fieldName);
                if (otherNode == null) {
                    return "";
                } else {
                    return otherNode.parent.getGUIIndicator(otherNode);
                }
            } catch (RuntimeException rte) {
                log.warn("Cannot load node from field " + fieldName +" in node " + node.getNumber() + ":" +rte);
                return "invalid";
            }
        } else {
            return null;
        }
    }


    /**
     * The GUIIndicator can depend on the locale. Override this function
     * @since MMBase-1.6
     */
    protected String getLocaleGUIIndicator(Locale locale, String field, MMObjectNode node) {
        return getGUIIndicator(field, node);
    }


    /**
     * The GUIIndicator can depend on the locale. Override this function
     * You may want to override {@link #getNodeGUIIndicator} for more flexibility.
     * @since MMBase-1.6
     */
    protected String getLocaleGUIIndicator(Locale locale, MMObjectNode node) {
        return getGUIIndicator(node);
    }


    /**
     * Gets the field definitions for the editor, sorted according
     * to the specified order, and excluding the fields that have
     * not been assigned a valid position (valid is >= 0).
     * This method makes an explicit sort (it does not use a cached list).
     *
     * @param sortOrder One of the sortorders defined in
     *        {@link org.mmbase.core.CoreField CoreField}
     * @return The ordered list of field definitions.
     */
    public List getFields(int sortOrder) {
        List orderedFields = (List)sortedFieldLists.get(new Integer(sortOrder));
        if (orderedFields == null) {
            orderedFields = new ArrayList();
            for (Iterator i = fields.values().iterator(); i.hasNext();) {
                CoreField field = (CoreField)i.next();
                if (field.isTemporary()) {
                    continue;
                }

                // include only fields which have been assigned a valid position, and are
                if ((sortOrder == NodeManager.ORDER_NONE) ||
                    ((sortOrder == NodeManager.ORDER_CREATE) && (field.getStoragePosition()>-1)) ||
                    ((sortOrder == NodeManager.ORDER_EDIT) && (field.getEditPosition()>-1)) ||
                    ((sortOrder == NodeManager.ORDER_SEARCH) && (field.getSearchPosition()>-1)) ||
                    ((sortOrder == NodeManager.ORDER_LIST) && (field.getListPosition()>-1))
                    ) {
                    orderedFields.add(field);
                }
            }
            Fields.sort(orderedFields, sortOrder);
            sortedFieldLists.put(new Integer(sortOrder), Collections.unmodifiableList(orderedFields));
        } else {
            //log.info("From cache!");
        }
        return orderedFields;
    }

    /**
     * Get the field definitions for the editor, sorted according to it's GUISearch property (as set in the builder xml file).
     * Used for creating search-forms.
     * @deprecated use getFields() with sortorder ORDER_SEARCH
     * @return a vector with CoreField objects
     */
    public Vector getEditFields() {
        return (Vector)getFields(NodeManager.ORDER_SEARCH);
    }

    /**
     * Get the field definitions for the editor, sorted accoring to it's GUIList property (as set in the builder xml file).
     * Used for creating list-forms (tables).
     * @deprecated use getFields() with sortorder ORDER_LIST
     * @return a vector with CoreField objects
     */
    public Vector getSortedListFields() {
        return (Vector)getFields(NodeManager.ORDER_LIST);
    }

    /**
     * Get the field definitions for the editor, sorted according to it's GUIPos property (as set in the builder xml file).
     * Used for creating edit-forms.
     * @deprecated use getFields() with sortorder ORDER_EDIT
     * @return a vector with CoreField objects
     */
    public Vector getSortedFields() {
        return (Vector)getFields(NodeManager.ORDER_EDIT);
    }

    /**
     * Returns the next field as defined by its sortorder, according to the specified order.
     */
    public FieldDefs getNextField(String currentfield, int sortorder) {
        CoreField cdef = getField(currentfield);
        List sortedFields = getFields(sortorder);
        int pos = sortedFields.indexOf(cdef);
        if (pos != -1  && (pos+1) < sortedFields.size()) {
            return (FieldDefs) sortedFields.get(pos+1);
        }
        return null;
    }

    /**
     * Returns the next field as defined by its sortorder, according to it's GUIPos property (as set in the builder xml file).
     * Used for moving between fields in an edit-form.
     * @deprecated use getNextField() with sortorder ORDER_EDIT
     */
    public FieldDefs getNextField(String currentfield) {
        return getNextField(currentfield,NodeManager.ORDER_EDIT);
    }

    /**
     * Returns
     * @since MMBase-1.7.4
     */
    protected BlobCache getBlobCache(String fieldName) {
        return genericBlobCache;
    }

    /**
     * @since MMBase-1.8
     */
    public int clearBlobCache(int nodeNumber) {
        Iterator i = getFields().iterator();
        int result = 0;
        while (i.hasNext()) {
            CoreField field = (CoreField) i.next();
            String fieldName = field.getName();
            BlobCache cache = getBlobCache(fieldName);
            String key = cache.getKey(nodeNumber,fieldName);
            if (cache.remove(key) != null) result++;
        }
        return result;
    }

    /**
     * Provides additional functionality when obtaining field values.
     * This method is called whenever a Node of the builder's type fails at evaluating a getValue() request
     * (generally when a fieldname is supplied that doesn't exist).
     * It allows the system to add 'functions' to be included with a field name, such as 'html(body)' or 'time(lastmodified)'.
     * This method will parse the fieldname, determining functions and calling the {@link #executeFunction} method to handle it.
     * Functions in fieldnames can be given in the format 'functionname(fieldname)'. An old format allows 'functionname_fieldname' instead,
     * though this only applies to the text functions 'short', 'html', and 'wap'.
     * Functions can be nested, i.e. 'html(shorted(body))'.
     * Derived builders should override this method only if they want to provide virtual fieldnames. To provide addiitonal functions,
     * call {@link #addFunction} instead. See also the source code for {@link org.mmbase.util.functions.ExampleBuilder}.
     * @param node the node whos efields are queries
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     */
    public Object getValue(MMObjectNode node, String field) {
        Object rtn = getObjectValue(node, field);

        // Old code
        if (field.indexOf("short_") == 0) {
            String val = node.getStringValue(field.substring(6));
            val = getShort(val,34);
            rtn = val;
        }  else if (field.indexOf("html_") == 0) {
            String val = node.getStringValue(field.substring(5));
            val = getHTML(val);
            rtn = val;
        } else if (field.indexOf("wap_") == 0) {
            String val = node.getStringValue(field.substring(4));
            val = getWAP(val);
            rtn = val;
        }
        // end old
        return rtn;
    }
    /**
     * Like getValue, but without the 'old' code (short_ html_ etc). This is for
     * protected use, when you are sure this is not used, and you can
     * avoid the overhead.
     *
     * @since MMBase-1.6
     * @see #getValue
     */

    protected Object getObjectValue(MMObjectNode node, String field) {
        Object rtn = null;
        int pos1 = field.indexOf('(');
        if (pos1 != -1) {
            int pos2 = field.lastIndexOf(')');
            if (pos2 != -1) {
                String name = field.substring(pos1 + 1, pos2);
                String function = field.substring(0, pos1);
                if (log.isDebugEnabled()) {
                    log.debug("function = '" + function + "', fieldname = '" + name + "'");
                }
                List a = new ArrayList();
                a.add(name);
                rtn = getFunctionValue(node, function, a);

            }
        }
        return rtn;
    }

    /**
     * Parses string containing function parameters.
     * The parameters must be separated by ',' or ';' and may be functions
     * themselves (i.e. a functionname, followed by a parameter list between
     * parenthesis).
     *
     * @param fields The string, containing function parameters.
     * @return List of function parameters (may be functions themselves).
     * @deprecated use executeFunction(node, function, list)
     */
    protected Vector getFunctionParameters(String fields) {
        int commapos =  0;
        int nested =  0;
        Vector v = new Vector();
        int i;
        if (log.isDebugEnabled()) log.debug("Fields=" + fields);
        for(i = 0; i<fields.length(); i++) {
            if ((fields.charAt(i)==',') || (fields.charAt(i)==';')){
                if(nested==0) {
                    v.add(fields.substring(commapos,i).trim());
                    commapos=i+1;
                }
            }
            if (fields.charAt(i)=='(') {
                nested++;
            }
            if (fields.charAt(i)==')') {
                nested--;
            }
        }
        if (i>0) {
            v.add(fields.substring(commapos).trim());
        }
        return v;
    }

    /**
     * Executes a 'function' on a MMObjectNode. The function is
     * identified by a string, and its arguments are passed by a List.
     *
     * The function 'info' should exist, and this will return a Map
     * with descriptions of the possible functions.
     *
     * Call {@link #addFunction} in your extension if you want to add functions.
     *
     * @param node The node on which the function must be executed
     * @param functionName The string identifying the funcion
     * @param parameters The list with function argument or null (which means 'no arguments')
     *
     * @see #executeFunction
     * @since MMBase-1.6
     */
    // package because called from MMObjectNode
    final Object getFunctionValue(MMObjectNode node, String functionName, List parameters) {
        if (parameters == null) parameters = new ArrayList();
        // for backwards compatibility (calling with string function with more than one argument)
        if (parameters.size() == 1 && parameters.get(0) instanceof String) {
            String arg = (String) parameters.get(0);
            Object result =  executeFunction(node, functionName, arg);
            if (result != null) {
                return result;
            }
            parameters = StringSplitter.splitFunctions(arg);
        }
        Function function = getFunction(node, functionName);
        if (function != null) {
            return function.getFunctionValueWithList(parameters);
        } else {
            // fallback
            return executeFunction(node, functionName, parameters);
        }
    }

    /**
     * Instantiates a Function object for a certain function on a certain node of this type.
     * @param node The Node for on which the function must work
     * @param functionName Name of the request function.
     * @return a Function object or <code>null</code> if no such function.
     * @since MMBase-1.8
     */
    protected Function getFunction(MMObjectNode node, String functionName) {
        Function function = getFunction(functionName);
        if (function instanceof NodeFunction) {
            return ((NodeFunction) function).newInstance(node);
        } else {
            return null;
        }
    }

    /**
     * Returns all Functions which are available (or at least known to be available) on a Node.
     * @since MMBase-1.8
     */
    protected Collection getFunctions(MMObjectNode node) {
        Collection builderFunctions = getFunctions();
        Collection nodeFunctions = new HashSet();
        for (Iterator i = builderFunctions.iterator(); i.hasNext();) {
            Object function = i.next();
            if (function instanceof NodeFunction) {
                nodeFunctions.add(((NodeFunction) function).newInstance(node));
            }
        }
        return nodeFunctions;
    }

    /**
     *
     * @inheritDoc
     * @since MMBase-1.8
     */
    protected Function newFunctionInstance(String name, Parameter[] parameters, ReturnType returnType) {
        return new NodeFunction(name, parameters, returnType) {
                public Object getFunctionValue(Node node, Parameters parameters) {
                    return MMObjectBuilder.this.executeFunction(getCoreNode(MMObjectBuilder.this, node),
                                                                name,
                                                                parameters.subList(0, parameters.size() - 1) // removes the node-argument, some legacy impl. get confused
                                                                );
                }
            };
    }

    /**
     * Executes a function on the field of a node, and returns the result.
     * This method is called by the builder's {@link #getValue} method.
     * Derived builders should override this method to provide additional functions.
     *
     * @since MMBase-1.6
     * @throws IllegalArgumentException if the argument List does not
     * fit the function
     * @see #executeFunction
     */
    protected Object executeFunction(MMObjectNode node, String function, List arguments) {
        if (log.isDebugEnabled()) {
            log.debug("Executing function " + function + " on node " + node.getNumber() + " with argument " + arguments);
        }

        if (function.equals("info")) {
            Map info = new HashMap();
            Iterator i = getFunctions(node).iterator();
            while (i.hasNext()) {
                Function f = (Function) i.next();
                info.put(f.getName(), f.getDescription());
            }
            info.put("info", "(functionname) Returns information about a certain 'function'. Or a map of all function if no arguments.");
            if (arguments == null || arguments.size() == 0 || arguments.get(0) == null) {
                log.info("returing " + info);
                return info;
            } else {
                return info.get(arguments.get(0));
            }
        } else if (function.equals("wrap")) {
            if (arguments.size() < 2) throw new IllegalArgumentException("wrap function needs 2 arguments (currently:" + arguments.size() + " : "  + arguments + ")");
            try {
                String val = node.getStringValue((String)arguments.get(0));
                int wrappos = Integer.parseInt((String)arguments.get(1));
                return wrap(val, wrappos);
            } catch(Exception e) {}

        } else if (function.equals("substring")) {
            if (arguments.size() < 2) throw new IllegalArgumentException("substring function needs 2 or 3 arguments (currently:" + arguments.size() + " : "  + arguments + ")");
            try {
                String val = node.getStringValue((String)arguments.get(0));
                int len = Integer.parseInt((String)arguments.get(1));
                if (arguments.size() > 2) {
                    String filler = (String)arguments.get(2);
                    return substring(val, len, filler);
                } else {
                    return substring(val, len, null);
                }
            } catch(Exception e) {
                log.debug(Logging.stackTrace(e));
                return e.toString();
            }
        } else if (function.equals("smartpath")) {
            try {
                String documentRoot = (String) arguments.get(0);
                String path = (String) arguments.get(1);
                String version = (String) arguments.get(2);
                if (version != null) {
                    if (version.equals("")) {
                        version = null;
                    }
                }
                return getSmartPath(documentRoot, path, "" + node.getNumber(), version);
            } catch(Exception e) {
                log.error("Evaluating smartpath for "+node.getNumber()+" went wrong " + e.toString());
            }
        }

        String field = "";
        if (arguments != null && arguments.size() > 0) {
            Object o = arguments.get(0);
            if (o instanceof String) {
                field = (String) o;
            }
        }

        // time functions
        if(function.equals("date")) {                    // date
            int v = node.getIntValue(field);
            return DateSupport.date2string(v);
        } else if (function.equals("time")) {            // time hh:mm
            int v = node.getIntValue(field);
            return DateSupport.getTime(v);
        } else if (function.equals("timesec")) {        // timesec hh:mm:ss
            int v = node.getIntValue(field);
            return DateSupport.getTimeSec(v);
        } else if (function.equals("longmonth")) {        // longmonth September
            int v = node.getIntValue(field);
            return DateStrings.ENGLISH_DATESTRINGS.getMonth(DateSupport.getMonthInt(v));
        } else if (function.equals("monthnumber")) {
            int v = node.getIntValue(field);
            return "" + (DateSupport.getMonthInt(v)+1);
        } else if (function.equals("month")) {            // month Sep
            int v = node.getIntValue(field);
            return DateStrings.DUTCH_DATESTRINGS.getShortMonth(DateSupport.getMonthInt(v));
        } else if (function.equals("weekday")) {        // weekday Sunday
            int v = node.getIntValue(field);
            return DateStrings.DUTCH_DATESTRINGS.getDay(DateSupport.getWeekDayInt(v));
        } else if (function.equals("shortday")) {        // shortday Sun
            int v = node.getIntValue(field);
            return DateStrings.DUTCH_DATESTRINGS.getShortDay(DateSupport.getWeekDayInt(v));
        } else if (function.equals("day")) {            // day 4
            int v = node.getIntValue(field);
            return ""+DateSupport.getDayInt(v);
        } else if (function.equals("shortyear")) {            // year 01
            int v = node.getIntValue(field);
            return (DateSupport.getYear(v)).substring(2);
        } else if (function.equals("year")) {            // year 2001
            int v = node.getIntValue(field);
            return DateSupport.getYear(v);
        } else if (function.equals("thisdaycurtime")) {            //
            int curtime=node.getIntValue(field);
            // gives us the next full day based on time (00:00)
            int days = curtime/(3600*24);
            return "" + ((days*(3600*24))-3600);
        } else if (function.equals("age")) {
            Integer val = new Integer(node.getAge());
            return val.toString();
        } else if (function.equals("wap")) {
            String val = node.getStringValue(field);
            return getWAP(val);
        } else if (function.equals("html")) {
            String val = node.getStringValue(field);
            return getHTML(val);
        } else if (function.equals("shorted")) {
            String val = node.getStringValue(field);
            return getShort(val,32);
        } else if (function.equals("uppercase")) {
            String val = node.getStringValue(field);
            return val.toUpperCase();
        } else if (function.equals("lowercase")) {
            String val = node.getStringValue(field);
            return val.toLowerCase();
        } else if (function.equals("hostname")) {
            String val = node.getStringValue(field);
            return hostname_function(val);
        } else if (function.equals("urlencode")) {
            String val = node.getStringValue(field);
            return getURLEncode(val);
        } else if (function.startsWith("wrap_")) {
            String val = node.getStringValue(field);
            try {
                int wrappos = Integer.parseInt(function.substring(5));
                return wrap(val, wrappos);
            } catch(Exception e) {}
        } else if (function.equals("currency_euro")) {
             double val = node.getDoubleValue(field);
             NumberFormat nf = NumberFormat.getNumberInstance (Locale.GERMANY);
             return  "" + nf.format(val);
        } else {
            StringBuffer arg = new StringBuffer(field);
             if (arguments != null) {
                 for (int i = 1; i < arguments.size(); i++) {
                     if (arg.length() > 0) arg.append(',');
                     arg.append(arguments.get(i));
                 }
             }
             return executeFunction(node, function, arg.toString());
        }
        return null;
    }

    /**
     * Executes a function on the field of a node, and returns the result.
     * This method is called by the builder's {@link #getValue} method.
     *
     * current functions are:<br />
     * on dates: date, time, timesec, longmonth, month, monthnumber, weekday, shortday, day, yearhort year<br />
     * on text:  wap, html, shorted, uppercase, lowercase <br />
     * on node:  age() <br />
     * on numbers: wrap_&lt;int&gt;, currency_euro <br />
     *
     * @param node the node whose fields are queries
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     * @deprecated use {@link #getFunction(MMObjectNode, String)}
     */
    protected Object executeFunction(MMObjectNode node, String function, String field) {
        if (log.isDebugEnabled()) {
            log.debug("Executing function " + function + " on node " + node.getNumber() + " with argument " + field);
        }
        return null;
    }

    /**
     * Returns all relations of a node.
     * This returns the relation objects, not the objects related to.
     * Note that the relations returned are always of builder type 'InsRel', even if they are really from a derived builser such as AuthRel.
     * @param src the number of the node to obtain the relations from
     * @return a <code>Vector</code> with InsRel nodes
     * @todo Return-type and name of this function are not sound.
     */
    public Vector getRelations_main(int src) {
        InsRel bul = mmb.getInsRel();
        if (bul == null) {
            log.error("getMMObject(): InsRel not yet loaded");
            return null;
        }
        return bul.getRelationsVector(src);
    }

    /**
     * Return the default url of this object.
     * The basic value returned is <code>null</code>.
     * @param src the number of the node to obtain the url from
     * @return the basic url as a <code>String</code>, or <code>null</code> if unknown.
     */
    public String getDefaultUrl(int src) {
        return null;
    }

    /**
     * Returns the path to use for TREEPART, TREEFILE, LEAFPART and LEAFFILE.
     * The system searches in a provided base path for a filename that matches the supplied number/alias of
     * a node (possibly extended with a version number). See the documentation on the TREEPART SCAN command for more info.
     * @move maybe to a different SmartPathFunction class?
     * @param documentRoot the root of the path to search
     * @param path the subpath of the path to search
     * @param nodeNumber the number or alias of the node to filter on
     * @param version the version number (or <code>null</code> if not applicable) to filter on
     * @return the found path as a <code>String</code>, or <code>null</code> if not found
     * This method should be added to the bridge so jsp can make use of it.
     * This method can be overriden to make an even smarter search possible.
     */
    public String getSmartPath(String documentRoot, String path, String nodeNumber, String version) {
        File dir = new File(documentRoot+path);
        if (version != null) nodeNumber += "." + version;
        String[] matches = dir.list( new SPartFileFilter( nodeNumber ));
        if ((matches == null) || (matches.length == 0)) {
            return null;
        }
        return path + matches[0] + File.separator;
    }

    /**
     * Get the next object key (unique index for an object).
     * @return an <code>int</code> value that is the next available key for an object.
     */
    public int getDBKey() {
        return mmb.getDBKey();
    }


    /**
     * Get the name of this mmserver from the MMBase Root
     * @return a <code>String</code> which is the server's name
     */
    public String getMachineName() {
        return mmb.getMachineName();
    }

    /**
     * Called when a remote node is changed.
     * Should be called by subclasses if they override it.
     * @param machine Name of the machine that changed the node.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return always <code>true</code>
     * @deprecated use notify(NodeEvent) in stead
     */
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        // signal all the other objects that have shown interest in changes of nodes of this builder type.
        for (Iterator i = remoteObservers.iterator(); i.hasNext();) {
            MMBaseObserver o = (MMBaseObserver) i.next();
            if (o != this) {
                o.nodeRemoteChanged(machine, number, builder, ctype);
            } else {
                log.warn(getClass().getName()  + " " + toString() + " observes itself");
            }
        }
        return true;
    }

    /**
     * Called when a local node is changed.
     * Should be called by subclasses if they override it.
     * @param machine Name of the machine that changed the node.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return always <code>true</code>
     * @deprecated use notify(NodeEvent) in stead
     */

   public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
       // signal all the other objects that have shown interest in changes of nodes of this builder type.
       synchronized(localObservers) {
           for (Iterator i = localObservers.iterator(); i.hasNext();) {
               MMBaseObserver o = (MMBaseObserver)i.next();
               if (o != this) {
                   o.nodeLocalChanged(machine, number, builder, ctype);
               } else {
                   log.warn(getClass().getName()  + " " + toString() + " observes itself");
               }
           }
       }

       return true;
   }

    /**
     * Called when a local field is changed.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param field name of the changed field
     * @param value value it changed to
     * @return always <code>true</code>
     */
    public boolean fieldLocalChanged(String number, String builder, String field, String value) {
        if (log.isDebugEnabled()) {
            log.debug("FLC=" + number + " BUL=" + builder + " FIELD=" + field + " value=" + value);
        }
        return true;
    }

    /**
     * Adds a remote observer to this builder.
     * The observer is notified whenever an object of this builder is changed, added, or removed.
     * @return always <code>true</code>
     * @deprecated use the new event system as well. check out addEventListener(Object listener) or MMBase.addEventListener(EventListener listener)
     */
    public boolean addRemoteObserver(MMBaseObserver obs) {
        if (!remoteObservers.contains(obs)) {
            remoteObservers.add(obs);
        }
        return true;
    }

    /**
     * Adds a local observer to this builder.
     * The observer is notified whenever an object of this builder is changed, added, or removed.
     * @return always <code>true</code>
     * @deprecated use the new event system as well. check out addEventListener(Object listener) or MMBase.addEventListener(EventListener listener)
     */
    public boolean addLocalObserver(MMBaseObserver obs) {
        if (!localObservers.contains(obs)) {
            localObservers.add(obs);
        }
        return true;
    }

    /**
     * @since MMBase-1.8
     */
    public boolean removeLocalObserver(MMBaseObserver obs) {
        return  localObservers.remove(obs);
    }
    /**
     * @since MMBase-1.8
     */
    public boolean removeRemoteObserver(MMBaseObserver obs) {
        return  remoteObservers.remove(obs);
    }

    /**
     *  Used to create a default teaser by any builder
     *  @deprecated Will be removed?
     */
    public MMObjectNode getDefaultTeaser(MMObjectNode node,MMObjectNode tnode) {
        log.warn("getDefaultTeaser(): Generate Teaser,Should be overridden");
        return tnode;
    }

    /**
     * Waits until a node is changed (multicast).
     * @param node the node to wait for
     */
    /*
    public boolean waitUntilNodeChanged(MMObjectNode node) {
        return mmb.mmc.waitUntilNodeChanged(node);
    }
    */

    /**
     * Obtains a list of string values by performing the provided command and parameters.
     * This method is SCAN related and may fail if called outside the context of the SCAN servlet.
     * @param sp The PageInfo (containing http and user info) that calls the function
     * @param tagger a Hashtable of parameters (name-value pairs) for the command
     * @param tok a list of strings that describe the (sub)command to execute
     * @return a <code>Vector</code> containing the result values as a <code>String</code>
     */
    public Vector getList(PageInfo sp, StringTagger tagger, StringTokenizer tok) {
        throw new UnsupportedOperationException(getClass().getName() +" should override the getList method (you've probably made a typo)");
    }


    /**
     * Obtains a string value by performing the provided command.
     * The command can be called:
     * <ul>
     *   <li>by SCAN : $MOD-MMBASE-BUILDER-[buildername]-[command]</li>
     *   <li>in jsp : cloud.getNodeManager(buildername).getInfo(command);</li>
     * </lu>
     * This method is SCAN related and some commands may fail if called outside the context of the SCAN servlet.
     * @param sp The PageInfo (containing http and user info) that calls the function
     * @param tok a list of strings that describe the (sub)command to execute
     * @return the result value as a <code>String</code>
     */
    public String replace(PageInfo sp, StringTokenizer tok) {
        log.warn("replace(): replace called should be overridden");
        return "";
    }

    /**
     * The hook that passes all form related pages to the correct handler.
     * This method is SCAN related and may fail if called outside the context of the SCAN servlet.
     * The methood is currentkly called by the MMEDIT module, whenever a 'PRC-CMD-BUILDER-...' command
     * is encountered in the list of commands to be processed.
     * @param sp The PageInfo (containing http and user info) that calls the function
     * @param command a list of strings that describe the (sub)command to execute (the portion after ' PRC-CMD-BUILDER')
     * @param cmds the commands (PRC-CMD) that are iurrently being processed, including the current command.
     * @param vars variables (PRC-VAR) thatw ere set to be used during processing.
     * @return the result value as a <code>String</code>
     */
    public boolean process(PageInfo sp, StringTokenizer command, Hashtable cmds, Hashtable vars) {
        return false;
    }

    /**
     * Set description of the builder
     * @param e the description text
     */
    public void setDescription(String e) {
        this.description=e;
        update();
    }

    /**
     * Set descriptions of the builder
     * @param e a <code>Hashtable</code> containing the descriptions
     */
    public void setDescriptions(Hashtable e) {
        this.descriptions=e;
        update();
    }

    /**
     * Get description of the builder
     * @return the description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets description of the builder, using the specified language.
     * @param lang The language requested
     * @return the descriptions in that language, or <code>null</code> if it is not avaialble
     */
    public String getDescription(String lang) {
        if (descriptions==null) return null;
        String retval =(String)descriptions.get(lang);
        if (retval == null){
            return getDescription();
        }
        return retval;
    }

    /**
     * Get descriptions of the builder
     * @return  a <code>Hashtable</code> containing the descriptions
     */
    public Hashtable getDescriptions() {
        return descriptions;
    }

    /**
     * Sets search Age.
     * @param age the search age as a <code>String</code>
     */
    public void setSearchAge(String age) {
        this.searchAge=age;
        update();
    }

    /**
     * Gets search Age
     * @return the search age as a <code>String</code>
     */
    public String getSearchAge() {
        return searchAge;
    }

    /**
     * Gets short name of the builder, using the specified language.
     * @param lang The language requested
     * @return the short name in that language, or <code>null</code> if it is not available
     */
    public String getSingularName(String lang) {
    String tmp =null;
        if (singularNames !=null) {
            tmp = (String)singularNames.get(lang);
            if (tmp==null) tmp = (String)singularNames.get(mmb.getLanguage());
            if (tmp==null) tmp = (String)singularNames.get("en");
    }
        if (tmp==null) tmp = tableName;
        return tmp;
    }

    /**
     * Gets short name of the builder in the current default language.
     * If the current language is not available, the "en" version is returned instead.
     * If that name is not available, the internal builder name (table name) is returned.
     * @return the short name in either the default language or in "en"
     */
    public String getSingularName() {
        return getSingularName(mmb.getLanguage());
    }

    /**
     * Gets long name of the builder, using the specified language.
     * @param lang The language requested
     * @return the long name in that language, or <code>null</code> if it is not available
     */
    public String getPluralName(String lang) {
        String tmp = null;
        if (pluralNames !=null){
        tmp= (String)pluralNames.get(lang);
            if (tmp==null) tmp = (String)pluralNames.get(mmb.getLanguage());
            if (tmp==null) tmp = (String)pluralNames.get("en");
            if (tmp==null) tmp = getSingularName(lang);
    }
        if (tmp==null) tmp = tableName;
        return tmp;
    }

    /**
     * Gets long name of the builder in the current default language.
     * If the current language is not available, the "en" version is returned instead.
     * If that name is not available, the singular name is returned.
     * @return the long name in either the default language or in "en"
     */
    public String getPluralName() {
        return getPluralName(mmb.getLanguage());
    }

    /**
     * Returns the classname of this builder
     * @deprecated don't use
     */
    public String getClassName() {
        return this.getClass().getName();
    }

    /**
     * Send a signal to other servers that a field was changed.
     * @param node the node the field was changed in
     * @param fieldName the name of the field that was changed
     * @return always <code>true</code>
     */
    public boolean    sendFieldChangeSignal(MMObjectNode node,String fieldName) {
        // we need to find out what the DBState is of this field so we know
        // who to notify of this change
        int state=getDBState(fieldName);
        log.debug("Changed field="+fieldName+" dbstate="+state);

        // still a large hack need to figure out remote changes
        if (state==0) {}
        // convert the field to a string

        int type=getDBType(fieldName);
        String value="";
        if ((type==Field.TYPE_INTEGER) || (type==Field.TYPE_NODE)) {
            value=""+node.getIntValue(fieldName);
        } else if (type==Field.TYPE_STRING) {
            value=node.getStringValue(fieldName);
        } else {
            // should be mapped to the builder
        }

        fieldLocalChanged("" + node.getNumber(), tableName, fieldName, value);
        //mmb.mmc.changedNode(node.getNumber(),tableName,"f");
        return true;
    }

    /**
     * Send a signal to other servers that a new node was created.
     * @param tableName the table in which a node was edited (?)
     * @param number the number of the new node
     * @return always <code>true</code>
     */
    /*
    public boolean signalNewObject(String tableName,int number) {
        if (mmb.mmc!=null) {
            mmb.mmc.changedNode(number,tableName,"n");
        }
        return true;
    }
    */


    /**
     * Sets a list of singular names (language - value pairs)
     */
    public void setSingularNames(Hashtable names) {
        singularNames=names;
        update();
    }

    /**
     * Gets a list of singular names (language - value pairs)
     */
    public Hashtable getSingularNames() {
        return singularNames;
    }

    /**
     * Sets a list of plural names (language - value pairs)
     */
    public void setPluralNames(Hashtable names) {
        pluralNames=names;
        update();
    }

    /**
     * Gets a list of plural names (language - value pairs)
     */
    public Hashtable getPluralNames() {
        return pluralNames;
    }

    /**
     * Get text from a blob field. This function is called to 'load' a field into the node, because
     * it was not loaded together with the node, because it is supposed to be too big.
     * @param fieldName name of the field
     * @param node
     * @return a <code>String</code> containing the complate contents of a field as text.
     * @since MMBase-1.8
     */
    protected String getShortedText(String fieldName, MMObjectNode node) {
        if (node.getNumber() < 0) return null; // capture calls from temporary nodes
        try {
            return mmb.getStorageManager().getStringValue(node, getField(fieldName));
        } catch (StorageException se) {
            log.error(se.getMessage());
            log.error(Logging.stackTrace(se));
            return null;
        }
    }

    /**
     * Get binary data of a blob field. This function is called to 'load' a field into the node, because
     * it was not loaded together with the node, because it is supposed to be too big.
     * @param fieldName name of the field
     * @param node
     * @return an array of <code>byte</code> containing the complete contents of the field.
     * @since MMBase-1.8
     */
    protected byte[] getShortedByte(String fieldName, MMObjectNode node) {
        if (node.getNumber() < 0) return null; // capture calls from temporary nodes
        try {
            return mmb.getStorageManager().getBinaryValue(node, getField(fieldName));
        } catch (StorageException se) {
            log.error(se.getMessage());
            log.error(Logging.stackTrace(se));
            return null;
        }
    }

    /**
     *  Sets a key/value pair in the main values of this node.
     *  Note that if this node is a node in cache, the changes are immediately visible to
     *  everyone, even if the changes are not committed.
     *  The fieldname is added to the (public) 'changed' vector to track changes.
     *  @param fieldName the name of the field to change
     *  @param node      The node on which to change the field (the new value is in this node)
     *  @param originalValue the value which was original in the field
     *  @return <code>true</code> When an update is required(when changed),
     *    <code>false</code> if original value was set back into the field.
     */
    public  boolean setValue(MMObjectNode node, String fieldName, Object originalValue) {
        return setValue(node, fieldName);
    }

    /**
     * Provides additional functionality when setting field values.
     * This method is called whenever a Node of the builder's type tries to change a value.
     * It allows the system to add functionality such as checking valid data.
     * Derived builders should override this method if they want to add functionality.
     * @param node the node whose fields are changed
     * @param fieldName the fieldname that is changed
     * @return <code>true</code> if the call was handled.
     */
    public  boolean setValue(MMObjectNode node, String fieldName) {
        return true;
    }

    /**
     * Returns a HTML-version of a string.
     * This replaces a number of tokens with HTML sequences.
     * The default output does not match well with the new xhtml standards (ugly html), nor does it replace all tokens.
     *
     * Default replacements can be overridden by setting the builder properties in your <builder>.xml:
     *
     * html.alinea
     * html.endofline
     *
     * Example:
     * <properties>
     *   <property name="html.alinea"> &lt;br /&gt; &lt;br /&gt;</property>
     *   <property name="html.endofline"> &lt;br /&gt; </property>
     * </properties>
     *
     * @param body text to convert
     * @return the convert text
     * @deprecated
     */

    protected String getHTML(String body) {
        String rtn="";
        if (body!=null) {
            StringObject obj=new StringObject(body);
            // escape ampersand first
            obj.replace("&", "&amp;");

            obj.replace("<","&lt;");
            obj.replace(">","&gt;");
            // escape dollar-sign (prevent SCAN code to be run)
            obj.replace("$","&#36;");
            // unquote ampersand and quotes (see escapeXML method)
            obj.replace("\"", "&quot;");
            obj.replace("'", "&#39;");

            String alinea=getInitParameter("html.alinea");
            String endofline=getInitParameter("html.endofline");

            if (alinea!=null) {
                obj.replace("\r\n\r\n",alinea);
                obj.replace("\n\n",alinea);
            } else {
                obj.replace("\r\n\r\n", DEFAULT_ALINEA);
                obj.replace("\n\n", DEFAULT_ALINEA);
            }

            if (endofline!=null) {
                obj.replace("\r\n",endofline);
                obj.replace("\n",endofline);
            } else {
                obj.replace("\r\n", DEFAULT_EOL);
                obj.replace("\n", DEFAULT_EOL);
            }

            rtn=obj.toString();
        }
        return rtn;
    }

    /**
     * Returns a WAP-version of a string.
     * This replaces a number of tokens with WAP sequences.
     * @param body text to convert
     * @return the convert text
     */
    protected String getWAP( String body ) {
        String result = "";
        if( body != null ) {
            StringObject obj=new StringObject(body);
            obj.replace("\"","&#34;");
            obj.replace("&","&#38;#38;");
            obj.replace("'","&#39;");
            obj.replace("<","&#38;#60;");
            obj.replace(">","&#62;");
            result = obj.toString();
        }
        return result;
    }

    /**
     * Returns a URLEncoded-version (MIME x-www-form-urlencoded) of a string.
     * This version uses the java.net.URLEncoder class to encode it.
     * @param body text to convert
     * @return the URLEncoded text
     */
    protected String getURLEncode(String body) {
        String rtn="";
        if (body != null) {
            rtn = URLEncoder.encode(body);
        }
        return rtn;
    }

    /**
     * Support routine to return shorter strings.
     * Cuts a string to a amximum length if it exceeds the length specified.
     * @param str the string to shorten
     * @param len the maximum length
     * @return the (possibly shortened) string
     */
    public String getShort(String str,int len) {
        if (str.length()>len) {
            return str.substring(0,(len-3))+"...";
        } else {
            return str;
        }
    }

    /**
     * Stores fields information of this table.
     * Asside from the fields supplied by the caller, a field 'otype' is added (if missing).
     *
     * @param f A List with fields (as CoreField objects) as defined by MMBase. This may not be in sync with the actual database table, about which Storage will report then.
     */
    public void setFields(List f) {
        fields.clear();

        Iterator i = f.iterator();
        while (i.hasNext()) {
            CoreField def = (CoreField) i.next();
            String name = def.getName();
            def.setParent(this);
            fields.put(name, def);
        }

        // should be TYPE_NODE ???
        if (fields.get(FIELD_OBJECT_TYPE) == null) {
            log.warn("Object 'otype' field is not defined. Please update your object.xml, or update '" + getConfigResource() + "' to extend object");
            // if not defined in XML (legacy?)
            // It does currently not work if otype is actually defined in object.xml (as a NODE field)
            CoreField def = Fields.createSystemField(FIELD_OBJECT_TYPE, Field.TYPE_NODE);
            def.setGUIName("Type");
            // here, we should set the DBPos to 2 and adapt those of the others fields
            def.setStoragePosition(2);
            def.getDataType().setRequired(true);
            i = f.iterator();
            while (i.hasNext()) {
                CoreField field = (CoreField) i.next();
                int pos = field.getStoragePosition();
                if (pos > 1) field.setStoragePosition(pos + 1);
            }
            def.setParent(this);
            def.finish();
            fields.put(FIELD_OBJECT_TYPE, def);
        }
        updateFields();
    }

    /**
     * Sets the subpath of the builder's xml configuration file.
     */
    public void setXMLPath(String m) {
        xmlPath = m;
        update();
    }

    /**
     * Retrieves the subpath of the builder's xml configuration file.
     * Needed for builders that reside in subdirectories in the builder configuration file directory.
     */
    public String getXMLPath() {
         return xmlPath;
    }

    /**
     * @since MMBase-1.8
     */

    public String getConfigResource() {
        return "builders/" + getXMLPath() + "/" + getTableName() + ".xml";
    }

    /**
     * Gets the file that contains the configuration of this builder
     * @return the builders configuration File object
     * @deprecated Need something as getConfigResource in stead.
     */
    public File getConfigFile() {
        // what is the location of our builder?
        List files = ResourceLoader.getConfigurationRoot().getFiles(getConfigResource());
        if (files.size() == 0) {
            return null;
        } else {
            return (File) files.get(0);
        }
        //return null;
    }

    /**
     * Set all builder properties
     * Changed properties will not be saved.
     * @param properties the properties to set
     */
    void setInitParameters(Hashtable properties) {
        this.properties = properties;
        update();
    }

    /**
     * Get all builder properties
     * @return a <code>Hashtable</code> containing the current properties
     */
    public Hashtable getInitParameters() {
        return properties;
    }

    /**
     * Set a single builder property
     * The propertie will not be saved.
     * @param name name of the property
     * @param value value of the property
     */
    public void setInitParameter(String name, String value) {
        if (properties == null) properties = new Hashtable();
        properties.put(name,value);
        update();
    }

    /**
     * Retrieve a specific property.
     * @param name the name of the property to get
     * @return the value of the property as a <code>String</code>
     */
    public String getInitParameter(String name) {
        if (properties == null) {
            return null;
        } else {
            return (String)properties.get(name);
        }
    }

    /**
     * Sets the version of this builder
     * @param i the version number
     */
    public void setVersion(int i) {
        version=i;
        update();
    }

    /**
     * Retrieves the version of this builder
     * @return the version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Retrieves the maintainer of this builder
     * @return the name of the maintainer
     */
    public String getMaintainer() {
        return maintainer;
    }

    /**
     * Sets the maintainer of this builder
     * @param m the name of the maintainer
     */
    public void setMaintainer(String m) {
        maintainer=m;
        update();
    }

    /**
     * hostname, parses the hostname from a url, so http://www.mmbase.org/bug
     * becomed www.mmbase.org
     * @deprecated Has nothing to do with mmbase nodes. Should be in org.mmbase.util
     */
    public String hostname_function(String url) {
        if (url.startsWith("http://")) {
            url=url.substring(7);
        }
        if (url.startsWith("https://")) {
            url=url.substring(8);
        }
        int pos=url.indexOf("/");
        if (pos!=-1) {
            url=url.substring(0,pos);
        }
        return url;
    }

    /**
     * Wraps a string.
     * Inserts newlines (\n) into a string at periodic intervals, to simulate wrapping.
     * This also removes whitespace to the start of a line.
     * @param text the text to wrap
     * @param width the maximum width to wrap at
     * @return the wrapped tekst
     */
    public String wrap(String text,int width) {
        StringTokenizer tok;
        String word;
        StringBuffer dst=new StringBuffer();
        int pos;

        tok=new StringTokenizer(text," \n\r",true);
        pos=0;
        while(tok.hasMoreTokens()) {
            word=tok.nextToken();
            if (word.equals("\n")) {
                pos=0;
            } else if (word.equals(" ")) {
                if (pos==0) {
                    word="";
                } else {
                    pos++;
                    if (pos>=width) {
                        word="\n";
                        pos=0;
                    }
                }
            } else {
                pos+=word.length();
                if (pos>=width) {
                    dst.append("\n");
                    pos=word.length();
                }
            }
            dst.append(word);
        }
        return dst.toString();
    }

    /**
     * Gets a substring.
     * @param value the string to get a substring of
     * @param len the length of the substring
     * @param filler if not null, this field is used as a trailing tekst
     * of the created substring.
     * @return the substring
     */
    private String substring(String value,int len,String filler) {
        if (filler==null) {
            if (value.length()>len) {
                return value.substring(0,len);
            } else {
                return value;
            }
        } else {
            int len2=filler.length();
            if ((value.length()+len2)>len) {
                return value.substring(0,(len-len2))+filler;
            } else {
                return value;
            }
        }
    }

    /**
     * Implmenting a sensible toString is usefull for debugging.
     *
     * @since MMBase-1.6.2
     */

    public String toString() {
        return getSingularName();
    }

    /**
     * Equals must be implemented because of the list of MMObjectBuilder which is used for ancestors
     *
     * Declared the method final, because the instanceof operator is used. This is the only
     * MMObjectBuilder is frequently extended and subclasses will always break
     * the equals contract.
     * When subclasses require to implement the equals method then we should use
     * getClass() == o.getClass(), but this has its own issues. For more info, search for equality in Java
     *
     * @since MMBase-1.6.2
     */
    public final boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof MMObjectBuilder) {
            MMObjectBuilder b = (MMObjectBuilder) o;
            return tableName.equals(b.tableName);
        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return tableName == null ? 0 : tableName.hashCode();
    }

    /**
     * Implements for MMObjectNode
     * @since MMBase-1.6.2
     */

    public String toString(MMObjectNode n) {
        return n.defaultToString();
    }

    /**
     * Implements equals for nodes (this is in MMObjectBuilder because you cannot override MMObjectNode)
     *
     * @since MMBase-1.6.2
     */

    public boolean equals(MMObjectNode o1, MMObjectNode o2) {
        return o1.defaultEquals(o2);
    }

    /**
     * Implements for MMObjectNode
     * @since MMBase-1.6.2
     */

    public int hashCode(MMObjectNode o) {
        return 127 * o.getNumber();
    }

    /**
     * simple way to register a NodeEvent listener and a RelationEventListener
     * at the same time.
     * @see MMBase#addNodeRelatedEventsListener(String, Object)
     * @param listener
     * @since MMBase-1.8
     */
    public void addEventListener(org.mmbase.core.event.EventListener listener){
        mmb.addNodeRelatedEventsListener(getTableName(), listener);
    }

    /**
     * @param listener
     * @since MMBase-1.8
     */
    public void removeEventListener(org.mmbase.core.event.EventListener listener){
        mmb.removeNodeRelatedEventsListener(getTableName(), listener);
    }

    /**
     * @see org.mmbase.core.event.NodeEventListener#notify(org.mmbase.core.event.NodeEvent)
     * here we handle all the backward compatibility stuff.
     * this method covers for both node and relation events.
     * @since MMBase-1.8
     */
    public void notify(NodeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("" + this + " received node event " + event);
        }
        int type = event.getType();
        eventBackwardsCompatible(event.getMachine(), event.getNodeNumber(), type);

        //update the cache
        Integer changedNodeNumber = new Integer(event.getNodeNumber());

        //and now refire the event for the parent builders
        MMObjectBuilder pb = getParentBuilder();
        if(pb != null) {
            if (log.isDebugEnabled()) {
                log.debug(getTableName() + "creating node event for parent builder: " + pb.getTableName());
            }
            NodeEvent parentBuilderEvent = event.clone(pb.getTableName());
            EventManager.getInstance().propagateEvent(parentBuilderEvent);
        }

        if(type == Event.TYPE_DELETE || ((! event.isLocal()) && type == Event.TYPE_CHANGE)) {
            if (nodeCache.remove(changedNodeNumber) != null && log.isDebugEnabled()) {
                log.debug("Removed node " + changedNodeNumber + " from node cache");
            }
        }



    }


    /**
     * @since MMBase-1.8
     */
    public void notify(RelationEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("" + this + " received relation event " + event);
        }

         //for backwards compatibilty: create relation changed calls
         if (event.getRelationSourceType().equals(getTableName())) {
             eventBackwardsCompatible(event.getMachine(), event.getRelationSourceNumber(), NodeEvent.TYPE_RELATION_CHANGE);
         }
         if (event.getRelationDestinationType().equals(getTableName())) {
             eventBackwardsCompatible(event.getMachine(), event.getRelationDestinationNumber(), NodeEvent.TYPE_RELATION_CHANGE);
         }

         //update the cache
         Integer changedNode = new Integer((event.getRelationDestinationType().equals(getTableName()) ? event.getRelationSourceNumber() : event.getRelationDestinationNumber()));
         MMObjectNode node = (MMObjectNode)nodeCache.get(changedNode);
         if (node != null) {
             node.delRelationsCache();
         }

         //      and now refire the event for the parent builders
         MMObjectBuilder pb = getParentBuilder();
         send_event:
         if(pb != null){
             RelationEvent parentBuilderEvent;
             if (log.isDebugEnabled()) {
                 log.debug(getTableName() + "creating relation event for parent builder: " + pb.getTableName());
             }

             if(getTableName().equals(event.getRelationSourceType())) {
                 log.debug(">> overwriting the relation source type");
                 parentBuilderEvent = event.clone(pb.getTableName(), event.getRelationDestinationType());
             } else if(getTableName().equals(event.getRelationDestinationType())) {
                 log.debug(">> overwriting the relation destination type");
                 parentBuilderEvent = event.clone(event.getRelationSourceType(), pb.getTableName());
             } else {
                log.error(" relation source and destination type do not match builder " + getTableName());
                break send_event;
             }
             EventManager.getInstance().propagateEvent(parentBuilderEvent);
         }
     }

    /**
     * @see org.mmbase.core.event.NodeEventListener#notify(org.mmbase.core.event.NodeEvent)
     * here we handle all the backward compatibility stuff.
     * this method covers for both node and relation events.
     * @since MMBase-1.8
     * @param event
     */
    private void eventBackwardsCompatible(String machineName, int nodeNumber, int eventType) {
        String ctype       = NodeEvent.newTypeToOldType(eventType);
        boolean localEvent = mmb.getMachineName().equals(machineName);

        if(localEvent) {
            nodeLocalChanged(machineName, "" + nodeNumber, getTableName(), ctype);
        } else {
            nodeRemoteChanged(machineName, "" + nodeNumber, getTableName(), ctype);
        }
    }

}


