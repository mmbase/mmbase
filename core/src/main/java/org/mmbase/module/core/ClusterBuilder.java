/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import java.util.Map.Entry;

import org.mmbase.module.corebuilders.*;
import org.mmbase.cache.MultilevelCache;
import org.mmbase.core.CoreField;
import org.mmbase.bridge.BridgeException;
import org.mmbase.bridge.Field;
import org.mmbase.core.util.Fields;
import org.mmbase.util.functions.*;
import org.mmbase.datatypes.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.storage.search.legacy.ConstraintParser;
import org.mmbase.storage.search.legacy.QueryConvertor;

import org.mmbase.util.logging.*;

/**
 * The builder for {@link ClusterNode clusternodes}.
 * <p>
 * Provides these methods to retrieve clusternodes:
 * <ul>
 *      <li>{@link #getClusterNodes(SearchQuery)}
 *          to retrieve clusternodes using a <code>SearchQuery</code> (recommended).
 *      <li>{@link #getMultiLevelSearchQuery(List,List,String,List,String,List,List,int)}
 *            as a convenience method to create a <code>SearchQuery</code>
 *      <li>{@link #searchMultiLevelVector(List,List,String,List,String,List,List,int)}
 *            to retrieve clusternodes using a constraint string.
 * </ul>
 * <p>
 * Individual nodes in a 'cluster' node can be retrieved by calling the node's
 * {@link MMObjectNode#getNodeValue(String) getNodeValue()} method, using
 * the builder name (or step alias) as argument.
 *
 * @todo XXXX. This 'builder' is actually singleton (only one instance is created).  It does
 *             therefore not support getFields, so this is more or less hacked in bridge.  Perhaps in 'core' a
 *             similar approach as now in birdge must be taken, so no ClusterBuilder, but only Virtual builders,
 *             one for every query result.
 *
 *
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Rob van Maris
 * @version $Id$
 * @see ClusterNode
 */
public class ClusterBuilder extends VirtualBuilder {

    /**
     * Search for all valid relations.
     * When searching relations, return both relations from source to deastination and from destination to source,
     * provided there is an allowed relation in that directon.
     * @deprecated use {@link RelationStep#DIRECTIONS_BOTH}
     *             In future versions of MMBase (1.8 and up) this will be the default value
     */
    public static final int SEARCH_BOTH = RelationStep.DIRECTIONS_BOTH;

    /**
     * Search for destinations,
     * When searching relations, return only relations from source to deastination.
     * @deprecated use {@link RelationStep#DIRECTIONS_DESTINATION}
     */
    public static final int SEARCH_DESTINATION = RelationStep.DIRECTIONS_DESTINATION;

    /**
     * Seach for sources.
     * When searching a multilevel, return only relations from destination to source, provided directionality allows
     * @deprecated use {@link RelationStep#DIRECTIONS_SOURCE}
     */
    public static final int SEARCH_SOURCE = RelationStep.DIRECTIONS_SOURCE;

    /**
     * Search for all relations.  When searching a multilevel, return both relations from source to
     * deastination and from destination to source.  Allowed relations are not checked - ALL
     * relations are used. This makes more inefficient queries, but it is not really wrong.
     * @deprecated use {@link RelationStep#DIRECTIONS_ALL}
     */
    public static final int SEARCH_ALL = RelationStep.DIRECTIONS_ALL;

    /**
     * Search for either destination or source.
     * When searching a multilevel, return either relations from source to destination OR from destination to source.
     * The returned set is decided through the typerel tabel. However, if both directions ARE somehow supported, the
     * system only returns source to destination relations.
     * This is the default value (for compatibility purposes).
     * @deprecated use {@link RelationStep#DIRECTIONS_EITHER}.
     *             In future versions of MMBase (1.8 and up) the default value will be
     *             {@link RelationStep#DIRECTIONS_BOTH}
     */
    public static final int SEARCH_EITHER = RelationStep.DIRECTIONS_EITHER;

    // logging variable
    private static final Logger log= Logging.getLoggerInstance(ClusterBuilder.class);

    /**
     * Creates <code>ClusterBuilder</code> instance.
     * Must be called from the MMBase class.
     * @param m the MMbase cloud creating the node
     * @scope package
     */
    public ClusterBuilder(MMBase m) {
        super(m, "clusternodes");
    }

    /**
     * Translates a string to a search direction constant.
     *
     * @since MMBase-1.6
     */
    public static int getSearchDir(String search) {
        if (search == null) {
            return RelationStep.DIRECTIONS_EITHER;
        }
        return org.mmbase.bridge.util.Queries.getRelationStepDirection(search);
    }

    /**
     * Translates a search direction constant to a string.
     *
     * @since MMBase-1.6
     */
    public static String getSearchDirString(int search) {
        if (search == RelationStep.DIRECTIONS_DESTINATION) {
            return "DESTINATION";
        } else if (search == RelationStep.DIRECTIONS_SOURCE) {
            return "SOURCE";
        } else if (search == RelationStep.DIRECTIONS_BOTH) {
            return "BOTH";
        } else if (search == RelationStep.DIRECTIONS_ALL) {
            return "ALL";
        } else {
            return "EITHER";
        }
    }

    /**
     * Get a new node, using this builder as its parent.
     * The new node is a cluster node.
     * Unlike most other nodes, a cluster node does not have a number,
     * owner, or otype fields.
     * @param owner The administrator creating the new node (ignored).
     * @return A newly initialized <code>VirtualNode</code>.
     */
    @Override
    public MMObjectNode getNewNode(String owner) {
        throw new UnsupportedOperationException("One cannot create new ClusterNodes");
    }

