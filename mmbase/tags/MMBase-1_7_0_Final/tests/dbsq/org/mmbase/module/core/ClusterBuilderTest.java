package org.mmbase.module.core;

import junit.framework.*;
import java.util.*;
import java.sql.*;
import org.mmbase.module.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.module.database.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * JUnit tests.
 *
 * @author Rob van Maris
 * @version $Revision: 1.2 $
 */
public class ClusterBuilderTest extends TestCase {
    
    /**
     * JUnit test user.
     * Nodes created by this user must be removed in the tearDown.
     */
    private final static String JUNIT_USER = "JUnitTester";
    
    /** Test instance. */
    private ClusterBuilder instance;
    
    /** Disallowed values map. */
    private Map disallowedValues = null;
    
    /** MMBase query. */
    private MMBase mmbase = null;
    
    /** Insrel builder. */
    private InsRel insrel = null;
    
    /** News builder, used as builder example. */
    private MMObjectBuilder news = null;
    
    /** People builder, used as builder example. */
    private MMObjectBuilder people = null;
    
    /** Insrel builder, used as relation builder example. */
    /** Test nodes, created in setUp, deleted in tearDown. */
    private List testNodes = new ArrayList();
    
    public ClusterBuilderTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
    
    /**
     * Sets up before each test.
     */
    public void setUp() throws Exception {
        MMBaseContext.init();
        mmbase = MMBase.getMMBase();
        insrel = mmbase.getInsRel();
        news = mmbase.getBuilder("news");
        people = mmbase.getBuilder("people");
        assertTrue(
            "Builder 'news' must be defined in order to run these tests.", 
            news != null);
        assertTrue(
            "Builder 'people' must be defined in order to run these tests.", 
            people != null);
        
        instance = mmbase.getClusterBuilder();
        
        // Add testnodes.
        MMObjectNode news1 = news.getNewNode(JUNIT_USER);
        news1.setValue("title", "_TE$T_1");
        news1.setValue("body", "News created for testing only.");
        news.insert(JUNIT_USER, news1);
        testNodes.add(news1);
        
        MMObjectNode news2 = news.getNewNode(JUNIT_USER);
        news2.setValue("title", "_TE$T_2");
        news2.setValue("body", "News created for testing only.");
        news.insert(JUNIT_USER, news2);
        testNodes.add(news2);
    }
    
