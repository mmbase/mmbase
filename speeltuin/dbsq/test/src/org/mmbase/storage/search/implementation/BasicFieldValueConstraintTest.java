package org.mmbase.storage.search.implementation;

import junit.framework.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.FieldDefs;
import org.mmbase.storage.search.*;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.3 $
 */
public class BasicFieldValueConstraintTest extends TestCase {
    
    private final static String BUILDER_NAME = "images";
    private final static String FIELD_NAME1 = "title";
    private final static String FIELD_NAME2 = "number";
    private final static String STRING_VALUE1 = "value1";
    private final static String STRING_VALUE2 = "aValue2";
    private final static Integer INTEGER_VALUE1 = new Integer(12345);
    private final static Integer INTEGER_VALUE2 = new Integer(34589);

    /** Test instance 1 (string field). */
    private BasicFieldValueConstraint instance1 = null;
    
    /** Test instance 2 (integer field). */
    private BasicFieldValueConstraint instance2 = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /** Field instance 1 (string field). */
    private BasicStepField field1 = null;
    
    /** Field instance 2 (integer field). */
    private StepField field2 = null;
    
    /** Builder example. */
    private MMObjectBuilder builder = null;
    
    /** FieldDefs example. */
    private FieldDefs fieldDefs = null;
    
    public BasicFieldValueConstraintTest(java.lang.String testName) {
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
        Step step = new BasicStep(builder);
        
        // Create instance 1 (string field).
        fieldDefs = builder.getField(FIELD_NAME1);
        field1 = new BasicStepField(step, fieldDefs);
        instance1 = new BasicFieldValueConstraint(field1, STRING_VALUE1);
        
        // Create instance 2 (integer field).
        fieldDefs = builder.getField(FIELD_NAME2);
        field2 = new BasicStepField(step, fieldDefs);
        instance2 = new BasicFieldValueConstraint(field2, INTEGER_VALUE1);        
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Test of setValue method, of class org.mmbase.storage.search.implementation.BasicFieldValueConstraint. */
    public void testSetValue() {
        assertTrue(instance1.getValue().equals(STRING_VALUE1));
        assertTrue(instance2.getValue().equals(INTEGER_VALUE1));
        
        try {
            // Null value, should throw IllegalArgumentException.
            instance1.setValue(null);
            fail("Null value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        try {
            // Null value, should throw IllegalArgumentException.
            instance2.setValue(null);
            fail("Null value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Integer value for string type, should throw IllegalArgumentException.
            instance1.setValue(INTEGER_VALUE1);
            fail("Integer value for string type, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        try {
            // String value for integer type, should throw IllegalArgumentException.
            instance2.setValue(STRING_VALUE1);
            fail("String value for integer type, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        assertTrue(!STRING_VALUE1.equals(STRING_VALUE2)); // Different test values!
        instance1.setValue(STRING_VALUE2);
        assertTrue(instance1.getValue().equals(STRING_VALUE2));
        assertTrue(!INTEGER_VALUE1.equals(INTEGER_VALUE2)); // Different test values!
        instance2.setValue(INTEGER_VALUE2);
        assertTrue(instance2.getValue().equals(INTEGER_VALUE2));
    }
    
    /** Test of getValue method, of class org.mmbase.storage.search.implementation.BasicFieldValueConstraint. */
    public void testGetValue() {
        // Same as:
        testSetValue();
    }
    
    /** Test of getBasicSupportLevel method. */
    public void testGetBasicSupportLevel() {
        // Returns SUPPORT_OPTIMAL.
        assertTrue(instance1.getBasicSupportLevel() == SearchQueryHandler.SUPPORT_OPTIMAL);
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicFieldValueConstraint. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicFieldValueConstraint. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicFieldValueConstraint. */
    public void testToString() {
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
        
         // Reverse inverse flag.
        instance1.setInverse(!instance1.isInverse());
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
        
        // Set field alias.
        field1.setAlias("yyuiwe");
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
        
        // Reverse case sensitive.
        instance1.setCaseSensitive(!instance1.isCaseSensitive());
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
        
        instance1.setValue(STRING_VALUE2);
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
        
        instance1.setOperator(FieldCompareConstraint.GREATER);
        assertTrue(instance1.toString(),
        instance1.toString().equals("FieldValueConstraint(inverse:"
        + instance1.isInverse() + ", field:"
        + instance1.getField().getAlias() + ", casesensitive:"
        + instance1.isCaseSensitive() + ", operator:"
        + instance1.getOperator() + ", value:"
        + instance1.getValue() + ")"));
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicFieldValueConstraintTest.class);
        
        return suite;
    }
    
}
