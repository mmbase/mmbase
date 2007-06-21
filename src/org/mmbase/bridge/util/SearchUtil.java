/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;


public class SearchUtil {

    public static final String SOURCE = "SOURCE";
    public static final String DESTINATION = "DESTINATION";

    private SearchUtil() {
        // Utility
    }

    public static Node findNode(Cloud cloud, String managerName, String fieldname, String value) {
        return findNode(cloud, managerName, fieldname, value, null, null); 
    }

    public static Node findNode(Cloud cloud, String managerName, String fieldname, String value, String sortName) {
        return findNode(cloud, managerName, fieldname, value, sortName, null); 
    }

    public static Node findOrderedNode(Cloud cloud, String managerName, String sortName) {
        return findNode(cloud, managerName, null, null, sortName, null); 
    }

    public static Node findOrderedNode(Cloud cloud, String managerName, String sortName, String sortDirection) {
        return findNode(cloud, managerName, null, null, sortName, sortDirection); 
    }
    
    public static Node findNode(Cloud cloud, String managerName, String fieldname, String value, String sortName, String sortDirection) {
        NodeList list = findNodeList(cloud, managerName, fieldname, value, sortName, sortDirection); 
        if (list.size() > 0) {
            return list.getNode(0); 
        }
        return null;
    }

    public static NodeList findNodeList(Cloud cloud, String managerName) {
        return findNodeList(cloud, managerName, null, null, null, null);
    }
    
    public static NodeList findNodeList(Cloud cloud, String managerName, String fieldname, Object value) {
        return findNodeList(cloud, managerName, fieldname, value, null, null);
    }

    public static NodeList findNodeList(Cloud cloud, String managerName, String fieldname, Object value, String sortName) {
        return findNodeList(cloud, managerName, fieldname, value, sortName, null); 
    }

    public static NodeList findOrderedNodeList(Cloud cloud, String managerName, String sortName) {
        return findNodeList(cloud, managerName, null, null, sortName, null);
    }

    public static NodeList findOrderedNodeList(Cloud cloud, String managerName, String sortName, String sortDirection) {
        return findNodeList(cloud, managerName, null, null, sortName, sortDirection);
    }
    
    public static NodeList findNodeList(Cloud cloud, String managerName, String fieldname, Object value, String sortName, String sortDirection) {
        NodeManager manager = cloud.getNodeManager(managerName);
        NodeQuery query = manager.createQuery();
        addEqualConstraint(query, manager, fieldname, value);
        if (!isEmptyOrWhitespace(sortName)) {
            addSortOrder(query, manager, sortName, sortDirection);
        }
        return manager.getList(query);
    }

    public static Node findRelatedNode(Node parent, String managerName, String role) {
        return findRelatedNode(parent, managerName, role, null, null, null, null); 
    }
    
    public static Node findRelatedNode(Node parent, String managerName, String role, String fieldname, Object value) {
        return findRelatedNode(parent, managerName, role, fieldname, value, null, null); 
    }

    public static Node findRelatedNode(Node parent, String managerName, String role, String fieldname, Object value, String sortName) {
        return findRelatedNode(parent, managerName, role, fieldname, value, sortName, null); 
    }

    public static Node findRelatedOrderedNode(Node parent, String managerName, String role, String sortName) {
        return findRelatedNode(parent, managerName, role, null, null, sortName, null); 
    }

    public static Node findRelatedOrderedNode(Node parent, String managerName, String role, String sortName, String sortDirection) {
        return findRelatedNode(parent, managerName, role, null, null, sortName, sortDirection); 
    }
    
    public static Node findRelatedNode(Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection) {
        NodeList list = findRelatedNodeList(parent, managerName, role, fieldname, value, sortName, sortDirection); 
        if (list.size() > 0) {
            return list.getNode(0); 
        }
        return null;
    }

    public static NodeList findRelatedNodeList(Node parent, String managerName, String role) {
        return findRelatedNodeList(parent, managerName, role, null, null, null, null); 
    }
    
