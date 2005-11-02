/*
 * Created on 2-nov-2005
 *
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import org.mmbase.bridge.Node;
import org.mmbase.bridge.Relation;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;

/**
 * @author Ernst Bunders
 *this is a utility class to create node event and relation event instances. the reason for it is 
 *that we want to references to core classes in the NodeEvent and RelationEvent classes, to keep them bridge-friendly,
 *but we need a little help for easy instantiation.
 */
public class NodeEventHelper {
    
    public static NodeEvent createNodeEventInstance(Node node, int eventType, String machineName){
        if(machineName == null)machineName = MMBase.getMMBase().getMachineName();
        MMObjectNode coreNode = MMBase.getMMBase().getBuilder(node.getNodeManager().getName()).getNode(node.getNumber());
        return createNodeEventInstance(coreNode, eventType, machineName);
    }

    /**
     * create an NodeEvent instance with an MMObjectNode
     * @param node 
     * @param eventType 
     * @param machineName or null to create a node event with local machine name
     * @return new instance of NodeEvent
     */
    public static NodeEvent createNodeEventInstance(MMObjectNode node, int eventType, String machineName){
        if(machineName == null)machineName = MMBase.getMMBase().getMachineName();
        return new NodeEvent(machineName, node.getBuilder().getTableName(), node.getNumber(), node.getOldValues(), node.getValues(), eventType);
    }
    
    public static RelationEvent createRelationEventInstance(Relation node, int eventType, String machineName){
        MMObjectNode coreNode = MMBase.getMMBase().getBuilder(node.getNodeManager().getName()).getNode(node.getNumber());
        return createRelationEventInstance(coreNode, eventType, machineName);
    }
    
    /**
     * create an RelationEvent instnce with an MMObjectNode (builder should be specialization of insrel)
     * @param node
     * @param eventType
     * @param machineName
     * @return a new RelationEvetn instance
     * @throws IllegalArgumentException when given node's builder is not a specialization of insrel
     */
    public static RelationEvent createRelationEventInstance(MMObjectNode node, int eventType, String machineName){
        if (!(node.getBuilder() instanceof InsRel)) {
            throw new IllegalArgumentException( "you can not create a relation changed event with this node");
        }
        if(machineName == null)machineName = MMBase.getMMBase().getMachineName();
        String builderName = node.getBuilder().getTableName();
        MMObjectNode reldef = node.getNodeValue("rnumber");
        MMObjectBuilder relationSourceBuilder = node.getNodeValue("snumber").getBuilder();
        MMObjectBuilder relationDestinationBuilder = node.getNodeValue("dnumber").getBuilder();

        int relationSourceNumber = node.getIntValue("snumber");
        int relationDestinationNumber = node.getIntValue("dnumber");
        String relationSourceType = relationSourceBuilder.getTableName();
        String relationDestinationType = relationDestinationBuilder.getTableName();

        int role = reldef.getNumber();
        return new RelationEvent(machineName, builderName, node.getNumber(), node.getOldValues(), node.getValues(), eventType, relationSourceNumber, relationDestinationNumber, relationSourceType, relationDestinationType, role);
    }
}