    /**
     * What should a GUI display for this node.
     * This version displays the contents of the 'name' field(s) that were retrieved.
     * XXX: should be changed to something better
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
    @Override
    public String getGUIIndicator(MMObjectNode node) {
        // Return "name"-field when available.
        String s = node.getStringValue("name");
        if (s != null) {
            return s;
        }

        // Else "name"-fields of contained nodes.
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Object> entry : node.getValues().entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(".name")) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(entry.getValue());
            }
        }
        if (sb.length() > 15) {
            return sb.substring(0, 12) + "...";
        } else {
            return sb.toString();
        }
    }

    /**
     * What should a GUI display for this node/field combo.
     * For a multilevel node, the builder tries to determine
     * the original builder of a field, and invoke the method using
     * that builder.
     *
     * @param node The node to display
     * @param pars Parameters, see {@link MMObjectBuilder#GUI_PARAMETERS}
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    @Override
    public String getGUIIndicator(MMObjectNode node, Parameters pars) {

        if (node == null) throw new RuntimeException("Tried to get GUIIndicator for  " + pars + " with NULL node");

        ClusterNode clusterNode = (ClusterNode) node;

        String field = pars.getString(Parameter.FIELD);
        if (field == null) {
            return super.getGUIIndicator(node, pars);
        } else {
            int pos = field.indexOf('.');
            if (pos != -1) {
                String bulName = getTrueTableName(field.substring(0, pos));
                MMObjectNode n = clusterNode.getRealNode(bulName);
                if (n != null) {
                    MMObjectBuilder bul= n.getBuilder();
                    if (bul != null) {
                        // what are we trying here?
                        String fieldName = field.substring(pos + 1);
                        Parameters newPars = new Parameters(pars.getDefinition(), pars);
                        newPars.set(Parameter.FIELD, fieldName);
                        newPars.set("stringvalue", null);
                        org.mmbase.bridge.Node bnode = pars.get(Parameter.NODE);
                        if (bnode != null) {
                            newPars.set(Parameter.NODE, bnode.getNodeValue(bulName));
                        }
                        newPars.set(Parameter.CORENODE, n);
                        return bul.guiFunction.getFunctionValue(newPars);
                    }
                }
            }
            return super.getGUIIndicator(node, pars);
        }
    }

    /**
     * Determines the builder part of a specified field.
     * @param fieldName the name of the field
     * @return the name of the field's builder
     */
    public String getBuilderNameFromField(String fieldName) {
        int pos = fieldName.indexOf(".");
        if (pos != -1) {
            String bulName = fieldName.substring(0, pos);
            return getTrueTableName(bulName);
        }
        return "";
    }

    /**
     * Determines the fieldname part of a specified field (without the builder name).
     * @param fieldname the name of the field
     * @return the name of the field without its builder
     */
    public static String getFieldNameFromField(String fieldname) {
        int pos= fieldname.indexOf(".");
        if (pos != -1) {
            fieldname = fieldname.substring(pos + 1);
        }
        return fieldname;
    }

    /**
     * Return a field.
     * @param fieldName the requested field's name
     * @return the field
     */
    @Override
    public FieldDefs getField(String fieldName) {
        String builderName = getBuilderNameFromField(fieldName);
        if (builderName.length() > 0) {
            MMObjectBuilder bul = mmb.getBuilder(builderName);
            if (bul == null) {
                throw new RuntimeException("No builder with name '" + builderName + "' found");
            }
            return bul.getField(getFieldNameFromField(fieldName));
        } else {
            //
            MMObjectBuilder bul = mmb.getBuilder(getTrueTableName(fieldName));
            if (bul != null) {
                return new FieldDefs(fieldName, Field.TYPE_NODE, -1, Field.STATE_VIRTUAL, org.mmbase.datatypes.DataTypes.getDataType("node"));
            }
        }
        return null;
    }

    @Override
    public List<CoreField> getFields(int order) {
        throw new UnsupportedOperationException("Cluster-nodes can have any field.");
    }
    public Collection<CoreField> getFields() {
        throw new UnsupportedOperationException("Cluster-nodes can have any field.");
    }

    /**
     * @since MMBase-1.8
     */
    public Map<String, CoreField> getFields(MMObjectNode node) {
        Map<String, CoreField> ret = new HashMap<String, CoreField>();
        Iterator<String> i = node.getValues().keySet().iterator();
        DataType<? extends Object> nodeType  = DataTypes.getDataType("node");
        while (i.hasNext()) {
            String name = i.next();
            int pos = name.indexOf(".");
            if (pos != -1) {
                String builderName = name.substring(0, pos);
                if (! ret.containsKey(builderName)) {
                    CoreField fd = Fields.createField(builderName, Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_VIRTUAL, nodeType);
                    ret.put(builderName, fd);
                }
            }
            ret.put(name, getField(name));
        }
        return ret;
    }

    /**
     * Same as {@link #searchMultiLevelVector(List,List,String,List,String,List,List,int)
     * searchMultiLevelVector(snodes, fields, pdistinct, tables, where, orderVec, direction, RelationStep.DIRECTIONS_EITHER)},
     * where <code>snodes</code> contains just the number specified by <code>snode</code>.
     *
     * @see #searchMultiLevelVector(List, List, String, List, String, List, List, List)
     */
   public Vector<MMObjectNode> searchMultiLevelVector(
        int snode,
        List<String> fields,
        String pdistinct,
        List<String> tables,
        String where,
        List<String> orderVec,
        List<String> direction) {

        List<String> v= new ArrayList<String>();
        v.add("" + snode);
        return searchMultiLevelVector(v, fields, pdistinct, tables, where, orderVec, direction, RelationStep.DIRECTIONS_EITHER);
    }

