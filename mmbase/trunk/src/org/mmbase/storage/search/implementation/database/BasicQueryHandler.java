/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation.database;

import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.database.support.MMJdbc2NodeInterface;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.*;
import org.mmbase.module.database.MultiConnection;
import java.sql.*;
import java.util.*;


/**
 * Basic implementation using a database.
 * Uses a {@link org.mmbase.storage.search.implementation.database.SqlHandler SqlHandler}
 * to create SQL string representations of search queries.
 * <p>
 * In order to execute search queries, these are represented as SQL strings
 * by the handler, and in this form executed on the database.
 *
 * @author Rob van Maris
 * @version $Revision: 1.3 $
 * @since MMBase-1.7
 */
public class BasicQueryHandler implements SearchQueryHandler {
    
    /** Empty StepField array. */
    private static final StepField[] STEP_FIELD_ARRAY = new StepField[0];
    
    /** Logger instance. */
    private static Logger log
    = Logging.getLoggerInstance(SearchQueryHandler.class.getName());
    
    /** Sql handler used to generate SQL statements. */
    private SqlHandler sqlHandler = null;
    
    /** MMBase instance. */
    private MMBase mmbase = null;
    
    /**
     * Default constructor.
     *
     * @param sqlHandler The handler use to create SQL string representations
     *        of search queries.
     */
    public BasicQueryHandler(SqlHandler sqlHandler) {
        this.sqlHandler = sqlHandler;
        // TODO: (later) test if MMBase is properly initialized first.
        mmbase = MMBase.getMMBase();
    }
    
