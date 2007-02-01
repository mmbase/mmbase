/**
 * Component description interface.
 */
package nl.didactor.component;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.corebuilders.FieldDefs;
import org.mmbase.module.core.MMBase;
import org.mmbase.bridge.Cloud;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.bridge.jsp.taglib.util.ContextContainer;

import org.apache.xpath.XPathAPI; //slow but useful
import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;

/**
 * @javadoc
 */
public abstract class Component {
    private static final Logger log = Logging.getLoggerInstance(Component.class);
 
    private static Map components = new Hashtable(); // why is this synchronized

    private List interestedComponents = new Vector();    // why is this synchronized
    private MMObjectNode node;

    /** Save the settings for this component */
    private Map settings = new HashMap();

    /** A list of all possible setting scopes */
    private List scopes = new Vector();                  // why is this synchronized

    private Map scopesReferid = new HashMap();

    /** The string indicating the path for templates of this component */
    private String templatepath = null;

    /** The string indicating in which bar (application, education, provider) the cockpit menuitem must be placed */
    private String templatebar = null;

    /** Location of the component in the bar. Default to 100, which is somewhere at the end. */
    private int barposition = 100;

    /**
     * Register a component in the registry.
     */
    public static void register(String name, Component component) {
        components.put(name, component);
    }

    /**
     * Retrieve a component from the registry.
     */
    public static Component getComponent(String name) {
        return (Component)components.get(name.toLowerCase());
    }

    public static Component[] getComponents() {
        Component[] comps = new Component[components.size()];
        int cnt = 0;
        for (Iterator e = components.values().iterator(); e.hasNext(); ) {
            comps[cnt] = (Component)e.next();
            cnt++;
        }
        log.debug("Returning " + comps.length + " components");
        return comps;
    }

    public void setNode(MMObjectNode node) {
        this.node = node;
    }

    /**
     * Returns the version of the component
     */
    abstract public String getVersion();

    /**
     * Returns the name of the component
     */
    abstract public String getName();