    /**
     * Same as {@link #searchMultiLevelVector(List,List,String,List,String,List,List,int)
              * searchMultiLevelVector(snodes, fields, pdistinct, tables, where, orderVec, direction, RelationStep.DIRECTIONS_EITHER)}.
     *
     * @see #searchMultiLevelVector(List,List,String,List,String,List,List,int)
     */
    public Vector<MMObjectNode> searchMultiLevelVector(List<String> snodes,
                                                       List<String> fields,
                                                       String pdistinct,
                                                       List<String> tables,
                                                       String where,
                                                       List<String> orderVec,
                                                       List<String> direction) {
        return searchMultiLevelVector(snodes, fields, pdistinct, tables, where, orderVec, direction, RelationStep.DIRECTIONS_EITHER);
    }

    /**
     * Return all the objects that match the searchkeys.
     * The constraint must be in one of the formats specified by {@link
     * QueryConvertor#setConstraint(BasicSearchQuery,String)
     * QueryConvertor#setConstraint()}.
     *
     * @param snodes The numbers of the nodes to start the search with. These have to be present in the first table
     *      listed in the tables parameter.
     * @param fields The fieldnames to return. This should include the name of the builder. Fieldnames without a builder prefix are ignored.
     *      Fieldnames are accessible in the nodes returned in the same format (i.e. with manager indication) as they are specified in this parameter.
     *      Examples: 'people.lastname'
     * @param pdistinct 'YES' indicates the records returned need to be distinct. Any other value indicates double values can be returned.
     * @param tables The builder chain. A list containing builder names.
     *      The search is formed by following the relations between successive builders in the list. It is possible to explicitly supply
     *      a relation builder by placing the name of the builder between two builders to search.
     *      Example: company,people or typedef,authrel,people.
     * @param where The constraint, must be in one of the formats specified by {@link
     *        QueryConvertor#setConstraint(BasicSearchQuery,String)
     *        QueryConvertor#setConstraint()}.
     *        E.g. "WHERE news.title LIKE '%MMBase%' AND news.title > 100"
     * @param sortFields the fieldnames on which you want to sort.
     * @param directions A list of values containing, for each field in the order parameter, a value indicating whether the sort is
     *      ascending (<code>UP</code>) or descending (<code>DOWN</code>). If less values are syupplied then there are fields in order,
     *      the first value in the list is used for the remaining fields. Default value is <code>'UP'</code>.
     * @param searchDir Specifies in which direction relations are to be
     *      followed, this must be one of the values defined by this class.
     * @return a <code>Vector</code> containing all matching nodes
     * @deprecated use {@link #searchMultiLevelVector(List snodes, List fields, String pdistinct, List tables, String where,
     *               List orderVec, List directions, List searchDirs)}
     */
    public Vector<MMObjectNode> searchMultiLevelVector(List<String> snodes, List<String> fields, String pdistinct, List<String> tables, String where, List<String> sortFields,
            List<String> directions, int searchDir) {
        List<Integer> searchDirs = new ArrayList<Integer>();
        searchDirs.add(searchDir);
        return searchMultiLevelVector(snodes, fields, pdistinct, tables, where, sortFields, directions, searchDirs);
    }

