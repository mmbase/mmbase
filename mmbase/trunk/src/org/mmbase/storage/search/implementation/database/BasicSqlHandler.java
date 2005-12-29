/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation.database;

import org.mmbase.bridge.Field;
import org.mmbase.module.core.MMBase;
import org.mmbase.storage.search.*;
import org.mmbase.util.logging.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;

/**
 * Basic implementation.
 *
 * @author Rob van Maris
 * @version $Id: BasicSqlHandler.java,v 1.55 2005-12-29 15:17:22 michiel Exp $
 * @since MMBase-1.7
 */

public class BasicSqlHandler implements SqlHandler {

    private static final Logger log = Logging.getLoggerInstance(BasicSqlHandler.class);

    private static final SimpleDateFormat dateFormat          = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final FieldPosition dontcareFieldPosition = new FieldPosition(DateFormat.YEAR_FIELD);
    
    protected MMBase mmbase = MMBase.getMMBase();

    /**
     * Constructor.
     */
    public BasicSqlHandler() {
    }

    /**
     * Utility method, modifies strings for use in SQL statements.
     * This amounts to replacing all single quotes by two single quotes.
     *
     * @param str The input string.
     * @return The modified string.
     */
    // XXX must wildcard characters be escaped?
    // XXX  perhaps place this somewhere else?
    public static String toSqlString(String str) {
        String result = str;
        if (str != null) {
            int offset = str.indexOf('\'');
            if (offset != -1) {
                // At least one single quote found.
                StringBuffer sb = new StringBuffer(str.length() + 4);
                int start = 0;
                do {
                    sb.append(str.substring(start, offset)).append("''");
                    start = offset + 1;
                    offset = str.indexOf('\'', start);
                } while (offset != -1);
                sb.append(str.substring(start, str.length()));
                result = sb.toString();
                if (log.isDebugEnabled()) {
                    log.debug("converted string \"" + str + "\" to \"" + result + "\"");
                }
            }
        }
        return result;
    }
    /**
     * Tests if a case sensitivity for a field constraint is false
     * and relevant, i.e. the constraint is set to case insensitive and
     * the field has string type.
     *
     * @param constraint The constraint.
     * @return true if the constraint is set to case insensitive
     *         and the field has string type, false otherwise.
     */
    private static boolean isRelevantCaseInsensitive(FieldConstraint constraint) {
        return !constraint.isCaseSensitive()
        && (constraint.getField().getType() == Field.TYPE_STRING
        || constraint.getField().getType() == Field.TYPE_XML);
    }

    /**
     * Wether the 'LOWER' function needs to be used to implement case insensitivity. This is
     * not always the case, because some database only match case insensitively, in which case it
     * does not make sense to lowercase.
     */
    protected boolean useLower(FieldCompareConstraint constraint) {
        return true;
    }

    protected void appendDateValue(StringBuffer sb, Date value) {
        dateFormat.format(value, sb, dontcareFieldPosition);
    }

    /**
     * Represents field value as a string, appending the result to a
     * stringbuffer.
     * <p>
     * Depending on the fieldType:
     * <ul>
     * <li> String values are converted to SQL-formatted string,
     *  surrounded by single quotes,
     * <li>Numerical values are represented as integer (integral values)
     *  or floating point.
     * </ul>
     *
     * @param sb The stringbuffer to append to.
     * @param value The field value.
     * @param toLowerCase True when <code>String</code> must be converted to
     *        lower case.
     * @param fieldType The field type.
     */
    // TODO: elaborate javadoc, add to SqlHandler interface?
    public void appendFieldValue(StringBuffer sb, Object value, boolean toLowerCase, int fieldType) {
        if (fieldType == Field.TYPE_STRING || fieldType == Field.TYPE_XML) {
            // escape single quotes in string
            String stringValue = toSqlString((String) value);
            // to lowercase when case insensitive
            if (toLowerCase) {
                stringValue = stringValue.toLowerCase();
            }
            sb.append("'").
            append(stringValue).
            append("'");
        } else if (fieldType == Field.TYPE_DATETIME) {
            if (value instanceof Integer) {
                sb.append(((Integer) value).intValue());
            } else {
                sb.append("'");
                appendDateValue(sb, (Date) value);
                sb.append("'");
            }
        } else if (fieldType == Field.TYPE_BOOLEAN) {
            boolean isTrue = ((Boolean) value).booleanValue();
            if (isTrue) {
                sb.append("TRUE");
            } else {
                sb.append("FALSE");
            }
        } else {
            // Numerical field:
            // represent integeral Number values as integer, other
            // Number values as floating point, and String values as-is.
            if (value instanceof Number) {
                Number numberValue = (Number) value;
                if (numberValue.doubleValue() == numberValue.intValue()) {
                    // Integral Number value.
                    sb.append(numberValue.intValue());
                } else {
                    // Non-integral Number value.
                    sb.append(numberValue.doubleValue());
                }
            } else {
                // String value.
                sb.append((String) value);
            }
        }
    }

