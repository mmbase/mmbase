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
            cloud1 =   getRemoteCloud("rmi://127.0.0.1:1221/remotecontext");
            cloud2 =   getRemoteCloud("rmi://127.0.0.1:1222/remotecontext");
            
            NodeManager aa2 = cloud2.getNodeManager("aa");
            NodeManager aa1 = cloud1.getNodeManager("aa");
            NodeManager bb2 = cloud2.getNodeManager("bb");
            aa2list = aa2.getList(null, null, null); // cache list result
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
     * It's no hard requirment that changes are visibility immediately on the other side. So, sometime wait a bit, to be on the safe side.
     */
    protected void allowLatency() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }

    public void fieldEquals(Node n1, Node n2) {
        FieldIterator fi = n1.getNodeManager().getFields(NodeManager.ORDER_CREATE).fieldIterator();
        while (fi.hasNext()) {
            Field f = fi.nextField();
            if (f.getName().equals("number")) {
                // sigh, 'number' is a NODE field...
                assertTrue(n1.getNumber() == n2.getNumber());
            } else {
                Object value1 = n1.getValue(f.getName());
                Object value2 = n2.getValue(f.getName());
                assertTrue("" + value1 + " != " + value2 + " (value of " + n1.getNumber() + "/" + f.getName() + ")", value1 == null ? value2 == null : value1.equals(value2));
            }
        }
    }

    public void testCreateNode() {
        Node nodea2 = cloud2.getNode(nodea1.getNumber());
        fieldEquals(nodea1, nodea2);
    }

    public void testList() {
        NodeManager aa2 = cloud2.getNodeManager("aa");
        NodeList aa2list2 = aa2.getList(null, null, null); // should not give error
        allowLatency();
        aa2list2 = aa2.getList(null, null, null);
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

    public void testCreateTypeRel() {
        // create new typerel, and see if that has influence on cloud 2.

        RelationManager rm = createTypeRel(cloud1, "posrel", "bb", "aa");

        NodeManager bb2 = cloud2.getNodeManager("bb");
        NodeQuery nq = Queries.createRelatedNodesQuery(cloud2.getNode(nodea1.getNumber()), bb2, null, "both");
        NodeList related2 = bb2.getList(nq);
        assertTrue("list is null!", related2 != null);
        assertTrue(related2.size() == 0);
        assertTrue(bb2related.size() == 0);

        Node bb1 = cloud1.getNodeManager("bb").createNode();
        bb1.commit();

        Relation r1 = bb1.createRelation(nodea1, rm);
        r1.commit();

        related2 = bb2.getList(nq);
        assertTrue("Just created a relation, but related list size is not 1, but " + related2.size(), related2.size() == 1);


    }

    public void testInstallBuilder() {

        // create a builder only in of the clouds (1).
        NodeManager typedef = cloud1.getNodeManager("typedef");
        Node zz = typedef.createNode();
        zz.setStringValue("name", "zz");
        Document builderXML = Casting.toXML("<?xml version='1.0' encoding='UTF-8'?>\n" +
                                            "<!DOCTYPE builder PUBLIC \"-//MMBase//builder config 1.1//EN\" \"http://www.mmbase.org/dtd/builder_1_1.dtd\">\n" +
                                            "<builder extends='object' maintainer='mmbase.org' name='zz' version='0'></builder>");
        System.out.println("Using builder XML " + org.mmbase.util.xml.XMLWriter.write(builderXML, true));
        zz.setXMLValue("config", builderXML);
        zz.commit();

        NodeManager zz1 = cloud1.getNodeManager("zz");
        assertTrue(cloud1.hasNodeManager("zz"));
        assertTrue(zz1 != null);
        if (cloud2.hasNodeManager("zz")) {
            throw new RuntimeException("Odd, the 'zz' node-manager should only appear in cloud 1 now!, appears in cloud 2 too. Something wrong with test-case, of this issue was solved, and these test-cases need review then.");
        }

    }

    public void testCreateNodeUnknownType() {
        // creating a node of this new type, and look what happens in the cloud which does not know
        // this type.
        Node zNode = cloud1.getNodeManager("zz").createNode();
        zNode.commit();
        Node zNode2 = cloud2.getNode(zNode.getNumber());
        assertTrue("A node of unknown type falls back to 'object'", zNode2.getNodeManager().getName().equals("object"));

    }

    public void testCreateTypeRelUnknownType() {
        NodeManager zz = cloud1.getNodeManager("zz");
        // now create a typerel from aa to zz (which in cloud2 will look quite odd).

        RelationManager rm = createTypeRel(cloud1, "related", "aa", "zz");

        Node zNode1 = zz.createNode(); zNode1.commit();
        Node zNode2 = zz.createNode(); zNode2.commit();
        Relation r1 = nodea1.createRelation(zNode1, rm); r1.commit();
        Relation r2 = nodea1.createRelation(zNode2, rm); r2.commit();

        // now ask the number of related nodes from nodea1;
        Node nodea2 = cloud2.getNode(nodea1.getNumber());

        List related1 = nodea1.getRelatedNodes();
        List related2 = nodea2.getRelatedNodes();
        assertTrue("relatednodes " + related1 + " != " + related2, related1.size() == related2.size());

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
        assertTrue("Size: " + related1.size() + " != 3", related1.size() == 3); // 1 to a bb, 2 to zz's.
        assertTrue("Size: " + related1.size() + " != " + related2.size(), related1.size() == related2.size());
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
        try {
            assertTrue(cloud2.getNode(nodea1.getNumber()).getRelatedNodes("bb", "newrole", null).size() == 1);
        } catch (Exception e) {
            // its stacktrace is a bit long...
            fail(e.getMessage());
        }
    }
}

