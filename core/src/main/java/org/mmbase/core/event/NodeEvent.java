/*
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import java.io.*;
import java.util.*;

import org.mmbase.util.HashCodeUtil;
import org.mmbase.util.xml.UtilReader;
import org.mmbase.cache.Cache;
import org.mmbase.cache.CacheManager;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This class communicates a node event. in case of a change event, it contains
 * a map of changed values, mapped to their field's name, as well as the
 * previous values of the changed fields.
 *
 * @author  Ernst Bunders
 * @since   MMBase-1.8
 * @version $Id$
 */
public class NodeEvent extends Event {
    private static final Logger log = Logging.getLoggerInstance(NodeEvent.class);

    private static final long serialVersionUID = 1L;

    private static Class[] unacceptableValueTypes = new Class[] { byte[].class };
    private static Class[] requiredValueTypes = new Class[] { Serializable.class };

    /**
     * Event type speicfic for MMBase nodes.
     */
    public static final int TYPE_RELATION_CHANGE = 3;

    private static final Map<String, Object> EMPTY = Collections.unmodifiableMap(new HashMap<>());
    private static final Object EMPTIED = null;

    /**
     * Removes all non-serializable values, and all values we don't want to serialize (binaries, because they are too big) (actually, it considers
     * {@link #getUnacceptableValueTypes} and {@link #getRequiredValueTypes}). This is put in a new (to not reflect further changes) unmodifiable map
     * and returned.
     * @since MMBase-1.9.4
     */
    private static Map<String, Object> values(final Map<String, Object> values) {
        if (values.size() == 0) {
            return Collections.unmodifiableMap(values);
        }
        Set<String> toremove = new HashSet<>();
        Map<String, Object> newMap = new HashMap<>();
        synchronized(values) {
            ENTRIES:
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    for (Class clazz : requiredValueTypes) {
                        if (! clazz.isInstance(value)) {
                            log.warn("Found non " + clazz + "'" + entry.getKey() + "' in " + values);
                            toremove.add(entry.getKey());
                            continue ENTRIES;
                        }
                    }
                    for (Class clazz : unacceptableValueTypes) {
                        if (clazz.isInstance(value)) {
                            log.debug("Found  " + clazz + "'" + entry.getKey() + "' in " + values);
                            toremove.add(entry.getKey());
                            continue ENTRIES;
                        }
                    }
                }
            }
            newMap.putAll(values);
        }
        for (String k : toremove) {
            newMap.put(k, EMPTIED);
        }
        return Collections.unmodifiableMap(newMap);
    }

    /**
     * @see #getUnacceptableValueTypes
     * @since MMBase-1.9.4
     */
    static void setUnacceptableValueTypes(Class[] types) {
        unacceptableValueTypes = types;
    }

    /**
     * @see #getRequiredValueTypes
     * @since MMBase-1.9.4
     */
    static void setRequiredValueTypes(Class[] types) {
        requiredValueTypes = types;
    }
    /**
     * Sets the types of which values are unacceptable in the values and oldvalues maps. This can be configured in config/utils/nodeevents.xml
     * This defaults to [byte[]], but you can add other larges types too e.g. String. You could also set it to Object.class not not include any values
     * in the event.
     *
     * @since MMBase-1.9.4
     */
    public static Class[] getUnacceptableValueTypes() {
        return unacceptableValueTypes;
    }
    /**
     * Gets the types (interfaces) which values must implement in the values and oldvalues maps.  This can be configured in config/utils/nodeevents.xml
     * This defaults to [java.io.Serializable]. The values must be serializable, otherwise the event itself is not properly serializable, which is needed
     * in a clustering environment.
     * @since MMBase-1.9.4
     */
    public static Class[] getRequiredValueTypes() {
        return requiredValueTypes;
    }


    /**
     * @since MMBase-1.9.4
     */
    static final UtilReader properties = new UtilReader("nodeevents.xml", new Runnable() {
            //@Override
            public void run() {
                configure();
            }
        });

    /**
     * Reads nodeevents.xml
     * @since MMBase-1.9.4
     */
    static void configure() {
        log.info("Reading " + properties);
        {
            String[] unacceptable = properties.getProperties().get("unacceptable").split(",");
            List<Class> classes = new ArrayList<>();
            for (String clazz : unacceptable) {
                try {
                    classes.add(Class.forName(clazz));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info("Setting unacceptable values types for NodeEvents to " + classes);
            setUnacceptableValueTypes(classes.toArray(new Class[] {}));
        }
        {
            String[] required = properties.getProperties().get("required").split(",");
            List<Class> classes = new ArrayList<>();
            for (String clazz : required) {
                try {
                    classes.add(Class.forName(clazz));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info("Setting required values types for NodeEvents to " + classes);
            setRequiredValueTypes(classes.toArray(new Class[] {}));
        }
    }
    static {
        configure();
    }


    private final int nodeNumber;
    private final String builderName;

    private final Map<String, Object> oldValues;
    private final Map<String, Object> newValues;


    /**
     * @param machineName (MMBase) name of the server
     * @param builderName name of builder of node event is about
     * @param oldValues map with fields and their values that have been changed by the event. This may be <code>null</code>
     * @param newValues map with new values of changed fields
     * @param eventType the type of event
     */
    public NodeEvent(String machineName, String builderName, int nodeNumber, final Map<String, Object> oldValues, final Map<String, Object> newValues, int eventType ){
        super(machineName, eventType);
        this.builderName = builderName;
        this.nodeNumber = nodeNumber;
        if (oldValues == null) {
            this.oldValues = EMPTY;
        } else {
            this.oldValues = values(oldValues);
        }
        if (newValues == null) {
            this.newValues = EMPTY;
        } else {
            this.newValues =  values(newValues);
        }
    }


    /**
     * @param fieldName the field you want to get the old value of
     * @return an Object containing the old value (in case of change event), or
     *         null if the fieldName was not found in the old value list
     */
    public final Object getOldValue(String fieldName) {
        return oldValues.get(fieldName);
    }

    /**
     * @return a set containing the names of the fields that have changed
     */
    public final Set<String> getChangedFields() {
        switch(getType()) {
        case TYPE_NEW:
            return newValues.keySet();
        case TYPE_CHANGE:
            //for changed both old and new values are good (similar keys)
            return newValues.keySet();
        case TYPE_DELETE:
            return oldValues.keySet();
        default:
            return Collections.emptySet();
        }
    }

    /**
     * @param fieldName the field you want the new value of (in case of change
     *        event), or null if the fieldName was not found in the new value
     *        list
     * @return the new value of the field
     */
    public final  Object getNewValue(String fieldName) {
        return newValues.get(fieldName);
    }



    /**
     * @return Returns the builderName.
     */
    public final String getBuilderName() {
        return builderName;
    }
    /**
     * @return Returns the nodeNumber.
     */
    public final int getNodeNumber() {
        return nodeNumber;
    }

    private static final Set<String> relationBuilders = new HashSet<>();

    /**
     * Especially events of the type NEW can also be about relations. You may want to know this. This determines it efficiently without actually
     * getting the node from the database.
     * @since MMBase-1.9.4
     */
    public boolean isRelation() {
        if (relationBuilders.isEmpty()) {
            org.mmbase.bridge.NodeManager insrel = org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null).getNodeManager("insrel");
            relationBuilders.add("insrel");
            for (org.mmbase.bridge.NodeManager desc : insrel.getDescendants()) {
                relationBuilders.add(desc.getName());
            }
        }
        return relationBuilders.contains(getBuilderName());
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Node event: '");
        buf.append(getEventTypeGuiName(eventType)).append( "', node: ").append(nodeNumber).append(", nodetype: ").append(builderName);
        buf.append(", oldValues: ");
        buf.append(oldValues.keySet().toString());
        buf.append(", newValues: ");
        buf.append(newValues.keySet().toString());
        buf.append("changedFields: ");
        buf.append(getChangedFields());
        return buf.toString();
    }

    protected static String getEventTypeGuiName(int eventType) {
        switch (eventType) {
        case Event.TYPE_CHANGE:
            return "node changed";
        case Event.TYPE_DELETE:
            return "node deleted";
        case Event.TYPE_NEW:
            return "new node";
        case NodeEvent.TYPE_RELATION_CHANGE:
            return "relation changed";
        default:
            throw new IllegalArgumentException("HELP! event of type " + eventType + " is unknown. This should not happen");
        }
    }



    /**
     * For conveneance: conversion of the new event type indication to the old
     * style
     *
     * @param eventType must be c,d,n or r
     * @return A String describing the type of an event. (like "c" (change), "d" (delete), "n" (new), or "r" (relation change))
     */
    public static String newTypeToOldType(int eventType) {
        switch (eventType) {
        case Event.TYPE_CHANGE:           return "c";
        case Event.TYPE_DELETE:           return "d";
        case Event.TYPE_NEW:              return "n";
        case NodeEvent.TYPE_RELATION_CHANGE: return "r";
        default: throw new IllegalArgumentException("HELP! event of type " + eventType + " is unknown. This should not happen");
        }
    }

    public static int oldTypeToNewType(String eventType) {
        if (eventType.length() > 1) {
            throw new IllegalArgumentException("HELP! event of type '" + eventType + "' is unknown. This should not happen. (length = " + eventType.length() + ")");
        }
        return oldTypeToNewType(eventType.charAt(0));

    }

    /**
     * For conveneance: conversion of the old event type indication to the new
     * style
     *
     * @param eventType
     * @since MMBase-1.9.2
     */
    public static int oldTypeToNewType(char eventType) {
        switch(eventType) {
        case 'c': return Event.TYPE_CHANGE;
        case 'd': return Event.TYPE_DELETE;
        case 'n': return Event.TYPE_NEW;
        case 'r': return NodeEvent.TYPE_RELATION_CHANGE;
        default: throw new IllegalArgumentException("HELP! event of type " + eventType + " is unknown. This should not happen");
        }
    }

    /**
     * utility method: check if a certain field has changed
     * @param fieldName
     * @return true if the field of given name is among the changed fields
     */
    public boolean hasChanged(String fieldName){
        return oldValues.keySet().contains(fieldName) || newValues.keySet().contains(fieldName);
    }


    @Override
    public int hashCode() {
        int result = 0;
        result = HashCodeUtil.hashCode(result, eventType);
        result = HashCodeUtil.hashCode(result, nodeNumber);
        result = HashCodeUtil.hashCode(result, builderName);
        return result;

    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof NodeEvent) {
            NodeEvent ne = (NodeEvent) o;
            return eventType == ne.eventType && nodeNumber == ne.nodeNumber && builderName.equals(ne.builderName);
        } else {
            return false;
        }
    }

    /**
     * old values can be different things.
     * <ul>
     * <li>if the event type is 'new' this collection is empty.
     * <li>if the event type is 'changed' this collection contains the old values of the changed fields.
     * <li>if the event type is 'delete' this collection contains all the values of the node to be deleted.
     * </ul>
     * @return a map where the key is a fieldname and the value the field's value
     */
    public final Map<String, Object> getOldValues(){
        return oldValues;
    }

    /**
     * new values can be different things.
     * <ul>
     * <li>if the event type is 'new' this collection contains all the fields of the node.
     * <li>if the event type is 'changed' this collection contains the new values of the changed fields.
     * <li>if the event type is 'delete' this collection is empty.
     * </ul>
     * @return a map where the key is a fieldname and the value the field's value
     */
    public final Map<String, Object> getNewValues(){
        return newValues;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log.debug("deserialized node event for " + nodeNumber);
        try {
            int otype = org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase").getNodeManager(builderName).getNumber();
            if (otype != -1) {
                Cache<Integer, Integer> typeCache = CacheManager.getCache("TypeCache");
                if (typeCache != null) {
                    Integer cachedType = typeCache.get(nodeNumber);
                    if (cachedType == null) {
                        log.debug("Putting in type cache " + nodeNumber + " -> " + otype);
                        typeCache.put(nodeNumber, otype);
                    } else {
                        if (otype == cachedType.intValue()) {
                            log.debug("Type already cached");
                        } else {
                            log.warn("Type in event not the same as in cache " + otype + " != " + cachedType + " Event: " + this + " from " + getMachine());
                        }
                    }
                } else {
                    log.service("No typecache?");
                }
            } else {
                log.service("Builder '" + builderName + "' from " + nodeNumber + " not found. Originating from " + getMachine());
            }
        } catch (Exception e) {
             log.error(e);
        }

    }


    public static void main(String[] args) {
        //test serializable
        Map<String,Object>  oldv = new HashMap<>(), newv = new HashMap<>();
        oldv.put("een","veen");
        oldv.put("twee","vtwee");
        newv.putAll(oldv);

        NodeEvent event = new NodeEvent(  "local", "builder", 0, oldv, newv, Event.TYPE_CHANGE);
        System.out.println("event 1: " + event.toString());

    }

}