    // javadoc is inherited
    // XXX what exception to throw when an unsupported feature is
    // encountered (currently throws UnsupportedOperationException)?
    public String toSql(SearchQuery query, SqlHandler firstInChain)
    throws SearchQueryException {
        // XXX should table and field aliases be tested for uniqueness?

        // Test for at least 1 step and 1 field.
        if (query.getSteps().isEmpty()) {
            throw new IllegalStateException(
            "Searchquery has no step (at leas 1 step is required).");
        }
        if (query.getFields().isEmpty()) {
            throw new IllegalStateException(
            "Searchquery has no field (at least 1 field is required).");
        }

        // SELECT
        StringBuffer sbQuery = new StringBuffer("SELECT ");

        // DISTINCT
        // Note that DISTINCT can be omitted for an aggregating query.
        // It is ommitted to make the resulting SQL more portable,
        // some databases will otherwise report a syntax error (e.g. Informix).
        if (query.isDistinct() && !query.isAggregating()) {
            sbQuery.append("DISTINCT ");
        }

        firstInChain.appendQueryBodyToSql(sbQuery, query, firstInChain);

        String strSQL = sbQuery.toString();
        if (log.isDebugEnabled()) {
            log.debug("generated SQL: " + strSQL);
        }
        return strSQL;
    }


    /**
     * @since MMBase-1.8
     */
    protected void appendRelationConstraints(StringBuffer sbRelations, RelationStep relationStep, boolean multipleSteps) {
        
        Step previousStep = relationStep.getPrevious();
        Step nextStep = relationStep.getNext();
        if (sbRelations.length() > 0) {
            sbRelations.append(" AND ");
        }
        switch (relationStep.getDirectionality()) {
        case RelationStep.DIRECTIONS_SOURCE:
            sbRelations.append('(');
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            if (relationStep.getCheckedDirectionality()) {
                sbRelations.append(" AND ");
                appendField(sbRelations, relationStep, "dir", multipleSteps);
                sbRelations.append("<>1");
            }
            break;
            
        case RelationStep.DIRECTIONS_DESTINATION:
            sbRelations.append('(');
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            break;
            
        case RelationStep.DIRECTIONS_BOTH:
            if (relationStep.getRole() != null) {
                sbRelations.append("(((");
            } else {
                sbRelations.append("((");
            }
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            if (relationStep.getCheckedDirectionality()) {
                sbRelations.append(" AND ");
                appendField(sbRelations, relationStep, "dir", multipleSteps);
                sbRelations.append("<>1");
            }
            sbRelations.append(") OR (");
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            if (relationStep.getRole() != null) {
                sbRelations.append("))");
            } else {
                sbRelations.append(')');
            }
            break;
            
        case RelationStep.DIRECTIONS_ALL:
            if (relationStep.getRole() != null) {
                sbRelations.append("(((");
            } else {
                sbRelations.append("((");
            }
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            sbRelations.append(") OR (");
            appendField(sbRelations, previousStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "snumber", multipleSteps);
            sbRelations.append(" AND ");
            appendField(sbRelations, nextStep, "number", multipleSteps);
            sbRelations.append('=');
            appendField(sbRelations, relationStep, "dnumber", multipleSteps);
            if (relationStep.getRole() != null) {
                sbRelations.append("))");
            } else {
                sbRelations.append(')');
            }
            break;
            
        case RelationStep.DIRECTIONS_EITHER:
            throw new UnsupportedOperationException("Directionality 'EITHER' is not (yet) supported");
            
        default: // Invalid directionality value.
            throw new IllegalStateException(
                                            "Invalid directionality value: " + relationStep.getDirectionality());
        }
        if (relationStep.getRole() != null) {
            sbRelations.append(" AND ");
            appendField(sbRelations, relationStep, "rnumber", multipleSteps);
            sbRelations.append('=').append(relationStep.getRole());
        }
        sbRelations.append(')');
    }

