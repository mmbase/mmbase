/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.corebuilders;

import java.io.*;
import java.util.*;

import org.xml.sax.InputSource;

import org.mmbase.storage.search.implementation.*;
import org.mmbase.storage.search.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.BuilderReader;

/**
 * TypeDef is used to define the* object types (builders).
 * Nodes of this builder have a virtual 'config' field.
 * This field contains the xml-Document of the builder represented by the node.
 * The filename used to reference the xml document is derived by extending the field 'name'.
 * Creating a new typedef node automatically creates a new xml file and loads a new builder.
 * Removing a node drops and unloads a builder (including the xml).
 * Changes to the config will also be active on commit of the node.
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @version $Id$
 */
public class TypeDef extends MMObjectBuilder {

    /**
     * The property in the builder file ('deploy-dir') that sets the directory
     * where new builder configuration files are to be deployed.
     */
    public static final String PROPERTY_DEPLOY_DIR = "deploy-dir";

    // Logger routine
    private static final Logger log = Logging.getLoggerInstance(TypeDef.class);
    // Directory where new builder configuration files are deployed by default
    String defaultDeploy = null;

    /**
     * Number-to-name cache.
     */
    private Map<Integer, String> numberToNameCache = null; // object number -> typedef name

    /**
     * Name-to-number cache.
     */
    private Map<String, Integer> nameToNumberCache = null; // typedef name -> object number

    /**
     * List of known builders.
     */
    private final Vector<String> typedefsLoaded = new Vector<String>();     // Contains the names of all active builders

    /**
     * Sets the default deploy directory for the builders.
     * @return true if init was completed, false if uncompleted.
     */
    @Override
    public boolean init() {
        broadCastChanges = false;
        boolean result = super.init();
        if (defaultDeploy == null) {
            // determine default deploy directory
            String builderDeployDir = getInitParameter(PROPERTY_DEPLOY_DIR);
            if (builderDeployDir == null) {
                builderDeployDir = "applications";
            }
            defaultDeploy = builderDeployDir;
        if (!defaultDeploy.endsWith("/") && !defaultDeploy.endsWith("\\")) {
        defaultDeploy+="/";
        }
            log.service("Using '" + defaultDeploy + "' as default deploy dir for our builders.");
        }
        return result;
    }

    protected Map<Integer, String> getNumberToNameCache() {
        if (numberToNameCache == null) readCache();
        return numberToNameCache;
    }

    protected Map<String, Integer> getNameToNumberCache() {
        if (nameToNumberCache == null) readCache();
        return nameToNumberCache;
    }

    /**
     * Insert a new object (content provided) in the cloud, including an entry for the object alias (if provided).
     * This method indirectly calls {@link #preCommit}.
     * Asside from that, this method loads the builder this node represents, and initalizes it. If you do
     * not wish to load the builder (i.e. because it is already loaded), use {@link #insert(String, MMObjectNode, boolean)}
     * @param owner The administrator creating the node
     * @param node The object to insert. The object need be of the same type as the current builder.
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     */
    @Override
    public int insert(String owner, MMObjectNode node) {
        return insert(owner, node, true);
    }