    /**
     * Return all the objects that match the searchkeys.
     * The constraint must be in one of the formats specified by {@link
     * QueryConvertor#setConstraint(BasicSearchQuery,String)
     * QueryConvertor#setConstraint()}.
     *
     * @param snodes The numbers of the nodes to start the search with. These have to be present in the first table
     *      listed in the tables parameter.
     * @param fields The fieldnames to return. This should include the name of the builder. Fieldnames without a builder prefix are ignored.
     *      Fieldnames are accessible in the nodes returned in the same format (i.e. with manager indication) as they are specified in this parameter.
     *      Examples: 'people.lastname'
     * @param pdistinct 'YES' indicates the records returned need to be distinct. Any other value indicates double values can be returned.
     * @param tables The builder chain. A list containing builder names.
     *      The search is formed by following the relations between successive builders in the list. It is possible to explicitly supply
     *      a relation builder by placing the name of the builder between two builders to search.
     *      Example: company,people or typedef,authrel,people.
     * @param where The constraint, must be in one of the formats specified by {@link
     *        QueryConvertor#setConstraint(BasicSearchQuery,String)
     *        QueryConvertor#setConstraint()}.
     *        E.g. "WHERE news.title LIKE '%MMBase%' AND news.title > 100"
     * @param sortFields the fieldnames on which you want to sort.
     * @param directions A list of values containing, for each field in the order parameter, a value indicating whether the sort is
     *      ascending (<code>UP</code>) or descending (<code>DOWN</code>). If less values are syupplied then there are fields in order,
     *      the first value in the list is used for the remaining fields. Default value is <code>'UP'</code>.
     * @param searchDirs Specifies in which direction relations are to be followed. You can specify a direction for each
     *      relation in the path. If you specify less directions than there are relations, the last specified direction is used
     *      for the remaining relations. If you specify an empty list the default direction is BOTH.
     * @return a <code>Vector</code> containing all matching nodes
     */
    public Vector<MMObjectNode> searchMultiLevelVector(List<String> snodes, List<String> fields, String pdistinct, List<String> tables, String where, List<String> sortFields,
        List<String> directions, List<Integer> searchDirs) {
        // Try to handle using the SearchQuery framework.
        try {
            SearchQuery query = getMultiLevelSearchQuery(snodes, fields, pdistinct, tables, where, sortFields, directions, searchDirs);
            List<MMObjectNode> clusterNodes = getClusterNodes(query);
            return new Vector<MMObjectNode>(clusterNodes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Executes query, returns results as {@link ClusterNode clusternodes} or MMObjectNodes if the
     * query is a Node-query.
     * This method uses the MultilevelCache for query results
     *
     * @param query The query.
     * @return The clusternodes.
     * @since MMBase-1.7
     */
    public List<MMObjectNode> getClusterNodes(SearchQuery query) {

        // start multilevel cache
        MultilevelCache multilevelCache = MultilevelCache.getCache();
        // check multilevel cache if needed
        List<MMObjectNode> resultList = null;
        if (query.getCachePolicy().checkPolicy(query)) {
            resultList = multilevelCache.get(query);
        }
        // if unavailable, obtain from database
        if (resultList == null) {
            log.debug("result list is null, getting from database");
            try {
                resultList = getClusterNodesFromQueryHandler(query);
            } catch (SearchQueryException sqe) {
                throw new BridgeException(query.toString() + ":" + sqe.getMessage(), sqe);
            }
            if (query.getCachePolicy().checkPolicy(query)) {
                multilevelCache.put(query, resultList);
            }
        }

        return resultList;
    }

    /**
     * Executes query, returns results as {@link ClusterNode clusternodes} or MMObjectNodes if the
     * query is a Node-query.
     * The results are retrieved directly from storage without the MultilevelCache
     * {@link #getClusterNodes(SearchQuery)} which uses the MultilevelCache
     *
     * @param query The query.
     * @return The clusternodes.
     * @throws org.mmbase.storage.search.SearchQueryException
     *         When an exception occurred while retrieving the results.
     * @since MMBase-1.7
     * @see org.mmbase.storage.search.SearchQueryHandler#getNodes
     */
    public List<MMObjectNode> getClusterNodesFromQueryHandler(SearchQuery query)
            throws SearchQueryException {
        // TODO (later): implement maximum set by maxNodesFromQuery?
        // Execute query, return results.

        return mmb.getSearchQueryHandler().getNodes(query, this);
    }

    /**
     * Returns the name part of a tablename.
     * The name part is the table name minus the numeric digit appended
     * to a name (if appliable).
     * @param table name of the original table
     * @return A <code>String</code> containing the table name
     */
    private String getTableName(String table) {
        int end = table.length() ;
        if (end == 0) throw new IllegalArgumentException("Table name too short '" + table + "'");
        while (Character.isDigit(table.charAt(end -1))) --end;
        return table.substring(0, end );
    }

    /**
     * Returns the name part of a tablename, and convert it to a buidler name.
     * This will catch specifying a rolename in stead of a builder name when using relations.
     * @param table name of the original table
     * @return A <code>String</code> containing the table name
     */
    private String getTrueTableName(String table) {
        String tab = getTableName(table);
        int rnumber = mmb.getRelDef().getNumberByName(tab);
        if (rnumber != -1) {
            return mmb.getRelDef().getBuilderName(rnumber);
        } else {
            return tab;
        }
    }

    /**
     * Get text from a blob field.
     * The text is cut if it is to long.
     * @param fieldname name of the field
     * @param number number of the object in the table
     * @return a <code>String</code> containing the contents of a field as text
     */
    public String getShortedText(String fieldname, int number) {
        String buildername= getBuilderNameFromField(fieldname);
        if (buildername.length() > 0) {
            MMObjectBuilder bul= mmb.getMMObject(buildername);
            return bul.getShortedText(getFieldNameFromField(fieldname), bul.getNode(number));
        }
        return null;
    }

    /**
     * Get binary data of a database blob field.
     * The data is cut if it is to long.
     * @param fieldname name of the field
     * @param number number of the object in the table
     * @return an array of <code>byte</code> containing the contents of a field as text
     */
    public byte[] getShortedByte(String fieldname, int number) {
        String buildername= getBuilderNameFromField(fieldname);
        if (buildername.length() > 0) {
            MMObjectBuilder bul= mmb.getMMObject(buildername);
            return bul.getShortedByte(getFieldNameFromField(fieldname), bul.getNode(number));
        }
        return null;
    }

    /**
     * Creates search query that selects all the objects that match the
     * searchkeys.
     * The constraint must be in one of the formats specified by {@link
     * QueryConvertor#setConstraint(BasicSearchQuery,String)
     * QueryConvertor#setConstraint()}.
     *
     * @param snodes <code>null</code> or a list of numbers
     *        of nodes to start the search with.
     *        These have to be present in the first table listed in the
     *        tables parameter.
     * @param fields List of fieldnames to return.
     *        These should be formatted as <em>stepalias.field</em>,
     *        e.g. 'people.lastname'
     * @param pdistinct 'YES' if the records returned need to be
     *        distinct (ignoring case).
     *        Any other value indicates double values can be returned.
     * @param tables The builder chain, a list containing builder names.
     *        The search is formed by following the relations between
     *        successive builders in the list.
     *        It is possible to explicitly supply a relation builder by
     *        placing the name of the builder between two builders to search.
     *        Example: company,people or typedef,authrel,people.
     * @param where The constraint, must be in one of the formats specified by {@link
     *        QueryConvertor#setConstraint(BasicSearchQuery,String)
     *        QueryConvertor#setConstraint()}.
     *        E.g. "WHERE news.title LIKE '%MMBase%' AND news.title > 100"
     * @param sortFields <code>null</code> or a list of  fieldnames on which you want to sort.
     * @param directions <code>null</code> or a list of values containing, for each field in the
     *        <code>sortFields</code> parameter, a value indicating whether the sort is
     *        ascending (<code>UP</code>) or descending (<code>DOWN</code>).
     *        If less values are supplied then there are fields in order,
     *        the first value in the list is used for the remaining fields.
     *        Default value is <code>'UP'</code>.
     * @param searchDir Specifies in which direction relations are to be
     *        followed, this must be one of the values defined by this class.
     * @deprecated use {@link #getMultiLevelSearchQuery(List snodes, List fields, String pdistinct, List tables, String where,
     *               List orderVec, List directions, int searchDir)}
     * @return the resulting search query.
     * @since MMBase-1.7
     */
    public BasicSearchQuery getMultiLevelSearchQuery(List<String> snodes, List<String> fields, String pdistinct, List<String> tables, String where,
            List<String> sortFields, List<String> directions, int searchDir) {
        List<Integer> searchDirs = new ArrayList<Integer>();
        searchDirs.add(searchDir);
        return getMultiLevelSearchQuery(snodes, fields, pdistinct, tables, where, sortFields, directions, searchDirs);
    }

    /**
     * Creates search query that selects all the objects that match the
     * searchkeys.
     * The constraint must be in one of the formats specified by {@link
     * QueryConvertor#setConstraint(BasicSearchQuery,String)
     * QueryConvertor#setConstraint()}.
     *
     * @param snodes <code>null</code> or a list of numbers
     *        of nodes to start the search with.
     *        These have to be present in the first table listed in the
     *        tables parameter.
     * @param fields List of fieldnames to return.
     *        These should be formatted as <em>stepalias.field</em>,
     *        e.g. 'people.lastname'
     * @param pdistinct 'YES' if the records returned need to be
     *        distinct (ignoring case).
     *        Any other value indicates double values can be returned.
     * @param tables The builder chain, a list containing builder names.
     *        The search is formed by following the relations between
     *        successive builders in the list.
     *        It is possible to explicitly supply a relation builder by
     *        placing the name of the builder between two builders to search.
     *        Example: company,people or typedef,authrel,people.
     * @param where The constraint, must be in one of the formats specified by {@link
     *        QueryConvertor#setConstraint(BasicSearchQuery,String)
     *        QueryConvertor#setConstraint()}.
     *        E.g. "WHERE news.title LIKE '%MMBase%' AND news.title > 100"
     * @param sortFields <code>null</code> or a list of  fieldnames on which you want to sort.
     * @param directions <code>null</code> or a list of values containing, for each field in the
     *        <code>sortFields</code> parameter, a value indicating whether the sort is
     *        ascending (<code>UP</code>) or descending (<code>DOWN</code>).
     *        If less values are supplied then there are fields in order,
     *        the first value in the list is used for the remaining fields.
     *        Default value is <code>'UP'</code>.
     * @param searchDirs Specifies in which direction relations are to be
     *        followed, this must be one of the values defined by this class.
     * @return the resulting search query.
     * @since MMBase-1.7
     */
    public BasicSearchQuery getMultiLevelSearchQuery(List<String> snodes, List<String> fields, String pdistinct, List<String> tables, String where,
            List<String> sortFields, List<String> directions, List<Integer> searchDirs) {

        // Create the query.
        BasicSearchQuery query= new BasicSearchQuery();

        // Set the distinct property.
        boolean distinct= pdistinct != null && pdistinct.equalsIgnoreCase("YES");
        query.setDistinct(distinct);

        // Get ALL tables (including missing reltables)
        Map<String, Integer> roles= new HashMap<String, Integer>();
        Map<String, BasicStepField> fieldsByAlias= new HashMap<String, BasicStepField>();
        Map<String, BasicStep> stepsByAlias= addSteps(query, tables, roles, !distinct, fieldsByAlias);

        // Add fields.
        Iterator<String> iFields= fields.iterator();
        while (iFields.hasNext()) {
            String field = iFields.next();
            addFields(query, field, stepsByAlias, fieldsByAlias);
        }

        // Add sortorders.
        addSortOrders(query, sortFields, directions, fieldsByAlias);

        // Supporting more then 1 source node or no source node at all
        // Note that node number -1 is seen as no source node
        if (snodes != null && snodes.size() > 0) {
            Integer nodeNumber= -1;

            // Copy list, so the original list is not affected.
            List<Integer> snodeNumbers = new ArrayList<Integer>();

            // Go trough the whole list of strings (each representing
            // either a nodenumber or an alias), convert all to Integer objects.
            // from last to first,,... since we want snode to be the one that
            // contains the first..
            for (int i= snodes.size() - 1; i >= 0; i--) {
                String str= snodes.get(i);
                try {
                    nodeNumber= Integer.valueOf(str);
                } catch (NumberFormatException e) {
                    // maybe it was not an integer, hmm lets look in OAlias
                    // table then
                    nodeNumber= mmb.getOAlias().getNumber(str);
                    if (nodeNumber.intValue() < 0) {
                        nodeNumber= 0;
                    }
                }
                snodeNumbers.add(nodeNumber);
            }

            Step nodesStep = getNodesStep(query.getSteps(), nodeNumber.intValue());

            if (nodesStep == null) {
                // specified a node which is not of the type of one of the steps.
                // take as default the 'first' step (which will make the result empty, compatible with 1.6, bug #6440).
                nodesStep = query.getSteps().get(0);
            }

            Iterator<Integer> iNodeNumbers= snodeNumbers.iterator();
            while (iNodeNumbers.hasNext()) {
                Integer number= iNodeNumbers.next();
                nodesStep.addNode(number.intValue());
            }
        }

        addRelationDirections(query, searchDirs, roles);

        // Add constraints.
        // QueryConverter supports the old formats for backward compatibility.
        QueryConvertor.setConstraint(query, where);

        return query;
    }

    /**
     * Creates a full chain of steps, adds these to the specified query.
     * This includes adding necessary relation tables when not explicitly
     * specified, and generating unique table aliases where necessary.
     * Optionally adds "number"-fields for all tables in the original chain.
     *
     * @param query The searchquery.
     * @param tables The original chain of tables.
     * @param roles Map of tablenames mapped to <code>Integer</code> values,
     *        representing the nodenumber of a corresponing RelDef node.
     *        This method adds entries for table aliases that specify a role,
     *        e.g. "related" or "related2".
     * @param includeAllReference Indicates if the "number"-fields must
     *        included in the query for all tables in the original chain.
     * @param fieldsByAlias Map, mapping aliases (fieldname prefixed by table
     *        alias) to the stepfields in the query. An entry is added for
     *        each stepfield added to the query.
     * @return Map, maps original table names to steps.
     * @since MMBase-1.7
     */
    // package access!
    Map<String, BasicStep> addSteps(BasicSearchQuery query, List<String> tables, Map<String, Integer> roles, boolean includeAllReference, Map<String, BasicStepField> fieldsByAlias) {

        Map<String, BasicStep> stepsByAlias= new HashMap<String, BasicStep>(); // Maps original table names to steps.
        Set<String> tableAliases= new HashSet<String>(); // All table aliases that are in use.

        Iterator<String> iTables= tables.iterator();
        if (iTables.hasNext()) {
            // First table.
            String tableName= iTables.next();
            MMObjectBuilder bul= getBuilder(tableName, roles);
            String tableAlias= getUniqueTableAlias(tableName, tableAliases, tables);
            BasicStep step= query.addStep(bul);
            step.setAlias(tableAlias);
            stepsByAlias.put(tableName, step);
            if (includeAllReference) {
                // Add number field.
                addField(query, step, "number", fieldsByAlias);
            }
        }
        while (iTables.hasNext()) {
            String tableName2 = iTables.next();
            MMObjectBuilder bul2 = getBuilder(tableName2, roles);
            BasicRelationStep relation;
            BasicStep step2;
            String tableName;
            if (bul2 instanceof InsRel) {
                // Explicit relation step.
                tableName = tableName2;
                InsRel bul = (InsRel)bul2;
                tableName2 = iTables.next();
                bul2 = getBuilder(tableName2, roles);
                relation = query.addRelationStep(bul, bul2);
                step2 = (BasicStep)relation.getNext();

                // MM: setting aliases used to be _inside_ the includeAllReference-if.
                // but I don't see how that would make sense. Trying a while like this.
                relation.setAlias(tableName);
                step2.setAlias(tableName2);
                if (includeAllReference) {
                    // Add number fields.
                    addField(query, relation, "number", fieldsByAlias);
                    addField(query, step2, "number", fieldsByAlias);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Created a relation step " + relation + " (explicit)" +  roles);
                }
            } else {
                // Not a relation, relation step is implicit.
                tableName =  "insrel";
                InsRel bul = mmb.getInsRel();
                relation = query.addRelationStep(bul, bul2);
                step2 = (BasicStep)relation.getNext();
                step2.setAlias(tableName2); //see above
                if (includeAllReference) {
                    // Add number field.
                    addField(query, step2, "number", fieldsByAlias);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Created a relation step " + relation + " (implicit)");
                }
            }
            String tableAlias = getUniqueTableAlias(tableName, tableAliases, tables);
            String tableAlias2 = getUniqueTableAlias(tableName2, tableAliases, tables);
            if (! tableName.equals(tableAlias)) {
                roles.put(tableAlias, roles.get(tableName));
            }
            relation.setAlias(tableAlias);
            step2.setAlias(tableAlias2);
            stepsByAlias.put(tableAlias, relation);
            stepsByAlias.put(tableAlias2, step2);
        }
        return stepsByAlias;
    }

    /**
     * Gets builder corresponding to the specified table alias.
     * This amounts to removing the optionally appended digit from the table
     * alias, and interpreting the result as either a tablename or a relation
     * role.
     *
     * @param tableAlias The table alias.
     *        Must be tablename or relation role, optionally appended
     *        with a digit, e.g. images, images3, related and related4.
     * @param roles Map of tablenames mapped to <code>Integer</code> values,
     *        representing the nodenumber of a corresponing RelDef node.
     *        This method adds entries for table aliases that specify a role,
     *        e.g. "related" or "related2".
     * @return The builder.
     * @since MMBase-1.7
     */
    // package access!
    MMObjectBuilder getBuilder(String tableAlias, Map<String, Integer> roles) {
        String tableName= getTableName(tableAlias);
        // check builder - should throw exception if builder doesn't exist ?
        MMObjectBuilder bul= null;
        try {
            bul= mmb.getBuilder(tableName);
        } catch (BuilderConfigurationException e) {}

        if (bul == null) {
            // check if it is a role name. if so, use the builder of the
            // rolename and store a filter on rnumber.
            int rnumber= mmb.getRelDef().getNumberByName(tableName);
            if (rnumber == -1) {
                throw new IllegalArgumentException("Specified builder '" + tableName + "' does not exist.");
            } else {
                bul = mmb.getRelDef().getBuilder(rnumber); // relation builder
                roles.put(tableAlias, rnumber);
            }
        } else if (bul instanceof InsRel) {
            int rnumber= mmb.getRelDef().getNumberByName(tableName);
            if (rnumber != -1) {
                roles.put(tableAlias, rnumber);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Resolved table alias \"" + tableAlias + "\" to builder \"" + bul.getTableName() + "\"");
        }
        return bul;
    }

    /**
     * Returns unique table alias, must be tablename/rolename, optionally
     * appended with a digit.
     * Tests the provided table alias for uniqueness, generates alternative
     * table alias if the provided alias is already in use.
     *
     * @param tableAlias The table alias.
     * @param tableAliases The table aliases that are already in use. The
     *        resulting table alias is added to this collection.
     * @param originalAliases The originally supplied aliases - generated
     *        aliases should not match any of these.
     * @return The resulting table alias.
     * @since MMBase-1.7
     */
    // package access!
    String getUniqueTableAlias(String tableAlias, Set<String> tableAliases, Collection<String> originalAliases) {

        // If provided alias is not unique, try alternatives,
        // skipping alternatives that are already in originalAliases.
        if (tableAliases.contains(tableAlias)) {
            tableName= getTableName(tableAlias);

            tableAlias= tableName;
            char ch= '0';
            while (originalAliases.contains(tableAlias) || tableAliases.contains(tableAlias)) {
                // Can't create more than 11 aliases for same tablename.
                if (ch > '9') {
                    throw new IndexOutOfBoundsException("Failed to create unique table alias, because there "
                                                        + "are already 11 aliases for this tablename: \"" + tableName + "\"");
                }
                tableAlias = tableName + ch;
                ch++;
            }
        }

        // Unique table alias: add to collection, return as result.
        tableAliases.add(tableAlias);
        return tableAlias;
    }

    /**
     * Retrieves fieldnames from an expression, and adds these to a search
     * query.
     * The expression may be either a fieldname or a a functionname with a
     * (commaseparated) parameterlist between parenthesis
     * (parameters being expressions themselves).
     * <p>
     * Fieldnames must be formatted as <em>stepalias.field</em>.
     *
     * @param query The query.
     * @param expression The expression.
     * @param stepsByAlias Map, mapping step aliases to the steps in the query.
     * @param fieldsByAlias Map, mapping field aliases (fieldname prefixed by
     *        table alias) to the stepfields in the query.
     *        An entry is added for each stepfield added to the query.
     * @since MMBase-1.7
     */
    // package access!
    void addFields(BasicSearchQuery query, String expression, Map<String, BasicStep> stepsByAlias, Map<String, BasicStepField> fieldsByAlias) {

        // TODO RvM: stripping functions is this (still) necessary?.
        // Strip function(s).
        int pos1= expression.indexOf('(');
        int pos2= expression.indexOf(')');
        if (pos1 != -1 ^ pos2 != -1) {
            // Parenthesis do not match.
            throw new IllegalArgumentException("Parenthesis do not match in expression: \"" + expression + "\"");
        } else if (pos1 != -1) {
            // Function parameter list containing subexpression(s).
            String parameters= expression.substring(pos1 + 1, pos2);
            Iterator<String> iParameters= getFunctionParameters(parameters).iterator();
            while (iParameters.hasNext()) {
                String parameter= iParameters.next();
                addFields(query, parameter, stepsByAlias, fieldsByAlias);
            }
        } else if (!Character.isDigit(expression.charAt(0))) {
            int pos= expression.indexOf('.');
            if (pos < 1 || pos == (expression.length() - 1)) {
                throw new IllegalArgumentException("Invalid fieldname: \"" + expression + "\"");
            }
            int bracketOffset = (expression.startsWith("[") && expression.endsWith("]")) ? 1 : 0;
            String stepAlias= expression.substring(0 + bracketOffset, pos);
            String fieldName= expression.substring(pos + 1 - bracketOffset);

            BasicStep step = stepsByAlias.get(stepAlias);
            if (step == null) {
                throw new IllegalArgumentException("Invalid step alias: \"" + stepAlias + "\" in fields list");
            }
            addField(query, step, fieldName, fieldsByAlias);
        }
    }

    /**
     * Adds field to a search query, unless it is already added.
     *
     * @param query The query.
     * @param step The non-null step corresponding to the field.
     * @param fieldName The fieldname.
     * @param fieldsByAlias Map, mapping field aliases (fieldname prefixed by
     *        table alias) to the stepfields in the query.
     *        An entry is added for each stepfield added to the query.
     * @since MMBase-1.7
     */
    private void addField(BasicSearchQuery query, BasicStep step, String fieldName, Map<String, BasicStepField> fieldsByAlias) {

        // Fieldalias = stepalias.fieldname.
        // This value is used to store the field in fieldsByAlias.
        // The actual alias of the field is not set.
        String fieldAlias= step.getAlias() + "." + fieldName;
        if (fieldsByAlias.containsKey(fieldAlias)) {
            // Added already.
            return;
        }

        MMObjectBuilder builder= mmb.getBuilder(step.getTableName());
        CoreField fieldDefs= builder.getField(fieldName);
        if (fieldDefs == null) {
            throw new IllegalArgumentException("Not a known field of builder " + step.getTableName() + ": \"" + fieldName + "\"");
        }

        // Add the stepfield.
        BasicStepField stepField= query.addField(step, fieldDefs);
        fieldsByAlias.put(fieldAlias, stepField);
    }

    /**
     * Adds sorting orders to a search query.
     *
     * @param query The query.
     * @param fieldNames The fieldnames prefixed by the table aliases.
     * @param directions The corresponding sorting directions ("UP"/"DOWN").
     * @param fieldsByAlias Map, mapping field aliases (fieldname prefixed by
     *        table alias) to the stepfields in the query.
     * @since MMBase-1.7
     */
    // package visibility!
    void addSortOrders(BasicSearchQuery query, List<String> fieldNames, List<String> directions, Map<String, BasicStepField> fieldsByAlias) {

        // Test if fieldnames are specified.
        if (fieldNames == null || fieldNames.size() == 0) {
            return;
        }

        int defaultSortOrder= SortOrder.ORDER_ASCENDING;
        if (directions != null && directions.size() != 0) {
            if (directions.get(0).trim().equalsIgnoreCase("DOWN")) {
                defaultSortOrder= SortOrder.ORDER_DESCENDING;
            }
        }

        Iterator<String> iFieldNames= fieldNames.iterator();
        Iterator<String> iDirections= directions.iterator();
        while (iFieldNames.hasNext()) {
            String fieldName = iFieldNames.next();
            StepField field= fieldsByAlias.get(fieldName);
            if (field == null) {
                // Field has not been added.
                field= ConstraintParser.getField(fieldName, query.getSteps());
            }
            if (field == null) {
                throw new IllegalArgumentException("Invalid fieldname: \"" + fieldName + "\"");
            }

            // Add sort order.
            BasicSortOrder sortOrder= query.addSortOrder(field); // ascending

            // Change direction if needed.
            if (iDirections.hasNext()) {
                String direction = iDirections.next();
                if (direction.trim().equalsIgnoreCase("DOWN")) {
                    sortOrder.setDirection(SortOrder.ORDER_DESCENDING);
                } else if (!direction.trim().equalsIgnoreCase("UP")) {
                    throw new IllegalArgumentException("Parameter directions contains an invalid value ("+direction+"), should be UP or DOWN.");
                }

            } else {
                sortOrder.setDirection(defaultSortOrder);
            }
        }
    }

    /**
     * Gets first step from list, that corresponds to the builder
     * of a specified node - or one of its parentbuilders.
     *
     * @param steps The steps.
     * @param nodeNumber The number identifying the node.
     * @return The step, or <code>null</code> when not found.
     * @since MMBase-1.7
     */
    // package visibility!
    Step getNodesStep(List<Step> steps, int nodeNumber) {
        if (nodeNumber < 0) {
            return null;
        }

        MMObjectNode node= getNode(nodeNumber);
        if (node == null) {
            return null;
        }

        MMObjectBuilder builder = node.parent;
        Step result = null;
        do {
            // Find step corresponding to builder.
            Iterator<Step> iSteps= steps.iterator();
            while (iSteps.hasNext() && result == null) {
                Step step= iSteps.next();
                if (step.getTableName().equals(builder.tableName)) {  // should inheritance not be considered?
                    // Found.
                    result = step;
                }
            }
            // Not found, then try again with parentbuilder.
            builder = builder.getParentBuilder();
        } while (builder != null && result == null);

        return result;
    }

    /**
     * Adds relation directions.
     *
     * @param query The search query.
     * @param searchDirs Specifies in which direction relations are to be followed. You can specify a direction for each
     *      relation in the path. If you specify less directions than there are relations, the last specified direction is used
     *      for the remaining relations. If you specify an empty list the default direction is BOTH.
     * @param roles Map of tablenames mapped to <code>Integer</code> values,
     *        representing the nodenumber of the corresponing RelDef node.
     * @since MMBase-1.7
     */
    // package visibility!
    void addRelationDirections(BasicSearchQuery query, List<Integer> searchDirs, Map<String, Integer> roles) {

        Iterator<Step> iSteps = query.getSteps().iterator();
        Iterator<Integer> iSearchDirs = searchDirs.iterator();
        int searchDir = RelationStep.DIRECTIONS_BOTH;

        if (! iSteps.hasNext()) return; // nothing to be done.
        Step sourceStep = iSteps.next();
        Step destinationStep = null;

        while (iSteps.hasNext()) {
            if (destinationStep != null) {
                sourceStep = destinationStep;
            }
            BasicRelationStep relationStep= (BasicRelationStep)iSteps.next();
            destinationStep= iSteps.next();
            if (iSearchDirs.hasNext()) searchDir = iSearchDirs.next().intValue();


            // FIXME this cast to BasicStep is ugly and should not be here in a clean implementation

            // Determine typedef number of the source-type.
            int sourceType = ((BasicStep)sourceStep).getBuilder().getObjectType();
            // Determine the typedef number of the destination-type.
            int destinationType = ((BasicStep)destinationStep).getBuilder().getObjectType();

            // Determine reldef number of the role.
            Integer role = roles.get(relationStep.getAlias());

            int roleInt;
            if (role != null) {
                roleInt =  role.intValue();
                relationStep.setRole(role);
            } else {
                roleInt = -1;
            }

            if (!mmb.getTypeRel().optimizeRelationStep(relationStep, sourceType, destinationType, roleInt, searchDir)) {
                if (searchDir != RelationStep.DIRECTIONS_SOURCE && searchDir != RelationStep.DIRECTIONS_DESTINATION) {
                    log.warn("No relation defined between " + sourceStep.getTableName() + " and " + destinationStep.getTableName() + " using " + relationStep + " with direction(s) " + getSearchDirString(searchDir) + ". Searching in 'destination' direction now, but perhaps the query should be fixed, because this should always result nothing.");
                } else {
                    log.warn("No relation defined between " + sourceStep.getTableName() + " and " + destinationStep.getTableName() + " using " + relationStep + " with direction(s) " + getSearchDirString(searchDir) + ". Trying anyway, but perhaps the query should be fixed, because this should always result nothing.");
                }
                log.warn(Logging.applicationStacktrace());
            }

        }
    }

}