    // javadoc is inherited
    public void appendQueryBodyToSql(StringBuffer sb, SearchQuery query, SqlHandler firstInChain) throws SearchQueryException {

        // Buffer expressions for included nodes, like
        // "x.number in (...)".
        StringBuffer sbNodes = new StringBuffer();

        // Buffer expressions for relations, like
        // "x.number = r.snumber AND y.number = r.dnumber".
        StringBuffer sbRelations = new StringBuffer();

        // Buffer fields to group by, like
        // "alias1, alias2, ..."
        StringBuffer sbGroups = new StringBuffer();

        boolean multipleSteps = query.getSteps().size() > 1;

        // Fields expression
        List lFields = new ArrayList();
        lFields.addAll(query.getFields());

        // When 'distinct', make sure all fields used for sorting are
        // included in the query.
        // Some databases require this (including PostgreSQL).
        // By fixing this here, the result of the query remains consistent
        // across databases, while requiring no modification in the calling
        // code.
        if (query.isDistinct()) {
            Iterator iSortOrder = query.getSortOrders().iterator();
            while (iSortOrder.hasNext()) {
                SortOrder sortOrder = (SortOrder) iSortOrder.next();
                StepField field = sortOrder.getField();
                if (lFields.indexOf(field) == -1) {
                    lFields.add(field);
                }
            }
        }

        boolean storesAsFile = mmbase.getStorageManagerFactory().hasOption(org.mmbase.storage.implementation.database.Attributes.STORES_BINARY_AS_FILE);
        Iterator iFields = lFields.iterator();
        boolean appended = false;
        while (iFields.hasNext()) {
            StepField field = (StepField) iFields.next();
            if (field.getType() == Field.TYPE_BINARY && storesAsFile) continue; 
            if (appended) {
                sb.append(',');
            }
            appended = true;
            // fieldname prefixed by table alias.
            Step step = field.getStep();
            String fieldName = field.getFieldName();
            String fieldAlias = field.getAlias();

            if (field instanceof AggregatedField) {
                int aggregationType = ((AggregatedField) field).getAggregationType();
                if (aggregationType == AggregatedField.AGGREGATION_TYPE_GROUP_BY) {

                    // Group by.
                    appendField(sb, step, fieldName, multipleSteps);

                    // Append to "GROUP BY"-buffer.
                    if (sbGroups.length() > 0) {
                        sbGroups.append(',');
                    }
                    if (fieldAlias != null) {
                        sbGroups.append(getAllowedValue(fieldAlias));
                    } else {
                        appendField(sbGroups, step, fieldName, multipleSteps);
                    }
                } else {

                    // Aggregate function.
                    switch (aggregationType) {
                    case AggregatedField.AGGREGATION_TYPE_COUNT:
                        sb.append("COUNT(");
                        break;

                    case AggregatedField.AGGREGATION_TYPE_COUNT_DISTINCT:
                        sb.append("COUNT(DISTINCT ");
                        break;

                    case AggregatedField.AGGREGATION_TYPE_MIN:
                        sb.append("MIN(");
                        break;

                    case AggregatedField.AGGREGATION_TYPE_MAX:
                        sb.append("MAX(");
                        break;

                    default:
                        throw new IllegalStateException("Invalid aggregationType value: " + aggregationType);
                    }
                    appendField(sb, step, fieldName, multipleSteps);
                    sb.append(')');
                }

            } else {

                // Non-aggregate field.
                appendField(sb, step, fieldName, multipleSteps);
            }

            // Field alias.
            if (fieldAlias != null) {
                sb.append(" AS ").append(getAllowedValue(fieldAlias));
            }

        }

        // Tables
        sb.append(" FROM ");
        Iterator iSteps = query.getSteps().iterator();
        Step previousStep = null;
        while (iSteps.hasNext()) {
            Step step = (Step) iSteps.next();
            appendTableName(sb, step, previousStep);

            if (iSteps.hasNext()) {
                sb.append(",");
            }

            // Included nodes.
            SortedSet nodes = step.getNodes();
            if (nodes.size() > 0) {
                if (sbNodes.length() > 0) {
                    sbNodes.append(" AND ");
                }
                appendField(sbNodes, step, "number", multipleSteps);
                if (nodes.size() > 1) {
                    // only use IN(...) if there are really more numbers
                    sbNodes.append(" IN (");
                    Iterator iNodes = nodes.iterator();
                    while (iNodes.hasNext()) {
                        Integer node = (Integer) iNodes.next();
                        sbNodes.append(node);
                        if (iNodes.hasNext()) {
                            sbNodes.append(',');
                        }
                    }
                    sbNodes.append(')');
                } else {
                    // otherwise use equals, which is a LOT faster in some cases
                    sbNodes.append('=');
                    sbNodes.append(nodes.first());
                }
            }

            // Relation steps.
            if (step instanceof RelationStep){
                appendRelationConstraints(sbRelations, (RelationStep) step, multipleSteps);
            }
            previousStep = step;
        }

        // Constraints
        StringBuffer sbConstraints = new StringBuffer();
        sbConstraints.append(sbNodes); // Constraints by included nodes.
        if (sbConstraints.length() > 0 && sbRelations.length() > 0) {
            sbConstraints.append(" AND ");
        }
        sbConstraints.append(sbRelations); // Constraints by relations.
        if (query.getConstraint() != null) {
            Constraint constraint = query.getConstraint();
            if (sbConstraints.length() > 0) {
                // Combine constraints.
                sbConstraints.append(" AND ");
                if (constraint instanceof CompositeConstraint) {
                    appendCompositeConstraintToSql(
                    sbConstraints, (CompositeConstraint) constraint,
                    query, false, true, firstInChain);
                } else {
                    firstInChain.appendConstraintToSql(
                    sbConstraints, constraint, query,
                    false, true);
                }
            } else {
                // Only regular constraints.
                if (constraint instanceof CompositeConstraint) {
                    appendCompositeConstraintToSql(
                    sbConstraints, (CompositeConstraint) constraint,
                    query, false, false, firstInChain);
                } else {
                    firstInChain.appendConstraintToSql(
                    sbConstraints, constraint, query,
                    false, false);
                }
            }
        }
        if (sbConstraints.length() > 0) {
            sb.append(" WHERE ").
            append(sbConstraints.toString());
        }

        // GROUP BY
        if (sbGroups.length() > 0) {
            sb.append(" GROUP BY ").
            append(sbGroups.toString());
        }

        appendSortOrders(sb, query);
    }


