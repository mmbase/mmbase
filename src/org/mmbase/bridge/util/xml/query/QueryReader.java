/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.xml.query;

import java.util.*;
import org.w3c.dom.*;
import org.w3c.dom.NodeList;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.BasicCompositeConstraint;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 *
 * @author Pierre van Rooden
 * @version $Id: QueryReader.java,v 1.3 2005-08-26 14:54:59 michiel Exp $
 **/
public class QueryReader {

    public static final String XSD_SEARCHQUERY_1_0 = "searchquery.xsd";
    public static final String NAMESPACE_SEARCHQUERY_1_0 = "http://www.mmbase.org/xmlns/searchquery";

    /** most recent version */
    public static final String NAMESPACE_SEARCHQUERY = NAMESPACE_SEARCHQUERY_1_0;

    /**
     * Register the namespace and XSD used by QueryReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerSystemIDs() {
        XMLEntityResolver.registerSystemID(NAMESPACE_SEARCHQUERY_1_0 + ".xsd",  XSD_SEARCHQUERY_1_0, QueryReader.class);
    }

    /**
     * Returns whether an element has a certain attribute, either an unqualified attribute or an attribute that fits in the
     * searchquery namespace
     */
    static public boolean hasAttribute(Element element, String localName) {
        return element.hasAttributeNS(NAMESPACE_SEARCHQUERY,localName) || element.hasAttribute(localName);
    }

    /**
     * Returns the value of a certain attribute, either an unqualified attribute or an attribute that fits in the
     * searchquery namespace
     */
    static public String getAttribute(Element element, String localName) {
        if (element.hasAttributeNS(NAMESPACE_SEARCHQUERY,localName)) {
            return element.getAttributeNS(NAMESPACE_SEARCHQUERY,localName);
        } else {
            return element.getAttribute(localName);
        }
    }


    static protected void addField(Element fieldElement, QueryDefinition queryDefinition, QueryConfigurer configurer) {
        if (hasAttribute(fieldElement,"name")) {
            FieldDefinition fieldDefinition = configurer.getFieldDefinition(queryDefinition);
            fieldDefinition.fieldName = fieldElement.getAttribute("name");
            try {
                fieldDefinition.stepField = queryDefinition.query.createStepField(fieldDefinition.fieldName);
            } catch (IllegalArgumentException iae) {
                // the field did not exist in the database.
                // this is possible if the field is, for instance, a bytefield that is stored on disc.
                fieldDefinition.stepField = null;
            }
            // custom configuration of field
            fieldDefinition.configure(fieldElement);
            queryDefinition.fields.add(fieldDefinition);
            if (queryDefinition.isMultiLevel) {
                // have to add field for multilevel queries
                queryDefinition.query.addField(fieldDefinition.fieldName);
            }
        } else {
             throw new IllegalArgumentException("field tag has no 'name' attribute");
        }
    }

    static protected Constraint getConstraint(Element constraintElement, QueryDefinition queryDefinition) throws SearchQueryException {
        if (!hasAttribute(constraintElement,"field")) {
            throw new IllegalArgumentException("A constraint tag must have a 'field' attribute");
        }
        String fieldName = getAttribute(constraintElement,"field");
        Object value = null;
        if (hasAttribute(constraintElement,"value")) {
            if (hasAttribute(constraintElement,"field2")) {
                throw new IllegalArgumentException("A constraint tag can only have one of 'value' or 'field2'");
            }
            value = getAttribute(constraintElement,"value");
        } else if (hasAttribute(constraintElement,"field2")) {
            value = queryDefinition.query.createStepField(getAttribute(constraintElement,"field2"));
        }
        int operator = FieldCompareConstraint.EQUAL;
        if (hasAttribute(constraintElement,"operator")) {
            String sOperator = getAttribute(constraintElement,"operator");
            operator = Queries.getOperator(sOperator);
        }
        int part = -1;
        if (hasAttribute(constraintElement,"part")) {
            String sPart = getAttribute(constraintElement,"part");
            part = Queries.getDateTimePart(sPart);
        }
        Object value2 = null;
        if (hasAttribute(constraintElement,"value2")) {
            if (operator != Queries.OPERATOR_BETWEEN) {
                throw new IllegalArgumentException("A constraint tag can only use 'value2' attribute with operator BETWEEN");
            }
            value2 = getAttribute(constraintElement,"value2");
        }
        if (operator == Queries.OPERATOR_BETWEEN && value2 == null) {
            throw new IllegalArgumentException("Operator BETWEEN in a constraint tag requires attribute 'value2'");
        }
        if (operator == Queries.OPERATOR_IN && (value instanceof String)) {
            value = Casting.toList(value);
        }
        boolean caseSensitive = false;
        if (hasAttribute(constraintElement,"casesensitive")) {
            caseSensitive = "true".equals(getAttribute(constraintElement,"casesensitive"));
        }
        return Queries.createConstraint(queryDefinition.query, fieldName, operator, value, value2, caseSensitive, part);
    }

