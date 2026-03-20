package org.mmbase.core.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mmbase.bridge.BridgeException;
import org.mmbase.core.CoreField;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This class facilitates the use of nodes for system properties.
 * The special property "mmservers" is used to switch between environments.
 * This class will search for the mmbaseroot machinename in the value of the "mmservers" property
 * @since MMBase-1.9
 * @author Nico Klasens
 * @version $Id$
 */
public class SystemProperties {

    private static final Logger log = Logging.getLoggerInstance(SystemProperties.class);

    private final static String PROPERTY_BUILDER = "systemproperties";

    private final static String DEVELOPMENT      = "development";
    private final static String TEST             = "test";
    private final static String ACCEPTANCE       = "acceptance";
    private final static String PRODUCTION       = "production";

    private final static String DEFAULT          = "value";


    private static final String COMPONENT = "component";
    private static final String KEY = "key";

    private static final String MMSERVERS_PROPERTY = "mmservers";

    /** Environment server is running in (development,test,acceptance,production) */
    private static String environment = DEFAULT;

    /** Notify in the log that the server will use the default values */
    private static boolean warnOnce = true;

    /**
     * Returns the value of the property with the key
     *
     * @param key The key of the property node.
     * @return The value of the properties node.
     */
    public static String getProperty(String key) {
        if (DEFAULT.equals(environment)) {
            setEnvironment();
            log.debug("Environment " + environment);
        }
        return getProp(key);
    }

    /**
     * Returns the value of the property with the key
     *
     * @param component The component name of the property
     * @param key The key of the property
     * @return The value of the properties node.
     */
    public static String getComponentProperty(String component, String key) {
        if (DEFAULT.equals(environment)) {
            setEnvironment();
            log.debug("Environment " + environment);
        }
        return getComponentProp(component, key);
    }

    /**
     * Returns the value of the property with the key
     *
     * @param component The component value of the properties
     * @return The properties.
     */
    public static Map<String, String> getComponentProperties(String component) {
        if (DEFAULT.equals(environment)) {
            setEnvironment();
            log.info("Environment " + environment);
        }
        return getComponentProps(component);
    }


    /**
     * Set the value of the property
     * @param key The key of the property
     * @param value The value of the property
     */
    public static void setProperty(String key, String value) {
        if (DEFAULT.equals(environment)) {
            setEnvironment();
            log.info("Environment " + environment);
        }
        setProp(key, value);
    }

    /**
     * Set the value of the property
     * @param component The component of the property
     * @param key The key of the property
     * @param value The value of the property
     */
    public static void setComponentProperty(String component, String key, String value) {
        if (DEFAULT.equals(environment)) {
            setEnvironment();
            log.info("Environment " + environment);
        }
        setProp(component, key, value);
    }


    private static void setEnvironment() {
        MMObjectNode mmservers = getPropertyNode(MMSERVERS_PROPERTY);
        if (mmservers != null) {
            String machineName = MMBase.getMMBase().getMachineName();
            if (isServerInEnv(machineName, mmservers.getStringValue(PRODUCTION))) {
                environment = PRODUCTION;
                return;
            }
            if (isServerInEnv(machineName, mmservers.getStringValue(ACCEPTANCE))) {
                environment = ACCEPTANCE;
                return;
            }
            if (isServerInEnv(machineName, mmservers.getStringValue(TEST))) {
                environment = TEST;
                return;
            }
            if (isServerInEnv(machineName, mmservers.getStringValue(DEVELOPMENT))) {
                environment = DEVELOPMENT;
                return;
            }
            if (warnOnce) {
                log.info("Server " + machineName
                        + " not in Property 'mmservers'. Using default value");
                warnOnce = false;
            }
        } else {
            if (warnOnce) {
                log.warn("Property '" + MMSERVERS_PROPERTY + "' missing. Using default value for environment='" + environment + "'");
                warnOnce = false;
            }
        }
    }