    /**
     * @param sb
     * @param step
     * @since MMBase-1.8
     */
    protected void appendTableName(StringBuffer sb, Step step, Step previousStep) {
        String tableName = step.getTableName();
        String tableAlias = step.getAlias();

        // Tablename, prefixed with basename and underscore
        sb.append(mmbase.getBaseName()).
        append("_").
        //Currently no replacement strategy is implemented for
        //invalid tablenames.
        //This would be useful, but requires modification to
        //the insert/update/delete code as well.
        //append(getAllowedValue(tableName));
        append(tableName);

        // Table alias (tablename when table alias not set).
        if (tableAlias != null) {
            sb.append(" ").
            append(getAllowedValue(tableAlias));
        } else {
            sb.append(" ").
            append(getAllowedValue(tableName));
        }
    }


    /**
     * @since MMBase-1.8
     */
    protected StringBuffer appendSortOrderDirection(StringBuffer sb, SortOrder sortOrder) throws IllegalStateException {
        // Sort direction.
        switch (sortOrder.getDirection()) {
        case SortOrder.ORDER_ASCENDING:
            sb.append(" ASC");
            break;            
        case SortOrder.ORDER_DESCENDING:
            sb.append(" DESC");
            break;            
        default: // Invalid direction value.
            throw new IllegalStateException("Invalid direction value: " + sortOrder.getDirection());
        }
        return sb;
    }