    static protected int getDayMark(Cloud cloud, int age) {
        // find day mark
        NodeManager dayMarks = cloud.getNodeManager("daymarks");
        NodeQuery query = dayMarks.createQuery();
        StepField step = query.createStepField("daycount");
        int currentDay = (int) (System.currentTimeMillis()/(1000*60*60*24));
        Integer day = new Integer(currentDay  - age);
        Constraint constraint = query.createConstraint(step, FieldCompareConstraint.LESS_EQUAL, day);
        query.setConstraint(constraint);
        query.addSortOrder(query.createStepField("daycount"), SortOrder.ORDER_DESCENDING);
        query.setMaxNumber(1);

        org.mmbase.bridge.NodeList result = dayMarks.getList(query);
        int daymark = -1;
        if (result.size() > 0) {
            daymark = result.getNode(0).getIntValue("mark");
        }
        return daymark;
    }

    static protected Constraint getAgeConstraint(Element constraintElement, QueryDefinition queryDefinition) throws SearchQueryException {
        // find day mark
        int minAge = -1;
        if (hasAttribute(constraintElement,"minage")) {
            minAge = Integer.parseInt(getAttribute(constraintElement,"minage"));
        }
        int maxAge = -1;
        if (hasAttribute(constraintElement,"maxage")) {
            maxAge = Integer.parseInt(getAttribute(constraintElement,"maxage"));
        }
        if (minAge < 0 && maxAge < 0) {
            throw new IllegalArgumentException("Either 'minage' or 'maxage' (or both) attributes must be present");
        }
        StepField stepField = null;
        String fieldName = "number";
        if (hasAttribute(constraintElement,"element")) {
            if (hasAttribute(constraintElement,"field")) {
                throw new IllegalArgumentException("Can not specify both 'field' and 'element' attributes on ageconstraint");
            }
            fieldName = getAttribute(constraintElement,"element") + ".number";
            stepField = queryDefinition.query.createStepField(fieldName);
        } else if (hasAttribute(constraintElement,"field")) {
            fieldName = getAttribute(constraintElement,"field");
            stepField = queryDefinition.query.createStepField(fieldName);
        } else {
            if (queryDefinition.elementStep != null) {
                stepField = queryDefinition.query.createStepField(queryDefinition.elementStep, "number");
            } else {
                throw new IllegalArgumentException("Don't know on what path element the ageconstraint must be applied. Use the 'element' attribute");
            }
        }

        Constraint constraint = null;
        // if minimal age given:
        // you need the day marker of the day after that (hence -1 in code below inside the getDayMark), node have to have this number or lower
        // if maximal age given:
        // daymarker object of that age must be included, but last object of previous day not, hece the +1 outside the getDayMark

        Cloud cloud = queryDefinition.query.getCloud();
        if (maxAge != -1 && minAge > 0) {
            int maxMarker = getDayMark(cloud, maxAge);
            if (maxMarker > 0) {
                // BETWEEN constraint
                constraint = queryDefinition.query.createConstraint(stepField, new Integer(maxMarker + 1), new Integer(getDayMark(cloud, minAge - 1)));
            } else {
                constraint = queryDefinition.query.createConstraint(stepField, FieldCompareConstraint.LESS_EQUAL, new Integer(getDayMark(cloud, minAge - 1)));
            }
        } else if (maxAge != -1) { // only on max
            int maxMarker = getDayMark(cloud, maxAge);
            if (maxMarker > 0) {
                constraint = queryDefinition.query.createConstraint(stepField, FieldCompareConstraint.GREATER_EQUAL, new Integer(maxMarker + 1));
            }
        } else if (minAge > 0) {
            constraint = queryDefinition.query.createConstraint(stepField, FieldCompareConstraint.LESS_EQUAL, new Integer(getDayMark(cloud, minAge - 1)));
        }
        return constraint;
    }


    static protected Integer getAlias(Cloud cloud, String name) {
        org.mmbase.bridge.Node node = cloud.getNode(name);
        return new Integer(node.getNumber());
    }

    static protected SortedSet getAliases(Cloud cloud, List names) {
        SortedSet set = new TreeSet();
        Iterator i = names.iterator();
        while (i.hasNext()) {
            set.add(getAlias(cloud, (String) i.next()));
        }
        return set;
    }

