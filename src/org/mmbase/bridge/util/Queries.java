/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util;
import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.legacy.*;
import org.mmbase.bridge.implementation.*;
import org.mmbase.util.StringSplitter;
import org.mmbase.util.Encode;
import org.mmbase.util.logging.*;
import org.mmbase.module.core.ClusterBuilder;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.database.support.MMJdbc2NodeInterface;
import java.util.*;

/**
 * This class contains various utility methods for manipulating and creating query objecs. Most
 * essential methods are available on the Query object itself, but too specific or legacy-ish
 * methods are put here.
 *
 * @author Michiel Meeuwissen
 * @version $Id: Queries.java,v 1.5 2003-09-08 08:21:34 michiel Exp $
 * @see  org.mmbase.bridge.Query
 * @since MMBase-1.7
 */
public class Queries {
    private static final Logger log = Logging.getLoggerInstance(Queries.class);

    /**
     * Creates a Query object using arguments for {@link Cloud#getList} (this function is of course
     * implemented using this utility). This is usefull to convert (legacy) code which uses
     * getList, but you want to use new Query features without rewriting the complete thing.
     *
     * It can also be simply handy to specify things as Strings.
     */
    public static Query createQuery(Cloud cloud,                            
                                    String startNodes,
                                    String nodePath,
                                    String fields,
                                    String constraints,
                                    String orderby,
                                    String directions,
                                    String searchDir,
                                    boolean distinct) {

        
        {
            // the bridge test case say that you may also specifiy empty string (why?)
            if ("".equals(startNodes))
                startNodes = null;
            if ("".equals(fields))
                fields = null;
            if ("".equals(constraints))
                constraints = null;
            if ("".equals(searchDir))
                searchDir = null;
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
        }

        // create query object
        Query query;
        {
            ClusterBuilder clusterBuilder = MMBase.getMMBase().getClusterBuilder();
            int search = -1;
            if (searchDir != null) {
                search = ClusterBuilder.getSearchDir(searchDir);
            }

            List snodes = StringSplitter.split(startNodes);
            List tables = StringSplitter.split(nodePath);
            List f = StringSplitter.split(fields);
            List orderVec = StringSplitter.split(orderby);
            List d = StringSplitter.split(directions);
            try {
                // pitty that we can't use cloud.createQuery for this.
                // but all essential methods are on ClusterBuilder
                query = new BasicQuery(cloud, clusterBuilder.getMultiLevelSearchQuery(snodes, f, distinct ? "YES" : "NO", tables, constraints, orderVec, d, search));
            } catch (IllegalArgumentException iae) {
                throw new BridgeException(iae);
            }
        }
        return query;
    }


