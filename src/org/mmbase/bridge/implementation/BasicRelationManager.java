/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;
import org.mmbase.security.*;
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;

/**
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 */
public class BasicRelationManager extends BasicNodeManager implements RelationManager {

    private MMObjectNode typeRelNode = null;
    private MMObjectNode relDefNode = null;
    private int snum = 0;
    private int dnum = 0;
    int roleID  = 0;

    /**
     * Creates a new instance of Relation manager.
     * The type of manager (a strictly constrained manager or a reole manager)
     * is dependend on type the passed node (from eitehr the reldef of typerel
     * builder).
     * @param node the node on whcih to base the relation manager
     * @param cloud the cloud for which to create the manager
     */
    BasicRelationManager(MMObjectNode node, Cloud cloud) {
        RelDef reldef = ((BasicCloudContext)cloud.getCloudContext()).mmb.getRelDef();
        if (node.parent==reldef) {
            relDefNode = node;
            roleID=node.getNumber();
        } else {
            typeRelNode = node;
            snum=node.getIntValue("snumber");
            dnum=node.getIntValue("dnumber");
            roleID=node.getIntValue("rnumber");
            relDefNode= reldef.getNode(roleID);
        }
        builder=reldef.getBuilder(relDefNode);
        init(builder,cloud);
      }

    public Node createNode() {
        Node relation = super.createNode();
        relation.setIntValue("rnumber",roleID);
        return relation;
    }

    public String getForwardRole() {
        return relDefNode.getStringValue("sname");
    }

    public String getReciprocalRole() {
        return relDefNode.getStringValue("dname");
    }

    public int getDirectionality() {
        return relDefNode.getIntValue("dir");
    }

    public NodeManager getSourceManager() {
        if (typeRelNode==null) {
          throw new BasicBridgeException("This relationmanager does not contain source information.");
        }
        int nr=typeRelNode.getIntValue("snumber");
        return cloud.getNodeManager(nr);
    }

    public NodeManager getDestinationManager() {
        if (typeRelNode==null) {
          throw new BasicBridgeException("This relationmanager does not contain destination information.");
        }
        int nr=typeRelNode.getIntValue("dnumber");
        return cloud.getNodeManager(nr);
    }

    public Relation createRelation(Node sourceNode, Node destinationNode) {
        //
        // checks whether all components are part of the same cloud/transaction
        // maybe should be made more flexible?
        //
        if (sourceNode.getCloud() != cloud) {
            throw new BasicBridgeException("Relationmanager and source node are not in the same transaction or in different clouds");
        }
        if (destinationNode.getCloud() != cloud) {
            throw new BasicBridgeException("Relationmanager type and destination node are not in the same transaction or in different clouds");
        }
        if (!(cloud instanceof Transaction)  &&
             (((BasicNode)sourceNode).isNew() || ((BasicNode)destinationNode).isNew())) {
            throw new BasicBridgeException("Cannot add a relation to a new node that has not been committed.");
        }

       BasicRelation relation = (BasicRelation)createNode();
       relation.setSource(sourceNode);
       relation.setDestination(destinationNode);
       relation.checkValid();
//       relation.commit();
       return relation;
    }

    /**
     * Compares two relationmanagers, and returns true if they are equal.
     * This effectively means that both objects are relationmanagers, and they both use to the same builder type
     * @param o the object to compare it with
     */
    public boolean equals(Object o) {
        return (o instanceof RelationManager) && (o.hashCode()==hashCode());
    }

    /**
     * Returns the relationmanager's hashCode.
     * This effectively returns the object number of the typerel record
     */
    public int hashCode() {
        if (typeRelNode==null) {
          return relDefNode.getNumber();
        }
        return typeRelNode.getNumber();
    }
}
