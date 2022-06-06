/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.tests;
import junit.framework.TestCase;
import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.storage.search.*;
import org.mmbase.util.Casting;
import org.w3c.dom.Document;
/**
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.8
 */
public class Clustering extends BridgeTest {

    protected static Cloud cloud1;
    protected static Cloud cloud2;
    protected static NodeList aa2list;
    protected static NodeList bb2related;
    protected static Node     nodea1;
    public void setUp() {
        if (cloud1 == null) {
            cloud1 =   getRemoteCloud("rmi://127.0.0.1:1111/remotecontext_test1");
            cloud2 =   getRemoteCloud("rmi://127.0.0.1:1111/remotecontext_test2");

            NodeManager aa2 = cloud2.getNodeManager("aa");
            NodeManager aa1 = cloud1.getNodeManager("aa");
            NodeManager bb2 = cloud2.getNodeManager("bb");
            aa2list = aa2.getList(null, null, null); // cache list result
            aa1.getList(null, null, null); // cache list result
            nodea1 = aa1.createNode();
            nodea1.commit();
            NodeQuery nq = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), bb2, "related", "both");
            bb2related = bb2.getList(nq);


            NodeManager object2 = cloud2.getNodeManager("object");
            NodeQuery nq2 = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), object2, null, null);
            object2.getList(nq2); // just to put it in some cache or so..
        }
    }


    /**
     * It's no hard requirement that changes are visible immediately on the other side. So, sometimes wait a bit, to be
     * on the safe side.
     */
    protected void allowLatency() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }
    }

    protected void fieldEquals(Node n1, Node n2) {
        for (Field f : n1.getNodeManager().getFields(NodeManager.ORDER_CREATE)) {
            if (f.getName().equals("number")) {
                // sigh, 'number' is a NODE field...
                assertEquals(n1.getNumber(), n2.getNumber());
            } else if (f.getName().equals("otype")) {
                // 'otype' is a NODE field too
                assertEquals(n1.getNumber(), n2.getNumber());
            } else {
                Object value1 = n1.getValue(f.getName());
                Object value2 = n2.getValue(f.getName());
                assertTrue("" +
                           (value1 == null ? "" : value1.getClass().getName()) + value1 +
                           " != " +
                           (value2 == null ? "" : value2.getClass().getName()) + value2 +
                           " (value of " + n1.getNumber() + "/" + f.getName() + ")", value1 == null ? value2 == null : value1.equals(value2));
            }
        }
    }

    public void testCreateNode() {
        // a node was created in cloud1. It must be available in cloud2.
        assertTrue(cloud2.hasNode(nodea1.getNumber()));
        Node nodea2 = cloud2.getNode(nodea1.getNumber());
        // and also be the same
        fieldEquals(nodea1, nodea2);
    }

    public void testList() {
        NodeList aa2List1 = cloud1.getNodeManager("aa").getList(null, null, null); // a node of type aa was created, so list must now be size 1.
        assertEquals(aa2List1.size(), 1);

        NodeManager aa2 = cloud2.getNodeManager("aa");
        NodeList aa2list2 = aa2.getList(null, null, null); // should not give error, but may be size 0, because still in cache

        assertTrue(aa2list2.size() == 0 || aa2list2.size() == 1);

        allowLatency();

        aa2list2 = aa2.getList(null, null, null); // should also be size 1 now!
        assertTrue("List before create was not size 0", aa2list.size() == 0); // was determined before the createNode, should still be 0.

        assertTrue("Check wether node-list got invalidated failed " +  aa2list2.size()  + " != " + aa2list.size() + " + 1", aa2list2.size() == aa2list.size() + 1);
    }

    private Node getRole(Cloud cl, String name) {
        NodeManager reldef = cl.getNodeManager("reldef");
        NodeQuery nq = reldef.createQuery();
        Constraint c = Queries.createConstraint(nq, "sname", Queries.getOperator("="), name);
        nq.setConstraint(c);
        NodeList roles = reldef.getList(nq);
        if (roles.size() == 0) throw new RuntimeException("Role '" + name + "' not found in " + (cl == cloud2 ? " cloud 2 " : " cloud 1"));
        if (roles.size() > 1) {
            throw new RuntimeException("More roles '" + name + "' not found in " + (cl == cloud2 ? " cloud 2 " : " cloud 1"));
        }
        return roles.getNode(0);
    }

    /**
     * @todo Perhaps these kind of functions would come in handy in a org.mmbase.bridge.util.System
     * utitility class or so
     */
    private RelationManager createTypeRel(Cloud cl, String role, String nm1, String nm2) {
        NodeManager typerel = cl.getNodeManager("typerel");

        Node newTypeRel = typerel.createNode();
        newTypeRel.setValue("rnumber", getRole(cl, role));
        newTypeRel.setValue("snumber", cl.getNodeManager(nm1));
        newTypeRel.setValue("dnumber", cl.getNodeManager(nm2));
        newTypeRel.commit();
        return (RelationManager) newTypeRel;
    }


    // 	 MMB-1128
    public void testCreateTypeRel() {
        // create new typerel, and see if that has influence on cloud 2.
        RelationManager rm = createTypeRel(cloud1, "posrel", "bb", "aa");
        NodeManager bb1 = cloud1.getNodeManager("bb");
        NodeManager bb2 = cloud2.getNodeManager("bb");

        {
            // query for local cloud
            NodeQuery nq1 = Queries.createRelatedNodesQuery(cloud1.getNode(nodea1.getNumber()), bb1, null, "both");
            NodeList related1 = bb1.getList(nq1);
            assertTrue("Locale list is null!", related1 != null);
            assertTrue("Locale list is not empty.",related1.size() == 0);
        }

        {
            // query for remote cloud
            NodeQuery nq2 = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), bb2, null, "both");
            NodeList related2 = bb2.getList(nq2);
            assertTrue("Remote list is null!", related2 != null);
            assertTrue("Locale list is not empty.", related2.size() == 0);
        }

        Relation r1;
        {
            // create node and relation
            Node bbnode1 = bb1.createNode();
            bbnode1.commit();
            r1 = bbnode1.createRelation(nodea1, rm);
            r1.commit();
        }
        {
            // re-query local cloud
            NodeQuery nq1 = Queries.createRelatedNodesQuery(cloud1.getNode(nodea1.getNumber()), bb1, null, "both");
            NodeList related1 = bb1.getList(nq1);
            assertTrue("Created a relation, but local related list size is not 1, but " + related1.size() +
                       ".\nUsed query: " + nq1, related1.size() == 1); // local
        }
        {


            // re-query remote cloud
            allowLatency();
            NodeQuery nq2 = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), bb2, null, "both");
            NodeList related2 = bb2.getList(nq2);
            assertTrue("Created a relation, but remote related list size is not 1, but " + related2.size() +
                       ".\nUsed query: " + nq2, related2.size() == 1); // remote
        }

        // drop the relation again
        r1.delete();

        {
            // re-query local cloud
            NodeQuery nq1 = Queries.createRelatedNodesQuery(cloud1.getNode(nodea1.getNumber()), bb1, null, "both");
            NodeList related1 = bb1.getList(nq1);
            assertTrue("Deleted a relation, but local related list size is not 0, but " + related1.size() +
                       ".\nUsed query: " + nq1, related1.size() == 0); // local
        }
        {
            // re-query remote cloud
            allowLatency();
            NodeQuery nq2 = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), bb2, null, "both");
            NodeList related2 = bb2.getList(nq2);
            assertTrue("Deleted a relation, but remote related list size is not 0, but " + related2.size() +
                       ".\nUsed query: " + nq2, related2.size() == 0); // remote
        }
    }

    public void testInstallBuilder() {

        // create a builder only in one of the clouds (1).
        NodeManager typedef = cloud1.getNodeManager("typedef");
        Node zz = typedef.createNode();
        zz.setStringValue("name", "zz");
        Document builderXML = Casting.toXML("<?xml version='1.0' encoding='UTF-8'?>\n" +
                                            "<!DOCTYPE builder PUBLIC \"-//MMBase//builder config 1.1//EN\" \"https://www.mmbase.org/dtd/builder_1_1.dtd\">\n" +
                                            "<builder extends='object' maintainer='mmbase.org' name='zz' version='0'></builder>");
        System.out.println("Using builder XML " + org.mmbase.util.xml.XMLWriter.write(builderXML, true));
        zz.setXMLValue("config", builderXML);
        zz.commit();

        NodeManager zz1 = cloud1.getNodeManager("zz");
        assertTrue(cloud1.hasNodeManager("zz"));
        assertTrue(zz1 != null);
        if (cloud2.hasNodeManager("zz")) {
            throw new RuntimeException("Odd, the 'zz' node-manager should only appear in cloud 1 now!, appears in cloud 2 too. Something wrong with test-case, or this issue was solved, and these test-cases need review then.");
        }

    }

    public void testCreateNodeUnknownType() {
        // creating a node of this new type, and look what happens in the cloud which does not know this type.
        Node zNode = cloud1.getNodeManager("zz").createNode();
        zNode.commit();
        // allowLatency(); no need, this is a _new_ node! Must result in database action any way, it should be in database!

        if (cloud2.hasNode(zNode.getNumber())) {
            Node zNode2 = cloud2.getNode(zNode.getNumber());
            assertTrue("A node of unknown type falls back to 'object'", zNode2.getNodeManager().getName().equals("object"));
        } else {
            fail("New node " + zNode + " (of unknown type in cloud 2) is not contained by cloud 2");
        }

    }

    public void testCreateTypeRelUnknownType() {
        NodeManager zz = cloud1.getNodeManager("zz");
        // now create a typerel from aa to zz (which in cloud2 will look quite odd).

        RelationManager rm = createTypeRel(cloud1, "related", "aa", "zz");

        Node zNode1 = zz.createNode(); zNode1.commit();
        Node zNode2 = zz.createNode(); zNode2.commit();
        Relation r1 = nodea1.createRelation(zNode1, rm); r1.commit();
        Relation r2 = nodea1.createRelation(zNode2, rm); r2.commit();

        assertEquals(1, zNode1.getRelatedNodes().size());
        assertEquals(1, zNode1.getRelations().size());

        assertTrue(cloud2.hasNode(nodea1.getNumber()));

        allowLatency();

        assertTrue(cloud2.hasNode(nodea1.getNumber()));

        // now ask the number of related nodes from nodea1;
        Node nodea2 = cloud2.getNode(nodea1.getNumber());

        List related1 = nodea1.getRelatedNodes();
        List related2 = nodea2.getRelatedNodes();
        assertTrue("relatednodes " + related1 + " != " + related2, related1.size() == related2.size());

        assertEquals(1, cloud1.getNode(zNode1.getNumber()).countRelatedNodes("object"));
        assertEquals(1, cloud1.getNode(zNode1.getNumber()).getRelatedNodes().size());

        assertEquals(1, cloud2.getNode(zNode1.getNumber()).countRelatedNodes("object"));

        NodeList relatedNodes2 = cloud2.getNode(zNode1.getNumber()).getRelatedNodes();
        assertEquals(1, relatedNodes2.size());

        RelationList relationNodes2 = cloud2.getNode(zNode1.getNumber()).getRelations();
        assertEquals(1, relationNodes2.size());

    }

    public void testRelatedUnknownType() {
        // in cloud 1 should be ok
        List related1 = nodea1.getRelatedNodes("zz");
        assertTrue("Size: " + related1.size() + " != 2", related1.size() == 2); // create 2 related nodes...

        // but in cloud 2...
        Node nodea2 = cloud2.getNode(nodea1.getNumber());
        try {
            List related2 = nodea2.getRelatedNodes("zz"); //
            fail("zz is unknown in cloud 2!, but could still find nodes of this type: " + related2);
        } catch (org.mmbase.bridge.NotFoundException nfe) {
        }

    }

    public void testRelatedUnknownType2() {

        NodeManager object1 = cloud1.getNodeManager("object");
        NodeManager object2 = cloud2.getNodeManager("object");
        // this version does not depend on MMobjectNode#getRelatedNodes
        NodeQuery nq1 = Queries.createRelatedNodesQuery(nodea1, object1, null, null);
        NodeQuery nq2 = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), object2, null, null);
        List related1 = object1.getList(nq1);
        List related2 = object2.getList(nq2);
        // nr of relations reported = 2 (0 x a -> bb, 2 x a -> zz).
        assertTrue("Nr of local relations reported is not 2 but " + related1.size(), related1.size() == 2);
        assertTrue("Nr of remote relations reported is not " + related1.size() + " but " + related2.size(), related1.size() == related2.size());
    }

    public void testCreateRelDef() {
        // create new reldef in cloud1, and see what happens in cloud 2.
        NodeManager reldef = cloud1.getNodeManager("reldef");

        Node newRole = reldef.createNode();
        newRole.setValue("sname", "newrole");
        newRole.setValue("builder", cloud1.getNodeManager("insrel"));
        newRole.commit();

        RelationManager rm = createTypeRel(cloud1, "newrole", "aa", "bb");

        NodeManager bb = cloud1.getNodeManager("bb");
        Node b1 = bb.createNode();
        b1.commit();

        Relation r = nodea1.createRelation(b1, rm); r.commit();

        assertTrue(nodea1.getRelatedNodes("bb", "newrole", null).size() == 1);

        allowLatency(); // give the new role some time to cast to other mmbase.
        try {
            assertTrue(cloud2.getNode(nodea1.getNumber()).getRelatedNodes("bb", "newrole", null).size() == 1);
        } catch (Exception e) {
            // its stacktrace is a bit long...
            fail(e.getMessage());
        }
    }



    public void testShutDown() {
        cloud1.shutdown();
        cloud2.shutdown();
    }

}