    private static MMObjectNode getPropertyNode(String propertyKey) {
        try {
            MMObjectBuilder propertiesManager = getPropertiesBuilder();
            NodeSearchQuery query = new NodeSearchQuery(propertiesManager);

            BasicFieldValueConstraint constraint = createConstraint(propertiesManager, query, KEY, propertyKey);
            query.setConstraint(constraint);

            List<MMObjectNode> resultList = propertiesManager.getNodes(query);
            if (resultList.size() > 0) {
                return resultList.get(0);
            }
            return null;
        }
        catch (SearchQueryException sqe) {
            throw new BridgeException(sqe);
        }
    }

    private static boolean isServerInEnv(String machineName, String servers) {
        String[] serversArray = servers.split(",");
        for (String element : serversArray) {
            if (element != null && machineName.equals(element.trim())) { return true; }
        }
        return false;
    }

    private static String getProp(String key) {
        MMObjectNode property = getPropertyNode(key);
        return readProperty(key, property);
    }


    private static void setProp(String key, String value) {
        MMObjectNode property = getPropertyNode(key);
        updateProperty(property, key, null,value);
    }

    private static void setProp(String component, String key, String value) {
        MMObjectNode property = getComponentPropertyNode(component, key);
        updateProperty(property, null, key, value);
    }

    private static void updateProperty(MMObjectNode property, String component, String key, String value) {
        if (property == null) {
            MMObjectBuilder propertiesManager = getPropertiesBuilder();
            property = propertiesManager.getNewNode("system");
            property.setValue(KEY, key);
            if (component != null && !"".equals(component)) {
                property.setValue(COMPONENT, component);
            }
        }

        property.setValue(environment, value);
        property.commit();
        log.info("Changed Property " + key + "in environment " + environment + " value=" + value);
    }

    private static Map<String, String> getComponentProps(String component) {
        try {
            Map<String, String> result = new TreeMap<String, String>();
            MMObjectBuilder propertiesManager = getPropertiesBuilder();
            NodeSearchQuery query = new NodeSearchQuery(propertiesManager);

            BasicFieldValueConstraint constraint = createConstraint(propertiesManager, query, COMPONENT, component);
            query.setConstraint(constraint);

            List<MMObjectNode> resultList = propertiesManager.getNodes(query);
            for (MMObjectNode node : resultList) {
                result.put(node.getStringValue(KEY), node.getStringValue(environment));
            }
            return result;
        }
        catch (SearchQueryException sqe) {
            throw new BridgeException(sqe);
        }
    }

    private static String getComponentProp(String component, String key) {
        MMObjectNode property = getComponentPropertyNode(component, key);
        return readProperty(key, property);
    }

    private static MMObjectNode getComponentPropertyNode(String component, String key) {
        try {
            MMObjectBuilder propertiesManager = getPropertiesBuilder();
            NodeSearchQuery query = new NodeSearchQuery(propertiesManager);

            BasicFieldValueConstraint componentConstraint = createConstraint(propertiesManager, query, COMPONENT, component);
            BasicFieldValueConstraint keyConstraint = createConstraint(propertiesManager, query, KEY, key);

            BasicCompositeConstraint composite = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);
            composite.addChild(componentConstraint);
            composite.addChild(keyConstraint);

            query.setConstraint(composite);

            List<MMObjectNode> resultList = propertiesManager.getNodes(query);
            if (resultList.size() > 0) {
                return resultList.get(0);
            }
            return null;
        }
        catch (SearchQueryException sqe) {
            throw new BridgeException(sqe);
        }
    }

    private static String readProperty(String key, MMObjectNode property) {
        String result = null;
        if (property != null) {
            result = property.getStringValue(environment);
            if (!DEFAULT.equals(environment) && (result == null || "".equals(result))) {
                log.warn("Property '" + key + "' empty in environment " + environment
                        + ". Using default value");
                result = property.getStringValue(DEFAULT);
            }
        }
        log.debug("Property=" + key + ", value=" + result);
        return result;
    }

    private static BasicFieldValueConstraint createConstraint(MMObjectBuilder propertiesManager,
            NodeSearchQuery query, String field, String value) {
        CoreField keyField = propertiesManager.getField(field);
        BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(query
                .getField(keyField), value);
        constraint.setOperator(FieldCompareConstraint.EQUAL);
        return constraint;
    }

    private static MMObjectBuilder getPropertiesBuilder() {
        return MMBase.getMMBase().getBuilder(PROPERTY_BUILDER);
    }

}