    public static NodeList findRelatedNodeList(Node parent, String managerName, String role, String fieldname, Object value) {
        return findRelatedNodeList(parent, managerName, role, fieldname, value, null, null); 
    }

    public static NodeList findRelatedNodeList(Node parent, String managerName, String role, String fieldname, Object value, String sortName) {
        return findRelatedNodeList(parent, managerName, role, fieldname, value, sortName, null); 
    }

    public static NodeList findRelatedOrderedNodeList(Node parent, String managerName, String role, String sortName) {
        return findRelatedNodeList(parent, managerName, role, null, null, sortName, null); 
    }
    
    public static NodeList findRelatedOrderedNodeList(Node parent, String managerName, String role, String sortName, String sortDirection) {
        return findRelatedNodeList(parent, managerName, role, null, null, sortName, sortDirection); 
    }
    
    public static NodeList findRelatedNodeList(Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection) {
        NodeQuery query = createRelatedNodeListQuery(parent, managerName, role, fieldname, value, sortName, sortDirection);
        return query.getNodeManager().getList(query);
    }

    public static NodeList findRelatedNodeList(Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection, String searchdir) {
        NodeQuery query = createRelatedNodeListQuery(parent, managerName, role, fieldname, value, sortName, sortDirection, searchdir);
        return query.getNodeManager().getList(query);
    }
    
    public static NodeQuery createRelatedNodeListQuery(Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection) {
        return createRelatedNodeListQuery(parent, managerName, role, fieldname, value, sortName, sortDirection, DESTINATION);
    }

    public static NodeQuery createRelatedNodeListQuery(Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection, String searchdir) {
        NodeQuery query = createRelatedNodeListQuery(parent, managerName, role, searchdir);
        addFeatures(query, parent, managerName, role, fieldname, value, sortName, sortDirection, searchdir);
        return query;
    }

    public static NodeQuery createRelatedNodeListQuery(Node parent, String managerName, String role) {
        return createRelatedNodeListQuery(parent, managerName, role, DESTINATION);
    }

    public static NodeQuery createRelatedNodeListQuery(Node parent, String managerName, String role, String searchdir) {
        NodeManager manager = parent.getCloud().getNodeManager(managerName);

        NodeQuery query = parent.getCloud().createNodeQuery();
        Step step1 = query.addStep(parent.getNodeManager());
        query.addNode(step1, parent);

        RelationStep step2 = query.addRelationStep(manager, role, searchdir);
        Step step3 = step2.getNext();
        query.setNodeStep(step3); // makes it ready for use as NodeQuery
        return query;
    }

    public static NodeQuery createRelatedNodeListQuery(NodeList parentNodes, String managerName, String role) {
        return createRelatedNodeListQuery(parentNodes, managerName, role, DESTINATION);
    }
    
    public static NodeQuery createRelatedNodeListQuery(NodeList parentNodes, String managerName, String role, String searchdir) {
        if (parentNodes.isEmpty()) {
            throw new IllegalArgumentException("paretnodes is empty. should be at leat one");
        }
        Node parent = parentNodes.getNode(0);
        
        NodeManager manager = parent.getCloud().getNodeManager(managerName);

        NodeQuery query = parent.getCloud().createNodeQuery();
        Step step1 = query.addStep(parent.getNodeManager());
        for (Object element : parentNodes) {
            Node parentNode = (Node) element;
            query.addNode(step1, parentNode);
        }

        RelationStep step2 = query.addRelationStep(manager, role, searchdir);
        Step step3 = step2.getNext();
        query.setNodeStep(step3); // makes it ready for use as NodeQuery
        return query;
    }
    
    public static RelationList findRelations(Node parent, String managerName, String role, String sortName, String sortDirection) {
        return findRelations(parent, managerName, role, sortName, sortDirection, DESTINATION);
    }

