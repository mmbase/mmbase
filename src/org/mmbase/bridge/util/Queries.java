/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.BasicQuery;
import org.mmbase.module.core.*;
import org.mmbase.module.database.support.MMJdbc2NodeInterface;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.legacy.ConstraintParser;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * This class contains various utility methods for manipulating and creating query objecs.
 * Most essential methods are available on the Query object itself, but too specific or legacy-ish
 * methods are put here.
 *
 * @author Michiel Meeuwissen
 * @version $Id: Queries.java,v 1.48 2004-11-30 14:06:55 pierre Exp $
 * @see  org.mmbase.bridge.Query
 * @since MMBase-1.7
 */
abstract public class Queries {

    public static final int OPERATOR_BETWEEN = -1; // not a FieldCompareConstraint (numeric)
    public static final int OPERATOR_IN = 10000; // not a FieldCompareConstraint (non numeric)
    public static final int OPERATOR_NULL = 10001; // FieldIsNullConstraint

    private static final Logger log = Logging.getLoggerInstance(Queries.class);

    /**
     * Translates a string to a search direction constant. If the string is <code>null</code> then
     * 'BOTH' is returned.
     * @param search string representation of the searchdir constant
     * @return Searchdir constant (@link RelationStep)
     * @see ClusterBuilder#getSearchDir The same function, only with another return value if String is <code>null</code>
     */
    public static int getRelationStepDirection(String search) {
        if (search == null) {
            return RelationStep.DIRECTIONS_BOTH;
        }
        search = search.toUpperCase();
        if ("DESTINATION".equals(search)) {
            return RelationStep.DIRECTIONS_DESTINATION;
        } else if ("SOURCE".equals(search)) {
            return RelationStep.DIRECTIONS_SOURCE;
        } else if ("BOTH".equals(search)) {
            return RelationStep.DIRECTIONS_BOTH;
        } else if ("ALL".equals(search)) {
            return RelationStep.DIRECTIONS_ALL;
        } else if ("EITHER".equals(search)) {
            return RelationStep.DIRECTIONS_EITHER;
        } else {
            throw new BridgeException("'" + search + "' cannot be converted to a relation-step direction constant");
        }
    }

    /**
     * Creates a Query object using arguments for {@link Cloud#getList(String, String, String, String, String, String, String, boolean)}
     * (this function is of course implemented using this utility). This is useful to convert (legacy) code which uses
     * getList, but you want to use new Query features without rewriting the complete thing.
     *
     * It can also be simply handy to specify things as Strings.
     *
     * @param cloud
     * @param startNodes
     * @param nodePath
     * @param fields
     * @param constraints
     * @param orderby
     * @param directions
     * @param searchDir
     * @param distinct
     * @return
     * @todo Should this method be part of Cloud itself?
     */
    public static Query createQuery(Cloud cloud, String startNodes, String nodePath, String fields, String constraints, String orderby, String directions, String searchDir, boolean distinct) {

        // the bridge test case say that you may also specifiy empty string (why?)
        if ("".equals(startNodes) || "-1".equals(startNodes)) {
            startNodes = null;
        }
        if ("".equals(fields)) {
            fields = null;
        }
        if ("".equals(constraints)) {
            constraints = null;
        }
        if ("".equals(searchDir)) {
            searchDir = null;
        }
        if ("".equals(orderby)) {
            orderby = null;
        }

        if ("".equals(directions)) {
            directions = null;
        }
        // check invalid search command
        Encode encoder = new Encode("ESCAPE_SINGLE_QUOTE");
        // if(startNodes != null) startNodes = encoder.encode(startNodes);
        // if(nodePath != null) nodePath = encoder.encode(nodePath);
        // if(fields != null) fields = encoder.encode(fields);
        if (orderby != null) {
            orderby = encoder.encode(orderby);
        }
        if (directions != null) {
            directions = encoder.encode(directions);
        }
        if (searchDir != null) {
            searchDir = encoder.encode(searchDir);
        }
        if (constraints != null) {
            constraints = convertClauseToDBS(constraints);
            if (!validConstraints(constraints)) {
                throw new BridgeException("invalid constraints:" + constraints);
            }
        }

        // create query object
        //TODO: remove this code... classes under org.mmbase.bridge.util must not use the core
        ClusterBuilder clusterBuilder = MMBase.getMMBase().getClusterBuilder();
        int search = -1;
        if (searchDir != null) {
            search = ClusterBuilder.getSearchDir(searchDir);
        }

        List snodes   = StringSplitter.split(startNodes);
        List tables   = StringSplitter.split(nodePath);
        List f        = StringSplitter.split(fields);
        List orderVec = StringSplitter.split(orderby);
        List d        = StringSplitter.split(directions);
        try {
            // pitty that we can't use cloud.createQuery for this.
            // but all essential methods are on ClusterBuilder
            Query query = new BasicQuery(cloud, clusterBuilder.getMultiLevelSearchQuery(snodes, f, distinct ? "YES" : "NO", tables, constraints, orderVec, d, search));
            return query;
        } catch (IllegalArgumentException iae) {
            throw new BridgeException(iae.getMessage() + ". (arguments: startNodes='" + startNodes + "', path='" + nodePath + "', fields='" + fields + "', constraints='" + constraints + "' orderby='" + orderby + "', directions='" + directions + "', searchdir='" + searchDir + "')" , iae);
        }
    }

