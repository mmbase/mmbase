/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

import java.util.*;
import org.mmbase.util.functions.*;
import org.mmbase.tests.*;
import org.mmbase.bridge.util.CollectionNodeList;

/**
 *
 * @author Simon Groenewolt (simon@submarine.nl)
 * @author Michiel Meeuwissen
 * @since $Id: FunctionsTest.java,v 1.13 2008-07-17 17:22:43 michiel Exp $
 * @since MMBase-1.8
 */
public class FunctionsTest extends BridgeTest {


    public FunctionsTest(String name) {
        super(name);
    }

    public void testPatternFunction() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node = nm.createNode();
        node.commit();
        assertTrue(node.getFunctionValue("test", null).toString().equals("[" + node.getNumber() + "]"));
    }

    public void testNodeManagerFunction() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Function function = nm.getFunction("aaa");
        Parameters params = function.createParameters();
        params.set("parameter2", new Integer(5));
        Object result = function.getFunctionValue(params);
        assertTrue("No instance of Integer but " + result.getClass(), result instanceof Integer);
        Integer i = (Integer) result;
        assertTrue(i.intValue() == 15);
        // can also be called on a node.
        Node node = nm.createNode();
        node.commit();
        assertTrue(node.getFunctionValue("aaa", params).toInt() == 15);
    }

    public void testNodeManagerFunction2() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("news");
        Node newsNode = nm.createNode();
        newsNode.commit();
        Function function = nm.getFunction("latest");
        Parameters params = function.createParameters();
        // remote clouds are not serializable. It is not needed anyway. Cloud parameters are
        // implicit, when using bridge.
        // params.set(Parameter.CLOUD, cloud);
        params.set("max", new Integer(1));
        NodeList nl = (NodeList) function.getFunctionValue(params);
        assertTrue(nl.getNode(0).getNumber() == newsNode.getNumber());
    }

    public void testNodeManagerFunction3() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("mmservers");
        Function function = nm.getFunction("uptime");
        Parameters params = function.createParameters();
        Long value = (Long) function.getFunctionValue(params);
        assertTrue(value.longValue() >= 0);
    }

    public void testNodeFunctionWithNodeResult() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node1 = nm.createNode();
        node1.commit();
        Node node2 = nm.createNode();
        node2.commit();
        Node successorOfNode1 = node1.getFunctionValue("successor", null).toNode();
        assertTrue(successorOfNode1.equals(node2));

    }

    public void testNodeFunctionWithNodeResult1() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node1 = nm.createNode();
        node1.commit();
        Function function = node1.getFunction("nodeFunction1");
        Parameters params = function.createParameters();
        params.set("parameter1", "hoi");
        Node n = node1.getFunctionValue("nodeFunction1", params).toNode();
        assertTrue(n.getStringValue("bloe").equals("hoi"));
        n = (Node) function.getFunctionValue(params);
        assertTrue(n.getStringValue("bloe").equals("hoi"));
    }

    public void testNodeFunctionWithNodeResult2() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node1 = nm.createNode();
        node1.commit();
        Function function = node1.getFunction("nodeFunction2");
        Parameters params = function.createParameters();
        params.set("parameter1", "hoi");
        Node n = node1.getFunctionValue("nodeFunction2", params).toNode();
        assertTrue(n.getStringValue("bloe").equals("hoi"));
        n = (Node) function.getFunctionValue(params);
        assertTrue(n.getStringValue("bloe").equals("hoi"));
    }

    public void testNodeFunctionWithNodeListResult() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node1 = nm.createNode();
        node1.commit();
        Function function = node1.getFunction("nodeListFunction");
        Parameters params = function.createParameters();
        params.set("parameter1", "hoi");
        NodeList nl = new CollectionNodeList((Collection) node1.getFunctionValue("nodeListFunction", params).get(), cloud);
        NodeIterator i = nl.nodeIterator();
        while (i.hasNext()) {
            Node n =  i.nextNode();
            assertTrue(n.getStringValue("bloe").equals("hoi"));
        }
        nl = new CollectionNodeList((Collection) function.getFunctionValue(params), cloud);
        i = nl.nodeIterator();
        while (i.hasNext()) {
            Node n =  i.nextNode();
            assertTrue(n.getStringValue("bloe").equals("hoi"));
        }
    }
    public void testNodeFunctionWithNodeListResult1() {
        Cloud cloud = getCloud();
        NodeManager nm = cloud.getNodeManager("datatypes");
        Node node1 = nm.createNode();
        node1.commit();
        Function function = node1.getFunction("nodeListFunction1");
        Parameters params = function.createParameters();
        params.set("parameter1", "hoi");
        NodeList nl = (NodeList) node1.getFunctionValue("nodeListFunction1", params).get();
        NodeIterator i = nl.nodeIterator();
        while (i.hasNext()) {
            Node n = i.nextNode();
            assertTrue("" + nl + " contains nulls", n != null);
            assertTrue(n.getStringValue("bloe").equals("hoi"));
        }
        nl = (NodeList) function.getFunctionValue(params);
        i = nl.nodeIterator();
        while (i.hasNext()) {
            Node n = i.nextNode();
            assertTrue(n.getStringValue("bloe").equals("hoi"));
        }
    }

    /**
     * test a variety of functionset possibilities
     * XXX really not complete yet
     */
    public void testFunctionSets() {
        if (getCloudContext().getUri().equals(ContextProvider.DEFAULT_CLOUD_CONTEXT_NAME)) {
            Function testfunc1 = FunctionSets.getFunction("utils", "randomLong");
            assertTrue("function 'randomLong' not found (functionset 'utils')", testfunc1 != null);
            // long randomLong = testfunc1.getFunctionValue(null);
            Function testfunc2 = FunctionSets.getFunction("testfunctions", "testBoolean");
            assertTrue("function 'testBoolean' not found (functionset 'testfunctions')", testfunc2 != null);

            Parameters params2 = testfunc2.createParameters();
            params2.set("inBoolean", Boolean.valueOf(true));


            Object result = testfunc2.getFunctionValue(params2);
            assertTrue("Expected return value of type 'Boolean', but got: '" + result + "'", result instanceof java.lang.Boolean);
            assertTrue("function 'testBoolean' didn't return true as was expected", ((Boolean) result).booleanValue());
        } else {
            System.out.println("Functionsets can only be used on local cloud");
        }
    }
}