    public static RelationList findRelations(Node parent, String managerName, String role, String sortName, String sortDirection, String searchdir) {
        NodeManager manager = parent.getCloud().getNodeManager(managerName);
        NodeQuery query = parent.getCloud().createNodeQuery();
        Step step1 = query.addStep(parent.getNodeManager());
        query.addNode(step1, parent);
        RelationStep step = query.addRelationStep(manager, role, searchdir);
        query.setNodeStep(step);

        if (!isEmptyOrWhitespace(sortName)) {
            RelationManager relationManager = parent.getCloud().getRelationManager(parent.getNodeManager(), manager, role);
            addRelationSortOrder(query, relationManager, sortName, sortDirection);
        }

        NodeManager nm = query.getNodeManager();
        return new CollectionRelationList(nm.getList(query), parent.getCloud());
    }


    /**
     * Finds the relation-nodes between two speficied nodes
     * @param source 
     * @param destination
     * @param role
     * @since MMBase-1.8.5
     */
    public static RelationList findRelations(Node source, Node destination, String role, String searchDir) {
        
        Cloud cloud = source.getCloud();
        RelationManager relationManager = 
            role == null ?
            cloud.getNodeManager("insrel").toRelationManager() :
            cloud.getRelationManager(source.getNodeManager(), destination.getNodeManager(), role);
        
        NodeQuery q = relationManager.createQuery();
        if ("destination".equals(searchDir)) {
            Queries.addConstraint(q, Queries.createConstraint(q, "snumber", FieldCompareConstraint.EQUAL, source));
            Queries.addConstraint(q, Queries.createConstraint(q, "dnumber", FieldCompareConstraint.EQUAL, destination));
        } else if ("source".equals(searchDir)) {
            Queries.addConstraint(q, Queries.createConstraint(q, "dnumber", FieldCompareConstraint.EQUAL, source));
            Queries.addConstraint(q, Queries.createConstraint(q, "snumber", FieldCompareConstraint.EQUAL, destination));
        } else {
            Queries.addConstraint(q, Queries.createConstraint(q, "snumber", FieldCompareConstraint.EQUAL, source));
            Queries.addConstraint(q, Queries.createConstraint(q, "dnumber", FieldCompareConstraint.EQUAL, destination));
            if (Queries.count(q) == 0) { 
                RelationManager relationManager2 = 
                    role == null ?
                    cloud.getNodeManager("insrel").toRelationManager() :
                    cloud.getRelationManager(destination.getNodeManager(), source.getNodeManager(), role);
                NodeQuery q2 = relationManager2.createQuery();
                Queries.addConstraint(q2, Queries.createConstraint(q2, "dnumber", FieldCompareConstraint.EQUAL, source));
                Queries.addConstraint(q2, Queries.createConstraint(q2, "snumber", FieldCompareConstraint.EQUAL, destination));
                if (Queries.count(q2) > 0) {
                    q = q2;
                    relationManager = relationManager2;
                }
            }
        }
        return new CollectionRelationList(relationManager.getList(q), cloud);
    }
   
    public static void addFeatures(NodeQuery query, Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection) {
        addFeatures(query, parent, managerName, role, fieldname, value, sortName, sortDirection, DESTINATION);
    }

    
    public static void addFeatures(NodeQuery query, Node parent, String managerName, String role, String fieldname, Object value, String sortName, String sortDirection, String searchdir) {
        NodeManager manager = parent.getCloud().getNodeManager(managerName);
        
        addEqualConstraint(query, manager, fieldname, value);

        if (!isEmptyOrWhitespace(sortName)) {
            if (sortName.startsWith(role + ".")) {
                String sortField = sortName.substring(role.length() + 1);
                RelationManager relationManager = null;
                if (SOURCE.equals(searchdir)) {
                    relationManager = parent.getCloud().getRelationManager(manager, parent.getNodeManager(), role);
                }
                else {
                    relationManager = parent.getCloud().getRelationManager(parent.getNodeManager(), manager, role);
                }
                addRelationSortOrder(query, relationManager, sortField, sortDirection);
            }
            else {
                addSortOrder(query, manager, sortName, sortDirection);
            }
        }
    }
    
    public static void addSortOrder(NodeQuery query, NodeManager manager, String sortName, String sortDirection) {
        StepField sf = query.getStepField(manager.getField(sortName));
        addSortOrder(query, sf, sortDirection);
    }