    /**
     * returns false, when escaping wasnt closed, or when a ";" was found outside a escaped part (to prefent spoofing)
     * This is used by createQuery (i wonder if it still makes sense)
     * @param constraints constraint to check
     * @return is valid constraint
     */
    static private boolean validConstraints(String constraints) {
        // first remove all the escaped "'" ('' occurences) chars...
        String remaining = constraints;
        while (remaining.indexOf("''") != -1) {
            int start = remaining.indexOf("''");
            int stop = start + 2;
            if (stop < remaining.length()) {
                String begin = remaining.substring(0, start);
                String end = remaining.substring(stop);
                remaining = begin + end;
            } else {
                remaining = remaining.substring(0, start);
            }
        }
        // assume we are not escaping... and search the string..
        // Keep in mind that at this point, the remaining string could contain different information
        // than the original string. This doesnt matter for the next sequence...
        // but it is important to realize!
        while (remaining.length() > 0) {
            if (remaining.indexOf('\'') != -1) {
                // we still contain a "'"
                int start = remaining.indexOf('\'');

                // escaping started, but no stop
                if (start == remaining.length()) {
                    log.warn("reached end, but we are still escaping(you should sql-escape the search query inside the jsp-page?)\noriginal:" + constraints);
                    return false;
                }

                String notEscaped = remaining.substring(0, start);
                if (notEscaped.indexOf(';') != -1) {
                    log.warn("found a ';' outside the constraints(you should sql-escape the search query inside the jsp-page?)\noriginal:" + constraints + "\nnot excaped:" + notEscaped);
                    return false;
                }

                int stop = remaining.substring(start + 1).indexOf('\'');
                if (stop < 0) {
                    log.warn("reached end, but we are still escaping(you should sql-escape the search query inside the jsp-page?)\noriginal:" + constraints + "\nlast escaping:" + remaining.substring(start + 1));
                    return false;
                }
                // we added one to to start, thus also add this one to stop...
                stop = start + stop + 1;

                // when the last character was the stop of our escaping
                if (stop == remaining.length()) {
                    return true;
                }

                // cut the escaped part from the string, and continue with resting sting...
                remaining = remaining.substring(stop + 1);
            } else {
                if (remaining.indexOf(';') != -1) {
                    log.warn("found a ';' inside our constrain:" + constraints);
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    /**
     * Converts a constraint by turning all 'quoted' fields into
     * database supported fields.
     * XXX: todo: escape characters for '[' and ']'.
     * @param constraints constraint to convert
     * @return Converted constraint
     */
    private static String convertClausePartToDBS(String constraints) {
        // obtain dbs for fieldname checks

        //TODO: remove this code... classes under org.mmbase.bridge.util must not use the core
        MMJdbc2NodeInterface dbs = MMBase.getMMBase().getDatabase();
        StringBuffer result = new StringBuffer();
        int posa = constraints.indexOf('[');
        while (posa > -1) {
            int posb = constraints.indexOf(']', posa);
            if (posb == -1) {
                posa = -1;
            } else {
                String fieldName = constraints.substring(posa + 1, posb);
                int posc = fieldName.indexOf('.');
                if (posc == -1) {
                    fieldName = dbs.getAllowedField(fieldName);
                } else {
                    fieldName = fieldName.substring(0, posc + 1) + dbs.getAllowedField(fieldName.substring(posc + 1));
                }
                result.append(constraints.substring(0, posa)).append(fieldName);
                constraints = constraints.substring(posb + 1);
                posa = constraints.indexOf('[');
            }
        }
        result.append(constraints);
        return result.toString();
    }

    /**
     * Converts a constraint by turning all 'quoted' fields into
     * database supported fields.
     * XXX: todo: escape characters for '[' and ']'.
     * @param constraints constraints to convert
     * @return converted constraint
     */
    private static String convertClauseToDBS(String constraints) {
        if (constraints.startsWith("MMNODE")) {
            //  wil probably not work
            // @todo check
            return constraints;
        } else if (constraints.startsWith("ALTA")) {
            //  wil probably not work
            // @todo check
            return constraints.substring(5);
        } else if (!constraints.substring(0, 5).equalsIgnoreCase("WHERE")) {
            // Must start with "WHERE "
            constraints = "WHERE " + constraints;
        }

        //keesj: here constraints will start with WHERE,ALTA or MMNODE

        //keesj: what does this code do?

        StringBuffer result = new StringBuffer();
        //if there is a quote in the constraints posa will not be equals -1

        int posa = constraints.indexOf('\'');
        while (posa > -1) {
            //keesj: posb can be the same a posa maybe the method should read indexOf("\"",posa) ?
            int posb = constraints.indexOf('\'', 1);
            if (posb == -1) {
                posa = -1;
            } else {
                //keesj:part is now the first part of the constraints if there is a quote in the query
                String part = constraints.substring(0, posa);

                //append to the string buffer "part" the first part
                result.append(convertClausePartToDBS(part)).append(constraints.substring(posa, posb + 1));

                //keesj:obfucation contest?
                constraints = constraints.substring(posb + 1);
                posa = constraints.indexOf('\'');
            }
        }
        result.append(convertClausePartToDBS(constraints));
        return result.toString();
    }

    /**
     * Adds a 'legacy' constraint to the query, i.e. constraint(s) represented
     * by a string. Alreading existing constraints remain ('AND' is used)
     * @param query query to add constraint to
     * @param constraints string representation of constraints
     * @return the new constraint, or null if nothing changed added.
     */
    public static Constraint addConstraints(Query query, String constraints) {
        if (constraints == null || constraints.equals("")) {
            return null;
        }
        constraints = convertClauseToDBS(constraints);
        if (!validConstraints(constraints)) {
            throw new BridgeException("invalid constraints:" + constraints);
        }
        // Before converting to legacy constraint,
        // the leading "WHERE" must be skipped when present.
        if (constraints.substring(0, 5).equalsIgnoreCase("WHERE")) {
            constraints = constraints.substring(5).trim();
        }
        // (Try to) parse constraints string to Constraint object.
        Constraint newConstraint = new ConstraintParser(query).toConstraint(constraints);
        addConstraint(query, newConstraint);
        return newConstraint;
    }

    /**
     * Adds a Constraint to the already present constraint (with AND).
     * @param query query to add the constraint to
     * @param newConstraint constraint to add
     * @return The new constraint.
     */
    public static Constraint addConstraint(Query query, Constraint newConstraint) {
        if (newConstraint == null) {
            return null;
        }

        Constraint constraint = query.getConstraint();

        if (constraint != null) {
            log.debug("compositing constraint");
            Constraint compConstraint = query.createConstraint(constraint, CompositeConstraint.LOGICAL_AND, newConstraint);
            query.setConstraint(compConstraint);
        } else {
            query.setConstraint(newConstraint);
        }
        return newConstraint;
    }

    /**
     * Creates a operator constant for use by createConstraint
     * @param s String representation of operator
     * @return FieldCompareConstraint operator constant
     * @see #createConstraint(Query, String, int, Object)
     * @see #createConstraint(Query, String, int, Object, Object, boolean)
     */
    public static int getOperator(String s) {
        String op = s.toUpperCase();
        // first: determine operator:
        if (op.equals("<") || op.equals("LESS")) {
            return FieldCompareConstraint.LESS;
        } else if (op.equals("<=") || op.equals("LESS_EQUAL")) {
            return FieldCompareConstraint.LESS_EQUAL;
        } else if (op.equals("=") || op.equals("EQUAL") || op.equals("")) {
            return FieldCompareConstraint.EQUAL;
        } else if (op.equals(">") || op.equals("GREATER")) {
            return FieldCompareConstraint.GREATER;
        } else if (op.equals(">=") || op.equals("GREATER_EQUAL")) {
            return FieldCompareConstraint.GREATER_EQUAL;
        } else if (op.equals("LIKE")) {
            return FieldCompareConstraint.LIKE;
        } else if (op.equals("BETWEEN")) {
            return OPERATOR_BETWEEN;
        } else if (op.equals("IN")) {
            return OPERATOR_IN;
        } else if (op.equals("NULL")) {
            return OPERATOR_NULL;
            //} else if (op.equals("~") || op.equals("REGEXP")) {
            //  return FieldCompareConstraint.REGEXP;
        } else {
            throw new BridgeException("Unknown Field Compare Operator '" + op + "'");
        }

    }
    /**
     * Used in implementation of createConstraint
     * @param stringValue string representation of a number
     * @return Number object
     * @throws BridgeException when failed to convert the string
     */
    protected static Number getNumberValue(String stringValue) throws BridgeException {
        try {
            return new Integer(stringValue);
        } catch (NumberFormatException e) {
            try {
                return new Double(stringValue);
            } catch (NumberFormatException e2) {
                throw new BridgeException("Operator requires number value ('" + stringValue + "' is not)");
            }
        }
    }

    /**
     * Used in implementation of createConstraint
     * @param fieldType Field Type constant (@link Field)
     * @param operator Compare operator
     * @param value value to convert
     * @return new Compare value
     */
    protected static Object getCompareValue(int fieldType, int operator, Object value) {
        return getCompareValue(fieldType, operator, value, -1);
    }

    /**
     * Used in implementation of createConstraint
     * @param fieldType Field Type constant (@link Field)
     * @param operator Compare operator
     * @param value value to convert
     * @return new Compare value
     */
    protected static Object getCompareValue(int fieldType, int operator, Object value, int datePart) {
        if (operator == OPERATOR_IN) {
            SortedSet set;
            if (value instanceof SortedSet) {
                set = (SortedSet)value;
            } else if (value instanceof NodeList) {
                set = new TreeSet();
                NodeIterator i = ((NodeList)value).nodeIterator();
                while (i.hasNext()) {
                    Node node = i.nextNode();
                    set.add(getCompareValue(fieldType, FieldCompareConstraint.EQUAL, new Integer(node.getNumber())));
                }
            } else if (value instanceof Collection) {
                set = new TreeSet();
                Iterator i = ((Collection)value).iterator();
                while (i.hasNext()) {
                    Object o = i.next();
                    set.add(getCompareValue(fieldType, FieldCompareConstraint.EQUAL, o));
                }
            } else {
                set = new TreeSet();
                if (!(value == null || value.equals(""))) {
                    set.add(getCompareValue(fieldType, FieldCompareConstraint.EQUAL, value));
                }
            }
            return set;
        }  else {
            switch(fieldType) {
            case Field.TYPE_INTEGER:
            case Field.TYPE_FLOAT:
            case Field.TYPE_LONG:
            case Field.TYPE_DOUBLE:
            case Field.TYPE_NODE:
                if (value instanceof Number) {
                    return value;
                } else {
                    return getNumberValue(Casting.toString(value));
                }
            case Field.TYPE_DATETIME:
                if (datePart > -1) {
                    return Casting.toInteger(value);
                } else {
                    return Casting.toDate(value);
                }
            case Field.TYPE_BOOLEAN:
                return Casting.toBoolean(value) ? Boolean.TRUE : Boolean.FALSE;
            default:
                return value;
            }
        }
    }

    /**
     * Defaulting version of {@link #createConstraint(Query, String, int, Object, Object, boolean, int)}.
     * Casesensitivity defaults to false, value2 to null (so 'BETWEEN' cannot be used), datePart set to -1 (so no date part comparison)
     * @param query      The query to create the constraint for
     * @param fieldName  The field to create the constraint on (as a string, so it can include the step), e.g. 'news.number'
     * @param operator   The operator to use. This constant can be produces from a string using {@link #getOperator(String)}.
     * @param value      The value to compare with, which must be of the right type. If field is number it might also be an alias.
     * @return The new constraint, or <code>null</code> it by chance the specified arguments did not lead to a new actual constraint (e.g. if value is an empty set)
     */
    public static Constraint createConstraint(Query query, String fieldName, int operator, Object value) {
        return createConstraint(query, fieldName, operator, value, null, false, -1);
    }

    /**
     * Defaulting version of {@link #createConstraint(Query, String, int, Object, Object, boolean, int)}.
     * DatePart set to -1 (so no date part comparison)
     * @param query      The query to create the constraint for
     * @param fieldName  The field to create the constraint on (as a string, so it can include the step), e.g. 'news.number'
     * @param operator   The operator to use. This constant can be produces from a string using {@link #getOperator(String)}.
     * @param value      The value to compare with, which must be of the right type. If field is number it might also be an alias.
     * @param value2     The other value (only relevant if operator is BETWEEN, the only terniary operator)
     * @param caseSensitive  Whether it should happen case sensitively (not relevant for number fields)
     * @return The new constraint, or <code>null</code> it by chance the specified arguments did not lead to a new actual constraint (e.g. if value is an empty set)
     */
    public static Constraint createConstraint(Query query, String fieldName, int operator, Object value,  Object value2, boolean caseSensitive) {
        return createConstraint(query, fieldName, operator, value, value2, caseSensitive, -1);
    }

    /**
     * Creates a constraint smartly, depending on the type of the field, the value is casted to the
     * right type, and the right type of constraint is created.
     * This is used in taglib implementation, but could be useful more generally.
     *
     * @param query      The query to create the constraint for
     * @param fieldName  The field to create the constraint on (as a string, so it can include the step), e.g. 'news.number'
     * @param operator   The operator to use. This constant can be produces from a string using {@link #getOperator(String)}.
     * @param value      The value to compare with, which must be of the right type. If field is number it might also be an alias.
     * @param value2     The other value (only relevant if operator is BETWEEN, the only terniary operator)
     * @param caseSensitive  Whether it should happen case sensitively (not relevant for number fields)
     * @param datePart       The part of a DATETIME value that is to be checked
     * @return The new constraint, or <code>null</code> it by chance the specified arguments did not lead to a new actual constraint (e.g. if value is an empty set)
     */
    public static Constraint createConstraint(Query query, String fieldName, int operator, Object value, Object value2, boolean caseSensitive, int datePart) {

        StepField stepField = query.createStepField(fieldName);
        if (stepField == null) {
            throw new BridgeException("Could not create stepfield with '" + fieldName + "'");
        }

        Cloud cloud = query.getCloud();
        FieldConstraint newConstraint;

        if (value instanceof StepField) {
            newConstraint = query.createConstraint(stepField, operator, (StepField)value);
        } else if (operator == OPERATOR_NULL || value == null) {
            newConstraint = query.createConstraint(stepField);
        } else {
            int fieldType = cloud.getNodeManager(stepField.getStep().getTableName()).getField(stepField.getFieldName()).getType();

            if (fieldName.equals("number") || fieldType == Field.TYPE_NODE) {
                if (value instanceof String) { // it might be an alias!
                    if (cloud.hasNode((String) value)) {
                        Node node = cloud.getNode((String)value);
                        value = new Integer(node.getNumber());
                    } else {
                        value = new Integer(-1);
                    }
                } else if (value instanceof Collection) {  // or even more aliases!
                    Iterator i = ((Collection) value).iterator();
                    value = new ArrayList();
                    List list = (List) value;
                    while (i.hasNext()) {
                        Object v = i.next();
                        if (v instanceof Number) {
                            list.add(v);
                        } else {
                            String s = Casting.toString(v);
                            if (cloud.hasNode(s)) {
                                Node node = cloud.getNode(s);
                                list.add(new Integer(node.getNumber()));
                            } else {
                                list.add(new Integer(-1));
                            }

                        }
                    }

                }
            }

            Object compareValue = getCompareValue(fieldType, operator, value, datePart);

            if (operator > 0 && operator < OPERATOR_IN) {
                if (fieldType == Field.TYPE_DATETIME && datePart> -1) {
                    newConstraint = query.createConstraint(stepField, operator, compareValue, datePart);
                } else {
                    newConstraint = query.createConstraint(stepField, operator, compareValue);
                }
            } else {
                if (fieldType == Field.TYPE_DATETIME && datePart> -1) {
                    throw new RuntimeException("Cannot apply IN or BETWEEN to a partial date field");
                }
                switch (operator) {
                case OPERATOR_BETWEEN :
                    Object compareValue2 = getCompareValue(fieldType, operator, value2);
                    newConstraint = query.createConstraint(stepField, compareValue, compareValue2);
                    break;
                case OPERATOR_IN :
                    newConstraint = query.createConstraint(stepField, (SortedSet)compareValue);
                    break;
                default :
                    throw new RuntimeException("Unknown value for operation " + operator);
                }
            }
        }
        query.setCaseSensitive(newConstraint, caseSensitive);
        return newConstraint;

    }

    /**
     * Takes a Constraint of a query, and takes al constraints on 'sourceStep' of it, and copies
     * those Constraints to the given step of the receiving query.
     *
     * Constraints on different steps then the given 'sourceStep' are ignored. CompositeConstraints
     * cause recursion and would work too (but same limitation are valid for the childs).
     *
     * @param c          The constrain to be copied (for example the result of sourceQuery.getConstraint()).
     * @param sourceStep The step in the 'source' query.
     * @param query      The receiving query
     * @param step       The step of the receiving query which must 'receive' the sort orders.
     * @since MMBase-1.7.1
     * @see   org.mmbase.storage.search.implementation.BasicSearchQuery#copyConstraint Functions are similar
     * @throws IllegalArgumentException If the given constraint is not compatible with the given step.
     * @throws UnsupportedOperationException If CompareFieldsConstraints or LegacyConstraints are encountered.
     * @return The new constraint or null
     */
    public static Constraint copyConstraint(Constraint c, Step sourceStep, Query query, Step step) {
        if (c == null) return null;

        if (c instanceof CompositeConstraint) {
            CompositeConstraint constraint = (CompositeConstraint) c;
            List constraints = new ArrayList();
            Iterator i = constraint.getChilds().iterator();
            while (i.hasNext()) {
                Constraint cons = copyConstraint((Constraint) i.next(), sourceStep, query, step);
                if (cons != null) constraints.add(cons);
            }
            int size = constraints.size();
            if (size == 0) return null;
            if (size == 1) return (Constraint) constraints.get(0);
            i = constraints.iterator();
            int op = constraint.getLogicalOperator();
            Constraint newConstraint    = query.createConstraint((Constraint) i.next(), op, (Constraint) i.next());
            while (i.hasNext()) {
                newConstraint = query.createConstraint(newConstraint, op, (Constraint) i.next());
            }
            query.setInverse(newConstraint, constraint.isInverse());
            return newConstraint;
        } else if (c instanceof CompareFieldsConstraint) {
            throw new UnsupportedOperationException("Cannot copy comparison between fields"); // at least not from different steps
        }


        FieldConstraint fieldConstraint = (FieldConstraint) c;
        if (! fieldConstraint.getField().getStep().equals(sourceStep)) return null; // constraint is not for the request step, so don't copy.

        StepField field = query.createStepField(step, fieldConstraint.getField().getFieldName());

        FieldConstraint newConstraint;
        if (c instanceof FieldValueConstraint) {
            newConstraint = query.createConstraint(field, ((FieldValueConstraint) c).getOperator(), ((FieldValueConstraint) c).getValue());
        } else if (c instanceof FieldNullConstraint) {
            newConstraint = query.createConstraint(field);
        } else if (c instanceof FieldValueBetweenConstraint) {
            FieldValueBetweenConstraint constraint = (FieldValueBetweenConstraint) c;
            try {
                newConstraint = query.createConstraint(field, new Integer(constraint.getLowerLimit()), new Integer(constraint.getUpperLimit()));
            } catch (NumberFormatException e) {
                newConstraint = query.createConstraint(field, new Double(constraint.getLowerLimit()), new Double(constraint.getUpperLimit()));
            }
        } else if (c instanceof FieldValueInConstraint) {
            FieldValueInConstraint constraint = (FieldValueInConstraint) c;

            // sigh
            SortedSet set = new TreeSet();
            int type   =  field.getType();
            Iterator k = constraint.getValues().iterator();
            while (k.hasNext()) {
                Object value = k.next();
                switch(type) {
                case Field.TYPE_INTEGER:
                case Field.TYPE_LONG:
                case Field.TYPE_NODE:
                    value = new Long((String) value);
                    break;
                case Field.TYPE_FLOAT:
                case Field.TYPE_DOUBLE:
                    value = new Double((String) value);
                    break;
                case Field.TYPE_DATETIME:
                    value = new Date((long) 1000 * Integer.parseInt("" + value));
                    break;
                }
                set.add(value);
            }
            newConstraint = query.createConstraint(field, set);
        } else if (c instanceof LegacyConstraint) {
            throw new UnsupportedOperationException("Cannot copy legacy constraint to other step");
        } else {
            throw new RuntimeException("Could not copy constraint " + c);
        }
        query.setInverse(newConstraint, fieldConstraint.isInverse());
        query.setCaseSensitive(newConstraint, fieldConstraint.isCaseSensitive());
        return newConstraint;

    }
    /**
     * Copies SortOrders to a given step of another query. SortOrders which do not sort the given
     * 'sourceStep' are ignored.
     * @param sortOrders A list of SortOrders (for example the result of sourceQuery.getSortOrders()).
     * @param sourceStep The step in the 'source' query.
     * @param query      The receiving query
     * @param step       The step of the receiving query which must 'receive' the sort orders.
     * @since MMBase-1.7.1

     */
    public static void copySortOrders(List sortOrders, Step sourceStep,  Query query, Step step) {
        Iterator i = sortOrders.iterator();
        while (i.hasNext()) {
            SortOrder sortOrder = (SortOrder) i.next();
            StepField sourceField = sortOrder.getField();
            if (! sourceField.getStep().equals(sourceStep)) continue; // for another step
            query.addSortOrder(query.createStepField(step, sourceField.getFieldName()), sortOrder.getDirection());
        }
    }

    /**
     * Converts a String to a SortOrder constant
     * @param dir string representation of direction of sortorder
     * @return SortOrder constant
     * @since MMBase-1.7.1
     */
    public static int getSortOrder(String dir) {
        dir = dir.toUpperCase();
        if (dir.equals("")) {
            return  SortOrder.ORDER_ASCENDING;
        } else if (dir.equals("DOWN")) {
            return SortOrder.ORDER_DESCENDING;
        } else if (dir.equals("UP")) {
            return SortOrder.ORDER_ASCENDING;
        } else if (dir.equals("ASCENDING")) {
            return SortOrder.ORDER_ASCENDING;
        } else if (dir.equals("DESCENDING")) {
            return SortOrder.ORDER_DESCENDING;
        } else {
            throw new BridgeException("Unknown sort-order '" + dir + "'");
        }
    }

    /**
     * Adds sort orders to the query, using two strings. Like in 'getList' of Cloud. Several tag-attributes need this.
     * @param query query to add the sortorders to
     * @param sorted string with comma-separated fields
     * @param directions string with comma-separated directions
     *
     * @todo implement for normal query.
     * @return The new sort orders
     */
    public static List addSortOrders(Query query, String sorted, String directions) {
        // following code was copied from MMObjectBuilder.setSearchQuery (bit ugly)
        if (sorted == null)
            return query.getSortOrders().subList(0, 0);
        if (directions == null) {
            directions = "";
        }
        List list = query.getSortOrders();
        int initialSize = list.size();

        StringTokenizer sortedTokenizer = new StringTokenizer(sorted, ",");
        StringTokenizer directionsTokenizer = new StringTokenizer(directions, ",");

        while (sortedTokenizer.hasMoreTokens()) {
            String fieldName = sortedTokenizer.nextToken().trim();
            int dot = fieldName.indexOf('.');

            StepField sf;
            if (dot == -1 && query instanceof NodeQuery) {
                NodeManager nodeManager = ((NodeQuery)query).getNodeManager();
                sf = ((NodeQuery)query).getStepField(nodeManager.getField(fieldName));
            } else {
                sf = query.createStepField(fieldName);
            }

            int dir = SortOrder.ORDER_ASCENDING;
            if (directionsTokenizer.hasMoreTokens()) {
                String direction = directionsTokenizer.nextToken().trim();
                dir = getSortOrder(direction);
            }
            query.addSortOrder(sf, dir);
        }

        return list.subList(initialSize, list.size());
    }

    /**
     * Returns substring of given string without the leading digits (used in 'paths')
     * @param complete string with leading digits
     * @return string with digits removed
     */
    protected static String removeDigits(String complete) {
        int end = complete.length() - 1;
        while (Character.isDigit(complete.charAt(end))) {
            --end;
        }
        return complete.substring(0, end + 1);
    }

    /**
     * Adds path of steps to an existing query. The query may contain steps already. Per step also
     * the 'search direction' may be specified.
     * @param query extend this query
     * @param path create steps from this path
     * @param searchDirs add steps with these relation directions
     * @return The new steps.
     */
    public static List addPath(Query query, String path, String searchDirs) {
        if (path == null || path.equals("")) {
            return query.getSteps().subList(0, 0);
        }
        if (searchDirs == null) {
            searchDirs = "";
        }

        List list = query.getSteps();
        int initialSize = list.size();

        StringTokenizer pathTokenizer       = new StringTokenizer(path, ",");
        StringTokenizer searchDirsTokenizer = new StringTokenizer(searchDirs, ",");

        Cloud cloud = query.getCloud();

        if (query.getSteps().size() == 0) { // if no steps yet, first step must be added with addStep
            String completeFirstToken = pathTokenizer.nextToken().trim();
            String firstToken = removeDigits(completeFirstToken);
            //if (cloud.hasRole(firstToken)) {
            // you cannot start with a role.., should we throw exception?
            // naa, the following code will throw exception that node type does not exist.
            //}
            Step step = query.addStep(cloud.getNodeManager(firstToken));
            if (!firstToken.equals(completeFirstToken)) {
                query.setAlias(step, completeFirstToken);
            }
        }

        String searchDir = null; // outside the loop, so defaulting to previous searchDir
        while (pathTokenizer.hasMoreTokens()) {
            String completeToken = pathTokenizer.nextToken().trim();
            String token = removeDigits(completeToken);

            if (searchDirsTokenizer.hasMoreTokens()) {
                searchDir = searchDirsTokenizer.nextToken();
            }

            if (cloud.hasRole(token)) {
                if (!pathTokenizer.hasMoreTokens()) {
                    throw new BridgeException("Path cannot end with a role (" + path + "/" + searchDirs + ")");
                }
                String nodeManagerAlias = pathTokenizer.nextToken().trim();
                String nodeManagerName = removeDigits(nodeManagerAlias);
                NodeManager nodeManager = cloud.getNodeManager(nodeManagerName);
                RelationStep relationStep = query.addRelationStep(nodeManager, token, searchDir);

                /// make it possible to postfix with numbers manually
                if (!cloud.hasRole(completeToken)) {
                    query.setAlias(relationStep, completeToken);
                }
                if (!nodeManagerName.equals(nodeManagerAlias)) {
                    Step next = relationStep.getNext();
                    query.setAlias(next, nodeManagerAlias);
                }
            } else {
                NodeManager nodeManager = cloud.getNodeManager(token);
                RelationStep step = query.addRelationStep(nodeManager, null /* role */ , searchDir);
                if (!completeToken.equals(nodeManager.getName())) {
                    Step next = step.getNext();
                    query.setAlias(next, completeToken);
                }
            }
        }
        if (searchDirsTokenizer.hasMoreTokens()) {
            throw new BridgeException("Too many search directions (" + path + "/" + searchDirs + ")");
        }
        return list.subList(initialSize, list.size());
    }

    /**
     * Adds a number of fields. Fields is represented as a comma separated string.
     * @param query The query where the fields should be added to
     * @param fields a comma separated string of fields
     * @return The new stepfields
     */
    public static List addFields(Query query, String fields) {
        List result = new ArrayList();
        if (fields == null || fields.equals("")) {
            return result;
        }
        List list = StringSplitter.split(fields);
        Iterator i = list.iterator();
        while (i.hasNext()) {
            String fieldName = (String)i.next();
            result.add(query.addField(fieldName));
        }
        return result;

    }



    /**
     * Add startNodes to the first step with the correct type to the given query. The nodes are identified
     * by a String, which could be prefixed with a step-alias, if you want to add the nodes to
     * another then this found step.
     *
     * Furthermore may the nodes by identified by their alias, if they have one.
     * @param query query to add the startnodes
     * @param startNodes start nodes
     *
     * @see org.mmbase.module.core.ClusterBuilder#getMultiLevelSearchQuery(List, List, String, List, String, List, List, int)
     * (this is essentially a 'bridge' version of the startnodes part)
     */
    public static void addStartNodes(Query query, String startNodes) {
        if (startNodes == null || "".equals(startNodes) || "-1".equals(startNodes)) {
            return;
        }

        Step firstStep = null; // the 'default' step to which nodes are added. It is the first step which corresponds with the type of the first node.

        Iterator nodes = StringSplitter.split(startNodes).iterator();
        while (nodes.hasNext()) {
            String nodeAlias = (String) nodes.next(); // can be a string, prefixed with the step alias.
            Step step;                                // the step to which the node must be added (defaults to 'firstStep').
            String nodeNumber;                        // a node number or perhaps also a node alias.
            {
                int dot = nodeAlias.indexOf('.'); // this feature is not in core. It should be considered experimental
                if (dot == -1) {
                    step = firstStep;
                    nodeNumber = nodeAlias;
                } else {
                    step = query.getStep(nodeAlias.substring(0, dot));
                    nodeNumber = nodeAlias.substring(dot + 1);
                }
            }

            if (firstStep == null) { // firstStep not yet determined, do that now.
                Node node;
                try {
                    node = query.getCloud().getNode(nodeNumber);
                } catch (NotFoundException nfe) { // alias with dot?
                    node = query.getCloud().getNode(nodeAlias);
                }
                NodeManager nodeManager = node.getNodeManager();
                Iterator i = query.getSteps().iterator();
                while (i.hasNext()) {
                    Step queryStep = (Step) i.next();
                    NodeManager queryNodeManager = query.getCloud().getNodeManager(queryStep.getTableName());
                    if (queryNodeManager.equals(nodeManager) || queryNodeManager.getDescendants().contains(nodeManager)) {
                        // considering inheritance. ClusterBuilder is not doing that, but I think it is a bug.
                        firstStep = queryStep;
                        break;
                    }
                }
                if (firstStep == null) {
                    // odd..
                    // See also org.mmbase.module.core.ClusterBuilder#getMultiLevelSearchQuery
                    // specified a node which is not of the type of one of the steps.
                    // take as default the 'first' step (which will make the result empty, compatible with 1.6, bug #6440).
                    firstStep = (Step) query.getSteps().get(0);
                }
                if (step == null) {
                    step = firstStep;
                }
            }

            try {
                try {
                    if (query instanceof BasicQuery) { // not needed anymore when addNode(step, int) is in Query itself.
                        ((BasicQuery) query).addNode(step, Integer.parseInt(nodeNumber));
                    } else {
                        query.addNode(step, query.getCloud().getNode(nodeNumber)); // disadvantage is that the node itself must be retrieved.
                    }
                } catch (NumberFormatException nfe) {
                    query.addNode(step, query.getCloud().getNode(nodeNumber));          // node was specified by alias.
                }
            } catch (NotFoundException nnfe) {
                query.addNode(step, query.getCloud().getNode(nodeAlias)); // perhas an alias containing a dot?
            }
        }
    }

    /**
     * Takes the query, and does a count with the same constraints (so ignoring 'offset' and 'max')
     * @param query query as base for the count
     * @return number of results
     */
    public static int count(Query query) {
        Cloud cloud = query.getCloud();
        Query count = query.aggregatingClone();
        int type = query.isDistinct() ? AggregatedField.AGGREGATION_TYPE_COUNT_DISTINCT : AggregatedField.AGGREGATION_TYPE_COUNT;

        String resultName;
        if (query instanceof NodeQuery) {
            // all fields are present of the node-step, so, we could use the number field simply.
            resultName = "number";
            NodeQuery nq = (NodeQuery) query;
            count.addAggregatedField(nq.getNodeStep(), nq.getNodeManager().getField(resultName), type);
        } else {
            List fields = query.getFields();
            if (fields.size() == 0) { // for non-distinct queries always the number fields would be available
                throw new IllegalArgumentException("Cannot count queries with less than one field: " + query);
            }

            if (query.isDistinct() && fields.size() > 1) {
                // aha hmm. Well, we also find it ok if all fields are of one step, and 'number' is present
                resultName = null;
                Step step = null;
                Iterator i = fields.iterator();
                while (i.hasNext()) {
                    StepField sf = (StepField) i.next();
                    if (step == null) {
                        step = sf.getStep();
                    } else {
                        if (! step.equals(sf.getStep())) {
                            throw new UnsupportedOperationException("Cannot count distinct queries with fields of more than one step. Current fields: " + fields);
                        }
                    }
                    if (sf.getFieldName().equals("number")) {
                        resultName = sf.getFieldName();
                    }
                }
                if (resultName == null) {
                    throw new UnsupportedOperationException("Cannot count distinct queries with more than one field if 'number' field is missing. Current fields: " + fields);
                }
                count.addAggregatedField(step, cloud.getNodeManager(step.getTableName()).getField(resultName), type);
            } else {
                // simply take this one field
                StepField sf = (StepField) fields.get(0);
                Step step = sf.getStep();
                resultName = sf.getFieldName();
                count.addAggregatedField(step, cloud.getNodeManager(step.getTableName()).getField(resultName), type);
            }
        }
        Node result = (Node) cloud.getList(count).get(0);
        return result.getIntValue(resultName);
    }

    /**
     * Searches a list of Steps for a step with a certain name. (alias or tableName)
     * @param steps steps to search through
     * @param stepAlias alias to search for
     * @return The Step if found, otherwise null
     * @throws ClassCastException if list does not contain only Steps
     */
    public static Step searchStep(List steps, String stepAlias) {
        if (log.isDebugEnabled()) {
            log.info("Searching '" + stepAlias + "' in " + steps);
        }
        // first try aliases
        Iterator i = steps.iterator();
        while (i.hasNext()) {
            Step step = (Step)i.next();
            if (stepAlias.equals(step.getAlias())) {
                return step;
            }
        }
        // if no aliases found, try table names
        i = steps.iterator();
        while (i.hasNext()) {
            Step step = (Step)i.next();
            if (stepAlias.equals(step.getTableName())) {
                return step;
            }
        }
        return null;
    }

    /**
     * Returns the NodeQuery returning the given Node. This query itself is not very useful, because
     * you already have its result (the node), but it is convenient as a base query for many other
     * goals.
     * @param node Node to create the query from
     * @return A new NodeQuery object
     */
    public static NodeQuery createNodeQuery(Node node) {
        NodeManager nm = node.getNodeManager();
        NodeQuery query = node.getCloud().createNodeQuery(); // use the version which can acept more steops
        Step step       = query.addStep(nm);
        query.setNodeStep(step);
        query.addNode(step, node);
        return query;
    }

    /**
     * Returns a query to find the nodes related to the given node.
     * @param node start node
     * @param otherNodeManager node manager on the other side of the relation
     * @param role role of the relation
     * @param direction direction of the relation
     * @return A new NodeQuery object
     */
    public static NodeQuery createRelatedNodesQuery(Node node, NodeManager otherNodeManager, String role, String direction) {
        NodeQuery query = createNodeQuery(node);
        if (otherNodeManager == null) otherNodeManager = node.getCloud().getNodeManager("object");
        RelationStep step = query.addRelationStep(otherNodeManager, role, direction);
        query.setNodeStep(step.getNext());
        return query;
    }

    /**
     * Returns a query to find the relations nodes of the given node.
     * @param node start node
     * @param otherNodeManager node manager on the other side of the relation
     * @param role role of the relation
     * @param direction direction of the relation
     * @return A new NodeQuery object
     */
    public static NodeQuery createRelationNodesQuery(Node node, NodeManager otherNodeManager, String role, String direction) {
        NodeQuery query = createNodeQuery(node);
        if (otherNodeManager == null) otherNodeManager = node.getCloud().getNodeManager("object");
        RelationStep step = query.addRelationStep(otherNodeManager, role, direction);
        query.setNodeStep(step);
        return query;
    }


    /**
     * Add a sortorder (DESCENDING) on al the'number' fields of the query, on which there is not yet a
     * sortorder. This ensures that the query result is ordered uniquely.
     * @param q query to change
     * @return The changed Query
     */
    public static Query sortUniquely(Query q) {
        List steps = new ArrayList(q.getSteps());

        // remove the ones which are already sorted
        Iterator i = q.getSortOrders().iterator();
        while (i.hasNext()) {
            SortOrder sortOrder = (SortOrder)i.next();
            if (sortOrder.getField().getFieldName().equals("number")) {
                Step step = sortOrder.getField().getStep();
                steps.remove(step);
            }
        }
        // add sort order on the remaining ones:
        i = steps.iterator();
        while (i.hasNext()) {
            Step step = (Step)i.next();
            q.addSortOrder(q.createStepField(step, "number"), SortOrder.ORDER_DESCENDING);
        }
        return q;
    }

}