    /**
     * Insert a new object (content provided) in the cloud, including an entry for the object alias (if provided).
     * This method indirectly calls {@link #preCommit}.
     * @param owner The administrator creating the node
     * @param node The object to insert. The object need be of the same type as the current builder.
     * @param loadBuilder if <code>true</code>, the builder should be loaded. This method is set to
     *        <code>false</code> when it is called from the init() method of MMObjectBuilder to prevent
     *        it from being loaded twice
     * @return An <code>int</code> value which is the new object's unique number, -1 if the insert failed.
     */
    public int insert(String owner, MMObjectNode node, boolean loadBuilder) {
        if (log.isDebugEnabled()) {
            // would be logical to log this in SERVICE but the same occurance is logged on INFO already in MMObjectBuilder.init()
            log.debug("Insert of builder-node with name '" + node.getStringValue("name") + "', loadBuilder = " + loadBuilder);
        }
        // look if we can store to file, if it aint there yet...
        String path = getBuilderConfiguration(node);
        java.net.URL url = mmb.getBuilderLoader().getResource(path);
        try {
            if (! url.openConnection().getDoInput()) {
                // first store our config....
                storeBuilderConfiguration(node);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // Quick fix around MMB-1590. Perhaps it should be solved more genericly, closer to the
        // storage layer.
        String desc = node.getStringValue("description");
        if (desc.length() > getField("description").getMaxLength()) {
            node.setValue("description", desc.substring(0, getField("description").getMaxLength()));
        }

        // try if the builder was already in TypeDef for some reason
        // this can happen when another thread was here first
        int result = getIntValue(node.getStringValue("name"));
        if (result < 0) {
            // otherwise save the node
            result = super.insert(owner, node);
        }
        if (result != -1) {
            // update the cache
            Integer number = result;
            String name = node.getStringValue("name");
            getNameToNumberCache().put(name, number);
            getNumberToNameCache().put(number, name);
            // Load the builder if needed
            if (loadBuilder) {
                loadBuilder(node);
            }
        }
        return result;
    }


    /**
     * Commit changes to this node to the database. This method indirectly calls {@link #preCommit}.
     * Use only to commit changes - for adding node, use {@link #insert}.
     * @param node The node to be committed
     * @return true if commit successful
     */

    @Override
    public boolean commit(MMObjectNode node) {
        log.service("Commit of builder-node with name '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        try {
            MMObjectBuilder builder = getBuilder(node);
            BuilderReader originalBuilderXml = new BuilderReader(mmb.getBuilderLoader().getDocument(getBuilderConfiguration(node)), getMMBase());
            String config = node.getStringValue("config");
            StringReader stringReader;
            if (config.indexOf("xmlns=\"http://www.mmbase.org/xmlns/builder\"") > 0) {
                stringReader = new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + config);
            } else {
                stringReader = new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                "<!DOCTYPE builder PUBLIC \"" + BuilderReader.PUBLIC_ID_BUILDER +
                                                "\" \":https://www.mmbase.org/dtd/" + BuilderReader.DTD_BUILDER + "\" >\n" +
                                                config);
            }
            BuilderReader newBuilderXml      = new BuilderReader(new InputSource(stringReader), getMMBase());
            if (!originalBuilderXml.equals(newBuilderXml)) {
                try {
                    // unload the builder...
                    builder = unloadBuilder(node);
                    // attempt to apply changes to the database
                    // by dropping the buildertable (ARGH!)
                    if (! originalBuilderXml.storageEquals(newBuilderXml)) {
                        builder.delete();
                    }
                    // finally save our new config.
                    storeBuilderConfiguration(node);
                } finally {
                    // clear config, so it will be refreshed later on
                    node.storeValue("config", null);
                    // load the builder again.. (will possibly create a new table)
                    loadBuilder(node);
                }
            }
        } catch (Exception ioe) {
            log.error(ioe.getMessage(), ioe);
        }
        return super.commit(node);
    }