    /**
     * Tears down after each test.
     */
    public void tearDown() throws Exception {
        // Remove all testnodes.
        Iterator iTestNodes = testNodes.iterator();
        while (iTestNodes.hasNext()) {
            MMObjectNode testNode = (MMObjectNode) iTestNodes.next();
            MMObjectBuilder builder = testNode.getBuilder();
            builder.removeRelations(testNode);
            builder.removeNode(testNode);
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(ClusterBuilderTest.class);
        
        return suite;
    }
    
    /** 
     * Test of TypeRel method getAllowedRelations. 
     * Tests for bug #5795 or similar bugs.
     */
    public void testGetAllowedRelations() {
       int newsNr = news.getObjectType();
       int peopleNr = people.getObjectType();
       int relatedNr = mmbase.getRelDef().getNumberByName("related");
       Enumeration eAllowedRelations = mmbase.getTypeRel().getAllowedRelations(newsNr, peopleNr);
       assertTrue(eAllowedRelations.hasMoreElements());
       MMObjectNode typeRelNode = (MMObjectNode) eAllowedRelations.nextElement();
       assertTrue(typeRelNode.toString(), 
         typeRelNode.getIntValue("rnumber") == relatedNr);
       eAllowedRelations = mmbase.getTypeRel().getAllowedRelations(peopleNr, newsNr);
       assertTrue("\nbug #5795 or a similar bug",  eAllowedRelations.hasMoreElements());
       typeRelNode = (MMObjectNode) eAllowedRelations.nextElement();
       assertTrue(typeRelNode.toString(), 
         typeRelNode.getIntValue("rnumber") == relatedNr);
    }
    
    /** Test of testGetClusterNodes method, of class org.mmbase.module.core.ClusterBuilder. */
    public void testGetClusterNodes() throws Exception {
        BasicSearchQuery query = new BasicSearchQuery();
        BasicStep newsStep = query.addStep(news)
            .setAlias("news1");
        FieldDefs newsTitle = news.getField("title");
        BasicStepField newsNameField = query.addField(newsStep, newsTitle)
            .setAlias("a_title"); // should not affect result node fieldnames!
        BasicSortOrder sortOrder = query.addSortOrder(newsNameField)
            .setDirection(SortOrder.ORDER_ASCENDING);
        FieldDefs newsBody = news.getField("body");
        query.addField(newsStep, newsBody);
        FieldDefs newsOwner = news.getField("owner");
        BasicStepField newsOwnerField = query.addField(newsStep, newsOwner);
        BasicFieldValueConstraint constraint
        = new BasicFieldValueConstraint(newsOwnerField, JUNIT_USER);
        query.setConstraint(constraint);
        List resultNodes = instance.getClusterNodes(query);
        Iterator iResultNodes = resultNodes.iterator();
        Iterator iTestNodes = testNodes.iterator();
        while (iTestNodes.hasNext()) {
            MMObjectNode testNode = (MMObjectNode) iTestNodes.next();
            assertTrue(iResultNodes.hasNext());
            MMObjectNode resultNode = (MMObjectNode) iResultNodes.next();
            assertTrue(resultNode instanceof ClusterNode);
            assertTrue(resultNode.getBuilder() == mmbase.getClusterBuilder());
            assertTrue(resultNode.getStringValue("news1.title") != null
            && resultNode.getStringValue("news1.title").length() > 0);
            assertTrue(resultNode.getStringValue("news1.title").equals(testNode.getStringValue("title")));
            assertTrue(resultNode.getStringValue("news1.body") != null
            && resultNode.getStringValue("news1.body").length() > 0);
            assertTrue(resultNode.getStringValue("news1.body").equals(testNode.getStringValue("body")));
            assertTrue(resultNode.getStringValue("news1.owner") != null
            && resultNode.getStringValue("news1.owner").length() > 0);
            assertTrue(resultNode.getStringValue("news1.owner").equals(testNode.getStringValue("owner")));
        }
        assertTrue(!iResultNodes.hasNext());
    }
    
    /** Test of getUniqueTableAlias() method, of class org.mmbase.module.core.ClusterBuilder. */
    public void testGetUniqueTableAlias() {
        List originalAliases = Arrays.asList(new Object[] {"test1", "test2"});
        Set tableAliases = new HashSet();
        String alias = instance.getUniqueTableAlias("test", tableAliases, originalAliases);
        assertTrue(alias.equals("test"));
        assertTrue(tableAliases.size() == 1);
        assertTrue(tableAliases.contains("test"));
        assertTrue(originalAliases.size() == 2);
        
        alias = instance.getUniqueTableAlias("test", tableAliases, originalAliases);
        assertTrue(alias.equals("test0"));
        assertTrue(tableAliases.size() == 2);
        assertTrue(tableAliases.contains("test0"));
        assertTrue(originalAliases.size() == 2);
        
        alias = instance.getUniqueTableAlias(
            "test1", tableAliases, originalAliases);
        assertTrue(alias.equals("test1"));
        assertTrue(tableAliases.size() == 3);
        assertTrue(tableAliases.contains("test1"));
        assertTrue(originalAliases.size() == 2);
        
        alias = instance.getUniqueTableAlias(
            "test", tableAliases, originalAliases);
        assertTrue(alias.equals("test3"));
        assertTrue(tableAliases.size() == 4);
        assertTrue(tableAliases.contains("test1"));
        assertTrue(originalAliases.size() == 2);
        
        for (int i = 4; i < 10; i++) {
            alias = instance.getUniqueTableAlias(
                "test" + (i - 1), tableAliases, originalAliases);
            assertTrue(alias, alias.equals("test" + i));
            assertTrue(tableAliases.size() == i + 1);
            assertTrue(tableAliases.contains("test" + i));
            assertTrue(originalAliases.size() == 2);
        }
        
        try {
            // Can't generate another unique value for this string, 
            // should throw IndexOutOfBoundsException.
            instance.getUniqueTableAlias("test", tableAliases, originalAliases);
            fail("Can't generate another unique value for this string, "
                + "should throw IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {}
        
        assertTrue(tableAliases.size() == 10);
        for (int i = 0; i < 10; i++) {
            if (i == 2) {
                assertTrue(!tableAliases.contains("test" + i));
            } else {
                assertTrue(tableAliases.contains("test" + i));
            }
        }
        assertTrue(tableAliases.contains("test"));
        
        // Alias containing white space.
        alias = instance.getUniqueTableAlias(
            "white space", tableAliases, originalAliases);
        assertTrue(alias, alias.equals("white space"));
        assertTrue(tableAliases.contains("white space"));
    }
    
    /** Test of getBuilder() method, of class org.mmbase.module.core.ClusterBuilder. */
    public void testGetBuilder() {
        // --- requires role "related" to be defined: ---
        Integer related = new Integer(
            mmbase.getRelDef().getNumberByName("related"));
        assertTrue("Role 'related' must be defined to run this test.", 
            related.intValue() != -1);

        Map nodes = new HashMap();
        assertTrue(instance.getBuilder("news", nodes).equals(news));
        assertTrue(nodes.size() == 0);
        
        assertTrue(instance.getBuilder("news0", nodes).equals(news));
        assertTrue(nodes.size() == 0);
        
        assertTrue(instance.getBuilder("news1", nodes).equals(news));
        assertTrue(nodes.size() == 0);
        
        assertTrue(instance.getBuilder("related", nodes).
            equals(mmbase.getInsRel()));
        assertTrue(nodes.size() == 1);
        assertTrue(nodes.get("related").equals(related));
        
        assertTrue(instance.getBuilder("related0", nodes).
            equals(mmbase.getInsRel()));
        assertTrue(nodes.size() == 2);
        assertTrue(nodes.get("related0").equals(related));
        
        try {
            // Unknown builder or role, should throw IllegalArgumentException.
            instance.getBuilder("xxxx", nodes);
            fail("Unknown builder or role, "
                + "should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
    }
    
    /** Test of addSteps() method, of class org.mmbase.module.core.ClusterBuilder. */
    public void testAddSteps() {
        // --- requires role "related" to be defined: ---
        int related = mmbase.getRelDef().getNumberByName("related");
        assertTrue("Role 'related' must be defined to run this test.", 
            related != -1);
        
        BasicSearchQuery query = new BasicSearchQuery();
        Map roles = new HashMap();
        Map fieldsByName = new HashMap();
        List tables = Arrays.asList(new Object[] {"news", "news1"});
        Map stepsByAlias = instance.addSteps(query, tables, roles, true, fieldsByName);
        // Test steps and returned map.
        assertTrue(stepsByAlias.size() == 3);
        List steps = query.getSteps();
        assertTrue(steps.size() == 3);
        Step step0 = (Step) steps.get(0);
        assertTrue(step0.getTableName().equals("news"));
        assertTrue(step0.getAlias().equals("news"));
        assertTrue(stepsByAlias.get("news") == step0);
        Step step1 = (RelationStep) steps.get(1);
        assertTrue(step1.getTableName().equals("insrel"));
        assertTrue(step1.getAlias().startsWith("insrel"));
        assertTrue(stepsByAlias.get("insrel") == step1);
        Step step2 = (Step) steps.get(2);
        assertTrue(step2.getTableName().equals("news"));
        assertTrue(step2.getAlias().equals("news1"));
        assertTrue(stepsByAlias.get("news1") == step2);
        // Test (number)fields and fields map.
        assertTrue(fieldsByName.toString(), fieldsByName.size() == 2);
        List fields = query.getFields();
        assertTrue(fields.size() == 2);
        StepField field0 = (StepField) fields.get(0);
        assertTrue(field0.getStep() == step0);
        assertTrue(field0.getFieldName().equals("number"));
        assertTrue(fieldsByName.get("news.number") == field0);
        StepField field1 = (StepField) fields.get(1);
        assertTrue(field1.getStep() == step2);
        assertTrue(field1.getFieldName().equals("number"));
        assertTrue(fieldsByName.get("news1.number") == field1);
        // Test roles.
        assertTrue(roles.size() == 0);
        
        query = new BasicSearchQuery();
        roles.clear();
        fieldsByName.clear();
        tables = Arrays.asList(new Object[] {"news", "news1"});
        stepsByAlias = instance.addSteps(query, tables, roles, false, fieldsByName);
        // Test steps and returned map.
        assertTrue(stepsByAlias.size() == 3);
        steps = query.getSteps();
        assertTrue(steps.size() == 3);
        step0 = (Step) steps.get(0);
        assertTrue(step0.getTableName().equals("news"));
        assertTrue(step0.getAlias().equals("news"));
        assertTrue(stepsByAlias.get("news") == step0);
        step1 = (RelationStep) steps.get(1);
        assertTrue(step1.getTableName().equals("insrel"));
        assertTrue(step1.getAlias().startsWith("insrel"));
        assertTrue(stepsByAlias.get("insrel") == step1);
        step2 = (Step) steps.get(2);
        assertTrue(step2.getTableName().equals("news"));
        assertTrue(step2.getAlias().equals("news1"));
        assertTrue(stepsByAlias.get("news1") == step2);
        // Test (number)fields and fields map.
        assertTrue(fieldsByName.size() == 0);
        fields = query.getFields();
        assertTrue(fields.size() == 0);
        // Test roles.
        assertTrue(roles.size() == 0);
        
        query = new BasicSearchQuery();
        roles.clear();
        fieldsByName.clear();
        tables = Arrays.asList(new Object[] {"news", "related", "news1"});
        stepsByAlias = instance.addSteps(query, tables, roles, true, fieldsByName);
        // Test steps and returned map.
        assertTrue(stepsByAlias.size() == 3);
        steps = query.getSteps();
        assertTrue(steps.size() == 3);
        step0 = (Step) steps.get(0);
        assertTrue(step0.getTableName().equals("news"));
        assertTrue(step0.getAlias().equals("news"));
        assertTrue(stepsByAlias.get("news") == step0);
        step1 = (RelationStep) steps.get(1);
        assertTrue(step1.getTableName().equals("insrel"));
        assertTrue(step1.getAlias().equals("related"));
        assertTrue(stepsByAlias.get("related") == step1);
        step2 = (Step) steps.get(2);
        assertTrue(step2.getTableName().equals("news"));
        assertTrue(step2.getAlias().equals("news1"));
        assertTrue(stepsByAlias.get("news1") == step2);
        // Test (number)fields and fields map.
        assertTrue(fieldsByName.size() == 3);
        fields = query.getFields();
        assertTrue(fields.size() == 3);
        field0 = (StepField) fields.get(0);
        assertTrue(field0.getStep() == step0);
        assertTrue(field0.getFieldName().equals("number"));
        assertTrue(fieldsByName.get("news.number") == field0);
        field1 = (StepField) fields.get(1);
        assertTrue(field1.getStep() == step1);
        assertTrue(field1.getFieldName().equals("number"));
        assertTrue(fieldsByName.get("related.number") == field1);
        StepField field2 = (StepField) fields.get(2);
        assertTrue(field2.getStep() == step2);
        assertTrue(field2.getFieldName().equals("number"));
        assertTrue(fieldsByName.get("news1.number") == field2);
        // Test roles.
        assertTrue(roles.size() == 1);
        Integer number = (Integer) roles.get("related");
        assertTrue(number.intValue() == related);
        
        query = new BasicSearchQuery();
        roles.clear();
        fieldsByName.clear();
        tables = Arrays.asList(
            new Object[] {"news", "related", "news1", "related", "news"});
        stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        // Test steps and returned map.
        assertTrue(stepsByAlias.size() == 5);
        steps = query.getSteps();
        assertTrue(steps.size() == 5);
        step0 = (Step) steps.get(0);
        assertTrue(step0.getTableName().equals("news"));
        assertTrue(step0.getAlias().equals("news"));
        assertTrue(stepsByAlias.get("news") == step0);
        step1 = (RelationStep) steps.get(1);
        assertTrue(step1.getTableName().equals("insrel"));
        assertTrue(step1.getAlias().equals("related"));
        assertTrue(stepsByAlias.get("related") == step1);
        step2 = (Step) steps.get(2);
        assertTrue(step2.getTableName().equals("news"));
        assertTrue(step2.getAlias().equals("news1"));
        assertTrue(stepsByAlias.get("news1") == step2);
        Step step3 = (Step) steps.get(3);
        assertTrue(step3.getTableName().equals("insrel"));
        assertTrue(step3.getAlias().equals("related0"));
        assertTrue(stepsByAlias.get("related0") == step3);
        Step step4 = (Step) steps.get(4);
        assertTrue(step4.getTableName().equals("news"));
        assertTrue(step4.getAlias().equals("news0"));
        assertTrue(stepsByAlias.get("news0") == step4);
        // Test (number)fields and field map.
        assertTrue(fieldsByName.size() == 3);
        fields = query.getFields();
        assertTrue(fields.toString(), fields.size() == 3);
        for (int i = 0; i < 3; i++) {
            StepField field = (StepField) fields.get(i);
            assertTrue(field.getStep() == steps.get(i));
            assertTrue(field.getFieldName().equals("number"));
            assertTrue(fieldsByName.get(tables.get(i) + ".number") == field);
        }
        // Test roles.
        assertTrue(roles.size() == 1);
        number = (Integer) roles.get("related");
        assertTrue(number.intValue() == related);
    }

    public void testAddFields() {
        BasicSearchQuery query = new BasicSearchQuery();
        Map roles = new HashMap();
        Map fieldsByName = new HashMap();
        List tables = Arrays.asList(
            new Object[] {"news", "related", "people", "news1"});
        Map stepsByAlias = instance.addSteps(query, tables, roles, true, fieldsByName);
        assertTrue(query.getFields().size() == 4);
        assertTrue(fieldsByName.size() == 4);

        instance.addFields(query, "news.title", stepsByAlias, fieldsByName);
        
        StepField stepField = getField(query, "news", "title");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("news.title")));
        assertTrue(stepField.getAlias() == null);
        assertTrue(query.getFields().size() == 5);
        assertTrue(fieldsByName.size() == 5);

        instance.addFields(query, "related.number", stepsByAlias, fieldsByName);
        stepField = getField(query, "related", "number");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("related.number")));
        assertTrue(stepField.getAlias() == null);
        // Not added twice.
        assertTrue(query.getFields().size() == 5);
        assertTrue(fieldsByName.size() == 5);

        instance.addFields(query, "people.firstname", stepsByAlias, fieldsByName);
        stepField = getField(query, "people", "firstname");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("people.firstname")));
        assertTrue(stepField.getAlias() == null);
        assertTrue(query.getFields().size() == 6);
        assertTrue(fieldsByName.size() == 6);

