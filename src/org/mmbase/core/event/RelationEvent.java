/*
 * Created on 21-jun-2005
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import java.io.Serializable;

import org.mmbase.bridge.NotFoundException;
import org.mmbase.module.core.*;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.corebuilders.InsRel;

/**
 * This class reflects a change relation event. it contains information about
 * the kind of event (new, delete, change), and it contains a reference to the
 * appropriate typerel node, which allows you to find out on which relation from
 * which builder to which builder, the event occered. This is usefull for
 * caching optimization.<br/> A relation changed event is called the twoo nodes
 * that the relation links (or used to).
 * 
 * @author  Ernst Bunders
 * @since   MMBase-1.8
 * @version $Id: RelationEvent.java,v 1.6 2005-10-31 13:20:02 ernst Exp $
 */
public class RelationEvent extends NodeEvent implements Serializable {

    private String relationSourceNumber, relationDestinationNumber;

    private String relationSourceType, relationDestinationType;

    private String sourceRoleName, destinationRoleName;

    private int relationEventType;

    private int role;
    /**
     * @param nodeNumber valid node number
     * @param type event type
     * @param machineName valid machine name or null (than the local machine name is used)
     * @return new NodeEvent instance
     * @throws NotFoundException when node of given number can not be found
     */
    public static RelationEvent getRelationEventInstance(int nodeNumber, int type, String machineName)throws NotFoundException{
        MMObjectNode node = MMBase.getMMBase().getBuilder("object").getNode(nodeNumber);
        if( node == null) throw new NotFoundException("node with number " + nodeNumber + "was not found in cloud");
        if(machineName == null)machineName = MMBase.getMMBase().getMachineName();
        return new RelationEvent(node, type, machineName);
    }
    
    /**
     * @param node the relation node
     * @param eventType the type of relation event
     */
    public RelationEvent(MMObjectNode node, int eventType) {
        this(node, eventType, MMBase.getMMBase().getMachineName());
    }
    /**
     * @param node the changed relation node
     * @param eventType the type of change
     * @throws IllegalArgumentException if the node is not of type InsRel
     */
    public RelationEvent(MMObjectNode node, int eventType, String machineName) {
        super(node, NodeEvent.EVENT_TYPE_RELATION_CHANGED, machineName);
        if (!(node.getBuilder() instanceof InsRel)) {
            throw new IllegalArgumentException( "you can not create a relation changed event with this node");
        }

        relationEventType = eventType;
        eventType = NodeEvent.EVENT_TYPE_RELATION_CHANGED;
        MMObjectNode reldef = node.getNodeValue("rnumber");
        MMObjectBuilder relationSourceBuilder = node.getNodeValue("snumber").getBuilder();
        MMObjectBuilder relationDestinationBuilder = node.getNodeValue("dnumber").getBuilder();

        relationSourceNumber = node.getStringValue("snumber");
        relationDestinationNumber = node.getStringValue("dnumber");
        relationSourceType = relationSourceBuilder.getTableName();
        relationDestinationType = relationDestinationBuilder.getTableName();

        role = reldef.getNumber();
        sourceRoleName = reldef.getStringValue("sname");
        destinationRoleName = reldef.getStringValue("dname");
    }

    public String getName() {
        return "relation event";
    }

    /**
     * @return Returns the relationSourceType.
     */
    public String getRelationSourceType() {
        return relationSourceType;
    }

    /**
     * @return Returns the relationDestinationType.
     */
    public String getRelationDestinationType() {
        return relationDestinationType;
    }

    /**
     * @return Returns the relationSourceNumber.
     */
    public String getRelationSourceNumber() {
        return relationSourceNumber;
    }

    /**
     * @return Returns the relationDestinationNumber.
     */
    public String getRelationDestinationNumber() {
        return relationDestinationNumber;
    }

    public int getRelationEventType() {
        return relationEventType;
    }

    /**
     * 
     * @return rolename of changed relation
     * @throws UnsupportedOperationException if source and destination role
     *         names are not similar
     */
    public String getRoleName() throws UnsupportedOperationException {
        if (!getDestinationRoleName().equals(getSourceRoleName())) { throw new UnsupportedOperationException(
            "the source and destination rolenames are not the same. Please specify"); }
        return getDestinationRoleName();
    }

    /**
     * @return the role number
     */
    public int getRole() {
        return role;
    }

    public String getDestinationRoleName() {
        return destinationRoleName;
    }

    public String getSourceRoleName() {
        return sourceRoleName;
    }

    public String toString() {
        return super.toString() + ", relation eventtype: "
            + getEventTypeGuiName(relationEventType) + ", sourcetype: "
            + relationSourceType + ", destinationtype: "
            + relationDestinationType + ", source-node number: "
            + relationSourceNumber + ", destination-node number: "
            + relationDestinationNumber;
    }

}
