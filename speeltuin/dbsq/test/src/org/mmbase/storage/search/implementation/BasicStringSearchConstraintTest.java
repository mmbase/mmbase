package org.mmbase.storage.search.implementation;

import junit.framework.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.StringSearchConstraint;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.1 $
 */
public class BasicStringSearchConstraintTest extends TestCase {
    
    private final static String BUILDER_NAME = "images";
    private final static String FIELD_NAME1 = "title";
    private final static String FIELD_NAME2 = "number";
    private final static String SEARCHTERMS1 = "some search terms";
    
    /** Test instance. */
    private BasicStringSearchConstraint instance = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /** Field instance 1 (string field). */
    private BasicStepField field1 = null;
    
    /** Field instance 2 (integer field). */
    private StepField field2 = null;
    
    public BasicStringSearchConstraintTest(java.lang.String testName) {
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
        MMObjectBuilder builder = mmbase.getBuilder(BUILDER_NAME);
        Step step = new BasicStep(builder);
        
        // Field1 (string field).
        FieldDefs fieldDefs = builder.getField(FIELD_NAME1);
        field1 = new BasicStepField(step, fieldDefs);
        
        // Field2 (integer field).
        fieldDefs = builder.getField(FIELD_NAME2);
        field2 = new BasicStepField(step, fieldDefs);
        
        instance = new BasicStringSearchConstraint(field1,
        StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED,
        StringSearchConstraint.MATCH_TYPE_LITERAL, SEARCHTERMS1);
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {}
    
    /** Tests constructor (with searchterms as string). */
    public void testConstructor1() {
        List searchTerms
        = Arrays.asList(new String[] {"some", "search", "terms"});
        // Applied to integer field, should throw IllegalArgumentException.
        try {
            new BasicStringSearchConstraint(field2,
            StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED,
            StringSearchConstraint.MATCH_TYPE_LITERAL, searchTerms);
            fail("Applied to integer field, should throw IllegalArgumentException.");
        } catch(IllegalArgumentException e) {}
        
        // Empty searchterm string, should throw IllegalArgumentException.
        try {
            new BasicStringSearchConstraint(field1,
            StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED,
            StringSearchConstraint.MATCH_TYPE_LITERAL, new ArrayList());
            fail("Empty searchterm string, should throw IllegalArgumentException.");
        } catch(IllegalArgumentException e) {}
    }
    
    /** Tests constructor (with searchterms as list). */
    public void testConstructor2() {
        // Applied to integer field, should throw IllegalArgumentException.
        try {
            new BasicStringSearchConstraint(field2,
            StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED,
            StringSearchConstraint.MATCH_TYPE_LITERAL, SEARCHTERMS1);
            fail("Applied to integer field, should throw IllegalArgumentException.");
        } catch(IllegalArgumentException e) {}
        
        // Empty searchterm string, should throw IllegalArgumentException.
        try {
            new BasicStringSearchConstraint(field1,
            StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED,
            StringSearchConstraint.MATCH_TYPE_LITERAL, " ");
            fail("Empty searchterm string, should throw IllegalArgumentException.");
        } catch(IllegalArgumentException e) {}
    }
    
    /** Test of getBasicSupportLevel method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testGetBasicSupportLevel() {
        // Not supported.
        assert(
        instance.getBasicSupportLevel() == SearchQueryHandler.SUPPORT_NONE);
    }
    
    /** Test of setMatchType method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testSetMatchType() {
        try {
            // Invalid value, should throw IllegalArgumentException.
            instance.setMatchType(0);
            fail("Invalid value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid value, should throw IllegalArgumentException.
            instance.setMatchType(4);
            fail("Invalid value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_LITERAL);
        assert(
        instance.getMatchType() == StringSearchConstraint.MATCH_TYPE_LITERAL);
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_FUZZY);
        assert(
        instance.getMatchType() == StringSearchConstraint.MATCH_TYPE_FUZZY);
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_SYNONYM);
        assert(
        instance.getMatchType() == StringSearchConstraint.MATCH_TYPE_SYNONYM);
    }
    
    /** Test of setSearchType method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testSetSearchType() {
        try {
            // Invalid value, should throw IllegalArgumentException.
            instance.setSearchType(0);
            fail("Invalid value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid value, should throw IllegalArgumentException.
            instance.setSearchType(4);
            fail("Invalid value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED);
        assert( instance.getSearchType()
        == StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED);
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED);
        assert( instance.getSearchType()
        == StringSearchConstraint.SEARCH_TYPE_PHRASE_ORIENTED);
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED);
        assert( instance.getSearchType()
        == StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED);
    }
    
    public void testSetParameter() {
        try {
            // Invalid parameter name, should throw IllegalArgumentException.
            instance.setParameter("ongeldige parameter", new Integer(0));
            fail("Invalid parameter name, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid parameter type, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_FUZZINESS, new Integer(0));
            fail("Invalid parameter type, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid parameter type, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_PROXIMITY_LIMIT, new Float(1.0));
            fail("Invalid parameter type, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid parameter value, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_FUZZINESS, new Float(-1.0));
            fail("Invalid parameter value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid parameter value, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_FUZZINESS, new Float(1.1));
            fail("Invalid parameter value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid parameter value, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_PROXIMITY_LIMIT, new Integer(0));
            fail("Invalid parameter value, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        Float fuzziness = new Float(0.5);
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_LITERAL);
        
        // Due to matchtype set to LITERAL, fuzziness parameter is invalid.
        try {
            // Invalid matchtype, should throw IllegalArgumentException.
            instance.setParameter(StringSearchConstraint.PARAM_FUZZINESS, fuzziness);
            fail("Invalid matchtype, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        assert(instance.getParameters().get(StringSearchConstraint.PARAM_FUZZINESS)
        == null);
        
        // due to matchtype fuzziness parameter should be set
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_FUZZY);
        instance.setParameter(StringSearchConstraint.PARAM_FUZZINESS, fuzziness);
        assert(instance.getParameters().get(StringSearchConstraint.PARAM_FUZZINESS).
        equals(fuzziness));
        
        // changing matchtype should clear fuzziness parameter
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_LITERAL);
        assert(instance.getParameters().get(StringSearchConstraint.PARAM_FUZZINESS)
        == null);
        
        Integer proximityLimit = new Integer(2);
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED);
        
        // Due to searchtype WORD_ORIENTED, proximity limit parameter is invalid.
        try {
            // Invalid matchtype, should throw IllegalArgumentException.
            instance.setParameter(
            StringSearchConstraint.PARAM_PROXIMITY_LIMIT, proximityLimit);
            fail("Invalid matchtype, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        assert(instance.getParameters().get(StringSearchConstraint.PARAM_PROXIMITY_LIMIT)
        == null);
        
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED);
        instance.setParameter(
        StringSearchConstraint.PARAM_PROXIMITY_LIMIT, proximityLimit);
        assert(
        instance.getParameters().get(StringSearchConstraint.PARAM_PROXIMITY_LIMIT).
        equals(proximityLimit));
        
        // changing searchtype should clear proximity limit parameter
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_WORD_ORIENTED);
        assert(instance.getParameters().get(StringSearchConstraint.PARAM_PROXIMITY_LIMIT)
        == null);
    }
    
    /** Test of getMatchType method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testGetMatchType() {
        // same as:
        testSetMatchType();
    }
    
    /** Test of getSearchType method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testGetSearchType() {
        // same as:
        testSetSearchType();
    }
    
    /** Test of addSearchTerm method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testAddSearchTerm() {
        String newTerm = "skeukowkk";
        int nrTerms = instance.getSearchTerms().size();
        instance.addSearchTerm(newTerm);
        List searchTerms = instance.getSearchTerms();
        assert(searchTerms.size() == (nrTerms + 1));
        assert(searchTerms.get(nrTerms).equals(newTerm));
        
        try {
            // Empty searchterm, should throw IllegalArgumentException.
            instance.addSearchTerm("  ");
            fail("White space searchterm, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        try {
            // Empty searchterm, should throw IllegalArgumentException.
            instance.addSearchTerm("");
            fail("White space searchterm, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
    }
    
    /** Test of setSearchTerms(List) method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testSetSearchTerms() {
        List searchTerms = new ArrayList();
        searchTerms.add("kjeid");
        searchTerms.add("uerui");
        searchTerms.add("zcvvc");
        instance.setSearchTerms(searchTerms);
        assert(instance.getSearchTerms().equals(searchTerms));
        
        try {
            // Empty list of searchterms, should throw IllegalArgumentException.
            instance.setSearchTerms(new ArrayList());
            fail("Empty list of searchterms, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        searchTerms.add(new Integer(0));
        try {
            // Non-string searchterms, should throw IllegalArgumentException.
            instance.setSearchTerms(new ArrayList());
            fail("Non-string searchterms, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
    }
    
    /** Test of setSearchTerms(String) method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testSetSearchTerms2() {
        String searchTerm1 = "qwei";
        String searchTerm2 = "2838";
        String searchTerm3 = "i3wn";
        instance.setSearchTerms("\n\t\r " + searchTerm1
        + "\r" + searchTerm2 + "  " + searchTerm3 + " \n ");
        assert(instance.getSearchTerms().size() == 3);
        assert(instance.getSearchTerms().get(0).equals(searchTerm1));
        assert(instance.getSearchTerms().get(1).equals(searchTerm2));
        assert(instance.getSearchTerms().get(2).equals(searchTerm3));
        
        try {
            // Empty searchterm string, should throw IllegalArgumentException.
            instance.setSearchTerms("\n\r\t ");
            fail("Empty list of searchterms, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
    }
    
    /** Test of getSearchTerms method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testGetSearchTerms() {
        // same as:
        testSetSearchTerms();
        testSetSearchTerms2();
    }
    
    /** Test of getParameters method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testGetParameters() {
        // same as:
        testSetParameter();
        
        Map map = instance.getParameters();
        try {
            // Trying to modify map, should throw UnsupportedOperationException.
            map.put("kdj", "iiup");
            fail("Trying to modify map, should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {}
    }
    
    /** Test of equals method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testEquals() {
        // TODO: implement
    }
    
    /** Test of hashCode method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testHashCode() {
        // TODO: implement
    }
    
    /** Test of toString method, of class org.mmbase.storage.search.implementation.BasicStringSearchConstraint. */
    public void testToString() {
        instance.setCaseSensitive(true);
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));

         // Reverse inverse flag.
        instance.setInverse(!instance.isInverse());
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
        
        instance.setCaseSensitive(false);
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
        
        instance.setSearchType(StringSearchConstraint.SEARCH_TYPE_PROXIMITY_ORIENTED);
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
        
        instance.setMatchType(StringSearchConstraint.MATCH_TYPE_FUZZY);
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
        
        instance.setParameter(
        StringSearchConstraint.PARAM_FUZZINESS, new Float(0.5));
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
        
        instance.addSearchTerm("kdkdkdk");
        assert(instance.toString(), instance.toString().equals(
        "StringSearchConstraint(inverse:"
        + instance.isInverse() + ", field:" + instance.getField()
        + ", casesensitive:" + instance.isCaseSensitive()
        + ", searchtype:" + instance.getSearchType()
        + ", matchtype:" + instance.getMatchType()
        + ", parameters:" + instance.getParameters()
        + ", searchterms:" + instance.getSearchTerms() + ")"));
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BasicStringSearchConstraintTest.class);
        
        return suite;
    }
    
}