    // javadoc is inherited
    public List getNodes(SearchQuery query, MMObjectBuilder builder)
    throws SearchQueryException {
        StepField[] fields =
        (StepField[]) query.getFields().toArray(STEP_FIELD_ARRAY);
        List steps = query.getSteps();
        List results = new ArrayList();
        String sqlString = null;
        MultiConnection con = null;
        Statement stmt = null;
        
//        // Flag, set if offset must be supported by skipping results.
//        boolean mustSkipResults = 
//        (query.getOffset() != SearchQuery.DEFAULT_OFFSET)
//        && (sqlHandler.getSupportLevel(SearchQueryHandler.FEATURE_OFFSET, query) 
//        == SearchQueryHandler.SUPPORT_NONE);
//        
//        // Flag, set if sql handler supports maxnumber.
//        boolean sqlHandlerSupportsMaxNumber = 
//        sqlHandler.getSupportLevel(SearchQueryHandler.FEATURE_MAX_NUMBER, query)
//        != SearchQueryHandler.SUPPORT_NONE;
//        
//        // Flag, set if maxnumber must be supported by truncating results.
//        boolean mustTruncateResults =
//        (query.getMaxNumber() != SearchQuery.DEFAULT_MAX_NUMBER)
//        && (sqlHandler.getSupportLevel(SearchQueryHandler.FEATURE_MAX_NUMBER, query)
//        == SearchQueryHandler.SUPPORT_NONE);
//
        // Generate the SQL string for the query.
        try {
//            if (mustSkipResults) {
//                if (mustTruncateResults) {
//                    // Weak support for offset, weak support for maxnumber:
//                    // Replace query(offset, maxnumber) by 
//                    // query(0, Integer.MAX_VALUE).
//                    ModifiableQuery modifiedQuery = new ModifiableQuery(query);
//                    modifiedQuery.setOffset(SearchQuery.DEFAULT_OFFSET);
//                    modifiedQuery.setMaxNumber(Integer.MAX_VALUE);
//                    sqlString = sqlHandler.toSql(modifiedQuery, sqlHandler);
//                } else 
//                if (query.getMaxNumber() != SearchQuery.DEFAULT_MAX_NUMBER) {
//                    // Weak support for offset, sql handler supports maxnumber:
//                    // Replace query (offset, maxnumber) by 
//                    // query( 0, offset+maxnumber).
//                    ModifiableQuery modifiedQuery = new ModifiableQuery(query);
//                    modifiedQuery.setOffset(0);
//                    modifiedQuery.setMaxNumber(
//                        query.getOffset() + query.getMaxNumber());
//                    sqlString = sqlHandler.toSql(modifiedQuery, sqlHandler);
//                } else {
//                    // Weak support for offset, maxnumber not used.
//                    sqlString = sqlHandler.toSql(query, sqlHandler);
//                }
//            } else {
//                if (mustTruncateResults) {
//                    // Sql handler supports offset, 
//                    // weak support for maxnumber:
//                    // Replace query(offset, maxnumber) by 
//                    // query(offset, Integer.MAX_VALUE)
//                    ModifiableQuery modifiedQuery = new ModifiableQuery(query);
//                    modifiedQuery.setMaxNumber(Integer.MAX_VALUE);
//                    sqlString = sqlHandler.toSql(modifiedQuery, sqlHandler);
//                } else
//                if (query.getMaxNumber() != SearchQuery.DEFAULT_MAX_NUMBER) {
//                    // Sql handler supports offset, 
//                    // sql handler supports maxnumber:
                    sqlString = sqlHandler.toSql(query, sqlHandler);
//                } else {
//                    // Offset not used, maxnumber not used.
//                    sqlString = sqlHandler.toSql(query, sqlHandler);
//                }
//            }

            // TODO: test maximum sql statement length is not exceeded.
            
            // Execute the SQL and store results as cluster-/real nodes.
            MMJdbc2NodeInterface database = mmbase.getDatabase();
            con = mmbase.getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sqlString);
            
//            // Skip results to provide weak support for offset.
//            if (mustSkipResults) {
//                for (int i = 0; i < query.getOffset(); i++) {
//                    rs.next();
//                }
//            }
            
            // Read results.
            // Truncate results to provide weak support for maxnumber.
            while (rs.next() 
//                && (sqlHandlerSupportsMaxNumber || results.size() < query.getMaxNumber())
                ) {
                MMObjectNode node = null;
                if (builder instanceof ClusterBuilder) {
                    // Cluster nodes.
                    node = new ClusterNode(builder, steps.size());
                } else if (builder instanceof ResultBuilder) {
                    // Result nodes.
                    node = new ResultNode((ResultBuilder) builder);
                } else {
                    // Real nodes.
                    node = new MMObjectNode(builder);
                }
                for (int i = 0; i < fields.length; i++) {
                    String fieldName;
                    String prefix;
                    if (builder instanceof ClusterBuilder) {
                        fieldName = fields[i].getFieldName();
                        prefix = fields[i].getStep().getAlias() + ".";
                    } else if (builder instanceof ResultBuilder) {
                        fieldName = fields[i].getAlias();
                        prefix = "";
                    } else {
                        fieldName = fields[i].getFieldName();
                        prefix = "";
                    }
                    int fieldType = fields[i].getType();
                    // TODO: (later) use alternative to decodeDBnodeField, to
                    // circumvent the code in decodeDBnodeField that tries to
                    // reverse replacement of "disallowed" fieldnames.
                    database.decodeDBnodeField(node, fieldName, rs, i + 1, prefix);
                }
                if (builder instanceof ClusterBuilder) {
                    // Finished initializing clusternode.
                    ((ClusterNode) node).initializing = false;
                }
                results.add(node);
            }
        } catch (Exception e) {
            // Something went wrong, log exception
            // and rethrow as SearchQueryException.
            log.error("Query failed:" + query + "\n" + Logging.stackTrace(e));
            throw new SearchQueryException(query.toString());
        } finally {
            mmbase.closeConnection(con, stmt);
        }
        return results;
    }
    
    // javadoc is inherited
    public int getSupportLevel(int feature, SearchQuery query) throws SearchQueryException {
        int supportLevel;
        switch (feature) {
            case SearchQueryHandler.FEATURE_OFFSET:
                // When sql handler does not support OFFSET, this query handler
                // provides weak support by skipping resultsets.
                // (falls through)
            case SearchQueryHandler.FEATURE_MAX_NUMBER:
                // When sql handler does not support MAX NUMBER, this query
                // handler provides weak support by truncating resultsets.
                int handlerSupport = sqlHandler.getSupportLevel(feature, query);
                if (handlerSupport == SearchQueryHandler.SUPPORT_NONE) {
                    // TODO: implement weak support.
                    //supportLevel = SearchQueryHandler.SUPPORT_WEAK;
                     supportLevel = SearchQueryHandler.SUPPORT_NONE;
               } else {
                    supportLevel = handlerSupport;
                }
                break;
                
            default:
                supportLevel = sqlHandler.getSupportLevel(feature, query);
        }
        return supportLevel;
    }
    
    // javadoc is inherited
    public int getSupportLevel(Constraint constraint, SearchQuery query) throws SearchQueryException {
        return sqlHandler.getSupportLevel(constraint, query);
    }
    
}
