/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.test.bridge;

import junit.framework.*;
import org.mmbase.bridge.*;

public class CloudTest extends TestCase {
    Cloud cloud;
    Node aaNode1;
    Node aaNode2;
    Node bbNode;
    Node[] bbNodes;
    int nrOfBBNodes;

    public CloudTest(String name) {
        super(name);
    }

    public void setUp() {
        // Create a test node.
        cloud = LocalContext.getCloudContext().getCloud("mmbase");
        aaNode1 = cloud.getNodeManager("aa").createNode();
        aaNode1.setStringValue("stringfield", "startnode1");
        aaNode1.commit();
        aaNode2 = cloud.getNodeManager("aa").createNode();
        aaNode2.setStringValue("stringfield", "startnode2");
        aaNode2.commit();
        bbNode = cloud.getNodeManager("bb").createNode();
        bbNode.setStringValue("stringfield", "bbNode");
        bbNode.commit();
        RelationManager relationManager;
        relationManager = cloud.getRelationManager("aa", "bb", "related");
        Relation relation;
        relation = relationManager.createRelation(aaNode2, bbNode);
        relation.commit();
        bbNodes = new Node[11];
        nrOfBBNodes = 0;
        for (int i = -5; i < 6; i++) {
            String s = new Integer(i).toString();
            Node node;
            node = cloud.getNodeManager("bb").createNode();
            node.setByteValue("bytefield", s.getBytes());
            node.setDoubleValue("doublefield", i);
            node.setFloatValue("floatfield", i);
            node.setIntValue("intfield", i);
            node.setLongValue("longfield", i);
            node.setStringValue("stringfield", s);
            node.commit();
            bbNodes[nrOfBBNodes] = node;
            relation = relationManager.createRelation(aaNode1, node);
            relation.commit();
            nrOfBBNodes++;
        }
    }

    public void tearDown() {
        // Remove test nodes.
        aaNode1.remove();
        aaNode2.remove();
        bbNode.remove();
        for (int i = 0; i < nrOfBBNodes; i++) {
            bbNodes[i].remove();
        }
    }

    public void testGetList() {
        NodeList nodeList;
        nodeList = cloud.getList("" + aaNode1.getNumber(), "aa,bb", "bytefield",
                                 "", "", "", false);
        assert(nodeList.size() == nrOfBBNodes);
    }

    public void testGetListWithNullParameters() {
        try {
            cloud.getList(null, null, null, null, null, null, false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithEmptyParameters() {
        try {
            cloud.getList("", "", "", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithNullParameterStartNodes() {
        NodeList nodeList;
        nodeList = cloud.getList(null, "aa,bb", "bytefield", "", "", "", false);
        assert(nodeList.size() == nrOfBBNodes + 1);
    }

    public void testGetListWithEmptyParameterStartNodes() {
        NodeList nodeList;
        nodeList = cloud.getList("", "aa,bb", "bytefield", "", "", "", false);
        assert(nodeList.size() == nrOfBBNodes + 1);
    }

    public void testGetListWithInvalidParameterStartNodes() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList("" + bbNode.getNumber(), "aa,bb",
                                     "bytefield", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithNullParameterNodePath() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, null, "bytefield", "", "", "",
                                     false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithEmptyParameterNodePath() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, "", "bytefield", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithInvalidParameterNodePath() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, "x", "bytefield", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithNullParameterFields() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, "aa,bb", null, "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithEmptyParameterFields() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, "aa,bb", "", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    public void testGetListWithInvalidParameterFields() {
        try {
            NodeList nodeList;
            nodeList = cloud.getList(null, "aa,bb", "x", "", "", "", false);
            fail("Should raise a BridgeException");
        } catch(BridgeException e) {
        }
    }

    // Add some more list test.

}