    /**
     * @since MMBase-1.8
     */
    protected StringBuffer appendSortOrderField(StringBuffer sb, SortOrder sortOrder, boolean multipleSteps) {
         boolean uppered = false;
         if (! sortOrder.isCaseSensitive() && sortOrder.getField().getType() == Field.TYPE_STRING) {
             sb.append("UPPER(");
             uppered = true;
         }
         // Fieldname.
         Step step = sortOrder.getField().getStep();
         appendField(sb, step, sortOrder.getField().getFieldName(), multipleSteps);
         if (uppered) {
             sb.append("),");
             // also order by field itself, so ensure uniqueness.
             appendField(sb, step, sortOrder.getField().getFieldName(), multipleSteps);
         }
         return sb;
    }

    /**
     * @since MMBase-1.8
     */
    protected StringBuffer appendSortOrders(StringBuffer sb, SearchQuery query) {
        boolean multipleSteps = query.getSteps().size() > 1;
        List sortOrders = query.getSortOrders();
        if (sortOrders.size() > 0) {
            sb.append(" ORDER BY ");
            Iterator iSortOrders = sortOrders.iterator();
            while (iSortOrders.hasNext()) {
                SortOrder sortOrder = (SortOrder) iSortOrders.next();
                appendSortOrderField(sb, sortOrder, multipleSteps);
                appendSortOrderDirection(sb, sortOrder);
                if (iSortOrders.hasNext()) {
                    sb.append(",");
                }
            }
        }
        return sb;
    }

    /**
     * Appends the 'LIKE' operator for the given case sensitiviy. Some databases support a case
     * insensitive LIKE ('ILIKE'). Implementations for those database can override this method.
     *
     * @return The string buffer.
     */
    protected StringBuffer appendLikeOperator(StringBuffer sb, boolean caseSensitive) {
        sb.append(" LIKE ");
        return sb;
    }


    /*
    protected StringBuffer appendRegularExpressionOperator(StringBuffer sb, boolean caseSensitive) {
        sb.append(" ~ ");
        return sb;
    }
    */

    /**
     * @javadoc
     */
    protected void appendDateField(StringBuffer sb, Step step, String fieldName, boolean multipleSteps, int datePart) {
        String datePartFunction = null;
        switch (datePart) {
        case -1:
            break;
        case FieldValueDateConstraint.YEAR:
            datePartFunction = "YEAR";
            break;
        case FieldValueDateConstraint.MONTH:
            datePartFunction = "MONTH";
            break;
        case FieldValueDateConstraint.DAY_OF_MONTH:
            datePartFunction = "DAY";
            break;
        case FieldValueDateConstraint.HOUR:
            datePartFunction = "HOUR";
            break;
        case FieldValueDateConstraint.MINUTE:
            datePartFunction = "MINUTE";
                break;
        case FieldValueDateConstraint.SECOND:
            datePartFunction = "SECOND";
            break;
        default:
            throw new UnsupportedOperationException("This date partition function (" + datePart + ") is not supported.");
        }
        if (datePartFunction != null) {
            sb.append("EXTRACT(");
            sb.append(datePartFunction);
            sb.append(" FROM ");
        }
        appendField(sb, step, fieldName, multipleSteps);
        if (datePartFunction != null) {
            sb.append(")");
        }
    }

