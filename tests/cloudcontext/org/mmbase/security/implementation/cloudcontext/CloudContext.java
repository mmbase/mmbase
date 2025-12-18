/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.security.implementation.cloudcontext;

import java.util.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.mmbase.security.Operation;
import org.mmbase.tests.BridgeTest;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.functions.Parameters;

/**
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.9.1
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudContext extends BridgeTest {

    static boolean started = false;

    public void setUp() throws Exception  {
        if (! started) {
            startMMBase();
            started = true;
        }
    }

    protected void waitForCache() throws InterruptedException {
        Caches.waitForCacheInvalidation();
    }


    public void test001Implementation() {
        assertNotNull(getCloud());
        assertNotNull(getCloud().getCloudContext());
        assertNotNull(getCloud().getCloudContext().getAuthentication());
        assertEquals(org.mmbase.security.implementation.cloudcontext.Authenticate.class,
                     getCloud().getCloudContext().getAuthentication().getClass());
        Cloud cloud = getCloud("foo");
        assertEquals("foo", cloud.getUser().getIdentifier());
    }


    public void test002CreateRights() {
        Cloud cloud = getCloud("foo");
        assertFalse(cloud.getNodeManager("mmbasecontexts").mayCreateNode());
        assertTrue(cloud.getNodeManager("news").mayCreateNode());
        Node n1 = cloud.getNodeManager("mmbasecontexts").createNode();
        try {
            n1.commit();
            fail("Should not have been possible to create node of type 'mmbasecontexts', but it did not throw exception: " + n1);
        } catch(SecurityException se)  {
            // ok
        }
        Node n2 = cloud.getNodeManager("news").createNode();
        n2.commit();
        Node n3 = cloud.getNodeManager("news").createNode();
        n3.commit();
    }

    public void test003WriteRights() {
        Cloud cloud = getCloud("foo");
        Node context = cloud.getNodeManager("mmbasecontexts").getList(null).get(0);
        Node news = cloud.getNodeManager("news").getList(null).get(0);
        assertFalse(context.mayWrite());
        assertTrue("May not write noded " + news.getNumber() + " with context " + news.getContext(), news.mayWrite());
        try {
            context.setStringValue("name", "bla bla");
            fail("Should not have been allowed to write in an mmbasecontext node");
        } catch (SecurityException se) {
            // ok
        }

        news.setStringValue("title", "blaa");
        news.commit();

    }

    public void test004DeleteRights() {
        Cloud cloud = getCloud("foo");
        Node context = cloud.getNodeManager("mmbasecontexts").getList(null).get(0);
        Node news = cloud.getNodeManager("news").getList(null).get(0);
        assertFalse(context.mayDelete());
        assertTrue(news.mayDelete());
        try {
            context.delete(true);
            fail("Should not have been allowed to delete an mmbasecontext node");
        } catch (SecurityException se) {
            // ok
        }

        news.delete(true);

    }


    public void test005ChangeContextRights() {
        Cloud cloud = getCloud("foo");
        Node context = cloud.getNodeManager("mmbasecontexts").getList(null).get(0);
        Node news = cloud.getNodeManager("news").getList(null).get(0);
        assertFalse(context.mayChangeContext());
        assertTrue(news.mayChangeContext());
        try {
            context.setContext("default");
            fail("Should not have been allowed to set context of an mmbasecontext node"); //MMB-1752
        } catch (SecurityException se) {
            // ok
        }

        news.setContext("admin");
        news.commit();
        // changed to a context which we may not change again
        assertFalse(news.mayChangeContext());

    }
    public void test006ReadRights() {
        // TODO, cannot be tested right now, because read all property
    }

    public void test007ChangeRelationRights() {
        // TODO, cannot be tested right now, probably because I don't
        // understand it properly
    }

    public void test008SetOwnPassord() {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));
        assertEquals("foo", userNode.getStringValue("username"));
        userNode.setStringValue("password", "bar2");
        userNode.commit();

        assertEquals(new org.mmbase.util.transformers.MD5().transform("bar2"), userNode.getStringValue("password"));
    }

    public void test009DeleteOwnNode() {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));
        try {
            userNode.delete();
            fail("Should not have been allowed to delete own node");
        } catch (SecurityException se) {
            // ok
        }
    }
    public void test010ChangedPassword() {
        Map<String, Object> loginInfo = new HashMap<String, Object>();
        loginInfo.put("username", "foo");
        loginInfo.put("password", "bar");

        try {
            Cloud cloud = getCloudContext().getCloud("mmbase", "name/password", loginInfo);
            fail("Should not have been allowed to login with wrong (old)  password ");
        } catch (SecurityException se) {
            //
        }

        loginInfo.put("password", "bar2");
        Cloud cloud = getCloudContext().getCloud("mmbase", "name/password", loginInfo);
        assertNotNull(cloud);

    }

    public void test011GrantToUser() throws InterruptedException {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));

        Cloud adminCloud = getCloud();
        Node contextNode = SearchUtil.findNode(adminCloud, "mmbasecontexts", "name", "security");
        Parameters params = contextNode.createParameters("grant");
        params.set("grouporuser", userNode);
        params.set("operation", Operation.CREATE);
        params.set("user", adminCloud.getUser());

        assertTrue(contextNode.getFunctionValue("grant", params).toBoolean());

        //assertFalse(cloud.getUser().isValid());

        waitForCache();

        // now foo should be allowed to create new contexts
        cloud = getCloud("foo");
        assertTrue(cloud.getNodeManager("mmbasecontexts").mayCreateNode());

        Node n3 = cloud.getNodeManager("mmbasecontexts").createNode();
        n3.setStringValue("name", "testcontextoffoo");
        n3.commit();
    }
    public void test012RevokeFromUser() throws InterruptedException {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));

        Cloud adminCloud = getCloud();
        Node contextNode = SearchUtil.findNode(adminCloud, "mmbasecontexts", "name", "security");
        Parameters params = contextNode.createParameters("revoke");
        params.set("grouporuser", userNode);
        params.set("operation", Operation.CREATE);
        params.set("user", adminCloud.getUser());

        assertTrue(contextNode.getFunctionValue("revoke", params).toBoolean());
        // a certain latency is allowed

        waitForCache();

        // now foo should be disallowed to create new contexts again
        cloud = getCloud("foo");
        assertFalse(cloud.getNodeManager("mmbasecontexts").mayCreateNode());

        try {
            Node n3 = cloud.getNodeManager("mmbasecontexts").createNode();
            n3.setStringValue("name", "testcontextoffoo2");
            n3.commit();
            fail("Should not have been allowed to create new mmbase contexts");
        } catch (SecurityException se) {
            // ok
        }
    }

    public void test013GrantToGroup() throws InterruptedException {
        Cloud adminCloud = getCloud();
        Node contextNode = SearchUtil.findNode(adminCloud, "mmbasecontexts", "name", "security");
        Node groupNode   = SearchUtil.findNode(adminCloud, "mmbasegroups",   "name", "users");
        Parameters params = contextNode.createParameters("grant");
        params.set("grouporuser", groupNode);
        params.set("operation", Operation.CREATE);
        params.set("user", adminCloud.getUser());

        assertTrue(contextNode.getFunctionValue("grant", params).toBoolean());
        // a certain latency is allowed
        waitForCache();
        // now foo should be allowed to create new contexts again, because is in the 'users' group
        Cloud cloud = getCloud("foo");
        assertTrue(cloud.getNodeManager("mmbasecontexts").mayCreateNode());

        Node n3 = cloud.getNodeManager("mmbasecontexts").createNode();
        n3.setStringValue("name", "testcontextoffoo3");
        n3.commit();
    }

    public void test014RevokeFromGroup() throws InterruptedException {

        Cloud adminCloud = getCloud();
        Node contextNode = SearchUtil.findNode(adminCloud, "mmbasecontexts", "name", "security");
        Node groupNode   = SearchUtil.findNode(adminCloud, "mmbasegroups",   "name", "users");
        Parameters params = contextNode.createParameters("revoke");
        params.set("grouporuser", groupNode);
        params.set("operation", Operation.CREATE);
        params.set("user", adminCloud.getUser());

        assertTrue(contextNode.getFunctionValue("revoke", params).toBoolean());
        // a certain latency is allowed
        waitForCache();

        // now foo should be disallowed to create new contexts again
        Cloud cloud = getCloud("foo");
        assertFalse(cloud.getNodeManager("mmbasecontexts").mayCreateNode());

        try {
            Node n3 = cloud.getNodeManager("mmbasecontexts").createNode();
            n3.setStringValue("name", "testcontextoffoo4");
            n3.commit();
            fail("Should not have been allowed to create new mmbase contexts");
        } catch (SecurityException se) {
            // ok
        }
    }

}

