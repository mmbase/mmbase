package org.mmbase.module.database.search.implementation;

import java.util.*;
import junit.framework.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.module.database.search.*;
import org.mmbase.module.core.MMObjectBuilder;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.1 $
 */
public class BasicRelationStepTest extends TestCase {
    
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
        MMObjectBuilder builder1 = mmbase.getBuilder("images");
        MMObjectBuilder builder2 = mmbase.getBuilder("icaches");
        InsRel relation = (InsRel) mmbase.getBuilder("insrel");
        previous = new BasicStep(builder1);
        next = new BasicStep(builder2);
        instance = new BasicRelationStep(relation, previous, next);
     }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Test of setDirectionality method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testSetDirectionality() {
       // Default is RelationStep.DIRECTIONS_BOTH.
       assert(instance.getDirectionality() == RelationStep.DIRECTIONS_BOTH);
       
        // Invalid value, should throw IllegalArgumentException.
       try {
            instance.setDirectionality(-1);
            fail("Invalid value, should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
       
       instance.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
       assert(instance.getDirectionality() == RelationStep.DIRECTIONS_SOURCE);
       
       instance.setDirectionality(RelationStep.DIRECTIONS_DESTINATION);
       assert(instance.getDirectionality() == RelationStep.DIRECTIONS_DESTINATION);
    }
    
    /** Test of getDirectionality method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testGetDirectionality() {
        // Same as:
        testSetDirectionality();
    }
    
    /** Test of getPrevious method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testGetPrevious() {
        Step step1 = instance.getPrevious();
        assert(step1 != null);
        assert(step1.equals(previous));
    }
    
    /** Test of getNext method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testGetNext() {
        Step step2 = instance.getNext();
        assert(next != null);
        assert(step2.equals(next));
    }
    
    /** Test of equals method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.module.database.search.implementation.BasicRelationStep. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicRelationStepTest.class);
        
        return suite;
    }
    
}
