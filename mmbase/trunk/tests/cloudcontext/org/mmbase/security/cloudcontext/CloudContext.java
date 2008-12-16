/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.security.cloudcontext;

import java.util.*;
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

public class CloudContext extends BridgeTest {

    static boolean started = false;

    public void setUp() throws Exception  {
        if (! started) {
            startMMBase();
            started = true;
        }
    }

    public void testImplementation() {
        assertEquals(org.mmbase.security.implementation.cloudcontext.Authenticate.class, 
                     getCloud().getCloudContext().getAuthentication().getClass());
        Cloud cloud = getCloud("foo");
        assertEquals("foo", cloud.getUser().getIdentifier());
    }


    public void testCreateRights() {
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

    public void testWriteRights() {
        Cloud cloud = getCloud("foo");
        Node context = cloud.getNodeManager("mmbasecontexts").getList(null).get(0);
        Node news = cloud.getNodeManager("news").getList(null).get(0);
        assertFalse(context.mayWrite());
        assertTrue(news.mayWrite());
        try {
            context.setStringValue("name", "bla bla");
            fail("Should not have been allowed to write in an mmbasecontext node");
        } catch (SecurityException se) {
            // ok
        }
        
        news.setStringValue("title", "blaa");
        news.commit();
        
    }

    public void testDeleteRights() {
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


    public void testChangeContextRights() {
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
    public void testReadRights() {
        // TODO, cannot be tested right now, because read all property
    }

    public void testChangeRelationRights() {
        // TODO, cannot be tested right now, probably because I don't
        // understand it properly
    }

    public void testSetOwnPassord() {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));
        assertEquals("foo", userNode.getStringValue("username"));        
        userNode.setStringValue("password", "bar2");
        userNode.commit();
        
        assertEquals(new org.mmbase.util.transformers.MD5().transform("bar2"), userNode.getStringValue("password"));
    }

    public void testDeleteOwnNode() {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));
        try {
            userNode.delete();
            fail("Should not have been allowed to delete own node");
        } catch (SecurityException se) {
            // ok
        }       
    }
    public void testChangedPassword() {
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

    public void testGrant() throws InterruptedException {
        Cloud cloud = getCloud("foo");
        Node userNode = cloud.getNode(cloud.getCloudContext().getAuthentication().getNode(cloud.getUser()));

        Cloud adminCloud = getCloud();
        Node contextNode = SearchUtil.findNode(adminCloud, "mmbasecontexts", "name", "security");
        Parameters params = contextNode.createParameters("grant");
        params.set("grouporuser", "" + userNode.getNumber());
        params.set("operation", "" + Operation.CREATE.toString());
        params.set("user", adminCloud.getUser());

        assertTrue(contextNode.getFunctionValue("grant", params).toBoolean());

        //assertFalse(cloud.getUser().isValid());

        // a certain latency is allowed
        Thread.sleep(1000);
        org.mmbase.cache.CacheManager.getInstance().clear(".*"); //  TODO TODO,  this should _NOT_ be necessary.
        // now foo should be allowed to create new contexts
        cloud = getCloud("foo");
        assertTrue(cloud.getNodeManager("mmbasecontexts").mayCreateNode());

        Node n3 = cloud.getNodeManager("mmbasecontexts").createNode();
        n3.setStringValue("name", "testcontextoffoo");
        n3.commit();
    }

}

