package org.mmbase.storage.search.implementation;

import junit.framework.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.FieldDefs;
import org.mmbase.storage.search.*;
import org.mmbase.module.corebuilders.*;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.1 $
 */
public class BasicFieldValueInConstraintTest extends TestCase {
    
    private final static String BUILDER_NAME = "images";
    private final static String STRING_FIELD_NAME = "title";
    private final static String INTEGER_FIELD_NAME = "number";
    
    private final static String STRING_TEST_VALUE1 = "kjjdf kjjkl";
    private final static String STRING_TEST_VALUE2 = " KLkljklj KJKLJ ERwe ";
    private final static Integer INTEGER_TEST_VALUE = new Integer(12345);
    private final static Long LONG_TEST_VALUE = new Long(12345);
    private final static Float FLOAT_TEST_VALUE2 = new Float(12345.1);

    /** Test instance. */
    private BasicFieldValueInConstraint instance = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /** Field instances. */
    private BasicStepField stringField = null;
    private StepField integerField = null;
    
    /** Builder example. */
    private MMObjectBuilder builder = null;
    
    /** FieldDefs examples. */
    private FieldDefs stringFieldDefs = null;
    private FieldDefs integerFieldDefs = null;
    
    public BasicFieldValueInConstraintTest(java.lang.String testName) {
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
        stringFieldDefs = builder.getField(STRING_FIELD_NAME);
        integerFieldDefs = builder.getField(INTEGER_FIELD_NAME);
        Step step = new BasicStep(builder);
        stringField = new BasicStepField(step, stringFieldDefs);
        integerField = new BasicStepField(step, integerFieldDefs);
        instance = new BasicFieldValueInConstraint(stringField);
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Test of addValue method, of class org.mmbase.storage.search.implementation.BasicFieldValueInConstraint. */
    public void testAddValue() {
        // Null value, should throw IllegalArgumentException
        try {
            instance.addValue(null);
            fail("Null value, should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
        
        // Trying to add integer value to string field, should throw IllegalArgumentException.
        try {
            instance.addValue(INTEGER_TEST_VALUE);
            fail("Trying to add integer value to string field, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        List values = new ArrayList(instance.getValues());
        assert(values.size() == 0);
        instance.addValue(STRING_TEST_VALUE1);
        values = new ArrayList(instance.getValues());
        assert(values.size() == 1);
        assert(values.indexOf(STRING_TEST_VALUE1) == 0);
        instance.addValue(STRING_TEST_VALUE2);
        values = new ArrayList(instance.getValues());
        assert(values.size() == 2);
        // Lexicographically ordering:
        assert(values.indexOf(STRING_TEST_VALUE1) == 1);
        assert(values.indexOf(STRING_TEST_VALUE2) == 0);
        
        // Trying to add string value to integer field, should throw IllegalArgumentException.
        BasicFieldValueInConstraint instance2 = new BasicFieldValueInConstraint(integerField);
        try {
            instance2.addValue("skdlw");
            fail("Trying to add string value to integer field, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        // Add integer value to integer field.
        values = new ArrayList(instance2.getValues());
        assert(values.size() == 0);
        instance2.addValue(INTEGER_TEST_VALUE);
        values = new ArrayList(instance2.getValues());
        assert(values.size() == 1);
        assert(values.indexOf(INTEGER_TEST_VALUE.toString()) == 0);
        // Same value, should not result in new element in values.
        instance2.addValue(LONG_TEST_VALUE);
        values = new ArrayList(instance2.getValues());
        assert(values.size() == 1);
        assert(values.indexOf(INTEGER_TEST_VALUE.toString()) == 0);
        assert(values.indexOf(LONG_TEST_VALUE.toString()) == 0);
        instance2.addValue(FLOAT_TEST_VALUE2);
        values = new ArrayList(instance2.getValues());
        assert(values.size() == 2);
        assert(values.indexOf(INTEGER_TEST_VALUE.toString()) == 0);
        assert(values.indexOf(FLOAT_TEST_VALUE2.toString()) == 1);
    }
    
    /** Test of getValues method, of class org.mmbase.storage.search.implementation.BasicFieldValueInConstraint. */
    public void testGetValues() {
        // See:
        testAddValue();
        
        Set values = instance.getValues();
        
        // List returned must be unmodifiable.
        try {
            values.add("ekowkdk kkj");
            fail("Attempt to modify list, must throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {}
        try {
            values.clear();
            fail("Attempt to modify list, must throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {}
   }
    
    /** Test of getBasicSupportLevel method. */
    public void testGetBasicSupportLevel() {
        // Returns SUPPORT_OPTIMAL.
        assert(instance.getBasicSupportLevel() == SearchQueryHandler.SUPPORT_OPTIMAL);
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicFieldValueInConstraint. */
    public void testEquals() {
        // TODO: implement test
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicFieldValueInConstraint. */
    public void testHashCode() {
        // TODO: implement test
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicFieldValueInConstraint. */
    public void testToString() {
        assert(instance.toString(),
        instance.toString().equals("FieldValueInConstraint(field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", values:"
        + instance.getValues() + ")"));
        
        // Set field alias.
        stringField.setAlias("yyuiwe");
        assert(instance.toString(),
        instance.toString().equals("FieldValueInConstraint(field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", values:"
        + instance.getValues() + ")"));
        
        // Reverse case sensitive.
        instance.setCaseSensitive(!instance.isCaseSensitive());
        assert(instance.toString(),
        instance.toString().equals("FieldValueInConstraint(field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", values:"
        + instance.getValues() + ")"));
        
        // Add values.
        instance.addValue(STRING_TEST_VALUE1);
        instance.addValue(STRING_TEST_VALUE2);
        assert(instance.toString(),
        instance.toString().equals("FieldValueInConstraint(field:"
        + instance.getField().getAlias() + ", casesensitive:"
        + instance.isCaseSensitive() + ", values:"
        + instance.getValues() + ")"));
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicFieldValueInConstraintTest.class);
        
        return suite;
    }
    
}
