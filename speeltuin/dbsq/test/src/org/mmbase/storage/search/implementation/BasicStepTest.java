package org.mmbase.storage.search.implementation;

import junit.framework.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.module.core.MMObjectBuilder;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.2 $
 */
public class BasicStepTest extends TestCase {
    
    private final static String TEST_ALIAS = "abcd";
    
    private final static String BUILDER_NAME = "images";
    
    /** Test instance. */
    private BasicStep instance;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /** Builder example. */
    private MMObjectBuilder builder = null;
    
    public BasicStepTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Sets up before each test.
     */
    public void setUp() throws Exception {
        MMBaseContext.init();
        mmbase = MMBase.getMMBase();
        builder = mmbase.getBuilder(BUILDER_NAME);
        instance = new BasicStep(builder);
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Test of getTableName method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testGetTableName() {
        String tableName = instance.getTableName();
        assertTrue(tableName != null);
        assertTrue(tableName.equals(BUILDER_NAME));
    }
    
    /** Test of setAlias method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testSetAlias() {
        // Default is builder name.
        assertTrue(instance.getAlias().equals(BUILDER_NAME));
        
        // Null value, should throw IllegalArgumentException.
        try {
            instance.setAlias(null);
            fail("Null value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        // Blank spaces, should throw IllegalArgumentException.
        try {
            instance.setAlias("   ");
            fail("Null value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        instance.setAlias(TEST_ALIAS);
        String alias = instance.getAlias();
        assertTrue(alias != null);
        assertTrue(alias.equals(TEST_ALIAS));
    }
    
    /** Test of getAlias method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testGetAlias() {
        // Same as:
        testSetAlias();
    }
    
    /** Test of addNode method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testAddNode() {
        // Negative node, should throw IllegalArgumentException
        try {
            instance.addNode(-1);
            fail("Negative node, should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
        
        SortedSet nodes = instance.getNodes();
        assertTrue(nodes.size() == 0);
        int nodeNumber0 = 23456;
        instance.addNode(nodeNumber0);
        nodes = instance.getNodes();
        assertTrue(nodes.size() == 1);
        Iterator iNodes = nodes.iterator();
        assertTrue(iNodes.hasNext());
        assertTrue(iNodes.next().equals(new Integer(nodeNumber0)));
        assertTrue(!iNodes.hasNext());
        int nodeNumber1 = 2345;
        instance.addNode(nodeNumber1);
        nodes = instance.getNodes();
        assertTrue(nodes.size() == 2);
        iNodes = nodes.iterator();
        assertTrue(iNodes.hasNext());
        assertTrue(iNodes.next().equals(new Integer(nodeNumber1)));
        assertTrue(iNodes.hasNext());
        assertTrue(iNodes.next().equals(new Integer(nodeNumber0)));
        assertTrue(!iNodes.hasNext());
    }
    
    /** Test of getNodes method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testGetNodes() {
        // See:
        testAddNode();
        
        SortedSet nodes = instance.getNodes();
        Object item = nodes.first();
        
        // List returned must be unmodifiable.
        try {
            nodes.add(item);
            fail("Attempt to modify list, must throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {}
        try {
            nodes.clear();
            fail("Attempt to modify list, must throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {}
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicStep. */
    public void testToString() {
        // With default alias.
        assertTrue(instance.toString(), 
        instance.toString().equals("Step(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ")"));
        
        // With test alias.
        instance.setAlias(TEST_ALIAS);
        assertTrue(instance.toString(), 
        instance.toString().equals("Step(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ")"));
         
        // With nodes.
        instance.addNode(123);
        instance.addNode(3456);
        assertTrue(instance.toString(), 
        instance.toString().equals("Step(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ")"));
     }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicStepTest.class);
        
        return suite;
    }
    
}
