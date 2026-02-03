/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.io.*;
import java.util.*;
import org.mmbase.bridge.Field;
import org.mmbase.bridge.Node;
import org.mmbase.cache.BlobCache;
import org.mmbase.cache.RelatedNodesCache;
import org.mmbase.cache.RelationsCache;
import org.mmbase.module.builders.DayMarkers;
import static org.mmbase.module.core.MMObjectBuilder.FIELD_NUMBER;
import static org.mmbase.module.core.MMObjectBuilder.FIELD_OBJECT_TYPE;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.security.MMBaseCop;
import org.mmbase.security.UserContext;
import org.mmbase.storage.search.RelationStep;
import org.mmbase.storage.search.SearchQuery;
import org.mmbase.storage.search.SearchQueryException;
import org.mmbase.storage.search.StepField;
import org.mmbase.storage.search.implementation.BasicFieldValueConstraint;
import org.mmbase.storage.search.implementation.NodeSearchQuery;
import org.mmbase.util.Casting;
import org.mmbase.util.DynamicDate;
import org.mmbase.util.SerializableInputStream;
import org.mmbase.util.SizeOf;
import org.mmbase.util.functions.Function;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Document;

/**
 * MMObjectNode is the core of the MMBase system.
 * This class is what its all about, because the instances of this class hold the content we are using.
 * All active Nodes with data and relations are MMObjectNodes and make up the
 * object world that is MMBase (Creating, searching, removing is done by the node's parent,
 * which is a class extended from MMObjectBuilder)
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @author Eduard Witteveen
 * @author Michiel Meeuwissen
 * @author Ernst Bunders
 * @version $Id$
 */

public class MMObjectNode implements org.mmbase.util.SizeMeasurable, java.io.Serializable, org.mmbase.util.PublicCloneable<MMObjectNode> { // Comparable<MMObjectNode>  {

    static final ThreadLocal<Boolean> CLEAR_CHANGES = new ThreadLocal() {
        @Override
        protected Boolean initialValue() {
            return Boolean.TRUE;
        }
    };

    private static final Logger log = Logging.getLoggerInstance(MMObjectNode.class);


    /**
     * Large fields (blobs) are loaded 'lazily', so only on explicit request. Until the first exlicit request this value is stored in such fields.
     * It can be set back into the field with {@link #storeValue}, to unload the field again.
     * @since MMBase-1.7.4
     */
    public final static String VALUE_SHORTED = "$SHORTED";

    /**
     * Map which stores the current database value for fields when
     * they change in the node.
     * it can be used to optimise cacheing
     * @since MMBase-1.8
     */
    private Map<String, Object> oldValues = null;


    /**
     * Holds the name - value pairs of this node (the node's fields).
     * Most nodes will have a 'number' and an 'otype' field, and fields which will differ by builder.
     * This collection should not be directly queried or changed -
     * use the SetValue and getXXXValue methods instead.
     * It should then be made private, and methods that change the map (storeValue) be made synchronized.
     * Note: To avoid synchronisation conflicts, we can't really change the type until the property is made private.
     */
    protected Map<String, Object> values = Collections.synchronizedMap(new HashMap<String, Object>());
    private Map<String, Long> sizes = null;


    /**
     * Determines whether the node is being initialized (typically when it is loaded from the database).
     * Use {@link #start} to start initializing, use {@link #finish} to end.
     * @since MMBase-1.7
     */
    protected boolean initializing = false;

    /**
     * Holds the 'extra' name-value pairs (the node's properties)
     * which are retrieved from the 'properties' table.
     */
    private Map<String,MMObjectNode> properties;
    // object to sync access to properties
    private final Object properties_sync = new Object();


    /**
     * Set which stores the keys of the fields that were changed
     * since the last commit.
     */
    private Set<String> changed = null;

    /**
     * Pointer to the parent builder that is responsible for this node.
     * Note: this may on occasion (due to optimization) differ for the node's original builder.
     * Use {@link #getBuilder} instead.
     * @scope private
     */
    protected MMObjectBuilder parent;

    /**
     * Pointer to the actual builder to which this node belongs.
     * This value is initialised by the first call to {@link #getBuilder}
     */
    private MMObjectBuilder builder = null;

    /**
     * If <code>true</code>, the node is a new node, which is not (yet) stored in storage.
     */
    protected boolean isNew = false;

    /**
     * New aliases of the node
     */
    public Set<String> aliases = null;


    /**
     * temporarily holds a new context for a node
     * @since MMBase-1.7
     */

    private String newContext = null;

    /**
     * @since MMBase-1.9.2
     */
    MMObjectBuilder oldBuilder = null;

    /**
     * Just a counter to keep track of the number of instances of MMObjectNode that are produced in
     * this mmbase
     */
    private static long seq = 0;
    public long sequence = seq++;

   /**
    * Default Main constructor, creates a node that is new and not (yet) in storage.
    * @param parent the node's parent, an instance of the node's builder.
    * @throws IllegalArgumentException If parent is <code>null</code>
    */
    public MMObjectNode(MMObjectBuilder parent) {
        this(parent, true);
    }

    /**
     * Main constructor.
     * @param parent the node's parent, an instance of the node's builder.
     * @param isNew if the node is a newly created node
     * @throws IllegalArgumentException If parent is <code>null</code>
     */
    public MMObjectNode(MMObjectBuilder parent, boolean isNew) {
        this.isNew = isNew;
        if (parent != null) {
            this.parent = parent;
        } else {
            throw new IllegalArgumentException("Constructor called with parent=null");
        }
    }

    /**
     * @since MMBase-1.8
     */
    public MMObjectNode(MMObjectNode node) {
        parent = node.parent;
        isNew  = node.isNew();
        newContext = node.newContext;
        values.putAll(node.getValues());
        Map<String, Object> nodeOldValues = node.getOldValues();
        if (nodeOldValues != null) {
           values.putAll(nodeOldValues);
        }
        if (node.sizes != null) {
           sizesMap().putAll(node.sizes);
        }
    }

    /**
     * Creates an MMObject based on a given Map.  This can e.g. be used to make an MMObjectNode of a bridge node (use {@link org.mmbase.bridge.util.NodeMap}).
     *
     * @since MMBase-1.8
     */
    public MMObjectNode(MMObjectBuilder parent, Map<String, Object> map) {
        isNew = false;
        this.parent = parent;
        values = map;
    }

    private Map<String, Object> oldValuesMap() {
        if (oldValues == null) {
            oldValues = Collections.synchronizedMap(new HashMap<String, Object>());
        }
        return oldValues;
    }

    private Set<String> changedMap() {
        if (changed == null) {
            changed = Collections.synchronizedSet(new HashSet<String>());
        }
        return changed;
    }

    private Map<String, Long> sizesMap() {
        if (sizes == null) {
            sizes = Collections.synchronizedMap(new HashMap<String, Long>());
        }
        return sizes;
    }

    /**
     * Returns the actual builder of the node.
     * Note that it is possible that, due to optimization, a node is currently associated with
     * another (parent) builder, i.e. a posrel node may be associated with a insrel builder.
     * This method returns the actual builder.
     * The node may miss vital information (not retrieved from the database) to act as a node of such
     * a builder - if you need actual status you need to reload it.
     * @since MMBase-1.6
     * @return the builder of this node
     */
    public MMObjectBuilder getBuilder() {
        if (builder == null) {
            int oType = getOType();
            if (oType == -1 || parent.getNumber() == oType) {
                builder = parent;
            } else {
                String builderName = parent.mmb.getTypeDef().getValue(oType);
                if (builderName != null) { // avoid NPE from mmb.getBuilder.
                    builder = parent.mmb.getBuilder(builderName);
                }
            }
            if (builder == null) {
                log.warn("Builder of node " + getNumber() + " not found, taking 'object'");
                builder = parent.mmb.getBuilder("object");
            }
        }
        return builder;
    }

    /**
     * Returns the original builder after {@link #setBuilder}.
     * @since 1.9.2
     */
    public MMObjectBuilder getOldBuilder() {
        return oldBuilder;
    }