    // javadoc is inherited
    // XXX what exception to throw when an unsupported constraint is
    // encountered (currently throws UnsupportedOperationException)?
    public void appendConstraintToSql(StringBuffer sb, Constraint constraint,
    SearchQuery query, boolean inverse, boolean inComposite) {

        // Net effect of inverse setting with constraint inverse property.
        boolean overallInverse = inverse ^ constraint.isInverse();

        boolean multipleSteps = query.getSteps().size() > 1;

        boolean toLowerCase = false;
        if (constraint instanceof FieldConstraint) {

            // Field constraint
            FieldConstraint fieldConstraint = (FieldConstraint) constraint;
            StepField field = fieldConstraint.getField();
            int fieldType = field.getType();
            String fieldName = field.getFieldName();
            Step step = field.getStep();


            // hardly nice and OO, the following code.
            //
            if (fieldConstraint instanceof FieldValueInConstraint) {

                // Field value-in constraint
                FieldValueInConstraint valueInConstraint = (FieldValueInConstraint) fieldConstraint;
                SortedSet values = valueInConstraint.getValues();
                if (values.size() == 0) {
                    throw new IllegalStateException(
                    "Field value-in constraint specifies no values "
                    + "(at least 1 value is required).");
                }
                if (isRelevantCaseInsensitive(fieldConstraint)) {
                    // case insensitive
                    sb.append("LOWER(");
                    appendField(sb, step, fieldName, multipleSteps);
                    sb.append(")");
                } else {
                    // case sensitive or case irrelevant
                    appendField(sb, step, fieldName, multipleSteps);
                }

                if (values.size() > 1) {
                    // only use IN(...) if there are really more numbers
                    sb.append(overallInverse? " NOT IN (": " IN (");
                    Iterator iValues = values.iterator();
                    while (iValues.hasNext()) {
                        Object value = iValues.next();
                        appendFieldValue(sb, value,
                            !fieldConstraint.isCaseSensitive(), fieldType);
                        if (iValues.hasNext()) {
                            sb.append(",");
                        }
                    }
                    sb.append(")");
                } else {
                    // otherwise use equals, which is a LOT faster in some cases
                    sb.append(overallInverse? "<>": "=");
                    appendFieldValue(sb, values.first(),
                        !fieldConstraint.isCaseSensitive(), fieldType);
                }

            } else if (fieldConstraint instanceof FieldValueBetweenConstraint) {

                // Field value-between constraint
                FieldValueBetweenConstraint valueBetweenConstraint = (FieldValueBetweenConstraint) fieldConstraint;
                if (isRelevantCaseInsensitive(fieldConstraint)) {
                    // case insensitive
                    sb.append("LOWER(");
                    appendField(sb, step, fieldName, multipleSteps);
                    sb.append(")");
                } else {
                    // case sensitive or case irrelevant
                    appendField(sb, step, fieldName, multipleSteps);
                }
                sb.append(overallInverse? " NOT BETWEEN ": " BETWEEN ");
                appendFieldValue(sb, valueBetweenConstraint.getLowerLimit(),
                    !fieldConstraint.isCaseSensitive(), fieldType);
                sb.append(" AND ");
                appendFieldValue(sb, valueBetweenConstraint.getUpperLimit(),
                    !fieldConstraint.isCaseSensitive(), fieldType);

            } else if (fieldConstraint instanceof FieldNullConstraint) {

                // Field null constraint
                appendField(sb, step, fieldName, multipleSteps);
                sb.append(overallInverse? " IS NOT NULL": " IS NULL");

            } else if (fieldConstraint instanceof FieldCompareConstraint) {

                // Field compare constraint
                FieldCompareConstraint fieldCompareConstraint = (FieldCompareConstraint) fieldConstraint;

                // Negate by leading NOT, unless it's a LIKE constraint,
                // in which case NOT LIKE is used.
                if (fieldCompareConstraint.getOperator() != FieldCompareConstraint.LIKE) {
                    sb.append(overallInverse? "NOT (": "");
                }

                if (fieldConstraint instanceof FieldValueDateConstraint) {
                    int part = ((FieldValueDateConstraint)fieldConstraint).getPart();
                    appendDateField(sb, step, fieldName, multipleSteps, part);
                } else if (useLower(fieldCompareConstraint) && isRelevantCaseInsensitive(fieldConstraint)) {
                    // case insensitive and database needs it
                    sb.append("LOWER(");
                    appendField(sb, step, fieldName, multipleSteps);
                    sb.append(")");
                } else {
                    // case sensitive or case irrelevant
                    appendField(sb, step, fieldName, multipleSteps);
                }
                switch (fieldCompareConstraint.getOperator()) {
                case FieldCompareConstraint.LESS:
                    sb.append("<");
                    break;

                case FieldCompareConstraint.LESS_EQUAL:
                    sb.append("<=");
                    break;

                case FieldCompareConstraint.EQUAL:
                    sb.append("=");
                    break;

                case FieldCompareConstraint.NOT_EQUAL:
                    sb.append("<>");
                    break;

                case FieldCompareConstraint.GREATER:
                    sb.append(">");
                    break;

                case FieldCompareConstraint.GREATER_EQUAL:
                    sb.append(">=");
                    break;

                case FieldCompareConstraint.LIKE:
                    if (overallInverse) {
                        sb.append(" NOT");
                    }
                    appendLikeOperator(sb, fieldConstraint.isCaseSensitive());
                    break;
                    /*
                case FieldValueConstraint.REGEXP:
                    sb.append(getRegularExpressionOperator());
                    break;
                    */
                default:
                    throw new IllegalStateException("Unknown operator value in constraint: " + fieldCompareConstraint.getOperator());
                }
                if (fieldCompareConstraint instanceof FieldValueConstraint) {
                    // FieldValueConstraint.
                    FieldValueConstraint fieldValueConstraint = (FieldValueConstraint) fieldCompareConstraint;
                    Object value = fieldValueConstraint.getValue();
                    appendFieldValue(sb, value, useLower(fieldValueConstraint) && isRelevantCaseInsensitive(fieldValueConstraint), fieldType);
                } else if (fieldCompareConstraint instanceof CompareFieldsConstraint) {
                    // CompareFieldsConstraint
                    CompareFieldsConstraint compareFieldsConstraint = (CompareFieldsConstraint) fieldCompareConstraint;
                    StepField field2 = compareFieldsConstraint.getField2();
                    String fieldName2 = field2.getFieldName();
                    Step step2 = field2.getStep();
                    if (useLower(fieldCompareConstraint) && isRelevantCaseInsensitive(fieldConstraint)) {
                        // case insensitive
                        sb.append("LOWER(");
                        appendField(sb, step2, fieldName2, multipleSteps);
                        sb.append(")");
                    } else {
                        // case sensitive or case irrelevant
                        appendField(sb, step2, fieldName2, multipleSteps);
                    }
                } else {
                    throw new UnsupportedOperationException("Unknown constraint type: " + constraint.getClass().getName());
                }
                // Negate by leading NOT, unless it's a LIKE constraint,
                // in which case NOT LIKE is used.
                if (fieldCompareConstraint.getOperator() != FieldCompareConstraint.LIKE) {
                    sb.append(overallInverse? ")": "");
                }
            } else {
                throw new UnsupportedOperationException("Unknown constraint type: " + constraint.getClass().getName());
            }

        } else if (constraint instanceof CompositeConstraint) {
            throw new IllegalArgumentException("Illegal constraint type for this method: " + constraint.getClass().getName());
        } else if (constraint instanceof LegacyConstraint) {
            LegacyConstraint legacyConstraint = (LegacyConstraint) constraint;
            if (legacyConstraint.getConstraint().trim().length() != 0) {
                if (overallInverse) {
                    sb.append("NOT ");
                }
                if (overallInverse || inComposite) {
                    sb.append("(");
                }
                sb.append(legacyConstraint.getConstraint());
                if (overallInverse || inComposite) {
                    sb.append(")");
                }
            }
        } else {
            throw new UnsupportedOperationException(
            "Unknown constraint type: " + constraint.getClass().getName());
        }
    }