    /**
     * Remove a node from the cloud, when the represented builder was active
     * it will also be unloaded
     * @param node The node to remove.
     * @throws RuntimeException When the operation could not be performed
     */
    @Override
    public void removeNode(MMObjectNode node) {
        log.info("Remove of builder-node with name '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        // only delete when builder is completely empty...
        MMObjectBuilder builder = getBuilder(node);
        testBuilderRemovable(builder, node);
        builder = unloadBuilder(node);
        // now that the builder cannot be started again (since config is now really missing)
        if (builder != null) {
            builder.delete();
        }
        // try to delete the configuration file first!.....
        if (!deleteBuilderConfiguration(node)) {
            // delete-ing failed, reload the builder again...
            loadBuilder(node);
            throw new RuntimeException("Could not delete builder config");
        }
        Integer number = node.getIntegerValue("number");
        String name = node.getStringValue("name");
        super.removeNode(node);
        getNameToNumberCache().remove(name);
        getNumberToNameCache().remove(number);
    }

    /**
     * Fill the typedef caches with the initial values.
     * Caches filled are a number-to-name and a name-to-number cache.
     * @duplicate should be moved to org.mmbase.cache
     * @return always true
     */
    private boolean readCache() {
        // at least fill in typedef
        log.service("Reading typedef caches");
        numberToNameCache = Collections.synchronizedMap(new HashMap<Integer, String>());
        nameToNumberCache = Collections.synchronizedMap(new HashMap<String, Integer>());
        NodeSearchQuery query = new NodeSearchQuery(this);
        try {
            for (MMObjectNode n : getNodes(query)) {
                Integer number = n.getIntegerValue("number");
                String name    = n.getStringValue("name");
                if (number != null && name != null) {
                    nameToNumberCache.put(name, number);
                    numberToNameCache.put(number, name);
                } else {
                    log.error("Could not add typedef cache-entry number/name= " + number + "/" + name);
                }
            }
        } catch (SearchQueryException sqe) {
            // should never happen.
            log.error(sqe);
        }
        return true;
    }

    /**
     * Obtain the type value of the requested builder
     * @todo smarter cache update
     * @param builderName name of the builder
     * @return the object type as an int, -1 if not defined.
     */
    public int getIntValue(String builderName) {
        Integer result = getNameToNumberCache().get(builderName);
        if (result != null) {
            return result.intValue();
        } else {
            return -1;
        }
    }

    /**
     * Obtain the buildername of the requested type
     * @param type the object type
     * @return the name of the builder as a string, null if not found
     */
    public String getValue(int type) {
        String result = getNumberToNameCache().get(type);
        if (result == null) {
            log.warn("Could not find builder name for typedef number " + type);
        }
        return result;
    }

    /**
     * Obtain the buildername of the requested type
     * @param type the object type
     * @return the name of the builder as a string, "unknown" if not found
     * @deprecated use getValue(int)
     */
    public String getValue(String type) {
        try {
            return getNumberToNameCache().get(Integer.parseInt(type));
        } catch(Exception e) {
            return "unknown";
        }
    }

    /**
     * @javadoc
     */
    public String getSingularName(String builderName, String language) {
        if (builderName == null) return "unknown";
        MMObjectBuilder bul = mmb.getBuilder(builderName);
        if (bul!=null) {
            if (language == null) {
                return bul.getSingularName();
            } else {
                return bul.getSingularName(language);
            }
        } else {
            return "inactive ("+builderName+")";
        }
    }

    /**
     * @javadoc
     */
    public boolean isRelationTable(String name) {
        return mmb.getRelDef().isRelationTable(name);
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
     * override {@link #executeFunction} instead.
     * @param node the node whos efields are queries
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     */
    @Override
    public Object getValue(MMObjectNode node, String field) {
        if (log.isDebugEnabled()) {
            log.debug("node:" + node.getNumber() + " field: " + field);
        }
        // return the Document from the config file..
        if (field.equals("config")) {
            // first check if we already have a value in node fields...
            Object o = super.getValue(node, field);
            if (o != null) {
                return o;
            }
            // otherwise, open the file to return it...
            if (log.isDebugEnabled()) {
               log.debug("retrieving the document for node #" + node.getNumber());
            }

                // method node.getStringValue("name") should work, since getStringValue("path") checked it already...
            String path = getBuilderConfiguration(node);
            org.w3c.dom.Document doc;
            try {
                doc = mmb.getBuilderLoader().getDocument(path);
            } catch (Exception e) {
                log.warn("Error reading builder with name: " + path + " " + e.getMessage());
                return null;
            }
            if (doc == null) {
                log.warn("Resource with name: " + path + " didnt exist, getValue will return null for builder config");
                return null;
            }
            node.setValue(field, doc);
            return doc;
        } else if (field.equals("state")) {
            int val=node.getIntValue("state");
            // is it set allready ? if not set it, this code should be
            // removed ones the autoreloader/state code is done.
            if (val==-1) {
                // state 1 is up and running
                node.setValue("state",1);
            }
            return ""+val;
        } else if (field.equals("dutchs(name)")) {
            // replace this function with gui(name) ?
            // but have to change admin pages first
            return getGUIIndicator("name",node);
        }
        return super.getValue(node, field);
    }

    /**
     * Sets a key/value pair in the main values of this node.
     * Note that if this node is a node in cache, the changes are immediately visible to
     * everyone, even if the changes are not committed.
     * The fieldname is added to the (public) 'changed' vector to track changes.
     * @param node
     * @param fieldName the name of the field to change
     * @param originalValue the value which was original in the field
     * @return <code>true</code> When an update is required(when changed),
     * <code>false</code> if original value was set back into the field.
     */
    @Override
    public boolean setValue(MMObjectNode node, String fieldName, Object originalValue) {
        Object newValue = node.retrieveValue(fieldName);
        if (fieldName.equals("name")) {
            // the field with the name 'name' may not be changed.....
            if (originalValue != null && // perhaps legacy, name is null becaue name field was nullable?
                ! originalValue.equals("") && // name field is
                !originalValue.equals(newValue)) {
                // restore the original value...
                node.storeValue(fieldName, originalValue);
                throw new RuntimeException("Cannot change a builder's name from '" + originalValue + "' to '" + newValue + "' typedef node " + node.getNumber());
/*            } else if (fieldName.equals("config")) {
                MMObjectBuilder builder = getBuilder(node);
                // TODO: active / not active code.. IT CAN MESS UP BUILDERS THAT ARE SET INACTIVE, AND STILL HAVE DATA IN DATABASE!
                if (builder == null) {
                    log.warn("No builder found for typedef node " + node);
                } else if (builder.size() > 0) {
                    throw new RuntimeException("Cannot change builder config it has nodes (otherwise information could get lost..)");
                } else {
                    log.info("Changing config for typedef " + node + " associated with builder '" + builder.getTableName() + "'");
                }
*/
            }
        }
        return true;
    }

    /**
     * @javadoc
     */
    @Override
    public boolean fieldLocalChanged(String number, String builder, String field, String value) {
        if (field.equals("state")) {
            if (value.equals("4")) {
                // reload request
                log.service("Reload wanted on : " + builder);
                // perform reload
                MMObjectNode node = getNode(number);
                String objectname = node.getStringValue("name");
                reloadBuilder(objectname);
                if (node != null) {
                    node.setValue("state", 1);
                }
            }
        }
        return true;
    }

    /**
     * Returns the path, where the builderfile can be found, for not exising builders, a path will be generated.
     * @param   node The node, from which we want to know it;s MMObjectBuilder
     * @return  The path where the builder should live or <code>null</code> in case of strange failures
     *          When the builder was not loaded.
     * @since MMBase-1.8
     */
    protected String getBuilderConfiguration(MMObjectNode node) {
        // call our code above, to get our path...
        String path = getBuilderPath(node);
        // do we have a path?
        if (path == null) {
            log.error("field 'path' was empty.");
            return null;
        }
        return path + node.getStringValue("name") + ".xml";
    }


    /**
     * Returns the MMObjectBuilder which is represented by the node.
     * @param   node The node, from which we want to know its MMObjectBuilder
     * @return  The builder which is represented by the node, or <code>null</code>
     *          if the builder was not loaded.
     */
    public MMObjectBuilder getBuilder(MMObjectNode node) {
        String builderName = node.getStringValue("name");
        return mmb.getMMObject(builderName);
    }

    /**
     * @javadoc
     */
    public boolean reloadBuilder(String objectname) {
        log.service("Trying to reload builder : "+objectname);
        // first get all the info we need from the builder allready running
        MMObjectBuilder oldbul = mmb.getBuilder(objectname);
        String classname = oldbul.getClass().getName();
        String description = oldbul.getDescription();

        try {
            Class newclass = Class.forName("org.mmbase.module.builders." + classname);
            log.debug("Loaded load class : "+newclass);

            MMObjectBuilder bul = (MMObjectBuilder)newclass.newInstance();
            log.debug("Started : "+newclass);

            bul.setMMBase(mmb);
            bul.setTableName(objectname);
            bul.setDescription(description);
            bul.init();
            mmb.addBuilder(objectname, bul);
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
            return false;
        }
        return true;
    }

    /**
     * What should a GUI display for this node.
     * This method returns the gui name (singular name) of the builder that goes with this node.
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
    @Override
    public String getGUIIndicator(MMObjectNode node) {
        return getSingularName(node.getStringValue("name"), null);
    }


    /**
     * The GUIIndicator can depend on the locale. Override this function
     * @since MMBase-1.6
     */
    @Override
    protected String getLocaleGUIIndicator(Locale locale, String field, MMObjectNode node) {
        if (field == null || "".equals(field)) {
            return getLocaleGUIIndicator(locale, node);
        } else if ("description".equals(field)) {
            MMObjectBuilder bul = mmb.getBuilder(node.getStringValue("name"));
            if (bul != null) {
                return bul.getDescription(locale.getLanguage());
            }
        }
        return null;
    }

    @Override
    protected String getLocaleGUIIndicator(Locale locale, MMObjectNode node) {
        String rtn =  getSingularName(node.getStringValue("name"), locale.getLanguage());
        if (rtn == null) return node.getStringValue("name");
        return rtn;
    }

    /**
     * @javadoc
     */
    public void loadTypeDef(String name) {
        if(!typedefsLoaded.contains(name)) {
            typedefsLoaded.add(name);
        } else {
            if (log.isDebugEnabled()) log.debug("Builder "+name+" is already loaded!");
        }
    }

    /**
     * @javadoc
     */
    public void unloadTypeDef(String name) {
        if(typedefsLoaded.contains(name)) {
            typedefsLoaded.remove(name);
        } else {
            log.debug("Builder "+name+" is not loaded!");
        }
    }

    /**
     * @javadoc
     */
    public Vector<String> getList(PageInfo sp,StringTagger tagger, StringTokenizer tok) {
        if (tok.hasMoreTokens()) {
            String cmd = tok.nextToken();
            if (cmd.equals("builders")) {
                return typedefsLoaded;
            }
        }
        return null;
    }

    @Override
    protected Object executeFunction(MMObjectNode node, String function, List<?> args) {
        log.debug("executefunction of typedef");
        if (function.equals("info")) {
            List<Object> empty = new ArrayList<Object>();
            java.util.Map<String,String> info = (java.util.Map<String,String>) super.executeFunction(node, function, empty);
            info.put("gui", info.get("info") + " (localized)");
            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        } else if (function.equals("gui")) {
            log.debug("GUI of servlet builder with " + args);
            if (args == null || args.size() ==0) {
                return getGUIIndicator(node);
            } else {
                String rtn;
                if (args.size() <= 1) {
                    rtn = getGUIIndicator((String) args.get(0), node);
                } else {
                    String language = (String) args.get(1);
                    if (language == null) language = mmb.getLanguage();
                    Locale locale = new Locale(language, "");
                    rtn = getLocaleGUIIndicator(locale, (String) args.get(0), node);
                }
                if (rtn == null) return super.executeFunction(node, function, args);
                return rtn;
            }
        } else if (function.equals("defaultsearchage")) {
            return getBuilder(node).getSearchAge();
        } else {
            return super.executeFunction(node, function, args);
        }
    }

    private void testBuilderRemovable(MMObjectBuilder builder, MMObjectNode typeDefNode) {
        if (builder != null && builder.size() > 0) {
            throw new RuntimeException("Cannot delete this builder, it still contains nodes");
        } else if (builder == null) {
            // inactive builder, does it have nodes?
            MMObjectBuilder rootBuilder = mmb.getRootBuilder();
            NodeSearchQuery q = new NodeSearchQuery(rootBuilder);
            Integer value = typeDefNode.getNumber();
            Constraint constraint = new BasicFieldValueConstraint(q.getField(rootBuilder.getField("otype")), value);
            q.setConstraint(constraint);
            try {
                if (rootBuilder.count(q) > 0) {
                    throw new RuntimeException("Cannot delete this (inactive) builder with otype=" + value + ", it still contains nodes " + q);
                }
            } catch (SearchQueryException sqe) {
                // should never happen
                log.error(sqe);
            }
        }

        // check if there are relations which use this builder
        {
            if (builder instanceof InsRel) {
                MMObjectNode reldef = mmb.getRelDef().getDefaultForBuilder((InsRel)builder);
                if (reldef != null) {
                    throw new RuntimeException("Cannot delete this builder, it is referenced in reldef #" + reldef.getNumber());
                }
            }
            try {
                MMObjectBuilder typeRel = mmb.getTypeRel();
                NodeSearchQuery q = new NodeSearchQuery(typeRel);
                Integer value = typeDefNode.getNumber();
                BasicCompositeConstraint constraint = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_OR);
                Constraint constraint1 = new BasicFieldValueConstraint(q.getField(typeRel.getField("snumber")), value);
                Constraint constraint2 = new BasicFieldValueConstraint(q.getField(typeRel.getField("dnumber")), value);
                constraint.addChild(constraint1);
                constraint.addChild(constraint2);
                q.setConstraint(constraint);
                List<MMObjectNode> typerels = typeRel.getNodes(q);
                if (typerels.size() > 0) {
                    throw new RuntimeException("Cannot delete this builder, it is referenced by typerels: " + typerels);
                }
            } catch (SearchQueryException sqe) {
                // should never happen
                log.error(sqe);
            }
        }
    }

    /**
     * Returns the path, where the builder configuration file can be found, for not exising builders, a path will be generated.
     * @param   node The node, from which we want to know it;s MMObjectBuilder
     * @return  The path where the builder should live or <code>null</code> in case of strange failures
     *          When the builder was not loaded.
     */
    protected String getBuilderPath(MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("retrieving the path for node #" + node.getNumber());
        }
        // some basic checking
        if (node == null) {
            log.error("node was null");
            return null;
        }
        if (node.getStringValue("name") == null) {
            log.error("field 'name' was null");
            return null;
        }
        if (node.getStringValue("name").trim().length() == 0) {
            log.error("field 'name' was empty.");
            return null;
        }

        String pathInBuilderDir = mmb.getBuilderPath(node.getStringValue("name"), "");
        if (pathInBuilderDir != null) {
            // return the file path,..
            String file = pathInBuilderDir;
            if (log.isDebugEnabled()) {
                log.debug("builder file:" + file);
            }
            return file;
        }
        // still null, make up a nice url for our builder!
        if (defaultDeploy != null) {
            String file = defaultDeploy;
            if (log.isDebugEnabled()) {
                log.debug("builder file:" + file);
            }
            return file;
        }
        return null;
    }

    /**
     */
    protected MMObjectBuilder loadBuilder(MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("Load builder '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        }
        String path = getBuilderPath(node);
        log.info("Loading builder from " + path);
        MMObjectBuilder builder = mmb.loadBuilderFromXML(node.getStringValue("name"), path);
        if (builder == null) {
            // inactive builder?
            log.info("could not load builder from xml, is in inactive?(name: '" + node.getStringValue("name") + "' path: '" + path + "')");
            return null;
        }
        mmb.initBuilder(builder);
        builder.createIfNotExists();
        return builder;
    }

    /**
     */
    protected void storeBuilderConfiguration(MMObjectNode node) throws java.io.IOException {
        if (log.isDebugEnabled()) {
            log.debug("Store builder '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        }

        org.w3c.dom.Document doc = node.getXMLValue("config");
        if (doc == null) {
            log.error("Field config was null! Could not save the file for " + node.getStringValue("name") + Logging.stackTrace(new Throwable()));
            return;
        }
        String path = getBuilderConfiguration(node);
        log.info("Store builder '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")  to " + path);
        mmb.getBuilderLoader().storeDocument(path, doc);

    }

    /**
     */
    protected MMObjectBuilder unloadBuilder(MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("Unload builder '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        }
        // unload the builder,...
        MMObjectBuilder builder = getBuilder(node);
        if (builder != null) {
            mmb.unloadBuilder(builder);
        }
        return builder;
    }

    /**
     */
    protected boolean deleteBuilderConfiguration(MMObjectNode node) {
        if (log.isDebugEnabled()) {
            log.debug("Delete file of builder '" + node.getStringValue("name") + "' ( #" + node.getNumber() + ")");
        }
        File file = new File(getBuilderConfiguration(node));
        if (file.exists()) {
            if (!file.canWrite()) {
                log.error("file: " + file + " had no write rights for me.");
                return false;
            }
            // remove the file from the file system..
            file.delete();
            if (log.isDebugEnabled()) {
                log.debug("file: " + file + " has been deleted");
            }
        }
        return true;
    }
}
