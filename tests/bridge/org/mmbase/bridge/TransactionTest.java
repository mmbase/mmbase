/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.mmbase.tests.*;
import org.mmbase.bridge.util.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Test class <code>Transaction</code> from the bridge package.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.8.6
  */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionTest extends BridgeTest {
    private static final Logger log = Logging.getLoggerInstance(TransactionTest.class);

    public TransactionTest(String name) {
        super(name);
    }

    static int seq = 0;
    int newNode;
    int newNode2;


    public void setUp() {
        seq++;
        // Create some test nodes
        Cloud cloud = getCloud();
        {
            Node node = cloud.getNodeManager("news").createNode();
            node.setStringValue("title", "foo");
            node.commit();
            newNode = node.getNumber();
        }
        {
            Node node = cloud.getNodeManager("news").createNode();
            node.setStringValue("title", "foo");
            node.createAlias("test.news." + seq);
            node.setContext("default");
            node.commit();
            newNode2 = node.getNumber();
        }
    }


    public void test001Cancel() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("cancel1");
        Node node = t.getNode(newNode);
        node.setStringValue("title", "xxxxx");
        node.commit();
        t.cancel();

        node = cloud.getNode(newNode);

        assertEquals("foo", node.getStringValue("title"));
    }


    public void test002Cancel2() {
        Cloud cloud = getCloud();
        {
            Transaction t = cloud.getTransaction("cancel2");
            Node node = t.getNode(newNode);
            node.setStringValue("title", "xxxxx");
            node.commit();
            t.cancel();
        }
        {
            Transaction t = cloud.getTransaction("cancel2");
            Node node = t.getNode(newNode);
            assertEquals("foo", node.getStringValue("title"));
            t.cancel();
        }
    }

    public void test003GetTransaction() {
        Cloud cloud = getCloud();

        {
            Transaction t = cloud.getTransaction("gettransactiontest");
            Node node = t.getNode(newNode);
            node.setStringValue("title", "xxxxx");
        }
        {
            Transaction t = cloud.getTransaction("gettransactiontest");
            Node node = t.getNode(newNode);
            assertEquals("xxxxx", node.getStringValue("title"));
            t.cancel();
        }
    }

    public void test004Commit() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar3");
        Node node = t.getNode(newNode);
        node.setStringValue("title", "yyyyy");
        node.commit();
        t.commit();

        node = cloud.getNode(newNode);

        assertEquals("yyyyy", node.getStringValue("title"));
    }

    public void test005MMB1546() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("test0");
        Node nt = t.getNode(newNode);
        nt.setValue("title", "bla");
        //t.cancel(); _DONT_ cancel
        Node nc = cloud.getNode(newNode);
        nc.setValue("title", "bloe");
        nc.commit();
        assertEquals("bloe", nc.getStringValue("title"));
        assertEquals("bloe", cloud.getNode(newNode).getStringValue("title"));
        t.cancel();
        assertEquals("bloe", cloud.getNode(newNode).getStringValue("title"));

    }


    //Test for http://www.mmbase.org/jira/browse/MMB-1621
    public void test006GetValue() {
        Cloud cloud = getCloud();

        String value = cloud.getNode(newNode).getStringValue("title");
        assertFalse("zzzzz".equals(value));

        Transaction t = cloud.getTransaction("bar4");
        System.out.println("Transaction now " + t);
        Node node = t.getNode(newNode);
        assertEquals(1, t.getNodes().size());
        node.setStringValue("title", "zzzzz");
        assertEquals("zzzzz", node.getStringValue("title"));
        node.commit(); // committing inside transaction


        assertEquals("zzzzz", node.getStringValue("title"));
        assertEquals(value, cloud.getNode(newNode).getStringValue("title"));

        t.commit();
        assertEquals("zzzzz", cloud.getNode(newNode).getStringValue("title"));

    }

    public void test007ReuseTransaction() {
        Cloud cloud = getCloud();
        {
            Node node = cloud.getNode(newNode);
            node.setStringValue("title", "zzyyxx");
            node.commit();
            assertEquals("zzyyxx", cloud.getNode(newNode).getStringValue("title"));
        }
        {
            Transaction t = cloud.getTransaction("bar4");
            assertEquals(0, t.getNodes().size());
            Node node = t.getNode(newNode);
            assertEquals("zzyyxx", node.getStringValue("title"));
            assertEquals(1, t.getNodes().size());
            node.setStringValue("title", "wwwwww");
            node.commit();
            assertEquals("wwwwww", node.getStringValue("title"));
            t.cancel();
            assertEquals("zzyyxx", cloud.getNode(newNode).getStringValue("title"));
        }
        assertEquals("zzyyxx", cloud.getNode(newNode).getStringValue("title"));

    }

    public void test008CancelDelete() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar5");
        Node node = t.getNode(newNode);
        node.delete();
        t.cancel();
        assertTrue(cloud.hasNode(newNode));

    }

    public void test009CommitDelete() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar6");
        Node node = t.getNode(newNode);
        node.delete();
        t.commit();
        assertFalse(cloud.hasNode(newNode));

    }

    public void test010SetContext() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar7");
        Node n = t.getNodeManager("news").createNode();

        assertEquals(1, t.getNodes().size());

        n.setContext("non_default");

        assertEquals(1, t.getNodes().size());

        assertEquals("non_default", n.getStringValue("owner"));
        assertEquals("non_default", n.getContext());

        assertEquals(1, t.getNodes().size());

        t.commit();

        Node n2 = cloud.getNode(n.getNumber());
        assertEquals("non_default", n2.getStringValue("owner"));
        assertEquals("non_default", n2.getContext());

    }

    public void test011SetContextSubTransaction() {
        Cloud cloud = getCloud();

        Transaction ot = cloud.getTransaction("bar8");
        Transaction t = ot.getTransaction("bar9");
        Node n = t.getNodeManager("news").createNode();
        n.setContext("non_default");
        assertEquals("non_default", n.getContext());
        t.commit();

        Node n2 = ot.getNode(n.getNumber());
        assertEquals("non_default", n2.getContext());

        ot.commit();
        Node n3 = cloud.getNode(n.getNumber());
        assertEquals("non_default", n3.getContext());
    }

    public void test012EditNodeOutsideTransaction() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar10");
        Node nodeInTransaction = t.getNode(newNode2);
        nodeInTransaction.setStringValue("title", "foo2");
        Node nodeOutTransaction = cloud.getNode(newNode2);
        nodeOutTransaction.setStringValue("title", "bar2");

        nodeOutTransaction.commit();
        t.commit();

        // transaction was committed _later_ so its commit of the node must have won
        assertEquals("foo2", cloud.getNode(newNode2).getStringValue("title"));
        assertEquals("foo2", nodeInTransaction.getStringValue("title"));
        //assertEquals("foo2", nodeOutTransaction.getStringValue("title")); // not sure what this should have done, but anyhow, it now fails

    }
    public void test013EditNodeOutsideTransaction2() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar10");
        Node nodeInTransaction = t.getNode(newNode2);
        nodeInTransaction.setStringValue("title", "foo2");
        Node nodeOutTransaction = cloud.getNode(newNode2);
        nodeOutTransaction.setStringValue("title", "bar2");

        t.commit();
        nodeOutTransaction.commit();

        // transaction was committed _earlier_ so the commit of the node must have won
        assertEquals("bar2", cloud.getNode(newNode2).getStringValue("title"));
        //assertEquals("bar2", nodeInTransaction.getStringValue("title"));// not sure what this should have done, but anyhow, it now fails
        assertEquals("bar2", nodeOutTransaction.getStringValue("title"));

    }

    public void test014DeleteNodeOutsideTransaction() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar11");
        Node nodeInTransaction = t.getNode(newNode2);
        nodeInTransaction.setStringValue("title", "foo2");
        {
            // now delete the node
            Node nodeOutTransaction = cloud.getNode(newNode2);
            nodeOutTransaction.delete();
            assertFalse(cloud.hasNode(newNode2));
        }

        try {
            t.commit();
        } catch (Exception e) {
            // should not give exception. MMB-1680
            log.error(e.getMessage(), e);
            fail(e.getMessage());
        }

        assertTrue(cloud.hasNode(newNode2));
    }

    // same case as above, only no changes are made to the node.
    public void test015DeleteNodeOutsideTransactionNodeInTransactionButNotChanged() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("bar11");
        Node nodeInTransaction = t.getNode(newNode2);
        //nodeInTransaction.setStringValue("title", "foo2");
        {
            // now delete the node
            Node nodeOutTransaction = cloud.getNode(newNode2);
            nodeOutTransaction.delete();
            assertFalse(cloud.hasNode(newNode2));
        }

        try {
            // make a relation to the (deleted) node, but in the transaction, where the node still
            // exists.
            // This demonstrate that there is an actual problem if the node ends up non-existing now.
            Node url = t.getNodeManager("urls").createNode();
            RelationManager rm = t.getRelationManager("news", "urls", "posrel");
            Relation r = nodeInTransaction.createRelation(url, rm);
            t.commit();
        } catch (Exception e) {
            // should not give exception. MMB-1680
            log.error(e.getMessage(), e);
            fail(e.getMessage());
        }

        assertTrue(cloud.hasNode(newNode2));
        assertEquals(1, cloud.getNode(newNode2).countRelations());
    }



    public void test016Alias() {
        Cloud cloud = getCloud();
        {
            Node node = cloud.getNode(newNode2);
            node.setStringValue("title", "abcdef");
            node.commit();
            assertEquals("abcdef", node.getStringValue("title"));
            node = cloud.getNode("test.news." + seq);
            assertEquals("abcdef", node.getStringValue("title"));
        }

        {
            Transaction t = cloud.getTransaction("bar12");

            Node node = t.getNode(newNode2);
            node.setStringValue("title", "abcdefg");
            node.commit();
            assertEquals("abcdefg", node.getStringValue("title"));
            t.commit();
            assertEquals("abcdefg", node.getStringValue("title"));

            node = cloud.getNode("test.news." + seq);
            assertEquals("abcdefg", node.getStringValue("title"));

            node = cloud.getNode(newNode2);
            assertEquals("abcdefg", node.getStringValue("title"));
        }



    }

    public void test017GetNodeTwiceWhileChanged() {
        Cloud cloud1 = getCloud();
        String originalTitleValue = cloud1.getNode(newNode2).getStringValue("title");
        {
            Node node = cloud1.getNode(newNode2);
            String title1 = node.getStringValue("title");
            node.setStringValue("title", "bla bla");
            // don't commit
        }
        // now change something by someone else
        {
            Cloud cloud2 = getCloud("foo");
            assertTrue(cloud1 != cloud2);
            Node node = cloud2.getNode(newNode2);
            node.setStringValue("title", "new title value");
            node.commit();
        }
        // now look to the original cloud
        {
            Node node = cloud1.getNode(newNode2);
            assertEquals("new title value", node.getStringValue("title"));
        }

    }

    public void test018CreateRelationBetweenNewNodes() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("createrelationtrans");
        NodeManager news = t.getNodeManager("news");
        Node n = news.createNode();
        NodeManager urls = t.getNodeManager("urls");
        Node url = urls.createNode();
        RelationManager rm = t.getRelationManager("news", "urls", "posrel");
        Relation r = n.createRelation(url, rm);
        t.commit();
    }

    // new node as argument
    public void test019CreateRelationToNewNode() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("createrelationtrans");
        Node n = t.getNode(newNode);
        NodeManager urls = t.getNodeManager("urls");
        Node url = urls.createNode();
        RelationManager rm = t.getRelationManager("news", "urls", "posrel");
        Relation r = n.createRelation(url, rm);
        t.commit();
    }

    // old node as argument
    public void test020CreateRelationToNewNode2() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("createrelationtrans");
        Node n = t.getNode(newNode);
        NodeManager urls = t.getNodeManager("urls");
        Node url = urls.createNode();
        RelationManager rm = t.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(n, rm);
        t.commit();
    }


    public void test021TransactionsAreEqual() {
        Cloud cloud = getCloud();
        Transaction t1 = cloud.getTransaction("testequals");
        Transaction t2 = cloud.getTransaction("testequals");
        assertEquals(t1, t2);
        Node n = t1.getNode(newNode);
        NodeManager urls = t2.getNodeManager("urls");
        Node url = urls.createNode();
        RelationManager rm = t2.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(n, rm);
        t2.commit();
        assertTrue(t2.isCommitted());
        assertTrue(t1.isCommitted());
        // assertTrue(t1 == t2); // FAILS IN RMMCI. Perhaps we should simply implement .equals on transactions
    }



    // MMB-1857
    public void test022GetNodes() {
        Cloud cloud = getCloud();
        Transaction t = cloud.getTransaction("testgetnodes");
        Node n = t.getNode(newNode);
        Node url = t.getNodeManager("urls").createNode();
        RelationManager rm = t.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(n, rm);

        {
            // should not give NPE's or so
            n.commit();
            url.commit();
            r.commit();
        }

        assertEquals(3, t.getNodes().size()); // 2 nodes and one relation

        for (Node rn : t.getNodes()) {
            // should occur no exceptions
            rn.commit(); // should have little effect in trans
        }
        t.cancel();
    }

    public void test023GetNode() {
        // Create new node. Request the node again.
        // Should work

        Cloud cloud = getCloud();
        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());

        Transaction t = cloud.getTransaction("testgetnode");
        Node url = t.getNodeManager("urls").createNode();
        url.setStringValue("url", "http://bla");
        url.commit();

        Node reurl = t.getNode(url.getNumber());


        assertEquals("http://bla", reurl.getStringValue("url"));




    }


    // MMB-1860
    public void test024CreateAndDelete() {
        // Create new node. Delete it. Commit the transaction.
        // The new node must not exist.

        Cloud cloud = getCloud();
        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());

        Transaction t = cloud.getTransaction("testcreateandelete");
        Node url = t.getNodeManager("urls").createNode();
        url.commit();
        assertEquals(1, t.getNodes().size());
        url.delete();
        assertEquals(1, t.getNodes().size()); // 0 would also be an option, but the node remaisn in the transaction as 'NOLONGER'
        t.commit();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());

        assertEquals(urlCount, urlCountAfter);

    }

    public void test025CreateAndDelete2() {
        // Create new node. Request the node again. Delete  that. Commit the transaction.
        // The new node must not exist.

        Cloud cloud = getCloud();
        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());

        Transaction t = cloud.getTransaction("testcreateandelete");
        Node url = t.getNodeManager("urls").createNode();
        url.commit();
        assertEquals(1, t.getNodes().size());

        Node reurl = t.getNode(url.getNumber());
        reurl.delete();

        assertEquals(1, t.getNodes().size()); // 0 would also be an option, but the node remaisn in the transaction as 'NOLONGER'

        t.commit();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());

        assertEquals(urlCount, urlCountAfter);

    }

    // MMB-1860 (2)
    public void test026CreateRelateAndDelete() {

        // Create new node. Make a relation to it. Delete the node again. Commit the transaction.
        // The new node must not exist, but the relation shouldn't have caused errors

        Cloud cloud = getCloud();
        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());

        Transaction t = cloud.getTransaction("testcreateandelete");

        Node news = t.getNode(newNode);
        Node url = t.getNodeManager("urls").createNode();
        RelationManager rm = t.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(news, rm);

        url.delete(true);

        t.commit();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());

        assertEquals(urlCount, urlCountAfter);

    }

    // MMB-1860 (3)
    public void test027CreateRelateAndDeleteRelation() {

        // Create new node. Make a relation to it. Delete the relation again. Commit the transaction.
        // The new node must exist, but the relation shouldn't.

        Cloud cloud = getCloud();
        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());
        int relCount = Queries.count(cloud.getNodeManager("insrel").createQuery());

        Transaction t = cloud.getTransaction("testcreateandelete");

        Node news = t.getNode(newNode);
        Node url = t.getNodeManager("urls").createNode();
        RelationManager rm = t.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(news, rm);
        r.commit();

        r.delete();

        t.commit();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());

        assertEquals(urlCount + 1, urlCountAfter);


        // Bit the relation should not exist, because it was deleted angain

        int relCountAfter = Queries.count(cloud.getNodeManager("insrel").createQuery());

        assertEquals(relCount, relCountAfter);
    }


    // MMB-1889
    public void test028CreateRelationAndDeleteNode() {
        // Make a relation to an existing node. Then delete that node with the 'delete relations' option'. Commit the transaction.
        // The new relations should not exist, since the node was deleted.
        // No errors.

        Cloud cloud = getCloud();

        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());
        int relCount = Queries.count(cloud.getNodeManager("insrel").createQuery());

        Node url = cloud.getNodeManager("urls").createNode();
        url.commit();

        Transaction t = cloud.getTransaction("relatedandelete");

        Node turl = t.getNode(url.getNumber());
        Node news = t.getNode(newNode);
        RelationManager rm = t.getRelationManager("urls", "news", "posrel");
        Relation r = turl.createRelation(news, rm);
        r.commit();
        turl.delete(true);
        t.commit();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());
        assertEquals(urlCount, urlCountAfter);
        int relCountAfter = Queries.count(cloud.getNodeManager("insrel").createQuery());
        assertEquals(relCount, relCountAfter);

    }


    // MMB-1893
    public void test029DeleteNodeWitRelationsAndCancel() {
        Cloud cloud = getCloud();

        int urlCount0 = Queries.count(cloud.getNodeManager("urls").createQuery());
        int relCount0 = Queries.count(cloud.getNodeManager("insrel").createQuery());


        Node url = cloud.getNodeManager("urls").createNode();
        url.commit();
        Node news = cloud.getNode(newNode);
        RelationManager rm = cloud.getRelationManager("urls", "news", "posrel");
        Relation r = url.createRelation(news, rm);
        r.commit();

        int urlCount = Queries.count(cloud.getNodeManager("urls").createQuery());
        int relCount = Queries.count(cloud.getNodeManager("insrel").createQuery());

        assertEquals(urlCount0 + 1, urlCount);
        assertEquals(relCount0 + 1, relCount);

        Transaction t = cloud.getTransaction("deletewithrelationsandcancel");

        Node turl = t.getNode(url.getNumber());
        turl.delete(true);


        t.cancel();

        int urlCountAfter = Queries.count(cloud.getNodeManager("urls").createQuery());
        assertEquals(urlCount, urlCountAfter);

        int relCountAfter = Queries.count(cloud.getNodeManager("insrel").createQuery());
        assertEquals(relCount, relCountAfter); // MMB-1893


    }


}