    // javadoc is inherited
    public int getSupportLevel(int feature, SearchQuery query)
    throws SearchQueryException {
        int result;
        switch (feature) {
            case SearchQueryHandler.FEATURE_MAX_NUMBER:
                if (query.getMaxNumber() == SearchQuery.DEFAULT_MAX_NUMBER){
                    result = SearchQueryHandler.SUPPORT_OPTIMAL;
                } else {
                    result = SearchQueryHandler.SUPPORT_NONE;
                }
                break;

            case SearchQueryHandler.FEATURE_OFFSET:
                if (query.getOffset() == SearchQuery.DEFAULT_OFFSET) {
                    result = SearchQueryHandler.SUPPORT_OPTIMAL;
                } else {
                    result = SearchQueryHandler.SUPPORT_NONE;
                }
                break;

            default:
                result = SearchQueryHandler.SUPPORT_NONE;
        }
        return result;
    }

    // javadoc is inherited
    public int getSupportLevel(Constraint constraint, SearchQuery query)
            throws SearchQueryException {
        return constraint.getBasicSupportLevel();
    }

    // javadoc is inherited
    public String getAllowedValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
        return (String) MMBase.getMMBase().getStorageManagerFactory().getStorageIdentifier(value);
    }

    /**
     * Represents a CompositeConstraint object as a constraint in SQL format,
     * appending the result to a stringbuffer.
     * When it is part of a composite expression, it will be surrounded by
     * parenthesis when needed.
     *
     * @param sb The stringbuffer to append to.
     * @param compositeConstraint The composite constraint.
     * @param query The searchquery containing the constraint.
     * @param inverse True when the inverse constraint must be represented,
     *        false otherwise.
     * @param inComposite True when the constraint is part of
     *        a composite expression.
     * @param firstInChain The first element in the chain of handlers.
     *        At some point <code>appendConstraintToSql()</code> will have
     *        to be called on this handler, to generate the constraints in
     *        the composite.
     */
    protected void appendCompositeConstraintToSql(
    StringBuffer sb, CompositeConstraint compositeConstraint, SearchQuery query,
    boolean inverse, boolean inComposite, SqlHandler firstInChain)
    throws SearchQueryException {

        // Net effect of inverse setting with constraint inverse property.
        boolean overallInverse = inverse ^ compositeConstraint.isInverse();

        String strOperator = null;
        if (compositeConstraint.getLogicalOperator() == CompositeConstraint.LOGICAL_AND) {
            if (overallInverse) {
                // Inverse: NOT (A and B and C) = NOT A or NOT B or NOT C
                strOperator = " OR ";
            } else {
                strOperator = " AND ";
            }
        } else if (compositeConstraint.getLogicalOperator() == CompositeConstraint.LOGICAL_OR) {
            if (overallInverse) {
                // Inverse: NOT (A or B or C) = NOT A and NOT B and NOT C
                strOperator = " AND ";
            } else {
                strOperator = " OR ";
            }
        } else {
            throw new IllegalStateException(
            "Invalid logical operator: " + compositeConstraint.getLogicalOperator()
            + ", must be either "
            + CompositeConstraint.LOGICAL_AND + " or " + CompositeConstraint.LOGICAL_OR);
        }
        List childs = compositeConstraint.getChilds();

        // Test for at least 1 child.
        if (childs.isEmpty()) {
            throw new IllegalStateException(
            "Composite constraint has no child (at least 1 child is required).");
        }

        boolean hasMultipleChilds = childs.size() > 1;

        // Opening parenthesis, when part of composite expression
        // and with multiple childs.
        if (inComposite && hasMultipleChilds) {
            sb.append("(");
        }

        // Recursively append all childs.
        Iterator iChilds = childs.iterator();
        while (iChilds.hasNext()) {
            Constraint child = (Constraint) iChilds.next();
            if (child instanceof CompositeConstraint) {
                // Child is composite constraint.
                appendCompositeConstraintToSql(
                    sb, (CompositeConstraint) child, query,
                    overallInverse, hasMultipleChilds, firstInChain);
            } else {
                // Child is non-composite constraint.
                firstInChain.appendConstraintToSql(
                    sb, child, query, overallInverse, hasMultipleChilds);
            }
            if (iChilds.hasNext()) {
                sb.append(strOperator);
            }
        }

        // Closing parenthesis, when part of composite expression
        // and with multiple childs.
        if (inComposite && hasMultipleChilds) {
            sb.append(")");
        }
    }

    /**
     * Creates an identifier for a field, and appends it to a stringbuffer.
     * The identifier is constructed from the fieldname, optionally prefixed
     * by the tablename or the tablealias - when available.
     *
     * @param sb The stringbuffer to append to.
     * @param step The Step the field belongs to.
     * @param fieldName The fields fieldname.
     * @param includeTablePrefix <code>true</code> when the fieldname must be
     *        prefixed with the tablename or tablealias (e.g. like in "images.number"),
     *        <code>false</code> otherwise.
     */
    // TODO RvM: add to interface, add javadoc
    protected void appendField(StringBuffer sb, Step step,
            String fieldName, boolean includeTablePrefix) {

        String tableAlias = step.getAlias();
        if (includeTablePrefix) {
            if (tableAlias != null) {
                sb.append(getAllowedValue(tableAlias));
            } else {
                sb.append(getAllowedValue(step.getTableName()));
            }
            sb.append(".");
        }
        sb.append(getAllowedValue(fieldName));
    }

}
