/*
 * ClassName: TmpObject.java
 *
 * Date: dec. 1st. 2001
 *
 * Copyright notice: 
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 *
 */

package org.mmbase.applications.xmlimporter;

import java.util.*;
import org.mmbase.module.Module;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.TemporaryNodeManager;
import org.mmbase.module.core.TemporaryNodeManagerInterface;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A TmpObject represents a temporary object in a transaction. 
 * TmpObject instances can only be created by methods of Transaction, and have
 * no meaning outside the context of the transaction in which they are created. 
 *
 * @author Rob van Maris: Finalist IT Group
 *
 * @version 1.0
 */
public class TmpObject {
    
    // Node field names.
    final static String NUMBER   = "number";
    final static String RNUMBER   = "rnumber";
    final static String SNUMBER   = "snumber";
    final static String DNUMBER   = "dnumber";
    final static String _SNUMBER  = "_snumber";
    final static String _DNUMBER  = "_dnumber";

    /** Logger instance. */
    private static Logger log 
        = Logging.getLoggerInstance(TmpObject.class.getName());
    
    /** The mmbase module. */
    private static MMBase mmbase;
    
    /** The temporary node manager. */
    private static TemporaryNodeManagerInterface tmpNodeManager;
    
    /** All user-related data. */
    private UserTransactionInfo uti;
    
    /** User specified id. */
    private String id;
    
    /** The MMObjectNode in the temporary cloud. */
    private MMObjectNode node;
    
    /** Relation flag, true if this a relation object, false otherwise. */
    private boolean relationFlag;
    
    /** Flag, true if this object is to be dropped when it has
     * no relations on commit, false otherwise. */
    private boolean disposeWhenNotReferenced = false;
    
    /**
     * Gets reference to the MMBase module.
     * @return The MMBase module.
     */
    private static MMBase getMMBase() {
        if (mmbase == null) {
            mmbase=(MMBase) Module.getModule("MMBASEROOT");
        }
        return mmbase;
    }
    
    /**
     * Gets reference to TemporaryNodeManager module.
     * @return the TemporaryNodeManager module.
     */
    private static synchronized TemporaryNodeManagerInterface getTmpNodeManager() {
        if (tmpNodeManager == null) {
            tmpNodeManager = new TemporaryNodeManager(getMMBase());
        }
        return tmpNodeManager;
    }
    
    /**
     * Creates new import TmpObject.
     * @param uti transaction info for current user.
     * @param objectId user-specified id for the new object
     * (must be unique in this transaction context),
     * or null for anonymous object.
     * @param relationFlag relation flag: true if this is a relation, false
     * otherwise.
     * @param disposeWhenNotReferenced flag: true if this object is
     * to be dropped when it has no relations on commit, false otherwise.
     */
    TmpObject(UserTransactionInfo uti, String objectId,
    boolean relationFlag, boolean disposeWhenNotReferenced) {
        this.uti = uti;
        this.id = objectId;
        this.node = getTmpNodeManager().getNode(uti.user.getName(), objectId);
        this.relationFlag = relationFlag;
        this.disposeWhenNotReferenced = disposeWhenNotReferenced;
    }
    
    /**
     * Creates new access TmpObject.
     * @param uti transaction info for current user.
     * @param objectId user-specified id for the new object
     * (must be unique in this transaction context),
     * or null for anonymous object.
     * @param mmbaseId the MMBase id.
     */
    TmpObject(UserTransactionInfo uti, String objectId, int mmbaseId) {
        // Ironically, the mmbaseId argument is no longer needed.
        this.uti = uti;
        this.id = objectId;
        this.node = getTmpNodeManager().getNode(uti.user.getName(), objectId);
        this.relationFlag = (node.parent instanceof InsRel);
    }
    
    /**
     * Gets field of the temporary node.
     * @param name The field.
     * @return The value of the field.
     */
    public Object getField(String name) {
        return node.getValue(name);
    }
    
    /**
     * Sets field of the temporary node represented by this TmpObject instance.
     * @param name The field name.
     * @param value The field value.
     */
    public void setField(String name, Object value) {
        node.setValue(name, value);
    }
    
    /**
     * Gets the relations of this object in the persistent cloud.
     * Note that the relations returned are always of builder type 'InsRel', 
     * even if they are really from a derived builder such as AuthRel.
     * @return All relations in the persistent cloud of the object in the 
     * persistent cloud represented by this TmpObject instance.
     */
    public Vector getRelationsInPersistentCloud() {
        Vector relations;
        int mmbaseId = getMMBaseId();
        if (mmbaseId != -1) {
            // Access object.
            relations = getMMBase().getInsRel().getRelations_main(mmbaseId);
            if (log.isDebugEnabled()) {
                log.debug("Relations in persistent cloud of " + this
                + ": " + relations);
            }
        } else {
            // Not an access object, so it has no relations in persistent cloud.
            relations = new Vector();
        }
        return relations;
    }

    /**
     * Gets the temporary node corresponding to this object.
     * @return The temporary node.
     */
    public MMObjectNode getNode() {
        return node;
    }
        