    static protected Constraint getAliasConstraint(Element constraintElement, QueryDefinition queryDefinition) throws SearchQueryException {
        if (!hasAttribute(constraintElement,"name")) {
            throw new IllegalArgumentException("An aliasconstraint tag must have a 'name' attribute");
        }
        String elementString = getAttribute(constraintElement,"element");
        Step step = queryDefinition.elementStep;
        if (elementString != null || !elementString.equals("")) {
            step = queryDefinition.query.getStep(elementString);
        }
        if (step == null) {
            throw new IllegalArgumentException("Don't know on what path element the aliasconstraint must be applied. Use the 'element' attribute");
        }
        StepField stepField = queryDefinition.query.createStepField(step, "number");

        String name = getAttribute(constraintElement,"name");
        List names =  Casting.toList(name);
        return queryDefinition.query.createConstraint(stepField, getAliases(queryDefinition.query.getCloud(),names));
    }

    static protected SortedSet getOTypes(Cloud cloud, List names, boolean descendants) {
        SortedSet set = new TreeSet();
        Iterator i = names.iterator();
        while (i.hasNext()) {
            NodeManager nm = cloud.getNodeManager((String) i.next());
            set.add(new Integer(nm.getNumber()));
            if (descendants) {
                NodeManagerIterator j = nm.getDescendants().nodeManagerIterator();
                while (j.hasNext()) {
                    set.add(new Integer(j.nextNodeManager().getNumber()));
                }
            }
        }
        return set;
    }

    static protected Constraint getTypeConstraint(Element constraintElement, QueryDefinition queryDefinition) throws SearchQueryException {
        if (!hasAttribute(constraintElement,"name")) {
            throw new IllegalArgumentException("A typeconstraint tag must have a 'name' attribute");
        }
        String elementString = getAttribute(constraintElement,"element");
        Step step = queryDefinition.elementStep;
        if (elementString != null || !elementString.equals("")) {
            step = queryDefinition.query.getStep(elementString);
        }
        if (step == null) {
            throw new IllegalArgumentException("Don't know on what path element the type constraint must be applied. Use the 'element' attribute");
        }
        StepField stepField = queryDefinition.query.createStepField(step, "otype");
        String name = getAttribute(constraintElement,"name");
        List names =  Casting.toList(name);
        boolean descendants = true;
        if (hasAttribute(constraintElement,"descendants")) {
            descendants = "true".equals(getAttribute(constraintElement,"descendants"));
        }
        return queryDefinition.query.createConstraint(stepField, getOTypes(queryDefinition.query.getCloud(), names, descendants));
    }

    static protected Constraint getCompositeConstraint(Element constraintElement, QueryDefinition queryDefinition) throws SearchQueryException {
        int operator = CompositeConstraint.LOGICAL_AND;
        if (hasAttribute(constraintElement,"operator")) {
            String sOperator = getAttribute(constraintElement,"operator");
            if (sOperator!= null && sOperator.toUpperCase().equals("OR")) {
                operator = CompositeConstraint.LOGICAL_OR;
            }
        }
        CompositeConstraint constraint = new BasicCompositeConstraint(operator);
        if (hasAttribute(constraintElement,"inverse")) {
            queryDefinition.query.setInverse(constraint, "true".equals(getAttribute(constraintElement,"inverse")));
        }
        NodeList childNodes = constraintElement.getChildNodes();
        for (int k = 0; k < childNodes.getLength(); k++) {
            if (childNodes.item(k) instanceof Element) {
                Element childElement = (Element) childNodes.item(k);
                addConstraint(childElement, queryDefinition, constraint);
            }
        }
        return constraint;
    }

    static protected void addConstraint(Element constraintElement, QueryDefinition queryDefinition, CompositeConstraint parentConstraint) throws SearchQueryException {
        Constraint constraint = null;
        if ("constraint".equals(constraintElement.getLocalName())) {
            constraint = getConstraint(constraintElement, queryDefinition);
        } else if ("ageconstraint".equals(constraintElement.getLocalName())) {
            constraint = getAgeConstraint(constraintElement, queryDefinition);
        } else if ("aliasconstraint".equals(constraintElement.getLocalName())) {
            constraint = getAliasConstraint(constraintElement, queryDefinition);
        } else if ("typeconstraint".equals(constraintElement.getLocalName())) {
            constraint = getTypeConstraint(constraintElement, queryDefinition);
        } else if ("compositeconstraint".equals(constraintElement.getLocalName())) {
            constraint = getCompositeConstraint(constraintElement, queryDefinition);
        }
        if (constraint != null) {
            if (hasAttribute(constraintElement,"inverse")) {
                queryDefinition.query.setInverse(constraint, "true".equals(getAttribute(constraintElement,"inverse")));
            }
            if (parentConstraint != null) {
                ((BasicCompositeConstraint)parentConstraint).addChild(constraint);
            } else {
                Queries.addConstraint(queryDefinition.query, constraint);
            }
        }
    }