    private void fixValues(final Map<String, Object> map, MMObjectBuilder bul) {
        if (map == null) {
            return;
        }
        synchronized(map) {

            Set<String> targetFields     = bul.getFieldNames();

            Set<String> commonFields = new HashSet<String>(getBuilder().getFieldNames());
            commonFields.retainAll(targetFields);

            {   // Determin the fields that are not present in the target builder
                Set<String> droppedFields = new HashSet<String>(getBuilder().getFieldNames());
                droppedFields.removeAll(commonFields);

                // and drop those from the map
                for (String dropField : droppedFields) {
                    map.remove(dropField);
                }
            }
            {   // Determin the fields that are not present in the original builder
                Set<String> newFields = new HashSet<String>(bul.getFieldNames());
                newFields.removeAll(commonFields);

                // And set those to null.
                // TODO, we should perhaps not set them to null, but to their default value if there is one.
                for (String newField : newFields) {
                    map.put(newField, null);
                }
            }
        }
    }
    /**
     * @since MMBase-1.9.1
     */
    public void setBuilder(final MMObjectBuilder bul) {
        getBuilder();
        if (bul.equals(builder)) {
            return;
        }
        fixValues(values, bul);
        fixValues(oldValues, bul);

        if (oldBuilder == null) {
            oldBuilder = builder;
        }
        assert bul.getNumber() > 0;
        storeValue(FIELD_OBJECT_TYPE, bul.getNumber());
        delRelationsCache();
        builder = bul;
        parent = bul;
    }

    /**
     * Start the loading of a node
     * @since MMBase-1.7
     */
    public void start() {
        initializing = true;
    }

    /**
     * Finish the loading of a node
     * @since MMBase-1.7
     */
    public void finish() {
        initializing = false;
    }

    /**
     * Tests whether the data in a node is valid (throws an exception if this is not the case).
     * @throws org.mmbase.module.core.InvalidDataException
     *   If the data was unrecoverably invalid (the references did not point to existing objects)
     */
    public void testValidData() throws InvalidDataException {
        parent.testValidData(this);
    };

    /**
     * Commit the node to the database or other storage system.
     * This can only be done on a existing (inserted) node. It will use the
     * changed Vector as its base of what to commit/change.
     * @return <code>true</code> if the commit was succesfull, <code>false</code> is it failed
     */
    public boolean commit() {
        assert values.get(FIELD_NUMBER) != null;
        boolean success = parent.commit(this);
        if (success) {
            isNew = false; // perhaps it is always already false (otherwise insert is called, I think), but no matter, now it certainly isn't new!
        } else {
            if (oldValues != null) {
                values.putAll(oldValues);
            }
        }
        clearChanged();

        return success;
    }

    /**
     * Undo changes made to the node.
     *
     * @since MMBase-1.8
     */
    public void cancel() {
        if (oldValues != null) {
            values.putAll(oldValues);
        }
        clearChanged();
    }
    /**
     * Insert this node into the storage
     * @param userName the name of the user who inserts the node. This value is ignored
     * @return the new node key (number field), or -1 if the insert failed
     */
    public int insert(String userName) {
        return parent.insert(userName, this);
    }