    /** 
     * returns false, when escaping wasnt closed, or when a ";" was found outside a escaped part (to prefent spoofing) 
     * This is used by createQuery (i wonder if it still makes sense)
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
                    log.warn(
                        "found a ';' outside the constraints(you should sql-escape the search query inside the jsp-page?)\noriginal:"
                            + constraints
                            + "\nnot excaped:"
                            + notEscaped);
                    return false;
                }

                int stop = remaining.substring(start + 1).indexOf('\'');
                if (stop < 0) {
                    log.warn(
                        "reached end, but we are still escaping(you should sql-escape the search query inside the jsp-page?)\noriginal:"
                            + constraints
                            + "\nlast escaping:"
                            + remaining.substring(start + 1));
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
     */
    private static String convertClausePartToDBS(String constraints) {
        // obtain dbs for fieldname checks
        MMJdbc2NodeInterface dbs = MMBase.getMMBase().getDatabase();
        StringBuffer result = new StringBuffer();
        int posa = constraints.indexOf('[');
        while (posa > -1) {
            int posb = constraints.indexOf(']', posa);
            if (posb == -1) {
                posa = -1;
            } else {
                String fieldName = constraints.substring(posa + 1, posb);
                int posc = fieldName.indexOf('.', posa);
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
        } else if (constraints.startsWith("WHERE")) {
            constraints = constraints.substring(5);
        }
        StringBuffer result = new StringBuffer();
        int posa = constraints.indexOf('\'');
        while (posa > -1) {
            int posb = constraints.indexOf('\'', 1);
            if (posb == -1) {
                posa = -1;
            } else {
                String part = constraints.substring(0, posa);
                result.append(convertClausePartToDBS(part)).append(constraints.substring(posa, posb + 1));
                constraints = constraints.substring(posb + 1);
                posa = constraints.indexOf('\'');
            }
        }
        result.append(convertClausePartToDBS(constraints));
        return result.toString();
    }


    /**
     * Adds a 'legacy' constraint to the query. Alreading existing constraints remain ('AND' is used)
     */
    public static Query addConstraints(Query query, String constraints) {
        if (constraints == null || constraints.equals("")) return query;
        constraints = convertClauseToDBS(constraints);
        if (!validConstraints(constraints)) {
            throw new BridgeException("invalid constraints:" + constraints);
        }
        Constraint newConstraint = query.createConstraint(constraints);
        Constraint constraint = query.getConstraint();
        if (constraint != null) {
            log.debug("compositing constraint");
            newConstraint = query.createConstraint(constraint, CompositeConstraint.LOGICAL_AND, newConstraint);
        }
        query.setConstraint(newConstraint);

        return query;

    }

    /**
     * Adds sort orders to the query, using two strings. Like in 'getList' of Cloud. Several tag-attributes need this.
     *
     * @todo implement for normal query.
     */
    public static Query addSortOrders(NodeQuery query, String sorted, String directions) {
        // following code was copied from MMObjectBuilder.setSearchQuery (bit ugly)
        if (sorted == null) return query;
        if (directions == null) {
            directions = "";
        }
       
        StringTokenizer sortedTokenizer     = new StringTokenizer(sorted, ",");
        StringTokenizer directionsTokenizer = new StringTokenizer(directions, ",");
        
        Step step = query.getNodeStep();
        NodeManager nodeManager = query.getNodeManager();
        
        while (sortedTokenizer.hasMoreTokens()) {
            String    fieldName = sortedTokenizer.nextToken().trim();
            int dot = fieldName.indexOf('.');
            
            StepField sf;
            if (dot == -1) {
                sf = query.getStepField(nodeManager.getField(fieldName));
            } else {
                sf =  query.createStepField(fieldName);
            }
            
            int dir = SortOrder.ORDER_ASCENDING;
            if (directionsTokenizer.hasMoreTokens()) {
                String direction = directionsTokenizer.nextToken().trim();
                if (direction.equalsIgnoreCase("DOWN")) {
                    dir = SortOrder.ORDER_DESCENDING;
                } else {
                    dir = SortOrder.ORDER_ASCENDING;
                }
                }
            query.addSortOrder(sf, dir);
        }
        return query;
    }

    /**
     * Returns substring of given string without the leading digits (used in 'paths')
     */
    protected static String removeDigits(String complete) {
        int end = complete.length() - 1;
        while (Character.isDigit(complete.charAt(end))) --end;
        return complete.substring(0, end + 1);
    }

    /**
     * Adds path of steps to an existing query. The query may contain steps already. Per step also
     * the 'search direction' may be specified.
     */
    public static Query addPath(Query query, String path, String searchDirs) {
        if (path == null || path.equals("")) return query;
        if (searchDirs == null) {
            searchDirs = "";
        }
       
        StringTokenizer pathTokenizer       = new StringTokenizer(path, ",");
        StringTokenizer searchDirsTokenizer = new StringTokenizer(searchDirs, ",");

        Cloud cloud = query.getCloud();

        if (query.getSteps().size() == 0 ) { // if no steps yet, first step must be added with addStep
            String completeFirstToken = pathTokenizer.nextToken().trim();
            String firstToken      = removeDigits(completeFirstToken);
            Step step = query.addStep(cloud.getNodeManager(firstToken));
            if (! firstToken.equals(completeFirstToken)) {
                query.setAlias(step, completeFirstToken);
            }
        }

        String searchDir = null; // outside the loop, so defaulting to previous searchDir
        while (pathTokenizer.hasMoreTokens()) {
            String completeToken = pathTokenizer.nextToken().trim();                          
            String token      = removeDigits(completeToken); 

            if (searchDirsTokenizer.hasMoreTokens()) {
                searchDir = searchDirsTokenizer.nextToken();
            }

            if (cloud.hasRole(token)) {
                if (! pathTokenizer.hasMoreTokens()) throw new BridgeException("Path cannot end with a role (" + path + "/" + searchDirs + ")" );
                String nodeManagerAlias = pathTokenizer.nextToken().trim();
                String nodeManagerName  = removeDigits(nodeManagerAlias);
                NodeManager nodeManager = cloud.getNodeManager(nodeManagerName);
                RelationStep relationStep = query.addRelationStep(nodeManager, token, searchDir);
                if (! token.equals(completeToken)) query.setAlias(relationStep, completeToken);
                if (! nodeManagerName.equals(nodeManagerAlias)) {
                    Step next = relationStep.getNext();
                    query.setAlias(next, nodeManagerAlias);
                }                
            } else {
                NodeManager nodeManager  = cloud.getNodeManager(token);
                Step step = query.addRelationStep(nodeManager, null, searchDir);
                if (! token.equals(completeToken)) query.setAlias(step, completeToken);
            }
        }
        if (searchDirsTokenizer.hasMoreTokens()) {
            throw new BridgeException("Too many search directions (" + path + "/" + searchDirs + ")" );
        }
        return query;
    }

    /**
     * Takes the query, and does a count with the same constraints.
     *
     */
    public static int count(Query query) {
        Cloud cloud = query.getCloud();       
        Query count = query.aggregatingClone();        
        Step step = (Step) (count.getSteps().get(0));
        count.addAggregatedField(step, 
                                 cloud.getNodeManager(step.getTableName()).getField("number"), 
                                 AggregatedField.AGGREGATION_TYPE_COUNT);        
        Node result = (Node) cloud.getList(count).get(0);
        return result.getIntValue("number");
    }

}
