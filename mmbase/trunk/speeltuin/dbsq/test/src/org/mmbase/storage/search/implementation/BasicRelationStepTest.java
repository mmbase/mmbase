package org.mmbase.storage.search.implementation;

import java.util.*;
import junit.framework.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.storage.search.*;
import org.mmbase.module.core.MMObjectBuilder;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.3 $
 */
public class BasicRelationStepTest extends TestCase {
    
    private final static String TEST_BUILDER1 = "images";
    private final static String TEST_BUILDER2 = "icaches";
    private final static String INSREL = "insrel";
    private final static String TEST_ALIAS = "test";
    
    /** Test instance. */
    private BasicRelationStep instance = null;
    
    /** Relation builder for relation step of test instance. */
    private InsRel relation = null;
    
    /** Previous step of test instance. */
    private Step previous = null;
    
    /** Next step of test instance. */
    private Step next = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    public BasicRelationStepTest(java.lang.String testName) {
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
        MMObjectBuilder builder1 = mmbase.getBuilder(TEST_BUILDER1);
        MMObjectBuilder builder2 = mmbase.getBuilder(TEST_BUILDER2);
        InsRel relation = (InsRel) mmbase.getBuilder(INSREL);
        previous = new BasicStep(builder1);
        next = new BasicStep(builder2);
        instance = new BasicRelationStep(relation, previous, next);
     }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Test of setDirectionality method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testSetDirectionality() {
       // Default is RelationStep.DIRECTIONS_BOTH.
       assertTrue(instance.getDirectionality() == RelationStep.DIRECTIONS_BOTH);
       
        // Invalid value, should throw IllegalArgumentException.
       try {
            instance.setDirectionality(-1);
            fail("Invalid value, should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
       
       instance.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
       assertTrue(instance.getDirectionality() == RelationStep.DIRECTIONS_SOURCE);
       
       instance.setDirectionality(RelationStep.DIRECTIONS_DESTINATION);
       assertTrue(instance.getDirectionality() == RelationStep.DIRECTIONS_DESTINATION);
    }
    
    /** Test of getDirectionality method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testGetDirectionality() {
        // Same as:
        testSetDirectionality();
    }
    
    /** Test of getPrevious method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testGetPrevious() {
        Step step1 = instance.getPrevious();
        assertTrue(step1 != null);
        assertTrue(step1.equals(previous));
    }
    
    /** Test of getNext method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testGetNext() {
        Step step2 = instance.getNext();
        assertTrue(next != null);
        assertTrue(step2.equals(next));
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicRelationStep. */
    public void testToString() {
        // With default alias.
        assertTrue(instance.toString(), 
        instance.toString().equals("RelationStep(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ", dir:" 
        + RelationStep.DIRECTIONALITY_NAMES[instance.getDirectionality()] + ")"));
        
        // With test alias.
        instance.setAlias(TEST_ALIAS);
        assertTrue(instance.toString(), 
        instance.toString().equals("RelationStep(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ", dir:" 
        + RelationStep.DIRECTIONALITY_NAMES[instance.getDirectionality()] + ")"));
         
        // With nodes.
        instance.addNode(123);
        instance.addNode(3456);
        assertTrue(instance.toString(), 
        instance.toString().equals("RelationStep(tablename:" + instance.getTableName() 
        + ", alias:" + instance.getAlias() + ", nodes:" 
        + instance.getNodes() + ", dir:" 
        + RelationStep.DIRECTIONALITY_NAMES[instance.getDirectionality()] + ")"));
     }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicRelationStepTest.class);
        
        return suite;
    }
    
}