    /**
     * Key accessor.
     * @return The key used internally by TemporaryNodeManager and
     * TransactionManager.
     */
    public String getKey() {
       // This reproduces the key used by the TemporaryNodeManager.
        return uti.user.getName() + "_" + id;
    }
    
    /**
     * Id accessor.
     * @return The id specified by the user.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Tests if this object is an accessed object 
     * (e.g. representing an object that already exists
     * in the persistent cloud) or an input object.
     * @return true if this is an access object, false otherwise.
     */
    public boolean isAccessObject() {
        return getMMBaseId() != -1;
    }
    
    /**
     * MMBaseId accessor (for access objects).
     * @return The MMBase id, null if this is not an access object.
     */
    public int getMMBaseId() {
        return node.getIntValue(NUMBER);
    }
    
    /**
     * Tests if this object is a relation.
     * @return true if this is a relation, false otherwise.
     */
    public boolean isRelation() {
        return relationFlag;
    }
    
    /**
     * DisposeWhenNotReferenced accessor.
     * @return disposeWhenNotReferenced flag: true if this object is 
     *  to be dropped when it has no relations on commit, false otherwise.
     */
    public boolean getDisposeWhenNotReferenced() {
        return disposeWhenNotReferenced;
    }
    
    /**
     * Tests if this node is the source node of a relation.
     * @param tempRel Temporary relation node.
     * @return true if the relation node has this node as source.
     */
    public boolean isSourceOf(TmpObject tempRel) {
        MMObjectNode relation = tempRel.node;
        String sourceId = relation.getStringValue(_SNUMBER);
        if (!sourceId.equals("")) {
            // Source is temporary node.
            return getKey().equals(sourceId);
        }
        int sourceMmbaseId = relation.getIntValue(SNUMBER);
        if (sourceMmbaseId != -1) {
            // Source is persistent node.
            return isAccessObject() && getMMBaseId() == sourceMmbaseId;
        }
        return false;
    }
    
    /**
     * Tests if this node is the destination node of a relation.
     * @param tempRel Temporary relation node.
     * @return true if the relation node has this node as destination.
     */
    public boolean isDestinationOf(TmpObject tempRel) {
        MMObjectNode relation = tempRel.node;
        String destinationId = relation.getStringValue(_DNUMBER);
        if (!destinationId.equals("")) {
            // Destination is temporary node.
            return getKey().equals(destinationId);
        }
        int destinationMmbaseId = relation.getIntValue(DNUMBER);
        if (destinationMmbaseId != -1) {
            // Destination is persistent node.
            return isAccessObject() && getMMBaseId() == destinationMmbaseId;
        }
        return false;
    }

    /**
     * Sets source for relation in temporary cloud.
     * Requires this object to be a relation.
     * @param tmpObj The temporary object to set as source. */
    public void setSource(TmpObject tmpObj) {
        setField(_SNUMBER, tmpObj.getKey());
        setField(SNUMBER, Integer.toString(tmpObj.getMMBaseId()));
    }
    
    /**
     * Sets destination for relation in temporary cloud.
     * Requires this object to be a relation.
     * @param tmpObj The temporary object to set as destination. */
    public void setDestination(TmpObject tmpObj) {
        setField(_DNUMBER, tmpObj.getKey());
        setField(DNUMBER, Integer.toString(tmpObj.getMMBaseId()));
    }
    
    /**
     * Compares value of specified field of this and another object.
     * @param tmpObj The other object.
     * @param name The field name.
     * @return True if the fieldvalues of both objects are equal,
     *  false otherwise.
     */
    private boolean compareField(TmpObject tmpObj, String name) {
        Object value = getField(name);
        if (value != null) {
            return value.equals(tmpObj.getField(name));
        } else {
            return tmpObj.getField(name) == null;
        }
    }
    
    /**
     * ToString() method, displays most important fields.
     * @return String representation of this object.
     */
    public String toString() {
        return "TmpObject(id=\"" + id + "\", user=\"" + uti.user.getName()
        + "\", key=\"" + getKey() + "\", node={" + node 
        + "}, disposeWhenNotReferenced=" + disposeWhenNotReferenced + ")";
    }
    
    /**
     * Displays XML representation of this object, e.g. the XML code 
     * necessary to create this object in a transaction.
     * @return XML representation of this object.
     */
    public String toXML() {
        StringBuffer sb = new StringBuffer();
        if (isAccessObject()) {
            sb.append("<accessObject id=\"" + getId()
                + "\" mmbaseId=\"" + getMMBaseId() 
                + "\">\n");
        } else {
            sb.append("<createObject id=\"" + getId() 
                + "\" type=\"" + getNode().getName()
                + "\" disposeWhenNotReferenced=\"" + disposeWhenNotReferenced 
                + "\">\n");
        }
        Iterator i = node.getValues().entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            String value = entry.getValue().toString();
            if (!name.equals("otype") && !name.equals("owner")
                && !name.equals("number") && !name.equals("_number")) {
                    sb.append("<setField name=\"" + name + "\">"
                        + value + "</setField>\n");
            }
        }
        if (isAccessObject()) {
            sb.append("</accessObject>\n");
        } else {
            sb.append("</createObject>\n");
        }
        
        return sb.toString();
    }

}
