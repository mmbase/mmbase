package org.mmbase.module.corebuilders;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.mmbase.bridge.*;
import org.mmbase.bridge.Module;
import java.util.*;
import java.util.regex.Pattern;

/**
 * JUnit tests for TypeRel
 *
 * @author  Michiel Meeuwissen
 * @version $Id$
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TypeRelTest extends org.mmbase.tests.BridgeTest {

    static protected String  UNIDIR_ROLE = "unidirectionalrelation";
    static protected String  BIDIR_ROLE  = "bidirectionalrelation";
    static protected String  INH_ROLE    =  "inheritancerelation";
    static protected String  MULTILEVEL_ROLE    =  "multileveltestrelation";
    static protected String  OTHER_ROLE   = "this_role_does_not_exist";
    static protected String  RELATED_ROLE   = "related";

    static protected String  SOURCE        = "source";
    static protected String  DESTINATION   = "destination";
    static protected String  BOTH          = "both";


    static protected Cloud cloud = null;
    static protected NodeManager relDefManager;
    static protected NodeManager typeRelManager;
    static protected NodeManager insRelManager;
    static protected NodeManager newsManager;
    static protected NodeManager urlsManager;
    static protected NodeManager objectManager;
    static protected NodeList    createdNodes;

    static protected Node        news;
    static protected Node        url;
    static protected Node        object;
    static protected Node        typerel;

    public TypeRelTest(String testName) {
        super(testName);
    }


    public void test001ListRelations() {
        RelationManagerList rml = cloud.getRelationManagers();
        assertTrue(rml != null);
        assertTrue(rml.size() > 0);
        if (rml.size() == 0) {
            fail("cannot test");
        }
    }

    protected Node createRelDefNode(String role, int dir) {
        // create a new relation-definition
        Node reldef = relDefManager.createNode();
        assertTrue(relDefManager.getName().equals("reldef"));
        assertTrue("Manager of reldefnode is not 'reldef' but " + reldef.getNodeManager().getName(), reldef.getNodeManager().getName().equals("reldef"));
        assertTrue(reldef.getNodeManager().hasField("sname"));
        reldef.setValue("sname", role);
        reldef.setValue("dname", "d" + role );
        reldef.setValue("sguiname", role);
        reldef.setValue("dguiname", "d" + role);
        reldef.setIntValue("dir", dir);
        reldef.setNodeValue("builder", insRelManager);
        reldef.commit();
        createdNodes.add(reldef);
        return reldef;
    }

    /**
     * Create bidirection relation type, and check if relationmanager in both directions can be found.
     */
    public void test002BidirectionalCloud1() {
        Node reldef = createRelDefNode(BIDIR_ROLE, 2);
        typerel = typeRelManager.createNode();
        typerel.setNodeValue("snumber", newsManager);
        typerel.setNodeValue("dnumber", urlsManager);
        typerel.setNodeValue("rnumber", reldef);
        typerel.commit();
        createdNodes.add(typerel);

        // now this relation must exist.
        // check if it can be found by cloud
        RelationManagerList rml = cloud.getRelationManagers(newsManager, urlsManager, BIDIR_ROLE);
        assertTrue(rml.size() > 0);
        assertTrue(rml.contains(typerel));
    }

    public void test003BidirectionalCloud2() {
        RelationManagerList rml = cloud.getRelationManagers(urlsManager, newsManager, BIDIR_ROLE);
        assertTrue(rml.size() > 0);
        assertTrue(rml.contains(typerel));
    }


    public void test004BidirectionalNodeManagerAllowedRelations1() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations((NodeManager) null, null, null);
        assertTrue(rml.contains(typerel));
    }
    public void test005BidirectionalNodeManagerAllowedRelations2() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, null, null);
        assertTrue(rml.contains(typerel));
    }
    public void test006BidirectionalNodeManagerAllowedRelations3() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, null);
        assertTrue(rml.contains(typerel));
    }


    public void test007BidirectionalNodeManagerAllowedRelations4() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, DESTINATION);
        assertTrue(rml.contains(typerel));
    }
    public void test008BidirectionalNodeManagerAllowedRelations5() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, SOURCE);
        assertFalse(rml.contains(typerel));
    }
    public void test009BidirectionalNodeManagerAllowedRelations6() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, BOTH);
        assertTrue(rml.contains(typerel));
    }
    public void test010BidirectionalNodeManagerAllowedRelations7() {
        // by source-manager
        try {
            newsManager.getAllowedRelations(urlsManager, OTHER_ROLE, null);
            fail("Should have thrown exception for non-existing relations");
        } catch (NotFoundException e) {
        };
    }
    public void test011BidirectionalNodeManagerAllowedRelations8() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, RELATED_ROLE, null);
        assertFalse(rml.contains(typerel));
    }

    public void test012BidirectionalNodeManagerAllowedRelations9() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations();
        assertTrue(rml.contains(typerel));
    }


    public void test013BidirectionalNodeManagerAllowedRelations10() {
        // by destination-manager
        RelationManagerList rml = urlsManager.getAllowedRelations();
        assertTrue(rml.contains(typerel));
    }




    public void test014BidirectionalNode1() {
        RelationManager rm = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, DESTINATION).getRelationManager(0);
        Relation r = rm.createRelation(news, url);
        r.commit();
        createdNodes.add(r);
        // no exception should have occured.
    }

    public void test015BidirectionalNode2() {
        RelationManager rm = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, DESTINATION).getRelationManager(0);
        Relation r = rm.createRelation(url, news);
        r.commit();
        createdNodes.add(r);
    }

    public void test016BidirectionalNode3() {
        RelationManager rm = newsManager.getAllowedRelations(urlsManager, BIDIR_ROLE, DESTINATION).getRelationManager(0);
        try {
            Relation r = rm.createRelation(news, object);
            r.commit();
            createdNodes.add(r);
            fail("Should not have been allowed");
        } catch (BridgeException e) {
        }
        // no exception should have occured.
    }



    public void test017BidirectionalNode4() {
        // make sure it is the right direction now.
        NodeList nl = news.getRelatedNodes(urlsManager, BIDIR_ROLE, null);
        assertTrue("" + nl, nl.contains(url));
    }
    public void test018BidirectionalNode5() {
        NodeList nl = url.getRelatedNodes(newsManager);
        assertTrue(nl.contains(news));
    }

    public void test019BidirectionalRelations() {

    }



    /*
     * Create unidirection relation type, and check if relationmanager in only one direction can be found.
     */

    public void test020UnidirectionalCloud1() {
        Node reldef = createRelDefNode(UNIDIR_ROLE, 1);

        typerel = typeRelManager.createNode();
        typerel.setNodeValue("snumber", newsManager);
        typerel.setNodeValue("dnumber", urlsManager);
        typerel.setNodeValue("rnumber", reldef);
        typerel.commit();
        createdNodes.add(typerel);


        // now this relation must exist.
        RelationManagerList rml = cloud.getRelationManagers(newsManager, urlsManager, UNIDIR_ROLE);
        assertTrue(rml.size() > 0);
        assertTrue(rml.contains(typerel));
    }

    public void test021UnidirectionalCloud2() {
        RelationManagerList rml = cloud.getRelationManagers(urlsManager, newsManager, UNIDIR_ROLE);
        assertTrue(rml.size() == 0);
        assertFalse(rml.contains(typerel));
    }


    public void test022UnidirectionalNodeManagerAllowedRelations1() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations((NodeManager) null, null, null);
        assertTrue(rml.contains(typerel));
    }
    public void test023UnidirectionalNodeManagerAllowedRelations2() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, null, null);
        assertTrue(rml.contains(typerel));
    }
    public void test024UnidirectionalNodeManagerAllowedRelations3() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, null);
        assertTrue(rml.contains(typerel));
    }


    public void tes025tUnidirectionalNodeManagerAllowedRelations4() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, DESTINATION);
        assertTrue(rml.contains(typerel));
    }
    public void test026UnidirectionalNodeManagerAllowedRelations5() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, SOURCE);
        assertFalse(rml.contains(typerel));
    }
    public void test027UnidirectionalNodeManagerAllowedRelations6() {
        // by source-manager
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, BOTH);
        assertTrue(rml.contains(typerel));
    }
    public void test028UnidirectionalNodeManagerAllowedRelations7() {
        try {
            newsManager.getAllowedRelations(urlsManager, OTHER_ROLE, null);
            fail("Should have thrown exception for non-existing relations");
        } catch (NotFoundException e) {
        };
    }
    public void test029UnidirectionalNodeManagerAllowedRelations8() {
        RelationManagerList rml = newsManager.getAllowedRelations(urlsManager, RELATED_ROLE, null);
        assertFalse(rml.contains(typerel));
    }

    public void test030UnidirectionalNodeManagerAllowedRelations9() {
        RelationManagerList rml = newsManager.getAllowedRelations();
        assertTrue(rml.contains(typerel));
    }

    public void test031UnidirectionalNodeManagerAllowedRelations10() {
        RelationManagerList rml = urlsManager.getAllowedRelations();
        assertFalse(rml.contains(typerel));
    }



    public void test032UnidirectionalNode1() {
        RelationManager rm = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, DESTINATION).getRelationManager(0);
        Relation r = rm.createRelation(news, url);
        r.commit();
        createdNodes.add(r);
        // no exception should have occured.
    }

    public void test033UnidirectionalNode2() {
        RelationManager rm = newsManager.getAllowedRelations(urlsManager, UNIDIR_ROLE, DESTINATION).getRelationManager(0);
        Relation r = rm.createRelation(url, news);
        r.commit();
        createdNodes.add(r);

    }

    public void test034UnidirectionalNode3() {
        // make sure it is the right direction now.
        NodeList nl = news.getRelatedNodes(urlsManager, UNIDIR_ROLE, null);
        assertTrue(nl.contains(url));
    }
    public void test035UnidirectionalNode4() {
        NodeList nl = url.getRelatedNodes(newsManager, UNIDIR_ROLE, null);
        assertFalse(nl.contains(news));
    }



    public void test036InheritanceRelations() {
        Node reldef = createRelDefNode(INH_ROLE, 2);

        Node typerel = typeRelManager.createNode();
        typerel.setNodeValue("snumber", objectManager);
        typerel.setNodeValue("dnumber", urlsManager);
        typerel.setNodeValue("rnumber", reldef);
        typerel.commit();
        createdNodes.add(typerel);

        // now this relation must exist.
        RelationManagerList rm1 = cloud.getRelationManagers(objectManager, urlsManager, INH_ROLE);
        assertTrue(rm1.size() > 0);

        RelationManagerIterator it = rm1.relationManagerIterator();
        while (it.hasNext()) {
            it.nextRelationManager(); // no exceptions..
        }

        RelationManager rmi11 = cloud.getRelationManager(objectManager, urlsManager, INH_ROLE);
        assertNotNull(rmi11);

        RelationManager rmi12 = cloud.getRelationManager("object", "urls", INH_ROLE);
        assertNotNull(rmi12);

        RelationManagerList rm2 = cloud.getRelationManagers(newsManager, urlsManager, INH_ROLE);
        assertTrue(rm2.size() > 0);

        RelationManager rmi21 = cloud.getRelationManager(newsManager, urlsManager, INH_ROLE);
        assertNotNull(rmi21);

        RelationManager rmi22 = cloud.getRelationManager("news", "urls", INH_ROLE);
        assertNotNull(rmi22);
    }


    public void test037MultiLevelQuery() {
        Node reldef = createRelDefNode(MULTILEVEL_ROLE, 2);
        Node typerel = typeRelManager.createNode();
        typerel.setNodeValue("snumber", objectManager);
        typerel.setNodeValue("dnumber", urlsManager);
        typerel.setNodeValue("rnumber", reldef);
        typerel.commit();
        createdNodes.add(typerel);

        RelationManager rm = newsManager.getAllowedRelations(urlsManager, MULTILEVEL_ROLE, DESTINATION).getRelationManager(0);
        Relation r = rm.createRelation(news, url);
        r.commit();
        createdNodes.add(r);

        NodeList nl = cloud.getList(null, // startnodes
                                    "news," + MULTILEVEL_ROLE + ",urls", // path
                                    "",   // fields
                                    null, // constraints
                                    null, // orderby
                                    null, // directions
                                    null, // searchDir
                                    false // distinct
                                    );

        assertTrue(nl.size() == 1);
        System.out.println(nl);
        System.out.println(nl.size());
        NodeList nl2 = cloud.getList(null, // startnodes
                                    "news," + MULTILEVEL_ROLE + ",object", // path
                                    "",   // fields
                                    null, // constraints
                                    null, // orderby
                                    null, // directions
                                    null, // searchDir
                                    false // distinct
                                    );
        System.out.println(nl2);
        assertTrue(nl2.size() == 1);

    }

    private void testDestinationManagers(NodeManager sourceManager) {
        RelationManagerList destinationManagers = sourceManager.getAllowedRelations((NodeManager) null, null, DESTINATION);
        RelationManagerIterator i = destinationManagers.relationManagerIterator();
        while(i.hasNext()) {
            RelationManager rm = i.nextRelationManager();
            assertTrue("" + rm.getSourceManager() + " is not " + sourceManager,
                       rm.getSourceManager().equals(sourceManager));
        }
    }

    public void test039DestinationManagers() {
        testDestinationManagers(newsManager);
        testDestinationManagers(urlsManager);
        testDestinationManagers(objectManager);

    }
    private void testSourceManagers(NodeManager destinationManager) {
        RelationManagerList sourceManagers      = destinationManager.getAllowedRelations((NodeManager) null, null, SOURCE);
        RelationManagerIterator i = sourceManagers.relationManagerIterator();
        while(i.hasNext()) {
            RelationManager rm = i.nextRelationManager();
            assertTrue("" + rm.getSourceManager() + " is not " + destinationManager,
                       rm.getDestinationManager().equals(destinationManager));
        }
    }
    public void test040SourceManagers() {
        testSourceManagers(newsManager);
        testSourceManagers(urlsManager);
        testSourceManagers(objectManager);
    }

    Pattern OK = Pattern.compile(".*loaded ok.*");
    public void test041InstallBridge2() { // Trigger MMB-1728
        // try to install another role 'related'
        Module mmadmin = getCloud().getCloudContext().getModule("mmadmin");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("APPLICATION", "BridgeTest2");
        mmadmin.process("LOAD", "BridgeTest2", params);
        String lastmsg = mmadmin.getInfo("LASTMSG");
        assertTrue(OK + " did not match " + lastmsg, OK.matcher(lastmsg).matches());
    }

    public void test042InstallBridge3() {
        // try to install another role 'related'
        Module mmadmin = getCloud().getCloudContext().getModule("mmadmin");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("APPLICATION", "BridgeTest3");
        mmadmin.process("LOAD", "BridgeTest3", params);

        // could not deploy, because it defined an incompatible reldef (related with a different builder)
        String lastmsg = mmadmin.getInfo("LASTMSG");
        assertFalse(OK + " should not match " + lastmsg, OK.matcher(lastmsg).matches());
    }



    private void testManagers(NodeManager manager) {

        RelationManagerList managers  = manager.getAllowedRelations((NodeManager) null, null, null);
        RelationManagerIterator i = managers.relationManagerIterator();
        Cloud cloud = manager.getCloud();
        while(i.hasNext()) {
            RelationManager rm = i.nextRelationManager();
            assertNotNull(rm);
            assertTrue("Both " + rm.getDestinationManager() + " and " + rm.getSourceManager() + " are not " + manager,
                       rm.getDestinationManager().equals(manager) || rm.getSourceManager().equals(manager));

            RelationManager refetched = cloud.getRelationManager(rm.getSourceManager(), rm.getDestinationManager(), rm.getForwardRole());
            assertTrue(refetched.getSourceManager().equals(rm.getSourceManager()));
            assertTrue(refetched.getDestinationManager().equals(rm.getDestinationManager()));
            assertTrue(refetched.getForwardRole().equals(rm.getForwardRole()));
        }
    }

    public void test043Managers() {
        testManagers(newsManager);
        testManagers(urlsManager);
        testManagers(objectManager);
    }


    public void test044ClearUpMess() {
        //        System.out.println("Clearing up the mess");
        NodeIterator i = createdNodes.nodeIterator();
        while (i.hasNext()) i.next(); // fast forward.

        while (i.hasPrevious()) {
            Node node = i.previous();
            System.out.print("D"); //eleting " + node);
            node.delete();
        }
    }


    public void test100Shutdown() {
        getCloud("admin").shutdown();
    }

    /**
     * Sets up before each test.
     */
    public void setUp() throws Exception {
        if (cloud == null) {
            startMMBase();
            //cloud = getRemoteCloud();
            cloud = getCloud();

            // needed builders for this test.
            try {
                relDefManager  = cloud.getNodeManager("reldef");
                typeRelManager = cloud.getNodeManager("typerel");
                insRelManager  = cloud.getNodeManager("insrel");
                newsManager    = cloud.getNodeManager("news");
                urlsManager    = cloud.getNodeManager("urls");
                objectManager  = cloud.getNodeManager("object");
            } catch (NotFoundException e) {
                throw new Exception("Test cases cannot be performed because " + e.getMessage() + " Please arrange this in your cloud before running this TestCase.");
             }

            createdNodes = cloud.createNodeList();
            assertNotNull("Could not create remotely a nodelist" , createdNodes);

            news = newsManager.createNode();
            news.setValue("title", "test node");
            news.commit();
            createdNodes.add(news);

            url = urlsManager.createNode();
            url.setValue("url", "http://test.mmbase.org");
            url.setValue("name", "test url");
            url.commit();
            createdNodes.add(url);

            object = objectManager.createNode();
            object.commit();
            createdNodes.add(object);

        }
    }

}

