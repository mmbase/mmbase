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
public class BasicCompareFieldsConstraintTest extends TestCase {
    
    private final static String BUILDER_NAME = "images";
    private final static String STRING_FIELD_NAME = "owner";
    private final static String INTEGER_FIELD_NAME = "number";
    private final static String BUILDER_NAME2 = "pools";
    private final static String STRING_FIELD_NAME2 = "owner";
    private final static String INTEGER_FIELD_NAME2 = "number";
    
    /** Test instance. */
    private BasicCompareFieldsConstraint instance = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /** String type Field instance. */
    private BasicStepField stringField = null;
    
    /** Integer type Field instance. */
    private StepField integerField = null;
    
    /** Builder example. */
    private MMObjectBuilder builder = null;
    
    /** FieldDefs example (string type). */
    private FieldDefs stringFieldDefs = null;
    
    /** FieldDefs example (integer type). */
    private FieldDefs integerFieldDefs = null;
    
    /** Second string type Field instance. */
    private BasicStepField stringField2 = null;
    
    /** Second integer type Field instance. */
    private StepField integerField2 = null;
    
    /** Second builder example. */
    private MMObjectBuilder builder2 = null;
    
    /** Second string FieldDefs example. */
    private FieldDefs stringFieldDefs2 = null;
    
    /** Second  integer FieldDefs example. */
    private FieldDefs integerFieldDefs2 = null;
    
    public BasicCompareFieldsConstraintTest(java.lang.String testName) {
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
        stringFieldDefs = builder.getField(STRING_FIELD_NAME);
        stringField = new BasicStepField(step, stringFieldDefs);
        integerFieldDefs = builder.getField(INTEGER_FIELD_NAME);
        integerField = new BasicStepField(step, integerFieldDefs);
        builder2 = mmbase.getBuilder(BUILDER_NAME2);
        Step step2 = new BasicStep(builder2);
        stringFieldDefs2 = builder2.getField(STRING_FIELD_NAME2);
        stringField2 = new BasicStepField(step2, stringFieldDefs2);
        integerFieldDefs2 = builder2.getField(INTEGER_FIELD_NAME2);
        integerField = new BasicStepField(step2, integerFieldDefs2);
        instance = new BasicCompareFieldsConstraint(stringField, stringField2);
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Tests constructor. */
    public void testConstructor() {
        try {
            // Null field2, should throw IllegalArgumentException.
            new BasicCompareFieldsConstraint(stringField, null);
            fail("Null field2, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Different field types, should throw IllegalArgumentException.
            new BasicCompareFieldsConstraint(stringField, integerField);
            fail("Different field types, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        try {
            // Different field types, should throw IllegalArgumentException.
            new BasicCompareFieldsConstraint(integerField, stringField2);
            fail("Different field types, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
            
    }
    
    /** Test of getField2 method, of class org.mmbase.storage.search.implementation.BasicCompareFieldsConstraint. */
    public void testGetField2() {
        assertTrue(instance.getField2() == stringField2);
    }
    
    /** Test of getBasicSupportLevel method. */
    public void testGetBasicSupportLevel() {
        // Returns SUPPORT_OPTIMAL.
        assertTrue(instance.getBasicSupportLevel() == SearchQueryHandler.SUPPORT_OPTIMAL);
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicCompareFieldsConstraint. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicCompareFieldsConstraint. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicCompareFieldsConstraint. */
    public void testToString() {
        assertTrue(instance.toString(),
        instance.toString().equals("CompareFieldsConstraint(inverse:"
        + instance.isInverse() + ", field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", operator:"
        + instance.getOperator() + ", field2:"
        + instance.getField2().getAlias() + ")"));
        
         // Reverse inverse flag.
        instance.setInverse(!instance.isInverse());
        assertTrue(instance.toString(),
        instance.toString().equals("CompareFieldsConstraint(inverse:"
        + instance.isInverse() + ", field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", operator:"
        + instance.getOperator() + ", field2:"
        + instance.getField2().getAlias() + ")"));
        
        // Set field alias.
        stringField.setAlias("yyuiwe");
        assertTrue(instance.toString(),
        instance.toString().equals("CompareFieldsConstraint(inverse:"
        + instance.isInverse() + ", field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", operator:"
        + instance.getOperator() + ", field2:"
        + instance.getField2().getAlias() + ")"));
       
        // Reverse case sensitive.
        instance.setCaseSensitive(!instance.isCaseSensitive());
        assertTrue(instance.toString(),
        instance.toString().equals("CompareFieldsConstraint(inverse:"
        + instance.isInverse() + ", field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", operator:"
        + instance.getOperator() + ", field2:"
        + instance.getField2().getAlias() + ")"));

        // Set second field alias.
        stringField2.setAlias("jwjidl");
        assertTrue(instance.toString(),
        instance.toString().equals("CompareFieldsConstraint(inverse:"
        + instance.isInverse() + ", field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", operator:"
        + instance.getOperator() + ", field2:"
        + instance.getField2().getAlias() + ")"));
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicCompareFieldsConstraintTest.class);
        
        return suite;
    }
    
}
