/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation.database;

import java.util.*;
import org.mmbase.storage.search.*;
import org.mmbase.util.logging.*;

/**
 * The PostgreSQL query handler, implements {@link
 * org.mmbase.storage.search.implementation.database.SqlHandler SqlHandler}
 * for standard PostgreSql functionality.
 * <br />
 * Derived from {@link BasicSqlHandler BasicSqlHandler}, overrides
 * <ul>
 * <li>{@link #toSql toSql()}, implements {@link
 * org.mmbase.storage.search.SearchQueryHandler#FEATURE_MAX_NUMBER
 * FEATURE_MAX_NUMBER} and {@link
 * org.mmbase.storage.search.SearchQueryHandler#FEATURE_OFFSET
 * FEATURE_OFFSET}, by adding a construct like "<code>LIMIT 20</code>" or
 * "<code>LIMIT 20 OFFSET 80</code>" after the body, when appropriate.
 * <li>{@link #getSupportLevel(int,SearchQuery) getSupportLevel(int,SearchQuery)},
 * returns {@link
 * org.mmbase.storage.search.SearchQueryHandler#SUPPORT_OPTIMAL
 * SUPPORT_OPTIMAL} for these features, delegates to the superclass for
 * other features.
 * </ul>
 *
 * @author Rob van Maris
 * @version $Id: PostgreSqlSqlHandler.java,v 1.8 2005-01-25 12:45:19 pierre Exp $
 * @since MMBase-1.7
 */
public class PostgreSqlSqlHandler extends BasicSqlHandler implements SqlHandler {

    /** Logger instance. */
    private static final Logger log = Logging.getLoggerInstance(PostgreSqlSqlHandler.class);

    /**
     * Constructor.
     *
     * @param disallowedValues Map mapping disallowed table/fieldnames
     *        to allowed alternatives.
     */
    public PostgreSqlSqlHandler() {
        super();
    }

    // javadoc is inherited
    public int getSupportLevel(int feature, SearchQuery query) throws SearchQueryException {
        int result;
        switch (feature) {
            case SearchQueryHandler.FEATURE_MAX_NUMBER:
                result = SearchQueryHandler.SUPPORT_OPTIMAL;
                break;

            case SearchQueryHandler.FEATURE_OFFSET:
                result = SearchQueryHandler.SUPPORT_OPTIMAL;
                break;
                /*
            case SearchQueryHandler.FEATURE_REGEXP:
                result = SearchQueryHandler.SUPPORT_OPTIMAL;
                break;
                */
            default:
                result = super.getSupportLevel(feature, query);
        }
        return result;
    }


    protected boolean useLower(FieldCompareConstraint constraint) {
        if (constraint.getOperator() == FieldValueConstraint.LIKE) {
            return false;
        } else {
            return true;
        }
    }


    protected StringBuffer appendLikeOperator(StringBuffer sb, boolean caseSensitive) {
        if (caseSensitive) {
            sb.append(" LIKE ");
        } else {
            sb.append(" ILIKE ");
        }
        return sb;
    }

    /*
    protected StringBuffer appendRegularExpressionOperator(StringBuffer sb, boolean caseSensitive) {
        if (caseSensitive) {
            sb.append(" ~ ");
        } else {
            sb.append(" ~* ");
        }
        return sb;
    }
    */

    /**
     * @javadoc
     */
    protected void appendDateField(StringBuffer sb, Step step, String fieldName, boolean multipleSteps, int datePart) {
        String datePartFunction = null;
        switch (datePart) {
            case FieldValueDateConstraint.CENTURY:
                datePartFunction = "CENTURY";
                break;
            case FieldValueDateConstraint.QUARTER:
                datePartFunction = "QUARTER";
                break;
            case FieldValueDateConstraint.WEEK:
                datePartFunction = "WEEK";
                break;
            case FieldValueDateConstraint.DAY_OF_YEAR:
                datePartFunction = "DAYOFYEAR";
                break;
            case FieldValueDateConstraint.DAY_OF_WEEK:
                datePartFunction = "DAYOFWEEK";
                break;
        }
        if (datePartFunction != null) {
            sb.append("EXTRACT(");
            sb.append(datePartFunction);
            sb.append(" FROM ");
            appendField(sb, step, fieldName, multipleSteps);
            sb.append(")");
        } else {
            super.appendDateField(sb, step, fieldName, multipleSteps, datePart);
        }
    }


    // javadoc is inherited
    public String toSql(SearchQuery query, SqlHandler firstInChain) throws SearchQueryException {
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
        if (query.isDistinct()) {
            sbQuery.append("DISTINCT ");
        }

        firstInChain.appendQueryBodyToSql(sbQuery, query, firstInChain);


        int offset = query.getOffset();
        // LIMIT
        if (query.getMaxNumber() != -1) {
            // Maxnumber set.
            sbQuery.append(" LIMIT ").append(query.getMaxNumber());
        } else {
            // could append LIMIT ALL, but why bother
        }

        if (offset != 0) {
            sbQuery.append(" OFFSET ").append(offset);
        }

        String strSQL = sbQuery.toString();
        if (log.isDebugEnabled()) {
            log.debug("generated SQL: " + strSQL);
        }
        return strSQL;
    }
}