    /**
     * Insert this node into the database or other storage system.
     * @param user the user who inserts the node.
     *        Used to set security-related information
     * @return the new node key (number field), or -1 if the insert failed
     * @since MMBase-1.7
     */
    public int insert(UserContext user) {
        String nc = newContext;
        int nodeID = parent.safeInsert(this, user.getIdentifier());
        if (nodeID != -1) {
            MMBaseCop mmbaseCop = parent.getMMBase().getMMBaseCop();
            mmbaseCop.getAuthorization().create(user, nodeID);
            if (nc != null) {
                // this node is new, so these don't actually should count as changes.
                changed.remove(FIELD_NUMBER);
                changed.remove(FIELD_OBJECT_TYPE);
                mmbaseCop.getAuthorization().setContext(user, nodeID, nc);
                if (log.isDebugEnabled()) {
                    log.debug("Context was set " + newContext + " " + this);
                }
                newContext = null;


                parent.safeCommit(this);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Context was not set " + this);
                }
            }
        }
        return nodeID;
    }

    /**
     * Commit this node to the storage
     * @param user the user who commits the node.
     *        Used to set security-related information
     * @return <code>true</code> if successful
     * @since MMBase-1.7
     */
    public boolean commit(UserContext user) {
        assert values.get("number") != null;
        boolean success = parent.safeCommit(this);
        if (success) {
            MMBaseCop mmbaseCop = parent.getMMBase().getMMBaseCop();
            mmbaseCop.getAuthorization().update(user, getNumber());
            if (newContext != null) {
                // can only be done with real number
                mmbaseCop.getAuthorization().setContext(user, getNumber(), newContext);
                newContext = null;
                parent.safeCommit(this);
            }
            mmbaseCop.getAuthorization().update(user, getNumber());
        }
        return success;
    }

    /**
     * Remove this node from the storage
     * @param user the user who removes the node.
     *        Used to set security-related information
     * @since MMBase-1.7
     */
    public void remove(UserContext user) {
        if (log.isDebugEnabled()) {
            log.debug("Deleting node " + getNumber() + " because " + Logging.stackTrace(5));
        }
        parent.removeNode(this);
        parent.getMMBase().getMMBaseCop().getAuthorization().remove(user, getNumber());
    }

    /**
     * Sets the security context for this node
     * @param user the user who changes the context of the node.
     * @param context the new context
     * @param now if <code>true</code>, the context is changed instantly, otherwise it is changed
     *        after the node is send to storage.
     * @since MMBase-1.7
     */
    public void setContext(UserContext user, String context, boolean now) {
        if (log.isDebugEnabled()) {
            log.debug("Setting context " + user + " -> " + context + " " + now + " on " + this);
        }
        if (now) {
            parent.getMMBase().getMMBaseCop().getAuthorization().setContext(user, getNumber(), context);
        } else {
            newContext = context;
        }
    }

    /**
     * Returns the security context for this node
     * @param user the user who requests the context of the node.
     * @since MMBase-1.7.1
     */
    public String getContext(UserContext user) {
        if (newContext != null) return newContext;
        if (getNumber() < 0) return user.getOwnerField();
        try {
            return parent.getMMBase().getMMBaseCop().getAuthorization().getContext(user, getNumber());
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return getStringValue("owner");
        }
    }

    /**
     * Returns the possible new security contexts for this node
     * @param user the user who requests the context of the node.
     * @since MMBase-1.7.1
     */
    public Set<String> getPossibleContexts(UserContext user) {
        if (getNumber() < 0) {
            // a new node has yet no context (except the default).
            // instead of searching the database for data, return a
            // standard set of values existing of the current context
            // and the contexts "system" and "admin".
            // A better way involves rewriting the security layer to accept
            // MMObjectNodes instead of node numbers
            Set<String> contexts = new HashSet<String>();
            contexts.add(getContext(user));
            contexts.add("admin");
            contexts.add("system");
            return contexts;
/*
            NodeSearchQuery query = new NodeSearchQuery(parent);
            CoreField fieldDefs = parent.getField("owner");
            StepField field = query.getField(fieldDefs);
            BasicFieldValueConstraint cons = new BasicFieldValueConstraint(field, getContext(user));
            query.setMaxNumber(1);
            try {
                Iterator resultList = parent.getNodes(query).iterator();
                if (resultList.hasNext()) {
                    return ((MMObjectNode) resultList.next()).getPossibleContexts(user);
                }
            } catch (SearchQueryException sqe) {
                log.error(sqe.toString());
            }
            return new HashSet();
*/
        }
        return parent.getMMBase().getMMBaseCop().getAuthorization().getPossibleContexts(user, getNumber());
    }

    /**
     * Returns the core of this node in a string.
     * Used for debugging.
     * For data exchange use toXML() and getDTD().
     * @return the contents of the node as a string.
     */
    public String toString() {
        if (parent != null) {
            return parent.toString(this);
        } else {
            return defaultToString();
        }
    }

    /**
     * @since MMBase-1.6.2
     */
    String defaultToString() {
        StringBuilder result = new StringBuilder();
        try {
            synchronized(values) {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    String key = entry.getKey();
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append(key);
                    Object value = entry.getValue();
                    if (value == null) {
                        result.append(" is null");
                    } else {
                        result.append("='").append(value.toString()).append("'");
                    }
                }
            }
        } catch(Throwable e) {
            result.append(values); // simpler version...
        }
        result.append(' ');
        result.append(super.toString());
        if (oldBuilder != null) {
            result.append(" (to be converted from " + oldBuilder.getTableName() + " to " + builder.getTableName() + ")");
        }
        return result.toString();
    }

    /**
     * @return <code>true</code> if field exists and may be used.
     * @since MMBase-1.8
     */
    protected boolean checkFieldExistance(String fieldName) {
        if (fieldName.charAt(0) == '_') {
            // don't complain then, a lot of hackery (apps1 import/export) is based on this.
            // This is just a hack to make app1 import/export working, withough exposing the values map.
            return true;
        }
        if (fieldName.indexOf('(') > 0) {
            return true;
        }
        if (!getBuilder().hasField(fieldName)) {
            if (MMBase.getMMBase().inDevelopment()) {
                throw new IllegalArgumentException("You cannot use non-existing field '" + fieldName + "' of node '" + getNumber() + "' existing fields of '" +
                                                   getBuilder().getTableName() + "' are " + getBuilder().getFieldNames());
            } else {
                log.warn("Tried to use non-existing field '" + fieldName + "' of node '" + getNumber() + "' from " + getBuilder().getTableName());
                if (log.isDebugEnabled()) {
                    log.debug(new Exception());
                } else {
                    log.warn(Logging.applicationStacktrace());

                }
                return false;
            }
        }
        return true;
    }

    /**
     * MMObjectNode's are serializable (which is used by e.g. NodeEvent's). So all values must be
     * serializable too.
     * This methods check that, and logs a warning if not so.
     * @since MMBase-1.9
     */
    protected final Object checkSerializable(String fieldName, Object fieldValue) {
        if (fieldValue instanceof org.mmbase.util.SortedBundle.ValueWrapper) {
            // TODO, I don't think that this is the correct spot to do this, but it solves some
            // test-cases failures for now
            fieldValue = ((org.mmbase.util.SortedBundle.ValueWrapper) fieldValue).getKey();
        }
        if (fieldValue != null && (! (fieldValue instanceof Serializable))) {
            log.warn("Value for " + fieldName + " is not serializable: " + fieldValue.getClass() + " " + fieldValue, new Exception());
        }
        return fieldValue;
    }

    /**
     * Stores a value in the values hashtable.
     * This is a low-level method that circumvents typechecking and the triggers of extended classes.
     * You should normally call {@link #setValue} to change fields.
     * @todo This should become a synchronized method, once values becomes a private HashMap instead of a
     * public Hashtable.
     *
     *@param fieldName the name of the field to change
     *@param fieldValue the value to assign
     */
    public void storeValue(String fieldName, Object fieldValue) {
        if (fieldName.startsWith("_") && fieldValue == null) {
            // This is just a hack to make app1 import/export working, withough exposing the values map.
            values.remove(fieldName);
        }
        if (fieldValue != null && (fieldValue instanceof InputStream && (! (fieldValue instanceof Serializable)))) {
            fieldValue = new SerializableInputStream((InputStream) fieldValue, getSize(fieldName));
        }
        if (fieldValue instanceof Node) {
            Node nodeValue = (Node) fieldValue;
            if (! (nodeValue instanceof org.mmbase.bridge.implementation.VirtualNode)) {
                int number = nodeValue.getNumber();
                if (number != -1) { // -1 is very meaningless, and it will not be possible to make a sensible node of that again
                    fieldValue = Integer.valueOf(number);
                }
            }
        }
        fieldValue = checkSerializable(fieldName, fieldValue);
        if (checkFieldExistance(fieldName)) {
            values.put(fieldName, fieldValue);
        }
        //assert (!fieldName.equals("otype") || Casting.toInt(fieldValue) > 0) : "Set otype to an invalid value " + fieldValue;
    }

    /**
     * this method stores a fieldvalue only once. the purpose is to
     * store the value only the first time a field changes, so it reflects
     * the value in the database.
     * @param fieldName
     * @param object
     * @since MMBase-1.8
     */
    private void storeOldValue(String fieldName, Object object) {
        if (! oldValuesMap().containsKey(fieldName)) {
            object = checkSerializable(fieldName,  object);
            oldValuesMap().put(fieldName, object);
        }
    }

    /**
     * Retrieves a value from the values hashtable.
     * This is a low-level method that circumvents typechecking and the triggers of extended classes.
     * You should normally call {@link #getValue} to load fields.
     *
     * @param fieldName the name of the field to change
     * @return the value of the field
     */
    public Object retrieveValue(String fieldName) {
        return values.get(fieldName);
    }

    /**
     * Determines whether the node is virtual.
     * A virtual node is not persistent (that is, stored in a database table).
     */
    public boolean isVirtual() {
        return false;
    }

    /**
     * If a node is still 'new' you must persistify it with {@link #insert(String)}, and otherwise with {@link #commit()}.
     * @since MMBase-1.8
     */
    public boolean isNew() {
        return isNew;
    }
    /*
     *
     * @since MMBase-1.6
     */

    protected Document toXML(Object value, String fieldName) {
        Document doc = Casting.toXML(value);
        if (doc == null && parent.getField(fieldName).isRequired()) {
            doc = Casting.toXML("<p/>");
        }
        return doc;
    }

    /**
     *  Sets a key/value pair in the main values of this node.
     *  Note that if this node is a node in cache, the changes are immediately visible to
     *  everyone, even if the changes are not committed.
     *  The fieldName is added to the (public) 'changed' vector to track changes.
     *  @param fieldName the name of the field to change
     *  @param fieldValue the value to assign
     *  @return <code>true</code> When the field was changed, false otherwise.
     */
    public boolean setValue(final String fieldName, Object fieldValue) {
        synchronized(values) {
            // check the value also when the parent thing is null
            Object originalValue = values.get(fieldName);

            if (fieldValue != VALUE_SHORTED) {
                // make sure this value remains not in the blob-cache.
                BlobCache blobs = parent.getBlobCache(fieldName);
                blobs.remove(blobs.getKey(getNumber(), fieldName));
            }

            if (fieldValue instanceof DynamicDate) {
                // 'dynamic' values can of course not be stored in database, and that is not the intentention too, so
                // store a static version
                fieldValue = new Date(((Date) fieldValue).getTime());
            }

            if (log.isDebugEnabled()) {
                String string;
                if (fieldValue instanceof byte[]) {
                    string = "byte array of size " + ((byte[])fieldValue).length;
                } else {
                    string = Casting.toString(fieldValue);
                    if (string.length() > 200) string = string.substring(0, 200);
                }
                log.debug("Setting " + fieldName + " to " +  string);
            }

            boolean changed =
                (! values.containsKey(fieldName)) ||
                (originalValue == null ? fieldValue != null : ! Casting.equals(originalValue, fieldValue));
            if (! changed) return false;

            if (log.isDebugEnabled()) {
                log.debug("" + fieldName + ":" + originalValue + " --> " + fieldValue);
            }

            //store the old value
            storeOldValue(fieldName, originalValue);

            // put the key/value in the value hashtable
            storeValue(fieldName, fieldValue);
            if (fieldValue instanceof byte[]) {
                setSize(fieldName, ((byte[]) fieldValue).length);
                log.debug("Setting length to " + ((byte[]) fieldValue).length);
            } else if (fieldValue instanceof org.apache.commons.fileupload.FileItem) {
                org.apache.commons.fileupload.FileItem fi = (org.apache.commons.fileupload.FileItem) fieldValue;
                setSize(fieldName, fi.getSize());
            } else if (fieldValue instanceof SerializableInputStream) {
                SerializableInputStream si = (SerializableInputStream) fieldValue;
                log.debug("Setting '" + fieldName + "' to " + si + " " + si.getSize());
                setSize(fieldName, si.getSize());
            }

            // process the changed value (?)
            if (parent != null) {
                if(!parent.setValue(this, fieldName, originalValue)) {
                    log.debug("setValue of parent returned false, no update needed...");
                    return false;
                }
            } else {
                log.error("parent was null for node with number" + getNumber());
            }
            setUpdate(fieldName);

            if (fieldValue instanceof SerializableInputStream) {
                // in case this is alled from a transaction, it must be possible to do it again on
                // actual commit
                try {
                    SerializableInputStream si = (SerializableInputStream) fieldValue;
                    si.reset();
                } catch (IOException ioe) {
                    log.error(ioe);
                }
            }
            log.debug("" + sequence + getChanged());
            return true;
        }
    }

    /**
     * Sets the size (in byte) of the given field. This is meant for byte-array fields, which you
     * fill using an InputStream.
     * @see #getSize(String)
     * @since MMBase-1.8
     */
    public void setSize(String fieldName, long size) {
        sizesMap().put(fieldName, size);
    }
    /**
     * Returns the size (in byte) of the given field. This is mainly targeted at fields of the type
     * byte array. For other fields this method will return something reasonable, but it is as yet
     * not well defined what...
     *
     * @since MMBase-1.8
     */
    public long getSize(String fieldName) {
        if (sizes != null) {
            Long l = sizes.get(fieldName);
            if (l != null)  {
                return l;
            }
        }
        Object value = values.get(fieldName);
        // Value is null so it does not occupy any space.
        if (value == null) {
            checkFieldExistance(fieldName);
            return 0;
        }
        // Value is not yet loaded from the database?
        if (VALUE_SHORTED.equals(value)) {
            return -1;
        }
        return SizeOf.getByteSize(value);
    }

    // Add the field to update to the changed Vector
    //
    private void setUpdate(String fieldName) {
        // obtain the type of field this is
        int state = getDBState(fieldName);

        // add it to the changed vector so we know that we have to update it
        // on the next commit
        if (! initializing) {
            log.trace("Marking '" + fieldName + "' as changed in " + sequence);
            changedMap().add(fieldName);
        }
        // is it a memory only field ? then send a fieldchange
        if (state == Field.STATE_VIRTUAL) {
            sendFieldChangeSignal(fieldName);
        }
    }

    /**
     * Retrieve an object's number.
     * In case of a new node that is not committed, this will return -1.
     * @return the number of the node
     */
    public int getNumber() {
        return Casting.toInt(values.get(FIELD_NUMBER));
    }

    /**
     * Retrieve an object's object type.
     * This is a number (an index in the typedef builer), rather than a name.
     * @return the object type number of the node
     */
    public int getOType() {
        return Casting.toInt(values.get(FIELD_OBJECT_TYPE));
    }

    /**
     * @since MMBase-1.8
     */
    public boolean isNull(final String fieldName) {
        if (checkFieldExistance(fieldName)) {
            Field field = getBuilder().getField(fieldName);
            if (field.isVirtual()) {
                return false;
            }
            if (field != null && field.getType() == Field.TYPE_NODE) {
                return getIntValue(fieldName) <= -1;
            }
            final Object value = values.get(fieldName);
            if (VALUE_SHORTED.equals(value)) {
                // value is not loaded from the database. We have to check the database to be sure.
                return parent.isNull(fieldName, this);
            }
            return value == null;
        } else {
            return true;
        }
    }

    /**
     * Get a value of a certain field.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>Object</code>
     */
    public Object getValue(String fieldName) {
        // get the value from the values table
        Object value = values.get(fieldName);

        //System.out.println(" " + value + " from " + values);
        // explicitly load byte values if they are 'shortened'
        if (VALUE_SHORTED.equals(value)) {   // could use == if we are sure that everybody uses the constant

            BlobCache blobs = parent.getBlobCache(fieldName);
            String key = blobs.getKey(getNumber(), fieldName);
            value = blobs.get(key);
            if (value == null) {
                int type = getDBType(fieldName);
                switch (type) {
                case Field.TYPE_BINARY:
                    if (getSize(fieldName) < blobs.getMaxEntrySize()) {
                        value = parent.getShortedByte(fieldName, this);
                    } else {
                        value = parent.getShortedInputStream(fieldName, this);
                    }
                    break;
                case Field.TYPE_STRING:
                    value = parent.getShortedText(fieldName, this);
                    break;
                default:
                    throw new UnsupportedOperationException("Found shorted value for type " + type);
                }
                if (getSize(fieldName) < blobs.getMaxEntrySize()) {
                    blobs.put(key, value);
                }
            }
        }

        // if we have an XML-dbtype field, we always have to return a Document (or null).
        // note that if the value is null we store it as a null value
        if (parent != null && value != null && !(value instanceof Document) &&
            getDBType(fieldName) == Field.TYPE_XML) {
            String string = Casting.toString(value).trim();
            Document doc = toXML(string, fieldName);
            if(doc != null) {
                // store the document inside the field.. much faster...
                value = doc;
                value = checkSerializable(fieldName, value);
                values.put(fieldName, value);
            } else {
                values.put(fieldName, null);
            }
        }

        // routine to check for indirect values
        // this are used for functions for example
        // its implemented per builder so lets give this
        // request to our builder
        if (value == null) {
            value = parent.getValue(this, fieldName);
        }
        // still null!
        if (value == null) {
            if (!checkFieldExistance(fieldName)) return null;
        }

        /*
          MMB-1808
        if (value instanceof InputStream) {
            value = useInputStream(fieldName, (InputStream) value);
        }
        */

        // return the found object
        return value;
    }

    /**
     * Get a value of a certain field.  The value is returned as a
     * String. Non-string values are automatically converted to
     * String. 'null' is converted to an empty string.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>String</code>
     */
    public String getStringValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return "" + ((MMObjectNode)value).getNumber();
        String s = Casting.toString(value);
        return s;
    }

    /**
     * XXX: return type of this method make it impossible to make MMObjectNode implements Node, perhaps it needs change
     * @javadoc
     * @since MMBase-1.6
     */
    public Object getFunctionValue(String functionName, List<?> parameters) {
        return parent.getFunctionValue(this, functionName, parameters);
    }

    /**
     * @javadoc
     * @since MMBase-1.8
     */
    public Parameters createParameters(String functionName) {
        return parent.createParameters(functionName);
    }

    /**
     * @javadoc
     * @since MMBase-1.8
     */
    public Function<?> getFunction(String functionName) {
        return parent.getFunction(this, functionName);
    }

    /**
     * @javadoc
     * @since MMBase-1.8
     */
    public Collection<Function<?>> getFunctions() {
        return parent.getFunctions(this);
    }

    /**
     * Returns the value of the specified field as a <code>dom.Document</code>
     * If the node value is not itself a Document, the method attempts to
     * attempts to convert the String value into an XML.
     * If the value cannot be converted, this method returns <code>null</code>
     *
     * @param fieldName  the name of the field to be returned
     * @return  the value of the specified field as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value cannot be converted.
     * @since MMBase-1.6
     */
    public Document getXMLValue(String fieldName) {
        Document o =  toXML(getValue(fieldName), fieldName);
        if(o != null && getDBType(fieldName) == Field.TYPE_XML) {
            storeValue(fieldName, o);
        }
        return o;
    }



    /**
     * Get a binary value of a certain field.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>byte []</code> (binary/blob field)
     */
    public byte[] getByteValue(String fieldName) {
        Object obj = getValue(fieldName);
        if (obj == null) {
            return new byte[0];
        } else if (obj instanceof byte[]) {
            // was already unmapped so return the value
            return (byte[]) obj;
        } else if (obj instanceof SerializableInputStream) {
            try {
                return ((SerializableInputStream) obj).get();
            } catch (IOException ioe) {
                log.warn(ioe);
                return new byte[0];
            }
        } else {
            byte[] b;
            if (getDBType(fieldName) == Field.TYPE_STRING) {
                String s = getStringValue(fieldName);
                try {
                    b = s.getBytes(parent.getMMBase().getEncoding());
                } catch (UnsupportedEncodingException uee) {
                    log.error(uee.getMessage());
                    b = s.getBytes();
                }
            } else {
                b = new byte[0];
            }
            return b;
        }
    }

    public InputStream getInputStreamValue(String fieldName) {
        Object value = values.get(fieldName); // don't use getValue here, it'll introduce MMB-1628
        if (value == null) {
            checkFieldExistance(fieldName);
            log.debug("NULL on " + fieldName + " " + this, new Exception());
            return new SerializableInputStream(new byte[0]);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Found " + value);
            }
        }

        if (value instanceof SerializableInputStream) {
            SerializableInputStream is = (SerializableInputStream) value;
            try {
                is.reset();
            } catch(IOException ioe) {
                log.warn(ioe);
            }
            return is;
        } else if (value instanceof InputStream) {
            // cannot return it directly, it would kill the inputstream, and perhaps it cannot be saved in db anymore then.
            // Sad, we have a buffer always now.
            // XXX think of something that the buffer is only needed if actually used a second time
            //      May be we don't have to do this if the InputStream#markSupported()
            //      We could #reset() then if needed
            return new SerializableInputStream((InputStream) value, getSize(fieldName));
        }

        if (VALUE_SHORTED.equals(value)) {
            BlobCache blobs = parent.getBlobCache(fieldName);
            String key = blobs.getKey(getNumber(), fieldName);
            byte[] v;
            if (! blobs.containsKey(key)) {
                if (getSize(fieldName) < blobs.getMaxEntrySize()) {
                    v = parent.mmb.getStorageManager().getBinaryValue(this, parent.getField(fieldName));
                    if (log.isDebugEnabled()) {
                        log.debug("Putting in blob cache " + key);
                    }
                    blobs.put(key, v);
                } else {
                    log.debug("Too big for cache, requesting InputStream directly from storage");
                    return parent.mmb.getStorageManager().getInputStreamValue(this, parent.getField(fieldName));
                }
            } else {
                v = (byte[]) blobs.get(key);
                log.debug("Found in " + v.length + " bytes in blob cache for field " + fieldName + " (expected  " + getSize(fieldName) + " )");
                //assert v.length == getSize(fieldName) : ("" + fieldName + ":" + v.length + " " + sizes);
                if (v.length != getSize(fieldName)) {
                    log.warn("Size " + getSize(fieldName) + ", not set or incorrect for #" + getNumber() + " (" + fieldName + " should be " + v.length + "). Correcting now.");
                    setSize(fieldName, v.length);
                }
            }
            return v == null ? null : new SerializableInputStream(v);
        } else {
            if (value instanceof byte[]) {
                return new SerializableInputStream((byte[]) value);
            } else {
                // probably not a byte-array field, do something.
                // this behavior is undefined!, don't depend on it.
                return new ByteArrayInputStream(("" + value).getBytes());
            }
        }
    }

    /**
     * Get a value of a certain field.
     * The value is returned as an MMObjectNode.
     * If the field contains an Numeric value, the method
     * tries to obtrain the object with that number.
     * If it is a String, the method tries to obtain the object with
     * that alias. The only other possible values are those created by
     * certain virtual fields.
     * All remaining situations return <code>null</code>.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>int</code>
     */
    public MMObjectNode getNodeValue(String fieldName) {
        if (fieldName == null || fieldName.equals(FIELD_NUMBER)) return this;
        Object value = getValue(fieldName);
        MMObjectNode res = null;
        if (value instanceof MMObjectNode) {
            res = (MMObjectNode) value;
        } else if (value instanceof Node) {
            Node node = (Node) value;
            if (node.isNew()) {
                throw new UnsupportedOperationException("dropped tmpnodemanager...");
            } else if (value instanceof org.mmbase.bridge.implementation.VirtualNode) {
                res = new VirtualNode(new org.mmbase.bridge.util.NodeMap(node));
            } else {
                res = parent.getNode(node.getNumber());
            }
        } else if (value instanceof Number) {
            int nodenumber = ((Number)value).intValue();
            if (nodenumber != -1) {
                res = parent.getNode(nodenumber);
            }
        } else if (value != null && !value.equals("")) {
            res = parent.getNode(value.toString());
        }
        return res;
    }

    /**
     * Get a value of a certain field.
     * The value is returned as an int value. Values of non-int, numeric fields are converted if possible.
     * Booelan fields return 0 for false, 1 for true.
     * String fields are parsed to a number, if possible.
     * If a value is an MMObjectNode, its numberfield is returned.
     * All remaining field values return -1.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>int</code>
     */
    public int getIntValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return ((MMObjectNode)value).getNumber();
        return Casting.toInt(value);
    }

    /**
     * Get a value of a certain field.
     * The value is returned as an boolean value.
     * If the actual value is numeric, this call returns <code>true</code>
     * if the value is a positive, non-zero, value. In other words, values '0'
     * and '-1' are concidered <code>false</code>.
     * If the value is a string, this call returns <code>true</code> if
     * the value is "true" or "yes" (case-insensitive).
     * In all other cases (including calling byte fields), <code>false</code>
     * is returned.
     * Note that there is currently no basic MMBase boolean type, but some
     * <code>excecuteFunction</code> calls may return a Boolean result.
     *
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>int</code>
     */
    public boolean getBooleanValue(String fieldName) {
        return Casting.toBoolean(getValue(fieldName));
    }

    /**
     * Get a value of a certain field.
     * The value is returned as an Integer value. Values of non-Integer, numeric fields are converted if possible.
     * Boolean fields return 0 for false, 1 for true.
     * String fields are parsed to a number, if possible.
     * All remaining field values return -1.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as an <code>Integer</code>
     */
    public Integer getIntegerValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return ((MMObjectNode)value).getNumber();
        return Casting.toInteger(value);
    }

    /**
     * Get a value of a certain field.
     * @see #getValue
     * @see Casting#toLong
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>long</code>
     */
    public long getLongValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return ((MMObjectNode)value).getNumber();
        return Casting.toLong(value);
    }

    /**
     * Get a value of a certain field.
     * The value is returned as a float value. Values of non-float, numeric fields are converted if possible.
     * Boolean fields return 0 for false, 1 for true.
     * String fields are parsed to a number, if possible.
     * All remaining field values return -1.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>float</code>
     */
    public float getFloatValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return ((MMObjectNode)value).getNumber();
        return Casting.toFloat(value);
    }

    /**
     * Get a value of a certain field.
     * The value is returned as a double value. Values of non-double, numeric fields are converted if possible.
     * Boolean fields return 0 for false, 1 for true.
     * String fields are parsed to a number, if possible.
     * All remaining field values return -1.
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>double</code>
     */
    public double getDoubleValue(String fieldName) {
        Object value = getValue(fieldName);
        if (value instanceof MMObjectNode) return ((MMObjectNode)value).getNumber();
        return Casting.toDouble(value);
    }

    /**
     * Get a value of a certain field.
     * The value is returned as a Date value. Values of numeric fields are converted as if they were
     * time in seconds since 1/1/1970.
     * String fields are parsed to a date, if possible.
     * All remaining field values return -1.
     * @since MMBase-1.8
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>Date</code>
     */
    public Date getDateValue(String fieldName) {
        Object value = getValue(fieldName);
        org.mmbase.core.CoreField cf = getBuilder().getField(fieldName);
        if (cf != null && cf.getType() == Field.TYPE_NODE) {
            // cannot be handled by casting, because it would receive object-number and cannot make distinction with Nodes.
            return new Date(-1);
        }
        return Casting.toDate(value);
    }

    /**
     * Get a value of a certain field.
     * The value is returned as a List value.
     * Strings are treated as comma-seperated value lists, and split into their component parts.
     * Values of other fields are returned as Lists of one object.
     * @since MMBase-1.8
     * @param fieldName the name of the field who's data to return
     * @return the field's value as a <code>List</code>
     */
    public List getListValue(String fieldName) {
        return Casting.toList(getValue(fieldName));
    }

    /**
     * Returns the DBType of a field.
     * @param fieldName the name of the field which' type to return
     * @return the field's DBType
     */
    public int getDBType(String fieldName) {
        return parent.getDBType(fieldName);
    }

    /**
     * Returns the DBState of a field.
     * @param fieldName the name of the field who's state to return
     * @return the field's DBState
     */
    public int getDBState(String fieldName) {
        if (parent != null)    {
            return parent.getDBState(fieldName);
        } else {
            return Field.STATE_UNKNOWN;
        }
    }

    /**
     * Return the names of all persistent fields that were changed.
     * Note that this is a direct reference. Changes (i.e. clearing the vector) will affect the node's status.
     * @return An unmodifiable Set containing Strings.
     */
    public Set<String> getChanged() {
        return Collections.unmodifiableSet(changedMap());
    }

    /**
     * Tests whether one of the values of this node was changed since the last commit/insert.
     * @return <code>true</code> if changes have been made, <code>false</code> otherwise
     */
    public boolean isChanged() {
        return oldBuilder != null || newContext != null || (changed != null && changed.size() > 0);
    }

    /**
     * Clear the 'signal' Vector with the changed keys since last commit/insert.
     * Marks the node as 'unchanged'.
     * Does not affect the values of the fields, nor does it commit the node.
     * @return always <code>true</code>
     */
    public boolean clearChanged() {
        if (CLEAR_CHANGES.get()) {
            oldBuilder = null;
            newContext = null;
            if (changed != null) {
                changed.clear();
            }
            if (oldValues != null) {
                oldValues.clear();
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * Deletes the propertie cache for this node.
     * Forces a reload of the properties on next use.
     */
    public void delPropertiesCache() {
        synchronized(properties_sync) {
            properties = null;
        }
    }

    /**
     * Returns an (unmodifiable) map representing all key/values present in this node.
     * This can be modified by other users of this object though. You'll need to synchronize on it,
     * when iterating (see {@link java.util.Collections#synchronizedMap}).
     */
    public Map<String, Object> getValues() {
        return  Collections.unmodifiableMap(values);
    }
    /**
     * This can be modified by other users of this object though. You'll need to synchronize on it,
     * when iterating (see {@link java.util.Collections#synchronizedMap}).
     * @since MMBase-1.8
     */
    public Map<String, Object> getOldValues() {
        return Collections.unmodifiableMap(oldValuesMap());
    }

    /**
     * Return a the properties for this node. Properties are stored in a dedicated builder
     * 'properties'. The property values are returned still wrapped in the nodes in that builder.
     *
     * @return the properties as an unmodifiable <code>Map</code>
     */
    public Map<String, MMObjectNode> getProperties() {
        synchronized(properties_sync) {
            if (properties == null) {
                properties = new HashMap<String,MMObjectNode>();
                MMObjectBuilder bul = parent.mmb.getBuilder("properties");
                NodeSearchQuery query = new NodeSearchQuery(bul);
                StepField parentField = query.getField(bul.getField("parent"));
                BasicFieldValueConstraint cons = new BasicFieldValueConstraint(parentField, getNumber());
                query.setConstraint(cons);
                try {
                    for (MMObjectNode property : bul.getNodes(query)) {
                        String key = property.getStringValue("key");
                        properties.put(key, property);
                    }
                } catch (SearchQueryException sqe) {
                    log.error(sqe.getMessage(), sqe);
                }
            }
        }
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns a specified property of this node.
     * @param key the name of the property to retrieve
     * @return the property object as a <code>MMObjectNode</code>
     */
    public MMObjectNode getProperty(String key) {
        synchronized(properties_sync) {
            if (properties == null) {
                getProperties();
            }
            return properties.get(key);
        }
    }

    /**
     * Sets a specified property for this node.
     * This method does not commit anything - it merely updates the node's propertylist.
     * @param node the property object as a <code>MMObjectNode</code>
     */
    public void putProperty(MMObjectNode node) {
        synchronized(properties_sync) {
            if (properties == null) {
                getProperties();
            }
            properties.put(node.getStringValue("key"), node);
        }
    }

    /**
     * Return the GUI indicator for this node.
     * The GUI indicator is a string that represents the contents of this node.
     * By default it is the string-representation of the first non-system field of the node.
     * Individual builders can alter this behavior.
     * @return the GUI iddicator as a <code>String</code>
     */
    public String getGUIIndicator() {
        if (parent!=null) {
            return parent.getGUIIndicator(this);
        } else {
            log.error("MMObjectNode -> can't get parent");
            return "problem";
        }
        //return "" + getFunctionValue("gui", null); // proposed fix for MMB-1575. No good, it can
        //cause infinite loops.
    }

    /**
     * Return the buildername of this node
     * @return the builder table name
     */
    public String getName() {
        return  parent.getTableName();
    }

    /**
     * Delete the relation cache for this node.
     * This means it will be reloaded from the database/storage on next use.
     */
    public void delRelationsCache() {
        delRelationsCache(getNumber());
    }

    /**
     * Delete the relation cache for this node.
     * This means it will be reloaded from the database/storage on next use.
     * @param number nodenumber
     */
    public static void delRelationsCache(Integer number) {
        RelationsCache.getCache().remove(number);
    }

    /**
     * Returns whether this node has relations.
     * This includes unidirection relations which would otherwise not be counted.
     * @return <code>true</code> if any relations exist, <code>false</code> otherwise.
     */
    public boolean hasRelations() {
        // return getRelationCount()>0;
        return parent.mmb.getInsRel().hasRelations(getNumber());
    }

    /**
     * Return all the relations of this node.
     * Use only to delete the relations of a node.
     * Note that this returns the nodes describing the relation - not the nodes 'related to'.
     * @return An <code>Enumeration</code> containing the nodes
     */
    public Enumeration<MMObjectNode> getAllRelations() {
        Vector<MMObjectNode> allrelations=parent.mmb.getInsRel().getAllRelationsVector(getNumber());
        if (allrelations!=null) {
            return allrelations.elements();
        } else {
            return null;
        }
    }

    /**
     * Return the relations of this node.
     * Note that this returns the nodes describing the relation - not the nodes 'related to'.
     *
     *
     * XXX: return type of this method makes it impossible to make MMObjectNode implements Node, perhaps it needs change
     *
     * @return An <code>Enumeration</code> containing the nodes
     */
    public Enumeration<MMObjectNode> getRelations() {
        List<MMObjectNode> relations = getRelationNodes();
        if (relations != null) {
            return Collections.enumeration(relations);
        } else {
            return null;
        }
    }

    /**
     * @since MMBase-1.7
     * @scope public?
     */
    protected List<MMObjectNode> getRelationNodes() {
        Integer number = Integer.valueOf(getNumber());
        List<MMObjectNode> relations;
        RelationsCache relationsCache = RelationsCache.getCache();
        if (! relationsCache.contains(number)) {
            relations = parent.getRelations_main(getNumber());
            relationsCache.put(number, relations);

        } else {
            relations = relationsCache.get(number);
        }
        return relations;
    }

    /**
     * Remove the relations of the node.
     */
    public void removeRelations() {
        parent.removeRelations(this);
    }

    /**
     * Returns the number of relations of this node.
     * @return An <code>int</code> indicating the number of nodes found
     */
    public int getRelationCount() {
        List<MMObjectNode> relations = getRelationNodes();
        if (relations!=null) {
            return relations.size();
        } else {
            return 0;
        }
    }

    /**
     * Return the relations of this node, filtered on a specified type.
     * Note that this returns the nodes describing the relation - not the nodes 'related to'.
     * @param otype the 'type' of relations to return. The type identifies a relation (InsRel-derived) builder, not a reldef object.
     * @return An <code>Enumeration</code> containing the nodes
     */
    public Enumeration<MMObjectNode> getRelations(int otype) {
        Enumeration<MMObjectNode> e = getRelations();
        Vector<MMObjectNode> result=new Vector<MMObjectNode>();
        if (e!=null) {
            while (e.hasMoreElements()) {
                MMObjectNode tnode = e.nextElement();
                if (tnode.getOType() == otype) {
                    result.addElement(tnode);
                }
            }
        }
        return result.elements();
    }

    /**
     * Return the relations of this node, filtered on a specified type.
     * Note that this returns the nodes describing the relation - not the nodes 'related to'.
     * @param wantedtype the 'type' of relations to return. The type identifies a relation (InsRel-derived) builder, not a reldef object.
     * @return An <code>Enumeration</code> containing the nodes
     */
    public Enumeration<MMObjectNode> getRelations(String wantedtype) {
        int otype=parent.mmb.getTypeDef().getIntValue(wantedtype);
        if (otype!=-1) {
            return getRelations(otype);
        }
        return null;
    }

    /**
     * Return the number of relations of this node, filtered on a specified type.
     * @param wt the 'type' of related nodes (NOT the relations!).
     * @return An <code>int</code> indicating the number of nodes found
     */
    public int getRelationCount(String wt) {
        int count = 0;
        MMObjectBuilder wantedType = parent.mmb.getBuilder(wt);
        if (wantedType != null) {
            List<MMObjectNode> relations = getRelationNodes();
            if (relations != null) {
                for(Enumeration<MMObjectNode> e= Collections.enumeration(relations); e.hasMoreElements();) {
                    MMObjectNode tnode = e.nextElement();
                    int relation_number =tnode.getIntValue("snumber");
                    int nodetype =0;

                    // bugfix #6432: marcel: determine source of relation, get type, display
                    // error when nodetype is determined to be -1, which is a possible wrongly inserted relation

                    if (relation_number==getNumber()) {
                        relation_number = tnode.getIntValue("dnumber");
                        nodetype = parent.getNodeType(relation_number);
                    } else {
                        nodetype = parent.getNodeType(relation_number);
                    }

                    // Display situation where snumber or dnumber from a relation-node does not seem to
                    // exsist in the database. This can be fixed by mannually removing the node out of the insrel-table
                    if(nodetype==-1) {
                        log.warn("Warning: relation_node("+tnode.getNumber()+") has a possible removed relation_number("+relation_number+"), manually check its consistency!");
                    }

                    MMObjectBuilder nodeType = parent.mmb.getBuilder(parent.mmb.getTypeDef().getValue(nodetype));
                    if (nodeType!=null && (nodeType.equals(wantedType) || nodeType.isExtensionOf(wantedType))) {
                        count++;
                    }
                }
            }
        } else {
            log.warn("getRelationCount is requested with an invalid Builder name (otype "+wt+" does not exist)");
        }
        return count;
    }

    /**
     * Return the age of the node, determined using the daymarks builder.
     * @return the age in days, or 0 if unknown (daymarks builder not present)
     */
    public int getAge() {
        DayMarkers dayMarkers = ((DayMarkers) parent.mmb.getBuilder("daymarks"));
        if (dayMarkers == null) return 0;
        return dayMarkers.getAge(getNumber());
    }

    /**
     * Sends a field-changed signal.
     * @param fieldName the name of the changed field.
     * @return always <code>true</code>
     */
    public boolean sendFieldChangeSignal(String fieldName) {
        return parent.sendFieldChangeSignal(this, fieldName);
    }

    /**
     * Sets the node's alias.
     * The code only sets a (memory) property, it does not actually add the alias to the database.
     * Only works for un-inserted Nodes. So this is actually only used for application import.
     * No need to use this. Use {@link MMObjectBuilder#createAlias(int, String)}.
     */
    public void setAlias(String alias) {
        if (aliases == null) aliases = new HashSet<String>();
        synchronized(aliases) {
            aliases.add(alias);
        }
    }

    /**
     */
    void useAliases() {
        if (aliases != null) {
            synchronized(aliases) {
                if (getNumber() <= 0) {
                    log.error("Trying to set aliases for uncommited node!!");
                    return;
                }
                for (String alias : aliases) {
                    try {
                        parent.createAlias(getNumber(), alias, getStringValue("owner"));
                    } catch (org.mmbase.storage.StorageException se) {
                        log.error(se);
                    }
                }
                aliases.clear();
            }
        }
    }

    /**
     * Get all related nodes. The returned nodes are not the
     * nodes directly attached to this node (the relation nodes) but the nodes
     * attached to the relation nodes of this node.
     *
     * XXX: return type of this method make it impossible to make MMObjectNode implements Node, perhaps it needs change
     *
     * @return a <code>Vector</code> containing <code>MMObjectNode</code>s
     */
    public Vector<MMObjectNode> getRelatedNodes() {
        return getRelatedNodes("object", null, RelationStep.DIRECTIONS_EITHER);
    }

    /**
     * Makes number -> MMObjectNode of a list of MMObjectNodes.
     * @since MMBase-1.6.2
     */
    private Map<Integer, MMObjectNode>  makeMap(List<MMObjectNode> v) {
        Map<Integer, MMObjectNode>     result = new HashMap<Integer, MMObjectNode>();
        for (MMObjectNode node : v) {
            result.put(node.getNumber(), node);
        }
        return result;
    }

    /**
     * Get the related nodes of a certain type. The returned nodes are not the
     * nodes directly attached to this node (the relation nodes) but the nodes
     * attached to the relation nodes of this
     *
     * XXX: return type of this method make it impossible to make MMObjectNode implements Node, perhaps it needs change
     *
     * @param type the type of objects to be returned
     * @return a <code>Vector</code> containing <code>MMObjectNode</code>s
     */
    public Vector<MMObjectNode> getRelatedNodes(String type) {
        if (log.isDebugEnabled()) {
            log.debug("Getting related nodes of " + this + " of type " + type);
        }

        if(InsRel.usesdir) {
            return  getRelatedNodes(type, RelationStep.DIRECTIONS_BOTH);
        } else {
            //
            // determine related nodes
            Map<Integer, MMObjectNode> source = makeMap(getRelatedNodes(type, RelationStep.DIRECTIONS_SOURCE));
            Map<Integer, MMObjectNode> destin = makeMap(getRelatedNodes(type, RelationStep.DIRECTIONS_DESTINATION));

            if (log.isDebugEnabled()) {
                log.debug("source("+source.size()+") - destin("+destin.size()+")");
            }
            // remove duplicates (can happen if multirel is being used when no dir on insrel exists)
            destin.putAll(source);
            return new Vector<MMObjectNode>(destin.values());
        }
    }

    /**
     * If you query from this_node_type(type) (typex, insrel, typey where typex == typey) {
     *   if the insrel table is directional, use the multirelations.SEARCH_BOTH
     *   if the insrel table is not directional, use the multirelations.SEARCH_SOURCE + multirelations.SEARCH_DESTINATION
     * }
     * Otherwise the SEARCH_BOTH will result in an OR on insrel which will never return in
     * (huge) databases.
     * @param type the type of teh realted node to return
     * @param search_type the type of directionality to use
     * @since MMBase-1.6.3
     */
    public Vector<MMObjectNode> getRelatedNodes(String type, int search_type) {
        return getRelatedNodes(type, "insrel",  search_type);
    }

    /**
     * If you query from this_node_type(type) (typex, insrel, typey where typex == typey) {
     *   if the insrel table is directional, use the multirelations.SEARCH_BOTH
     *   if the insrel table is not directional, use the multirelations.SEARCH_SOURCE + multirelations.SEARCH_DESTINATION
     * }
     * Otherwise the SEARCH_BOTH will result in an OR on insrel which will never return in
     * (huge) databases.
     * @param type the type of teh realted node to return
     * @param role the role of the relation (null if no role specified)
     * @param search_type the type of directionality to use
     * @since MMBase-1.6.3
     */
    public Vector<MMObjectNode> getRelatedNodes(String type, String role, int search_type) {
        Vector<MMObjectNode> result = null;

        MMObjectBuilder builder = parent.mmb.getBuilder(type);

        // example: we want a thisnode.relatedNodes(mediaparts) where mediaparts are of type
        // audioparts and videoparts. This method will return the real nodes (thus of type audio/videoparts)
        // when asked to get nodes of type mediaparts.
        //
        // - get a list of virtual nodes from a multilevel("this.parent.name, type") ordered on otype
        //   (this will return virtual audio- and/or videoparts ordered on their *real* parent)
        // - construct a list of nodes for each parentbuilder seperately
        // - ask the parentbuilder for each list of virtual nodes to get a list of the real nodes
        if( builder != null ) {

            ClusterBuilder clusterBuilder = parent.mmb.getClusterBuilder();

            // multilevel from table this.parent.name -> type
            List<String> tables = new ArrayList<String>();
            tables.add(parent.getTableName() + "1");
            if (role != null) {
                tables.add(role);
            }
            tables.add(type + "2");

            // return type.number (and otype for sorting)
            List<String> fields = new ArrayList<String>();
            fields.add(type + "2.number");
            fields.add(type + "2.otype");

            // order list UP
            List<String> directions = new ArrayList<String>();
            directions.add("UP");

            // and order on otype
            List<String> ordered = new ArrayList<String>();
            ordered.add(type + "2.otype");

            List<String> snodes = new ArrayList<String>();
            snodes.add("" + getNumber());

            SearchQuery query = clusterBuilder.getMultiLevelSearchQuery(snodes, fields, "NO", tables,  null, ordered, directions, search_type);
            RelatedNodesCache relatedCache = RelatedNodesCache.getCache();
            List<MMObjectNode> v = relatedCache.get(query);
            if (v == null) {
                try {
                    v = clusterBuilder.getClusterNodesFromQueryHandler(query);
                    relatedCache.put(query, v);
                } catch (SearchQueryException sqe) {
                    log.error(sqe.toString());
                    v = null;
                }
            }
            if(v == null) {
                result = new Vector<MMObjectNode>();
            } else {
                result = new Vector<MMObjectNode>(getRealNodes(v, type + "2"));
            }
        } else {
            log.error("This type(" + type + ") is not a valid buildername!");
            result = new Vector<MMObjectNode>(); // return empty vector
        }

        if (log.isDebugEnabled()) {
            log.debug("related(" + parent.getTableName() + "("+getNumber() + ")) -> " + type + " = size(" + result.size() + ")");
        }

        return result;
    }

    /**
     * Loop through the virtuals vector, group all same nodes based on parent and fetch the real nodes from those parents
     *
     * @param virtuals  of virtual nodes (only type.number and type.otype fields are set)
     * @param type needed to retreive the otype, which is set in node as type + ".otype"
     * @returns List of real nodes
     *
     * @see #getRelatedNodes(String type)
     * @since MMBase-1.6.2
     */
    private List<MMObjectNode> getRealNodes(List<MMObjectNode> virtuals, String type) {

        log.debug("Getting real nodes");
        List<MMObjectNode> result  = new ArrayList<MMObjectNode>();

        List<MMObjectNode> list    = new ArrayList<MMObjectNode>();
        int             ootype  = -1;
        List <Integer> virtualNumbers = new ArrayList<Integer>();

        // fill the list
        Iterator<MMObjectNode> i = virtuals.iterator();
        while (i.hasNext()) {
            MMObjectNode node = i.next();
            Integer number = node.getIntegerValue(type + ".number");
            if (!virtualNumbers.contains(number)) {
                virtualNumbers.add(number);

                int otype   = node.getIntValue(type + ".otype");

                // convert the nodes of type ootype to real numbers
                if(otype != ootype) {
                    // if we have nodes return real values
                    if(ootype != -1) {
                        result.addAll(getRealNodesFromBuilder(list, ootype));
                        list = new ArrayList<MMObjectNode>();
                    }
                    ootype  = otype;
                }
                // convert current node type.number and type.otype to number and otype
                String builderName = parent.mmb.getTypeDef().getValue(otype);
                if (builderName == null) {
                    log.warn("Could not find builder of node " + node.getNumber() + " taking 'object'");
                    builderName = "object";
                    otype = parent.mmb.getBuilder(builderName).getObjectType();
                }

                MMObjectNode convert = new MMObjectNode(parent.mmb.getBuilder(builderName), false);
                // parent needs to be set or else mmbase does nag nag nag on a setValue()
                convert.setValue(FIELD_NUMBER, node.getValue(type + ".number"));
                assert ootype > 0;
                convert.setValue(FIELD_OBJECT_TYPE, ootype);
                list.add(convert);
            }
            // first and only list or last list, return real values
            if(!i.hasNext()) {
                // log.debug("subconverting last "+list.size()+" nodes of type("+otype+")");
                result.addAll(getRealNodesFromBuilder(list, ootype));
            }
        }

        // check that we didnt loose any nodes
        if(virtualNumbers.size() != result.size()) {
            log.error("We lost a few nodes during conversion from virtualnodes(" + virtuals.size() + ") to realnodes(" + result.size() + ")");
            StringBuffer vNumbers = new StringBuffer();
            for (int j = 0; j < virtualNumbers.size(); j++) {
                vNumbers.append(virtualNumbers.get(j)).append(" ");
            }
            log.error("Virtual node numbers: " + vNumbers.toString());
            StringBuffer rNumbers = new StringBuffer();
            for (int j = 0; j < result.size(); j++) {
                int resultNumber = (result.get(j)).getIntValue("number");
                rNumbers.append(resultNumber).append(" ");
            }
            log.error("Real node numbers: " + rNumbers.toString());
        }

        return result;
    }

    /**
     * Upgrade a certain list of MMObectNodes to the right type.
     * @since MMBase-1.6.2
     */
    private List<MMObjectNode> getRealNodesFromBuilder(List<MMObjectNode> list, int otype) {
        List<MMObjectNode> result = new ArrayList<MMObjectNode>();
        String name = parent.mmb.getTypeDef().getValue(otype);
        if(name != null) {
            MMObjectBuilder rparent = parent.mmb.getBuilder(name);
            if(rparent != null) {
                result.addAll(rparent.getNodes(list));
            } else {
                log.error("This otype(" + otype + ") does not denote a valid typedef-name(" + name + ")!");
            }
        } else {
            log.error("This otype(" + otype + ") gives no name from typedef!");
        }
        return result;
    }

    public int getByteSize() {
        return getByteSize(new SizeOf());
    }

    public int getByteSize(SizeOf sizeof) {
        return sizeof.sizeof(values) +
            sizeof.sizeof(oldValues) +
            sizeof.sizeof(sizes) +
            sizeof.sizeof(properties) +
            sizeof.sizeof(changed) +
            12 * SizeOf.SZ_REF;
    }

    /**
     * @since MMBase-1.6.2
     */
    public int hashCode() {
        if (parent != null) {
            return parent.hashCode(this);
        } else {
            return super.hashCode();
        }
    }

    /**
     * @since MMBase-1.6.2
     */
    public boolean equals(Object o) {
        if (o instanceof MMObjectNode) {
            MMObjectNode n = (MMObjectNode) o;
            if (parent != null) {
                return parent.equals(this, n);
            } else {
                return defaultEquals(n);
            }
        }
        return false;
    }
    /**
     * @since MMBase-1.6.2
     */
    public boolean defaultEquals(MMObjectNode n) {
        /*
          if (getNumber() >= 0) {  // we know when real nodes are equal
          return n.getNumber() == getNumber();
          } else { // I don't know about others
          return super.equals(n); // compare as objects.
          }
        */
        return super.equals(n); // compare as objects.
    }

    /**
     * Custom serialize method for MMObjectNode. The main reason this method exists is
     * that the builder for an object will not be serialized, but the tablename for
     * the object will be saved instead. During deserialization the builder will
     * be recovered using that name.
     * @since MMBase-1.8.0
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(oldValues);
        out.writeObject(values);
        out.writeObject(sizes);
        out.writeBoolean(initializing);
        out.writeObject(properties);
        out.writeObject(changed);

        // Save parent and builder by name, not by object
        if (parent == null) {
            out.writeObject(null);
        } else {
            out.writeObject(parent.getTableName());
        }
        if (builder == null) {
            out.writeObject(null);
        } else {
            out.writeObject(builder.getTableName());
        }
        out.writeBoolean(isNew);
        out.writeObject(aliases);
        out.writeObject(newContext);
    }

    /**
     * Custom deserialize method for MMObjectNode. The main reason this method exists is
     * that the builder for an object will not be serialized, but the tablename for
     * the object will be saved instead. During deserialization the builder will
     * be recovered using that name.
     * @since MMBase-1.8.0
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        oldValues = (Map<String, Object>) in.readObject();
        values = (Map<String, Object>) in.readObject();
        sizes = (Map<String, Long>) in.readObject();
        initializing = in.readBoolean();
        properties = (Hashtable<String,MMObjectNode>) in.readObject();
        changed = (Set<String>)in.readObject();

        // Retrieve parent and builder by name, not by object
        String parentName = (String)in.readObject();
        if (parentName != null) {
            parent = MMBase.getMMBase().getBuilder(parentName);
        }
        String builderName = (String)in.readObject();
        if (builderName != null) {
            builder = MMBase.getMMBase().getBuilder(builderName);
        }
        isNew = in.readBoolean();
        aliases = (Set<String>)in.readObject();
        newContext = (String)in.readObject();
    }

    public int compareTo(MMObjectNode n) {
        return getNumber() - n.getNumber();
    }

    public MMObjectNode clone() {
        try {
            MMObjectNode clone = (MMObjectNode) super.clone();
            clone.values = new HashMap<String, Object>();
            clone.values.putAll(this.values);
            return clone;
        } catch (CloneNotSupportedException cnse) {
            log.error("Java sucks");
            return null;
        }
    }
}