    static protected void addDistinct(Element distinctElement, QueryDefinition queryDefinition) throws SearchQueryException {
        boolean distinct = true;
        if (hasAttribute(distinctElement,"value")) {
            distinct = "true".equals(getAttribute(distinctElement,"value"));
        }
        queryDefinition.query.setDistinct(distinct);
    }

    static protected void addSortOrder(Element sortOrderElement, QueryDefinition queryDefinition) throws SearchQueryException {
        if (!hasAttribute(sortOrderElement,"field")) {
            throw new IllegalArgumentException("A sortorder tag must have a 'field' attribute");
        }
        StepField stepField = queryDefinition.query.createStepField(getAttribute(sortOrderElement,"field"));
        int order = SortOrder.ORDER_ASCENDING;
        if (hasAttribute(sortOrderElement,"direction")) {
            order = Queries.getSortOrder(getAttribute(sortOrderElement,"direction"));
        }
        boolean casesensitive = false;
        if (hasAttribute(sortOrderElement,"casesensitive")) {
            casesensitive = "true".equals(getAttribute(sortOrderElement,"casesensitive"));
        }
        queryDefinition.query.addSortOrder(stepField, order, casesensitive);
    }

    static public QueryDefinition parseQuery(Element queryElement, Cloud cloud, String relateFrom) throws SearchQueryException {
        return parseQuery(queryElement, null, cloud, relateFrom);
    }

    static public QueryDefinition parseQuery(Element queryElement, QueryConfigurer configurer, Cloud cloud, String relateFrom) throws SearchQueryException {
        if (configurer == null) {
            configurer = QueryConfigurer.getDefaultConfigurer();
        }
        if (hasAttribute(queryElement,"type") || hasAttribute(queryElement,"name") || hasAttribute(queryElement,"path")) {

            String element = null;
            String path = null;
            String searchDirs = null;

            if (hasAttribute(queryElement,"type")) {
                path = getAttribute(queryElement,"type");
                element = path;
            } else if (hasAttribute(queryElement,"name")) {
                path = getAttribute(queryElement,"name");
                element = path;
            } else{
                path = getAttribute(queryElement,"path");
                searchDirs = getAttribute(queryElement,"searchdirs");
                if (hasAttribute(queryElement,"element")) {
                  element = getAttribute(queryElement,"element");
                } else {
                    List builders  = StringSplitter.split(path);
                    element = (String)builders.get(builders.size()-1);
                }
            }
            if (relateFrom != null) {
                path = relateFrom + "," + path;
            }

            QueryDefinition queryDefinition = configurer.getQueryDefinition();
            queryDefinition.isMultiLevel = !path.equals(element);
            Step elementStep = null;

            if (element != null) {
                queryDefinition.elementManager = cloud.getNodeManager(element);
            }
            if (queryDefinition.isMultiLevel) {
                queryDefinition.query = cloud.createQuery();
                Queries.addPath(queryDefinition.query,path, searchDirs);
            } else {
                queryDefinition.query = queryDefinition.elementManager.createQuery();
            }
            if (element != null) {
                queryDefinition.elementStep = queryDefinition.query.getStep(element);
            }
            if (queryDefinition.fields == null) queryDefinition.fields = new ArrayList();

            // custom configurations to the query
            queryDefinition.configure(queryElement);

            NodeList childNodes = queryElement.getChildNodes();
            for (int k = 0; k < childNodes.getLength(); k++) {
                if (childNodes.item(k) instanceof Element) {
                    Element childElement = (Element) childNodes.item(k);
                    if ("field".equals(childElement.getLocalName())) {
                        addField(childElement, queryDefinition, configurer);
                    } else if ("distinct".equals(childElement.getLocalName())) {
                        addDistinct(childElement, queryDefinition);
                    } else if ("sortorder".equals(childElement.getLocalName())) {
                        addSortOrder(childElement, queryDefinition);
                    } else {
                        addConstraint(childElement, queryDefinition, null);
                    }
                }
            }
            return queryDefinition;
        } else {
            throw new IllegalArgumentException("query has no 'path' or 'type' attribute");
        }
    }

}