        instance.addFields(query, "news1.title", stepsByAlias, fieldsByName);
        stepField = getField(query, "news1", "title");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("news1.title")));
        assertTrue(stepField.getAlias() == null);
        assertTrue(query.getFields().size() == 7);
        assertTrue(fieldsByName.size() == 7);
        
        try {
            // Invalid expression, should throw IllegalArgumentException.
            instance.addFields(query, "newsnumber", stepsByAlias, fieldsByName);
            fail("Invalid expression, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid expression, should throw IllegalArgumentException.
            instance.addFields(query, ".newsnumber", stepsByAlias, fieldsByName);
            fail("Invalid expression, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Invalid expression, should throw IllegalArgumentException.
            instance.addFields(query, "newsnumber.", stepsByAlias, fieldsByName);
            fail("Invalid expression, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Unknown table alias, should throw IllegalArgumentException.
            instance.addFields(query, "xxx.number", stepsByAlias, fieldsByName);
            fail("Unknown table alias, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        try {
            // Unknown field name, should throw IllegalArgumentException.
            instance.addFields(query, "news.xxx", stepsByAlias, fieldsByName);
            fail("Unknown table alias, should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}
        
        instance.addFields(query, "f1(news.body,news1.body)", stepsByAlias, fieldsByName);
        stepField = getField(query, "news", "body");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("news.body")));
        assertTrue(stepField.getAlias() == null);
        stepField = getField(query, "news1", "body");
        assertTrue(stepField != null);
        assertTrue(stepField.equals(fieldsByName.get("news1.body")));
        assertTrue(query.getFields().size() == 9);
        assertTrue(fieldsByName.size() == 9);
    }
    
    /** Test of addRelationDirections() method, of class org.mmbase.module.core.ClusterBuilder. */
    public void testAddRelationDirections() {
        // --- requires role "related" to be defined: ---
        int related = mmbase.getRelDef().getNumberByName("related");
        assertTrue("Role 'related' must be defined to run this test.", 
            related != -1);
        // --- requires typerel to be defined for 
        //     source, role, destination = "news", "related", "people": ---
        assertTrue("This (bidirectional) relation-type must be defined to run "
            + "this test: source, role, destination = "
            + "'news', 'related', 'people'",
            mmbase.getTypeRel().reldefCorrect(
                mmbase.getTypeDef().getIntValue("news"), 
                mmbase.getTypeDef().getIntValue("people"), related));
        // --- requires relations to define a 'dir' field --
        assertTrue("Relations must define a 'dir' field to run this test.",
            insrel.usesdir);

        BasicSearchQuery query = new BasicSearchQuery();
        Map roles = new HashMap();
        Map fieldsByName = new HashMap();
        List tables = Arrays.asList(
            new Object[] {"news", "related", "people", "insrel", "news0"});
        Map stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        instance.addRelationDirections(query, ClusterBuilder.SEARCH_ALL, roles);
        RelationStep relation1 = (RelationStep) stepsByAlias.get("related");
        assertTrue(relation1.getDirectionality() 
            == RelationStep.DIRECTIONS_DESTINATION);
        assertTrue(!relation1.getCheckedDirectionality());
        RelationStep relation2 = (RelationStep) stepsByAlias.get("insrel");
        assertTrue(
            relation2.getDirectionality() == RelationStep.DIRECTIONS_SOURCE);
        assertTrue(!relation2.getCheckedDirectionality());
        
        query = new BasicSearchQuery();
        stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        instance.addRelationDirections(
            query, ClusterBuilder.SEARCH_BOTH, roles);
        relation1 = (RelationStep) stepsByAlias.get("related");
        assertTrue(relation1.getDirectionality() 
            == RelationStep.DIRECTIONS_DESTINATION);
        assertTrue(relation1.getCheckedDirectionality());
        relation2 = (RelationStep) stepsByAlias.get("insrel");
        assertTrue(relation2.getDirectionality()
            == RelationStep.DIRECTIONS_SOURCE);
        assertTrue(relation2.getCheckedDirectionality());
        
        query = new BasicSearchQuery();
        stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        instance.addRelationDirections(
            query, ClusterBuilder.SEARCH_EITHER, roles);
        relation1 = (RelationStep) stepsByAlias.get("related");
        assertTrue(relation1.getDirectionality() 
            == RelationStep.DIRECTIONS_DESTINATION);
        assertTrue(relation1.getCheckedDirectionality());
        relation2 = (RelationStep) stepsByAlias.get("insrel");
        assertTrue(relation2.getDirectionality()
            == RelationStep.DIRECTIONS_SOURCE);
        assertTrue(relation2.getCheckedDirectionality());
        
        query = new BasicSearchQuery();
        stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        instance.addRelationDirections(
            query, ClusterBuilder.SEARCH_DESTINATION, roles);
        relation1 = (RelationStep) stepsByAlias.get("related");
        assertTrue(relation1.getDirectionality() 
            == RelationStep.DIRECTIONS_DESTINATION);
        assertTrue(relation1.getCheckedDirectionality());
        relation2 = (RelationStep) stepsByAlias.get("insrel");
        assertTrue(relation2.getDirectionality()
            == RelationStep.DIRECTIONS_DESTINATION);
        assertTrue(relation2.getCheckedDirectionality());
        
        query = new BasicSearchQuery();
        stepsByAlias 
            = instance.addSteps(query, tables, roles, true, fieldsByName);
        instance.addRelationDirections(
            query, ClusterBuilder.SEARCH_SOURCE, roles);
        relation1 = (RelationStep) stepsByAlias.get("related");
        assertTrue(relation1.getDirectionality() 
            == RelationStep.DIRECTIONS_SOURCE);
        assertTrue(relation1.getCheckedDirectionality());
        relation2 = (RelationStep) stepsByAlias.get("insrel");
        assertTrue(relation2.getDirectionality()
            == RelationStep.DIRECTIONS_SOURCE);
        assertTrue(relation2.getCheckedDirectionality());
    }
    
    public void testAddSortOrders() {
        BasicSearchQuery query = new BasicSearchQuery();
        Map roles = new HashMap();
        Map fieldsByName = new HashMap();
        List tables = Arrays.asList(
            new Object[] {"news", "related", "people", "news1"});
        Map stepsByAlias = instance.addSteps(query, tables, roles, true, fieldsByName);
        Vector fieldNames = new Vector(Arrays.asList(
            new Object[] {"news.number"}));
        Vector directions = new Vector();
        instance.addSortOrders(query, fieldNames, directions, fieldsByName);
        List sortOrders = query.getSortOrders();
        assertTrue(sortOrders.size() == 1);
        SortOrder sortOrder0 = (SortOrder) sortOrders.get(0);
        assertTrue(sortOrder0.getField() 
            == (StepField) fieldsByName.get("news.number"));
        assertTrue(sortOrder0.getDirection() == SortOrder.ORDER_ASCENDING);
        
        query = new BasicSearchQuery();
        stepsByAlias = instance.addSteps(query, tables, roles, true, fieldsByName);
        fieldNames = new Vector(Arrays.asList(
            new Object[] {"news.number", "related.number", "people.firstname"}));
        directions = new Vector(Arrays.asList(
            new Object[] {"DOWN", "UP"}));
        instance.addSortOrders(query, fieldNames, directions, fieldsByName);
        sortOrders = query.getSortOrders();
        assertTrue(sortOrders.size() == 3);
        sortOrder0 = (SortOrder) sortOrders.get(0);
        assertTrue(sortOrder0.getField() 
            == (StepField) fieldsByName.get("news.number"));
        assertTrue(sortOrder0.getDirection() == SortOrder.ORDER_DESCENDING);
        SortOrder sortOrder1 = (SortOrder) sortOrders.get(1);
        assertTrue(sortOrder1.getField() 
            == (StepField) fieldsByName.get("related.number"));
        assertTrue(sortOrder1.getDirection() == SortOrder.ORDER_ASCENDING);
        
        // sorOrder2 is on a field that has not been added.
        SortOrder sortOrder2 = (SortOrder) sortOrders.get(2);
        StepField sortField2 = sortOrder2.getField();
        assertTrue(sortField2.getAlias() == null);
        assertTrue(sortOrder2.getDirection() == SortOrder.ORDER_DESCENDING);
    }
    
    public void testGetNodesStep() throws Exception {
        // Get number of a news node.
        List newsNodes = news.getNodes(new NodeSearchQuery(news));
        MMObjectNode newsNode = (MMObjectNode) newsNodes.get(0);
        int nodeNumber = newsNode.getNumber();
        
        BasicSearchQuery query = new BasicSearchQuery();
        BasicStep objectStep = query.addStep(mmbase.getBuilder("object"));
        
        assertTrue(
            instance.getNodesStep(query.getSteps(), -1) == null);
        
        // Find step for parentbuilder.
        assertTrue(
            instance.getNodesStep(query.getSteps(), nodeNumber) == objectStep);

        BasicStep insrelStep = query.addStep(mmbase.getInsRel());
        BasicStep newsStep = query.addStep(mmbase.getBuilder("news"));
        
        // Find step for builder.
        assertTrue(
            instance.getNodesStep(query.getSteps(), nodeNumber) == newsStep);
    }
    
    public void testGetMultiLevelSearchQuery() {
        
        List snodes = new ArrayList();
        Iterator iTestNodes = testNodes.iterator();
        while (iTestNodes.hasNext()) {
            MMObjectNode testNode = (MMObjectNode) iTestNodes.next();
            snodes.add(testNode.getIntegerValue("number").toString());
        }
        List fields = Arrays.asList(
            new String[] {"news.title", "people.number"});
        String pdistinct = "YES";
        List tables = Arrays.asList(
            new String[] {"people", "insrel", "news"});
        String where = "where lower(people.firstname) like '%test%'";
        List order = Arrays.asList(
            new String[] {"people.number", "news.title"});
        List directions = Arrays.asList(
            new String[] {"DOWN", "UP"});
        int searchDir = ClusterBuilder.SEARCH_BOTH;
            
        SearchQuery query = instance.getMultiLevelSearchQuery(
            snodes, fields, pdistinct, tables, where, order, 
            directions, searchDir);
        
        // Test steps.
        List steps = query.getSteps();
        assertTrue(steps.size() == 3);
        Step step0 = (Step) steps.get(0);
        assertTrue(step0.getTableName().equals("people"));
        RelationStep step1 = (RelationStep) steps.get(1);
        assertTrue(step1.getTableName().equals("insrel"));
        assertTrue(step1.getRole() == null);
        assertTrue(step1.getCheckedDirectionality());
        Step step2 = (Step) steps.get(2);
        assertTrue(step2.getTableName().equals("news"));
        
        // Test nodes.
        assertTrue(snodes.size() > 0); // For test to succeed.
        assertTrue(step0.getNodes().size() == 0);
        assertTrue(step1.getNodes().size() == 0);
        assertTrue(step2.getNodes().size() == snodes.size());
        Iterator iSNodes = snodes.iterator();
        Iterator iNodes = step2.getNodes().iterator();
        while (iNodes.hasNext()) {
            Integer node = (Integer) iNodes.next();
            String snode = (String) iSNodes.next();
            assertTrue(node.toString() + " " + snode, node.toString().equals(snode));
        }
        
        // Test distinct
        assertTrue(query.isDistinct());
        
        // Test fields.
        List stepFields = query.getFields();
        assertTrue(stepFields.size() == 2);
        StepField field0 = (StepField) stepFields.get(0);
        assertTrue(field0.getStep().getTableName().equals("news"));
        assertTrue(field0.getFieldName().equals("title"));
        StepField field1 = (StepField) stepFields.get(1);
        assertTrue(field1.getStep().getTableName().equals("people"));
        assertTrue(field1.getFieldName().equals("number"));
        
        // Test sortorders.
        List sortOrders = query.getSortOrders();
        assertTrue(sortOrders.size() == 2);
        SortOrder sortOrder0 = (SortOrder) sortOrders.get(0);
        assertTrue(sortOrder0.getField() == field1);
        assertTrue(sortOrder0.getDirection() == SortOrder.ORDER_DESCENDING);
        SortOrder sortOrder1 = (SortOrder) sortOrders.get(1);
        assertTrue(sortOrder1.getField() == field0);
        assertTrue(sortOrder1.getDirection() == SortOrder.ORDER_ASCENDING);
        
        // Test constraint.
        FieldValueConstraint constraint 
            = (FieldValueConstraint) query.getConstraint();
        assertTrue(constraint.getField().getStep() == step0);
        assertTrue(constraint.getField().getFieldName().equals("firstname"));
        assertTrue(constraint.getOperator() == FieldCompareConstraint.LIKE);
        assertTrue(!constraint.isCaseSensitive());
        assertTrue(constraint.getValue().equals("%test%"));
        assertTrue(!constraint.isInverse());
    }
    
    /**
     * Gets field in query.
     *
     * @param query The query.
     * @param stepAlias The alias of the step.
     * @param fieldName The name of the field.
     * @return The field if the field is present in the query, null otherwise.
     */
    private BasicStepField getField(
            SearchQuery query, String stepAlias, String fieldName) {
        BasicStepField result = null;
        Iterator iFields = query.getFields().iterator();
        while (iFields.hasNext()) {
            BasicStepField field = (BasicStepField) iFields.next();
            if (field.getStep().getAlias().equals(stepAlias)
            && field.getFieldName().equals(fieldName)) {
                // Found.
                result = field;
                break;
            }
        }
        return result;
    }

}