    /**
     * Initializes the component. This is called during startup 
     * of Didactor. This method will be called every time your Didastor
     * installation is restarted.
     */
    public void init() {
        String configfile = org.mmbase.module.core.MMBaseContext.getConfigPath() +
                            File.separator + "components" +
                            File.separator + getName() + ".xml";
        log.debug("Reading component configuration from file '" + configfile + "'");
        if ((new File(configfile)).exists()) {
            try {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(configfile);
                Node rootNode = doc.getDocumentElement();
                Node componentNode = XPathAPI.selectSingleNode(rootNode, "/component");
                this.templatepath = getAttribute(componentNode, "templatepath");
                this.templatebar = getAttribute(componentNode, "templatebar");
                try {
                    this.barposition = Integer.parseInt(getAttribute(componentNode, "barposition"));
                } catch (Exception e) {}

                NodeList scopeNodes = XPathAPI.selectNodeList(componentNode, "scope");
                log.debug("Number of scopes: " + scopeNodes.getLength());
                for (int i=0; i<scopeNodes.getLength(); i++) {
                    Node scope = scopeNodes.item(i);
                    String scopeName = getAttribute(scope, "name");
                    String scopeReferid = getAttribute(scope, "referid");
                    log.debug("Scope name: " + scopeName);
                    scopes.add(scopeName);
                    scopesReferid.put(scopeName, scopeReferid);
                    NodeList settingNodes  = XPathAPI.selectNodeList(scope, "setting");
                    for (int j=0; j<settingNodes.getLength(); j++) {
                        Node settingNode = settingNodes.item(j);
                        String settingName = getAttribute(settingNode, "name");
                        String settingRef = getAttribute(settingNode, "ref");
                        if (settingName == null && settingRef != null) {
                            Setting setting = (Setting)settings.get(settingRef);
                            setting.addScope(scopeName);
                            log.debug("Added scope '" + scopeName + "' for setting '" + settingRef + "'");
                        } else if (settingName != null) {
                            String settingType = getAttribute(settingNode, "type");
                            String settingDefault = getAttribute(settingNode, "default");
                            String settingPrompt = getAttribute(settingNode, "prompt");

                            Setting setting = new Setting(settingName, settingType, settingPrompt);

                            if ("domain".equals(settingType)) {
                                NodeList options = XPathAPI.selectNodeList(settingNode, "option");
                                String domain[] = new String[options.getLength()];
                                for (int k=0; k<options.getLength(); k++) {
                                    Node option = options.item(k);
                                    String optionName = getAttribute(option, "name");
                                    domain[k] = optionName;
                                }
                                setting.setDomain(domain);
                            }
                            setting.setDefault(settingDefault);
                            setting.addScope(scopeName);
                            settings.put(settingName, setting);
                            log.debug("Added setting '" + settingName + "' of type '" + settingType + "' for scope '" + scopeName + "', default = '" + settingDefault + "'");
                        }
                    }
                }

                NodeList builderNodes = XPathAPI.selectNodeList(componentNode, "builder");
                log.debug("Number of builders: " + builderNodes.getLength());
                for (int i=0; i<builderNodes.getLength(); i++) {
                    Node builderNode = builderNodes.item(i);
                    String builderName = getAttribute(builderNode, "name");
                    log.debug("Builder name: " + builderName);
                    MMObjectBuilder builder = MMBase.getMMBase().getBuilder(builderName);
                    if (builder == null) {
                        log.warn("Cannot load builder '" + builderName + "' for component '" + getName() + "')");
                    }
                    NodeList fieldNodes = XPathAPI.selectNodeList(builderNode, "field");
                    for (int j=0; j<fieldNodes.getLength(); j++) {
                        Node fieldNode = fieldNodes.item(j);
                        String fieldName = getAttribute(fieldNode, "name");
                        String fieldType = getAttribute(fieldNode, "type");
                        int ftype = FieldDefs.TYPE_UNKNOWN;
                        if ("string".equals(fieldType)) {
                            ftype = FieldDefs.TYPE_STRING;
                        } else if ("integer".equals(fieldType)) {
                            ftype = FieldDefs.TYPE_INTEGER;
                        }

                        FieldDefs fd = new FieldDefs(fieldName, fieldType, -1, -1, fieldName, ftype, 200, FieldDefs.DBSTATE_VIRTUAL);
                        // position 300: used later on internally to figure out which virtual fields are didactor-managed
                        fd.setDBPos(300);
                        builder.addField(fd);
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * Installs the component. This method will only be called once, 
     * during the first initial installation of the component. The component
     * can update objectstructures if it needs to.
     */
    public void install() {

    }

    /**
     * Returns an array of components this component depends on.
     * This should always contain at least one component: 'DidactorCore'
     */
    abstract public Component[] dependsOn();

    /**
     * This method is called just before when a new object is added to Didactor. If the component
     * needs to insert objects for this object, it can do so. 
     */
    public boolean preInsert(MMObjectNode node) {
        return true;
    }

    /**
     * This method is called just after when a new object is added to Didactor. If the component
     * needs to insert objects for this object, it can do so. 
     */
    public boolean postInsert(MMObjectNode node) {
        return true;
    }

    /**
     * This method is called just before an object is being committed.
     */
    public boolean preCommit(MMObjectNode node) {
        return true;
    }

    /**
     * This method is called right after an object is being committed.
     */
    public boolean postCommit(MMObjectNode node) {
        return true;
    }

    /**
     * This method is called just before an object is being deleted.
     */
    public boolean preDelete(MMObjectNode node) {
        return true;
    }

    /**
     * Permission framework: indicate whether or not a given operation may be done, with the
     * given arguments. The return value is a list of 2 booleans; the first boolean indicates
     * whether or not the operation is allowed, the second boolean indicates whether or not
     * this result may be cached.
    */
    public boolean[] may (String operation, Cloud cloud, Map context, String[] arguments) {
        return new boolean[]{true, true};
    }

    public String getTemplatePath() {
        return templatepath;
    }

    public String getTemplateBar() {
        return templatebar;
    }

    public int getBarPosition() {
        return barposition;
    }

    public int getNumber() {
        return node.getNumber();
    }

    public String getValue(String variablename, Cloud cloud, Map context, String[] arguments) {
        return "";
    }

    /**
     * Settings: return a setting in the given context. If no value can be found for any
     * of the scopes in the context, the default value for the setting will be returned.
     * @param setting The name of the setting for which a value should be 
     * returned
     * @param context A 'Map' containing name-value pairs, that can be needed
     * to retrieve the setting value. For instance the current username or
     * education node number.
     */
    public Object getSetting(String settingName, Cloud cloud, Map context) {
        log.debug("Retrieving value for setting '" + settingName + "', with in context: " + context.keySet());
        Setting setting = (Setting) settings.get(settingName);
        if (setting == null) {
            throw new RuntimeException("Setting '" + settingName + "' is not defined for component '" + getName() + "'");
        }
        List scope = setting.getScope();
        Object retval = null;

        for (int i = 0; i < scope.size(); i++) { // 
            String scopeName    = (String)scope.get(i);
            String scopeReferId = (String)scopesReferid.get(scopeName);
            log.debug("Trying on scope '" + scopeName + "' (" + scopeReferId + ")");
            int objectid = -1;
            if ("component".equals(scopeName)) {
                objectid =  node.getNumber();
            } else if (context.get(scopeReferId) != null) {
                objectid = Integer.parseInt(org.mmbase.util.Casting.toString(context.get(scopeReferId)));
                log.debug("" + scopeReferId + " = " + objectid);
            }

            if (objectid > 0) {
                Object cloudSetting = getObjectSetting(settingName, objectid, cloud);
                if (cloudSetting != null) {
                    retval = cloudSetting;
                    log.debug("Found value: " + retval);
                }
            }
        }

        if (retval != null) {
            return retval;
        } else {
            return setting.getDefault();
        }
    }

    /**
     * Get the setting for an object and a component from MMBase. This object can be a 'people' object,
     * 'component' object, etc. If the object is the component, and no value can be found in the database,
     * the setting's default value will be returned.
     * This method is used internally to get a setting on a specific layer. This is the reason that the
     * default value is not returned for objects other than the component itself, because it would be
     * impossible to distinguish between a 'real' value and a default value later on.
     * @param settingname The name of the setting in MMBase.
     * @param objectid The number of the node representing this object to get the component setting value for
     */
    public Object getObjectSetting(String settingName, int id, Cloud cloud) {
        org.mmbase.bridge.NodeList settingNodes = null;
        Object defaultValue = null;
        Setting setting = (Setting)settings.get(settingName);
        if (setting == null) {
            throw new RuntimeException("Setting with name '" + settingName + "' is not defined for component '" + getName() + "'");
        }

        if (id == node.getNumber()) {
            // direct setting to this component
            settingNodes = cloud.getNode(id).getRelatedNodes("settings");
            defaultValue = setting.getDefault();
        } else {
            org.mmbase.bridge.NodeList settingrel = nl.didactor.util.GetRelation.getRelations(id, node.getNumber(), "settingrel", cloud);

            if (settingrel.size() == 0) {
                return null;
            }
            if (settingrel.size() > 1) {
                log.warn("Too many relations from " + id + " to " + node.getNumber() +". Picking first one!");
            }
            org.mmbase.bridge.Node settingRelNode = settingrel.getNode(0);
            settingNodes = settingRelNode.getRelatedNodes("settings");
        }

        if (settingNodes == null) {
            return defaultValue;
        }

        for (int i = 0; i < settingNodes.size(); i++) {
            if (settingNodes.getNode(i).getStringValue("name").equals(settingName)) {
                return setting.cast(settingNodes.getNode(i).getStringValue("value"));
            }
        }

        return defaultValue;
    }

    /**
     * Set a new value for a setting on a specific object.
     * @param settingName The name of the setting
     * @param newValue the new value for the setting
     * @param id The objectnumber of the object to set the setting for
     * @param cloud The cloud in which to set the setting
     * @throws RuntimeException In case the setting doesnt exist, or the new value falls outside of
     * the domain of the datatype of the setting.
     */
    public void setObjectSetting(String settingName, int id, Cloud cloud, String newValue) {
        Setting setting = (Setting)settings.get(settingName);
        if (setting == null) {
            throw new RuntimeException("Setting with name '" + settingName + "' is not defined for component '" + getName() + "'");
        }

        // Verify that the new value is valid for this setting
        if (setting.getType() == Setting.TYPE_INTEGER) {
            try {
                int i = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Value '" + newValue + "' is invalid for setting '" + settingName + "' of type Integer");
            }
        } else if (setting.getType() == Setting.TYPE_BOOLEAN || setting.getType() == Setting.TYPE_DOMAIN) {
            String[] domain = setting.getDomain();
            boolean valid = false;
            for (int i=0; i<domain.length; i++) {
                if (domain[i].equals(newValue)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new RuntimeException("Value '" + newValue + "' is invalid for setting '" + settingName + ", not inside domain");
            }
        }

        org.mmbase.bridge.Node baseNode = null;
        if (id == node.getNumber()) {
            baseNode = cloud.getNode(id);
        } else {
            org.mmbase.bridge.NodeList settingrel = nl.didactor.util.GetRelation.getRelations(id, node.getNumber(), "settingrel", cloud);

            if (settingrel.size() == 0) {
                org.mmbase.bridge.RelationManager nm = cloud.getRelationManager("settingrel");
                baseNode = nm.createRelation(cloud.getNode(id), cloud.getNode(node.getNumber()));
                baseNode.commit();
            } else if (settingrel.size() > 1) {
                baseNode = settingrel.getNode(0);
                log.warn("Too many relations from " + id + " to " + node.getNumber() +". Picking first one!");
            } else {
                baseNode = settingrel.getNode(0);
            }
        }

        org.mmbase.bridge.NodeList settingNodes = baseNode.getRelatedNodes("settings");

        for (int i=0; i<settingNodes.size(); i++) {
            org.mmbase.bridge.Node settingNode = settingNodes.getNode(i);
            if (settingNode.getStringValue("name").equals(settingName)) {
                settingNode.setValue("value", newValue);
                settingNode.commit();
                return;
            }
        }
        // not found, we need to create a new setting node.
        org.mmbase.bridge.NodeManager nm = cloud.getNodeManager("settings");
        org.mmbase.bridge.Node node = nm.createNode();
        node.setValue("name", settingName);
        node.setValue("value", newValue);
        node.commit();
        org.mmbase.bridge.RelationManager rm = cloud.getRelationManager("related");
        org.mmbase.bridge.Relation rel = rm.createRelation(baseNode, node);
        rel.commit();
    }

    /**
     * Get the setting for a user and a component from MMBase.
     * @param settingname The name of the setting in MMBase.
     * @param userid The number of the node representing this user
     */
    public Object getUserSetting(String settingname, String userid, Cloud cloud) {
        log.debug("getUserSetting(" + settingname + ", " + userid + ", " + cloud + ")");
        Setting setting = (Setting)settings.get(settingname);
        if (setting == null) {
            throw new RuntimeException("Setting with name '" + settingname + "' is not defined for component '" + getName() + "'");
        }
        org.mmbase.bridge.NodeList settingrel = nl.didactor.util.GetRelation.getRelations(Integer.parseInt(userid), node.getNumber(), "settingrel", cloud);

        if (settingrel.size() == 0) {
            Object retVal = getObjectSetting(settingname, node.getNumber(), cloud);
            log.debug("Returning default value: " + retVal);
            return retVal;
        }
        if (settingrel.size() > 1) {
            log.warn("Too many relations from " + userid + " to " + node.getNumber() +". Picking first one!");
        }
        org.mmbase.bridge.Node settingRelNode = settingrel.getNode(0);
        org.mmbase.bridge.NodeList settings = settingRelNode.getRelatedNodes("settings");
        for (int i=0; i<settings.size(); i++) {
            if (settings.getNode(i).getStringValue("name").equals(settingname)) {
                log.debug("Returning database value: " + settings.getNode(i).getStringValue("value"));
                return setting.cast(settings.getNode(i).getStringValue("value"));
            }
        }
        log.debug("Returning default value: " + setting.getDefault());
        return setting.getDefault();
    }


    /**
     * Register a component as interested. This can be used for example in the 'getSetting' or 'may' method
     * to retrieve a setting based on some extra installed components.
     */
    public void registerInterested(Component comp) {
        interestedComponents.add(comp);
    }

    /**
     * Small helper method that returns an attribute value of a w3c DOM Node.
     */
    private static String getAttribute(Node n, String attr) {
        if (n == null) {
            throw new RuntimeException("Node is null!");
        } else if (n.getAttributes() == null) {
            return null;
        } else if (n.getAttributes().getNamedItem(attr) == null) {
            return null;
        }
        return n.getAttributes().getNamedItem(attr).getNodeValue();
    }


    public Map getSettings() {
        return settings;
    }

    /**
     * Return a list of settings that are settable on a given scope
     * @todo should return List
     */
    public List getSettings(String scope) {
        List result = new Vector();
        Iterator i = settings.values().iterator();
        while (i.hasNext()) {
            Setting s = (Setting)i.next();
            if (s.getScope().contains(scope)) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * @todo should return List
     */
    public List getScopes() {
        return scopes;
    }

    public class Setting {
        public static final int TYPE_INTEGER = 1;
        public static final int TYPE_BOOLEAN = 2;
        public static final int TYPE_DOMAIN = 3;
        public static final int TYPE_STRING = 4;

        private String name;
        private int type;
        private String[] domain;
        private Object defaultvalue;
        private List scope;
        private String prompt;

        public Setting(String name, String type, String prompt) {
            this.name = name;
            if ("integer".equals(type)) {
                this.type = TYPE_INTEGER;
            } else if ("boolean".equals(type)) {
                this.type = TYPE_BOOLEAN;
                this.domain = new String[]{"true", "false"};
            } else if ("domain".equals(type)) {
                this.type = TYPE_DOMAIN;
            } else {
                this.type = TYPE_STRING;
            }
            this.scope = new Vector();
            this.prompt = prompt;
        }

        public void setDefault(String defaultvalue) {
            this.defaultvalue = cast(defaultvalue);
        }

        public Object cast(String value) {
            switch (this.type) {
                case TYPE_BOOLEAN:
                    if ("true".equals(value)) {
                        return Boolean.TRUE;
                    } else if ("false".equals(value)) {
                        return Boolean.FALSE;
                    } else {
                        log.warn("Warning: boolean value '" + value + "' is not one of {true,false}, defaulting to false");
                        return Boolean.FALSE;
                    }
                case TYPE_INTEGER:
                    return new Integer(value);
                case TYPE_STRING:
                case TYPE_DOMAIN:
                    return value;
            }
            return null;
        }

        public void setDomain(String[] domain) {
            this.domain = domain;
        }

        public void addScope(String scopeName) {
            scope.add(scopeName);
        }

        public List getScope() {
            return scope;
        }

        public Object getDefault() {
            return defaultvalue;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getPrompt() {
            return prompt;
        }

        public String[] getDomain() {
            return domain;
        }
    }
}