    public static void addRelationSortOrder(NodeQuery query, RelationManager role, String sortName, String sortDirection) {
        Field field = role.getField(sortName);
        StepField sf = query.createStepField(query.getStep(role.getForwardRole()), field);
        addSortOrder(query, sf, sortDirection);
    }

    public static void addSortOrder(NodeQuery query, StepField sf, String sortDirection) {
        int dir = SortOrder.ORDER_ASCENDING;
        if ("DOWN".equalsIgnoreCase(sortDirection)) {
           dir = SortOrder.ORDER_DESCENDING;
        }
        query.addSortOrder(sf, dir);
    }
    
    public static void addEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, String value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }

    public static void addEqualConstraint(Query query, NodeManager manager, String fieldname, String value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }

    public static FieldValueConstraint createEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, String value) {
        Field keyField = manager.getField(fieldname);
        FieldValueConstraint constraint = createEqualConstraint(query, keyField, value);
        return constraint;
    }

    public static FieldValueConstraint createEqualConstraint(Query query, NodeManager manager, String fieldname, String value) {
        Field keyField = manager.getField(fieldname);
        FieldValueConstraint constraint = createEqualConstraint(query, keyField, value);
        return constraint;
    }
    
    public static void addEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Integer value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }

    public static void addEqualConstraint(Query query, NodeManager manager, String fieldname, Integer value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Integer value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }

    public static FieldValueConstraint createEqualConstraint(Query query, NodeManager manager, String fieldname, Integer value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }

    public static void addEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Boolean value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }

    public static void addEqualConstraint(Query query, NodeManager manager, String fieldname, Boolean value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Boolean value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }

    public static FieldValueConstraint createEqualConstraint(Query query, NodeManager manager, String fieldname, Boolean value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }

    public static void addEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Object value) {
        if (!isEmptyOrWhitespace(fieldname)) {
            if (value instanceof String) {
                addEqualConstraint(query, manager, fieldname, (String) value);    
            }
            else if (value instanceof Integer) {
                addEqualConstraint(query, manager, fieldname, (Integer) value);    
            }
            else if (value instanceof Boolean) {
                addEqualConstraint(query, manager, fieldname, (Boolean) value);    
            }
            else {
                FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
                addConstraint(query, constraint);
            }
        }
    }
    
    public static void addEqualConstraint(Query query, NodeManager manager, String fieldname, Object value) {
        FieldValueConstraint constraint = createEqualConstraint(query, manager, fieldname, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, NodeManager manager, String fieldname, Object value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }

    public static FieldValueConstraint createEqualConstraint(Query query, NodeManager manager, String fieldname, Object value) {
        Field keyField = manager.getField(fieldname);
        return createEqualConstraint(query, keyField, value);
    }
    
    public static  void addEqualConstraint(NodeQuery query, Field field, String value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }

    public static  void addEqualConstraint(Query query, Field field, String value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, Field field, String value) {
        boolean caseSensitive = false;
        return createEqualConstraint(query, field, value, caseSensitive);
    }

    public static FieldValueConstraint createEqualConstraint(NodeQuery query, Field field, String value, boolean caseSensitive) {
        FieldValueConstraint constraint = query.createConstraint(query.getStepField(field),
                FieldCompareConstraint.EQUAL, value);
        query.setCaseSensitive(constraint, caseSensitive);
        return constraint;
    }

    public static FieldValueConstraint createEqualConstraint(Query query, Field field, String value) {
        boolean caseSensitive = false;
        return createEqualConstraint(query, field, value, caseSensitive);
    }

    public static FieldValueConstraint createEqualConstraint(Query query, Field field, String value, boolean caseSensitive) {
        StepField equalsField = findField(query, field);
        FieldValueConstraint constraint = query.createConstraint(equalsField,
                FieldCompareConstraint.EQUAL, value);
        query.setCaseSensitive(constraint, caseSensitive);
        return constraint;
    }
    
    public static  void addEqualConstraint(NodeQuery query, Field field, Integer value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }

    public static  void addEqualConstraint(Query query, Field field, Integer value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, Field field, Integer value) {
        FieldValueConstraint constraint = query.createConstraint(query.getStepField(field),
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }
    
    public static FieldValueConstraint createEqualConstraint(Query query, Field field, Integer value) {
        StepField equalsField = findField(query, field);
        FieldValueConstraint constraint = query.createConstraint(equalsField,
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }

    public static  void addEqualConstraint(NodeQuery query, Field field, Boolean value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }

    public static  void addEqualConstraint(Query query, Field field, Boolean value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, Field field, Boolean value) {
        FieldValueConstraint constraint = query.createConstraint(query.getStepField(field),
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }
    
    public static FieldValueConstraint createEqualConstraint(Query query, Field field, Boolean value) {
        StepField equalsField = findField(query, field);
        FieldValueConstraint constraint = query.createConstraint(equalsField,
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }

    public static  void addEqualConstraint(NodeQuery query, Field field, Object value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }

    public static  void addEqualConstraint(Query query, Field field, Object value) {
        FieldValueConstraint constraint = createEqualConstraint(query, field, value);
        addConstraint(query, constraint);
    }
    
    public static FieldValueConstraint createEqualConstraint(NodeQuery query, Field field, Object value) {
        FieldValueConstraint constraint = query.createConstraint(query.getStepField(field),
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }
    
    public static FieldValueConstraint createEqualConstraint(Query query, Field field, Object value) {
        StepField equalsField = findField(query, field);
        FieldValueConstraint constraint = query.createConstraint(equalsField,
                FieldCompareConstraint.EQUAL, value);
        return constraint;
    }
    
    public static StepField findField(Query query, Field field) {
        StepField equalsField = null;
        Iterator<StepField> fields = query.getFields().iterator();
        while(fields.hasNext()) {
            StepField stepField = fields.next();
            if (stepField.getStep().getTableName().equals(field.getNodeManager().getName())) {
                if (stepField.getFieldName().equals(field.getName())) {
                    equalsField = stepField;
                }
            }
        }
        if (equalsField == null) {
            Step equalsStep = query.getStep(field.getNodeManager().getName());
            if (equalsStep == null) {
                throw new IllegalArgumentException("Step " + field.getNodeManager().getName() + " not found in query");

            }
            equalsField = query.createStepField(equalsStep, field);
        }
        if (equalsField == null) {
            throw new IllegalArgumentException("Field " + field.getName() + " not found in query");
        }

        return equalsField;
    }
    
    public static  void addLikeConstraint(NodeQuery query, Field field, String value) {
        FieldValueConstraint constraint = createLikeConstraint(query, field, value);
        addConstraint(query, constraint);
    }

    public static FieldValueConstraint createLikeConstraint(NodeQuery query, Field field, String value) {
        FieldValueConstraint constraint = query.createConstraint(query.getStepField(field),
                FieldCompareConstraint.LIKE, "%" + value + "%");
        query.setCaseSensitive(constraint, false);
        return constraint;
    }
    
    public static  void addDayConstraint(NodeQuery query, NodeManager nodeManager, String fieldName,
            String daysToCompare) {

        if (!isEmptyOrWhitespace(daysToCompare) && !daysToCompare.equals("0")
                && daysToCompare.matches("\\-?\\d+")) {

            Field field = nodeManager.getField(fieldName);

            long now = (System.currentTimeMillis());

            long msecondsToCompare = (1000 * 60 * 60 * 24 * Long.parseLong(daysToCompare));
            long date = now + msecondsToCompare;

            long compareField1 = (msecondsToCompare < 0) ? date : now;
            long compareField2 = (msecondsToCompare < 0) ? now : date;

            addDatetimeConstraint(query, field, compareField1, compareField2);
        }
    }

    public static void addDatetimeConstraint(NodeQuery query, Field field, long from, long to) {
        FieldValueBetweenConstraint constraint = createDatetimeConstraint(query, field, from, to);
        addConstraint(query, constraint);
    }

    public static FieldValueBetweenConstraint createDatetimeConstraint(NodeQuery query, Field field, long from, long to) {
        FieldValueBetweenConstraint constraint = null;
        if (field.getType() == Field.TYPE_DATETIME) {
            constraint = query.createConstraint(query.getStepField(field),
                    new Date(from), new Date(to));
        }
        else {
            constraint = query.createConstraint(query.getStepField(field),
                    Long.valueOf(from), Long.valueOf(to));
        }
        return constraint;
    }

    public static void addLimitConstraint(NodeQuery query, int offset, int maxNumber) {
        if (offset > 0) {
            query.setOffset(offset);
        }
        if (maxNumber > 0) {
            query.setMaxNumber(maxNumber);
        }
    }

    public static void addTypeConstraints(NodeQuery query, List<String> types) {
        FieldValueInConstraint constraint = createTypeConstraints(query, types);
        addConstraint(query, constraint);
    }

    public static FieldValueInConstraint createTypeConstraints(NodeQuery query, List<String> types) {
        Cloud cloud = query.getCloud();
        SortedSet<Integer> set = new TreeSet<Integer>();
        for (String type : types) {
            NodeManager manager = cloud.getNodeManager(type);
            set.add(manager.getNumber());
        }
        Field field = query.getNodeManager().getField("otype");
        return createInConstraint(query, field, set);
    }

    public static void addNodesConstraints(Query query, Field field, NodeList nodes) {
        SortedSet<Integer> set = createNodesConstraints(nodes);
        addInConstraint(query, field, set);
    }

    public static SortedSet<Integer> createNodesConstraints(NodeList nodes) {
        SortedSet<Integer> set = new TreeSet<Integer>();
        for (Object element : nodes) {
            Node node = (Node) element;
            set.add(node.getNumber());
        }
        return set;
    }
    
    public static void addInConstraint(Query query, Field field, SortedSet<? extends Object> set) {
        FieldValueInConstraint constraint = createInConstraint(query, field, set);
        addConstraint(query, constraint);
    }

    public static FieldValueInConstraint createInConstraint(Query query, Field field, SortedSet<? extends Object> set) {
        query.getStep(field.getNodeManager().getName());
        StepField stepfield = getStepField(query, field);        
        FieldValueInConstraint constraint = query.createConstraint(stepfield, set);
        return constraint;
    }

    public static StepField getStepField(Query query, Field field) {
        StepField stepfield = null;
        Iterator<StepField> fields = query.getFields().iterator();
        while(fields.hasNext()) {
            StepField tempStepField = fields.next();
            if (tempStepField.getFieldName().equals(field.getName())) {
                if (tempStepField.getStep().getAlias().equals(field.getNodeManager().getName())) {
                    stepfield = tempStepField;
                }
            }
        }
        return stepfield;
    }
    
    public static void addConstraint(Query query, Constraint constraint) {
        addConstraint(query, constraint, CompositeConstraint.LOGICAL_AND);
    }

    public static Constraint createANDConstraint(Query query, Constraint one, Constraint two) {
        return createLogicalConstraint(query, one, two, CompositeConstraint.LOGICAL_OR);
    }
    
    public static void addORConstraint(Query query, Constraint constraint) {
        addConstraint(query, constraint, CompositeConstraint.LOGICAL_OR);
    }

    public static Constraint addORConstraint(Query query, Constraint one, Constraint two) {
        return createLogicalConstraint(query, one, two, CompositeConstraint.LOGICAL_OR);
    }
    
    public static void addConstraint(Query query, Constraint constraint, int operator) {
        if (query.getConstraint() == null) {
            query.setConstraint(constraint);
        }
        else {
            CompositeConstraint newc = createLogicalConstraint(query, query.getConstraint(), constraint, operator);
            query.setConstraint(newc);
        }
    }

    public static CompositeConstraint createLogicalConstraint(Query query, Constraint one, Constraint two, int operator) {
        CompositeConstraint newc = query.createConstraint(one,
                operator, two);
        return newc;
    }

    /**
     * is Empty Or Whitespace.String
     * 
     * @param str String to check emptiness
     * @return boolean is it empty
     */
    public static boolean isEmptyOrWhitespace(String str) {
        return (str == null) || "".equals(str.trim());
    }

}
