/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import java.sql.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.module.database.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.storage.search.legacy.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * ClusterBuilder is a builder which creates 'virtual' nodes.
 * This class replaces {@link org.mmbase.module.builders.MultiRelations}.
 * <br />
 * The nodes are build out of a set of fields from different nodes, combined through a complex query,
 * which is in turn based on the relations that exist between nodes.<br />
 * The builder supplies a method to retrieve these virtual nodes: {@link
 * #searchMultiLevelVector(Vector,Vector,String,Vector,String,Vector,Vector,int)
 * searchMultiLevelVector()}.
 * Other public methods in this builder function to handle the requests for data obtained from this particular node.
 * Individual nodes in a 'cluster' node can be retrieved by calling the getNodeValue() method, with the builder name
 * of the needed node as parameter value.
 *
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Rob van Maris
 * @version $Id: ClusterBuilder.java,v 1.34 2003-03-25 19:40:25 robmaris Exp $
 */
public class ClusterBuilder extends VirtualBuilder {

    /**
     * Search for all valid relations.
     * When searching relations, return both relations from source to deastination and from destination to source,
     * provided directionality allows
     */
    public static final int SEARCH_BOTH = 0;
    /**
     * Search for destinations,
     * When searching relations, return only relations from source to deastination.
     */
    public static final int SEARCH_DESTINATION = 1;
    /**
     * Seach for sources.
     * When searching a multilevel, return only relations from destination to source, provided directionality allows
     */
    public static final int SEARCH_SOURCE = 2;
    /**
     * Search for all relations.
     * When searching a multilevel, return both relations from source to deastination and from destination to source.
     * Directionality is not checked - ALL relations are used.
     */
    public static final int SEARCH_ALL = 3;

    /**
     * Search for either destination or source.
     * When searching a multilevel, return either relations from source to destination OR from destination to source.
     * The returned set is decided through the typerel tabel. However, if both directiosn ARE somehow supported, the
     * system onyl returns source to destination relations.
     * This is the default value (for compatibility purposes).
     */
    public static final int SEARCH_EITHER = 4;

    // logging variable
    private static Logger log = Logging.getLoggerInstance(ClusterBuilder.class.getName());

    /**
     * Creates <code>ClusterBuilder</code> instance.
     * Must be called from the MMBase class.
     * @param m the MMbase cloud creating the node
     * @scope package
     */
    public ClusterBuilder(MMBase m) {
        super(m,"clusternodes");
    }

    /**
     * Translates a string to a search direction constant.
     *
     * @since MMBase-1.6
     */
    public static int getSearchDir(String search) {
        if (search == null) return SEARCH_EITHER;
        search = search.toUpperCase();
        if ("DESTINATION".equals(search)) {
            return SEARCH_DESTINATION;
        } else if ("SOURCE".equals(search)) {
            return SEARCH_SOURCE;
        } else if ("BOTH".equals(search)) {
            return SEARCH_BOTH;
        } else if ("ALL".equals(search)) {
            return SEARCH_ALL;
        } else if ("EITHER".equals(search)) {
            return SEARCH_EITHER;
        } else {
            throw  new RuntimeException("'" + search + "' cannot be converted to a search-direction constant");
        }

    }

    /**
     * Translates a search direction constant to a string.
     *
     * @since MMBase-1.6
     */
    public static String getSearchDirString(int search) {
        if (search==SEARCH_DESTINATION) {
            return "DESTINATION";
        } else if (search==SEARCH_SOURCE) {
            return "SOURCE";
        } else if (search==SEARCH_BOTH) {
            return "BOTH";
        } else if (search==SEARCH_ALL) {
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
    public MMObjectNode getNewNode(String owner) {
        MMObjectNode node=new ClusterNode(this);
        return node;
    }

    /**
     * What should a GUI display for this node.
     * This version displays the contents of the 'name' field(s) that were retrieved.
     * XXX: should be changed to something better
     * @param node The node to display
     * @return the display of the node as a <code>String</code>
     */
     public String getGUIIndicator(MMObjectNode node) {
        // Return "name"-field when available.
        String s = node.getStringValue("name");
        if (s!=null) {
            return s;
        }

        // Else "name"-fields of contained nodes.
        s = "";
        for (Enumeration i=node.values.keys(); i.hasMoreElements(); ) {
            String key = (String)i.nextElement();
            if (key.endsWith(".name")) {
                if (s.length()!=0) s+=", ";
                s+=node.values.get(key);
            }
        }
        if (s.length()>15) {
            return s.substring(0,12)+"...";
        } else {
            return s;
        }
    }

    /**
     * What should a GUI display for this node/field combo.
     * For a multilevel node, the builder tries to determine
     * the original builder of a field, and invoke the method using
     * that builder.
     *
     * @param node The node to display
     * @param field the name field of the field to display
     * @return the display of the node's field as a <code>String</code>, null if not specified
     */
    public String getGUIIndicator(String field,MMObjectNode node) {
        int pos=field.indexOf('.');
        String bulname=null;
        if ((pos!=-1) && (node instanceof ClusterNode)) {
            bulname=field.substring(0,pos);
        }
        MMObjectNode n=((ClusterNode)node).getRealNode(bulname);
        if (n==null) n=node;
        bulname=getTrueTableName(bulname);
        MMObjectBuilder bul=mmb.getMMObject(bulname);
        if (bul!=null) {
            String tmp=field.substring(pos+1);
            return bul.getGUIIndicator(tmp, n);
        }
        return null;
    }

    /**
     * Determines the builder part of a specified field.
     * @param fieldname the name of the field
     * @return the name of the field's builder
     */
    public String getBuilderNameFromField(String fieldname) {
        String bulname="";
        int pos=fieldname.indexOf(".");
        if (pos!=-1) {
            bulname=fieldname.substring(0,pos);
            return getTrueTableName(bulname);
        }
        return "";
    }

    /**
     * Determines the fieldname part of a specified field (without the builder name).
     * @param fieldname the name of the field
     * @return the name of the field without its builder
     */
    public String getFieldNameFromField(String fieldname) {
        int pos=fieldname.indexOf(".");
        if (pos!=-1) {
            fieldname=fieldname.substring(pos+1);
        }
        return fieldname;
    }

    /**
     * Return a field.
     * @param the requested field's name
     * @return the field
     */
    public FieldDefs getField(String fieldName) {
        String buildername=getBuilderNameFromField(fieldName);
        if (buildername.length()>0) {
            MMObjectBuilder bul=mmb.getMMObject(buildername);
            return bul.getField(getFieldNameFromField(fieldName));
        }
        return null;
    }

    /**
     * Return all the objects that match the searchkeys.
     * @param snode The number of the node to start the search with. The node has to be present in the first table
     *      listed in the tables parameter.
     * @param fields The fieldnames to return. This should include the name of the builder. Fieldnames without a builder prefix are ignored.
     *      Fieldnames are accessible in the nodes returned in the same format (i.e. with manager indication) as they are specified in this parameter.
     *      Examples: 'people.lastname'
     * @param pdistinct 'YES' indicates the records returned need to be distinct. Any other value indicates double values can be returned.
     * @param tables The builder chain. A list containing builder names.
     *      The search is formed by following the relations between successive builders in the list. It is possible to explicitly supply
     *      a relation builder by placing the name of the builder between two builders to search.
     *      Example: company,people or typedef,authrel,people.
     * @param where The contraint. this is in essence a SQL where clause, using the NodeManager names from the nodes as tablenames.
     *      The syntax is either sql (if preceded by "WHERE') or
     *      Examples: "WHERE people.email IS NOT NULL", "(authrel.creat=1) and (people.lastname='admin')"
     * @param orderVec the fieldnames on which you want to sort.
     * @param direction A list of values containing, for each field in the order parameter, a value indicating whether the sort is
     *      ascending (<code>UP</code>) or descending (<code>DOWN</code>). If less values are syupplied then there are fields in order,
     *      the first value in the list is used for the remaining fields. Default value is <code>'UP'</code>.
     * @return a <code>Vector</code> containing all matching nodes
     */
    public Vector searchMultiLevelVector(int snode,Vector fields,String pdistinct,Vector tables,String where, Vector orderVec,Vector direction) {
        Vector v=new Vector();
        v.addElement(""+snode);
        return searchMultiLevelVector(v,fields,pdistinct,tables,where,orderVec,direction, SEARCH_EITHER);
    }

    /**
     * Return all the objects that match the searchkeys.
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
     * @param where The contraint. this is in essence a SQL where clause, using the NodeManager names from the nodes as tablenames.
     *      The syntax is either sql (if preceded by "WHERE') or
     *      Examples: "WHERE people.email IS NOT NULL", "(authrel.creat=1) and (people.lastname='admin')"
     * @param orderVec the fieldnames on which you want to sort.
     * @param direction A list of values containing, for each field in the order parameter, a value indicating whether the sort is
     *      ascending (<code>UP</code>) or descending (<code>DOWN</code>). If less values are syupplied then there are fields in order,
     *      the first value in the list is used for the remaining fields. Default value is <code>'UP'</code>.
     * @return a <code>Vector</code> containing all matching nodes
     */
    public Vector searchMultiLevelVector(Vector snodes,Vector fields,String pdistinct,Vector tables,String where, Vector orderVec,Vector direction) {
        return searchMultiLevelVector(snodes,fields,pdistinct,tables,where,orderVec,direction, SEARCH_EITHER);
    }

    /**
     * Return all the objects that match the searchkeys.
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
     * @param where The contraint. this is in essence a SQL where clause, using the NodeManager names from the nodes as tablenames.
     *      The syntax is either sql (if preceded by "WHERE') or
     *      Examples: "WHERE people.email IS NOT NULL", "(authrel.creat=1) and (people.lastname='admin')"
     * @param orderVec the fieldnames on which you want to sort.
     * @param direction A list of values containing, for each field in the order parameter, a value indicating whether the sort is
     *      ascending (<code>UP</code>) or descending (<code>DOWN</code>). If less values are syupplied then there are fields in order,
     *      the first value in the list is used for the remaining fields. Default value is <code>'UP'</code>.
     * @param searchDir Specifies in which direction relations are to be
     *      followed, this must be one of the values defined by this class.
     * @return a <code>Vector</code> containing all matching nodes
     */
    public Vector searchMultiLevelVector(
            Vector snodes,Vector fields,String pdistinct,
            Vector tables,String where, Vector orderVec,Vector direction,
            int searchdir) {

        // Try to handle using the SearchQuery framework.
        try {
            SearchQuery query =
            getMultiLevelSearchQuery(snodes, fields, pdistinct, tables, where,
            orderVec, direction, searchdir);
            List clusterNodes = getClusterNodes(query);
            return new Vector(clusterNodes);

        // If this fails, fall back to legacy code.
        } catch (Exception e) {
            if (log.isServiceEnabled()) {
                log.service(
                    "Failed to create SearchQuery for multilevel search, "
                    + "exception:\n"
                    + Logging.stackTrace(e)
                    + "\nFalling back to legacy code in ClusterBuilder...");
            }
        }

        // Legacy code starts here.
        String stables,relstring,select,order,basenodestring,distinct;
        Vector alltables,selectTypes;
        MMObjectNode basenode;
        HashMap roles= new HashMap();
        int snode;

        boolean isdistinct = pdistinct!=null && pdistinct.equalsIgnoreCase("YES");
        if (isdistinct) {
            distinct="distinct";
        }  else {
            distinct="";
        }

        // Get ALL tables (including missing reltables)
        alltables=getAllTables(tables,roles);
        if (alltables==null) return null;

        // Get the destination select string;
        // if the requested set is not DISTINCT, the
        // query also searches all number-fields of the builders
        // involved.
        // Possibly, we want to turn this off or on, i.e. by using another
        // value for the distinct parameter (such as "BYREFERENCE")
        // Note that due to teh problems with distinct result sets, this is not
        // yet an optimal sollution for the multilevel authorization problem

        select=getSelectString(alltables,tables,fields,!isdistinct);
        if (select==null) return null;

        // Get the tables names corresponding to the fields (for the mapping)
        selectTypes=getSelectTypes(alltables,select);

        // create the order parts
        order=getOrderString(alltables,orderVec,direction);

        // get all the table names
        stables=getTableString(alltables,roles);

        // Supporting more then 1 source node or no source node at all
        // Note that node number -1 is seen as no source node
        if ((snodes!=null) && (snodes.size()>0)) {
            String str;
            basenode=null;

            // go trough the whole list and verify that it are all integers
            // from last to first,,... since we want snode to be the one that contains the first..
            for (int i=snodes.size() - 1 ; i >= 0 ; i--) {
                str = ((String)snodes.elementAt(i)).trim();
                // '-1' means no node, so skip
                if (!str.equals("-1")) {
                    basenode=getNode(str);
                    if (basenode==null) {
                        throw new RuntimeException("Cannot find node: "+str);
                    }
                    snodes.setElementAt(""+basenode.getNumber(), i);
                }
            }

            int sidx;
            StringBuffer bb=new StringBuffer();

            // if a basenode is given (i.e. node is not -1)
            if (basenode!=null) {
                // not very neat... but it works
                sidx=alltables.indexOf(basenode.parent.tableName);
                if (sidx<0) sidx=alltables.indexOf(basenode.parent.tableName+"1");
                // if we can't find the real parent assume object
                if (sidx<0) sidx=alltables.indexOf("object");
                if (sidx<0) sidx=0;
                str=idx2char(sidx);
                bb.append(getNodeString(str,snodes));
                // Check if we got a relation to ourself
                basenodestring=bb.toString();
            } else {
                basenodestring="";
            }
        } else {
            basenodestring="";
        }

        // get the relation string
        relstring=getRelationString(alltables, searchdir, roles);
        // check if this is an 'invalid' condition (one which never produces results,
        // in that case, return empty resultset
        if (relstring==null) {
            return new Vector();
        }
        if ((relstring.length()>0) && (basenodestring.length()>0)) {
                relstring=" AND "+relstring;
        }

        // create the extra where parts

        if (where!=null && !where.trim().equals("")) {
            where=QueryConvertor.altaVista2SQL(where).substring(5);
            where=getWhereConvert(alltables,where,tables);
            if (basenodestring.length()+relstring.length()>0) {
                where=" AND ("+where+")";
            }
        } else {
            where="";
        }

        String query="";
        try {
            MultiConnection con=null;
            Statement stmt=null;
            try {
                con=mmb.getConnection();
                stmt=con.createStatement();
                if (basenodestring.length()+relstring.length()+where.length()>1) {
                    query="select "+distinct+" "+select+" from "+stables+" where "+basenodestring+relstring+where+" "+order;
                } else {
                    query="select "+distinct+" "+select+" from "+stables+" "+order;
                }
                log.debug("Query "+query);

                ResultSet rs=stmt.executeQuery(query);
                try {
                    ClusterNode node;
                    Vector results=new Vector();
                    String tmp,prefix;
                    while(rs.next()) {
                        // create a new VIRTUAL object and add it to the result vector
                        node=new ClusterNode(this,tables.size());
                        ResultSetMetaData rd=rs.getMetaData();
                        String fieldname;
                        for (int i=1;i<=rd.getColumnCount();i++) {
                            prefix=selectTypes.elementAt(i-1)+".";
                            fieldname=rd.getColumnName(i);
                            mmb.getDatabase().decodeDBnodeField(node,fieldname,rs,i,prefix);
                        }
                        node.initializing=false;
                        results.addElement(node);
                    }
                    //  return the results
                    return results;
                } finally {
                    rs.close();
                }
            } finally {
                mmb.closeConnection(con,stmt);
            }
        } catch (Exception e) {
            // something went wrong print it to the logs
            log.error("Query failed:"+query);
            log.error(Logging.stackTrace(e));
            return null;
        }
    }

    /**
     * Executes query, returns results as {@link ClusterNode clusternodes}.
     *
     * @param query The query.
     * @return The clusternodes.
     * @throws org.mmbase.storage.search.SearchQueryException
     *         When an exception occurred while retrieving the results.
     * @since MMBase-1.7
     * @see org.mmbase.storage.search.SearchQueryHandler#getNodes
     */
    public List getClusterNodes(SearchQuery query) throws SearchQueryException {

        // TODO (later): implement maximum set by maxNodesFromQuery?

        // Execute query, return results.
        return mmb.getDatabase().getNodes(query, this);
    }

    /**
     * Stores the tables/builder names used in the request for each field to return.
     * @param fields the list of requested fields
     * @return a list of prefixes of fieldnames
     */
    private Vector getSelectTypes(Vector alltables, String fields) {
        Vector result=new Vector();
        String val;
        int pos;
        for (Enumeration e=getFunctionParameters(fields).elements();e.hasMoreElements();) {
            val=(String)e.nextElement();
            int idx=val.charAt(0) - 'a';
            result.addElement(alltables.get(idx));
        }
        return result;
    }


    /**
     * Creates a full chain of table names.
     * This includes adding relation tables when not specified, and converting table names by
     * removing numeric extensions (such as people1,people2).
     * @param tables the original chain of tables
     * @param roles Map of tablenames mapped to <code>Integer</code> values,
     *        representing the nodenumber of a corresponing RelDef node.
     *        This method adds entries for tablenames that specify a role,
     *        e.g. "related" or "related2".
     * @return an expanded list of tablesnames
     */
    private Vector getAllTables(Vector tables, HashMap roles) {
        Vector alltables=new Vector();
        boolean lastrel=true;  // true: prevents the first table to be preceded by a relation table
        String orgtable,curtable;

        for (Enumeration e=tables.elements();e.hasMoreElements();) {
            orgtable=(String)e.nextElement();
            curtable= getTableName(orgtable);
            // check builder - should throw exception if builder doesn't exist ?
            MMObjectBuilder bul = mmb.getMMObject(curtable);
            if (bul==null) {
                // check if it is a role name. if so, use the builder of the rolename and
                // store a filter on rnumber.
                int rnumber = mmb.getRelDef().getNumberByName(curtable);
                if (rnumber==-1) {
                    String msg = "Specified builder "+curtable+" does not exist.";
                    log.error(msg);
                    throw new RuntimeException(msg);
                } else {
                    bul=mmb.getInsRel(); // dummy
                    roles.put(orgtable,new Integer(rnumber));
                }
            } else if (bul instanceof InsRel) {
                int rnumber = mmb.getRelDef().getNumberByName(curtable);
                if (rnumber!=-1) {
                    roles.put(orgtable,new Integer(rnumber));
                }
            }
            if (bul instanceof InsRel) {
                alltables.addElement(orgtable);
                lastrel=!lastrel;  // toggle lastrel - allows for relations to be made to relationnnodes
            } else {
                // nonrel, nonrel
                if (!lastrel) {
                    alltables.addElement(mmb.getInsRel().getTableName());
                }
                alltables.addElement(orgtable);
                lastrel=false;
            }
        }
        return alltables ;
    }

    /**
     * Returns the number part of a tablename, provided it has one.
     * The number is the numeric digit appended at a name in order to make using a table more than once possible.
     * @param table name of the original table
     * @return An <code>int</code> containing the table number, or -1 if the table has no number
     */
    private int getTableNumber(String table) {
        char ch;
        ch=table.charAt(table.length()-1);
        if (Character.isDigit(ch)) {
            return Integer.parseInt(""+ch);
        } else {
            return -1;
        }
    }

    /**
     * Returns the name part of a tablename.
     * The name part is the table name minus the numeric digit appended
     * to a name (if appliable).
     * @param table name of the original table
     * @return A <code>String</code> containing the table name
     */
    private String getTableName(String table) {
        char ch;
        ch=table.charAt(table.length()-1);
        if (Character.isDigit(ch)) {
            return table.substring(0,table.length()-1);
        } else {
            return table;
        }
    }

    /**
     * Returns the name part of a tablename, and convert it to a buidler name.
     * This will catch specifying a rolename in stead of a builder name when using relations.
     * @param table name of the original table
     * @return A <code>String</code> containing the table name
     */
    private String getTrueTableName(String table) {
        String tab=getTableName(table);
        int rnumber = mmb.getRelDef().getNumberByName(tab);
        if (rnumber!=-1) {
            return mmb.getRelDef().getBuilderName(getNode(rnumber));
        } else {
            return tab;
        }
    }

    /**
     * Determines the SQL-query version of a table alias.
     * This is done by searching for the appropriate tablename in a known list, and
     * calculating a name based on the index in that list.
     * @param alltables the tablenames known (used to determine the SQL tablename)
     * @param table the table name to convert
     * @return the SQL table alias as a <code>String</code>
     */
    private String getSQLTableAlias(Vector alltables,String table) {
        int idx=alltables.indexOf(table);
        if (idx>=0) {
            return idx2char(idx);
        } else {
            return null;
        }
    }

    /**
     * Determines the SQL-query version of a prefixed field name.
     * This means: replacing the table name prefix in the specified field name
     * by the table alias created for the query, and replacing the fieldname by
     * a so-called 'allowed' fieldname.
     *
     * @param alltables the tablenames known (used to determine the SQL tablename)
     * @param fieldname the field name to convert
     * @return the SQL field name as a <code>String</code>
     */
    private String getSQLFieldName(Vector alltables, String fieldName) {
        int pos=fieldName.indexOf('.'); // check if a tablename precedes the fieldname
        if (pos!=-1) {
            String table=fieldName.substring(0,pos); // the table
            String idxn=getSQLTableAlias(alltables,table);
            if (idxn==null) {
                log.error("getSQLFieldName(): The field \""+fieldName+"\" has an invalid type specified");
            } else {
                String field=fieldName.substring(pos+1); // the field
                field=mmb.getDatabase().getAllowedField(field);
                return idxn+"."+field;
            }
        } else {
            // field has no type
            log.error("getSQLFieldName(): The field \""+fieldName+"\" has no type specified");
        }
        return null;
    }

    /**
     * Retrieves fieldnames from an ezpression, and adds these to a set of
     * fieldnames.
     * The expression may be either a fieldname or a a functionname with a
     * parameterlist between parenthesis.
     *
     * @param alltables List of tablenames, used for deriving a SQL table name
     * @param val the value to retrieve the fieldname from
     * @param realfields the set to add the fieldnames to
     */
    private void obtainSelectField(Vector alltables, String val, HashSet realfields) {
        // strip the function(s)
        int pos=val.indexOf('(');
        if (pos!=-1) {
            val=val.substring(pos+1);
            pos=val.lastIndexOf(')');
            if (pos!=-1) {
                val=val.substring(0,pos);
            }
            Vector fields=getFunctionParameters(val);
            for (int i=0; i<fields.size(); i++) {
                obtainSelectField(alltables,(String)fields.get(i), realfields);
            }
        } else {
            if (!Character.isDigit(val.charAt(0))) {
                String field=getSQLFieldName(alltables,val);
                if (field!=null) {
                    realfields.add(field);
                }
            }
        }
    }

    /**
     * Creates a select string for the Multi level query.
     * This consists of a list of fieldnames, preceded by a tablename.
     * @param alltables the tablenames to use
     * @param rfields the fields that were requested
     * @return a select <code>String</code>
     */
    protected String getSelectString(Vector alltables,Vector rfields) {
        return getSelectString(alltables, null,rfields,false);
    }

    /**
     * Creates a select string for the Multi level query.
     * This consists of a list of fieldnames, preceded by a tablename.
     * @param alltables the tablenames to use
     * @param originaltables the original tablenames that were specified
     * @param rfields the fields that were requested
     * @param includeAllReferences if <code>true</code>, for each trable specified in
     *        <code>originaltables</code>, the number field will be added to the
     *        returned select string. If <code>false</code>, no additional fields
     *        will be added (this should change : fields should be added, but
     *        only for those tables for which fields have been requested).
     * @return a select <code>String</code>
     */
    protected String getSelectString(Vector alltables,Vector originaltables,
                                     Vector rfields, boolean includeAllReferences) {
        String result=null;
        String val,field;
        HashSet realfields = new HashSet();
        for (Enumeration r=rfields.elements();r.hasMoreElements();) {
            val=(String)r.nextElement();
            obtainSelectField(alltables,val,realfields);
        }

        // optionally add "number" fields for all originally specified tables
        if (includeAllReferences) {
            for (Enumeration r=originaltables.elements();r.hasMoreElements();) {
                val=(String)r.nextElement();
                realfields.add(numberOf(getSQLTableAlias(alltables,val)));
            }
        }
        for (Iterator r=realfields.iterator();r.hasNext();) {
            val=(String)r.next();
            if (result==null) {
              result=val;
            } else {
              result+=", "+val;
            }
        }
        return result;
    }

    /**
     * Creates an order string for the Multi level query.
     * This consists of a list of fieldnames (preceded by a tablename), with an ascending or descending order.
     * @param alltables the tablenames to use
     * @param orders the fields that were requested
     * @param direction the direction of each order field ("UP" or "DOWN")
     * @return a order <code>String</code>
     */
    private String getOrderString(Vector alltables,Vector orders,Vector direction) {
        String result="";
        String val,field,dir;
        int opos;

        if (orders==null) return result.toString();
        // Convert direction table
        for (int pos=0; pos<direction.size(); pos++) {
            val=(String)direction.elementAt(pos);
            if (val.equalsIgnoreCase("DOWN")) {
                direction.setElementAt("DESC",pos); // DOWN is DESC
            } else {
                direction.setElementAt("ASC",pos);  // UP is ASC
            }
        }

        opos=0;
        for (Enumeration r=orders.elements();r.hasMoreElements();opos++) {
            val=(String)r.nextElement();
            field=getSQLFieldName(alltables,val);
            if (field==null) {
                return null;
            } else {
                if (!result.equals("")) {
                    result+=", ";
                } else {
                    result+=" ORDER BY ";
                }
                if (opos<direction.size()) {
                    dir=(String)direction.elementAt(opos);
                } else {
                    dir=(String)direction.elementAt(0);
                }
                result+=field+" "+dir;
            }
        }
        return result;
    }

    /**
     * Creates a WHERE clause for the Multi level query.
     * This involves replacing fieldnames in the clouse with those fit for the SQL query.
     * @param alltables the tablenames to use
     * @param string the original where clause
     * @param tables ?
     * @return a where clause <code>String</code>
     */
    private String getWhereConvert(Vector alltables,String where,Vector tables) {
        String atable,table,result=where;
        int cx;
        char ch;

        for (Enumeration e=tables.elements();e.hasMoreElements();) {
            atable=(String)e.nextElement();
            table = getSQLTableAlias(alltables,atable);

            // This translates the long tablename to the short one
            // i.e. people.account to a.account.
            cx=result.indexOf(atable+".",0);
            while (cx!=-1) {
                if (cx>0)
                    ch=result.charAt(cx-1);
                else
                    ch=0;
                if (!isTableNameChar(ch)) {
                    int fx=cx+atable.length()+1;
                    int lx;
                    for (lx=fx;
                         lx < result.length() && (Character.isLetterOrDigit(result.charAt(lx)) || result.charAt(lx) == '_');
                         lx++);
                    result=result.substring(0,cx)+
                           table+"."+
                           mmb.getDatabase().getAllowedField(result.substring(fx,lx))+
                           result.substring(lx);
                }
                cx=result.indexOf(atable+".",cx+1);
            }
            log.debug("getWhereConvert for table "+atable+"|"+result+"|");
        }
        return result;
    }

    /**
     * This method defines what is 'allowed' in tablenames.
     * Multilevel uses this to find out what is a tablename and what not
     */
    private boolean isTableNameChar(char ch) {
        return  (ch=='_') || Character.isLetterOrDigit(ch);
    }

    /**
     * This method defines what is 'allowed' in tablenames.
     * Multilevel uses this to find out what is a tablename and what not
     */
    protected String getTableString(Vector alltables) {
        return getTableString(alltables, new HashMap());
    }

    /**
     * This method defines what is 'allowed' in tablenames.
     * Multilevel uses this to find out what is a tablename and what not
     */
    protected String getTableString(Vector alltables, HashMap roles) {
        StringBuffer result=new StringBuffer("");
        String val;
        int idx=0;

        for (Enumeration r=alltables.elements();r.hasMoreElements();) {
            val=(String)r.nextElement();
            if (!result.toString().equals("")) result.append(", ");

            Integer role=(Integer)roles.get(val);
            if (role!=null) {
                val=mmb.getRelDef().getBuilderName(getNode(role.intValue()));
                result.append(mmb.baseName+"_"+val);
            } else {
                result.append(mmb.baseName+"_"+getTableName(val));
            }

            result.append(" "+idx2char(idx));
            idx++;
        }
        return result.toString();
    }

    // get a reference to the number field in a table
    private String numberOf(String table) {
        return table + "." + mmb.getDatabase().getNumberString();
    };

    /**
     * Creates a condition string which checks the relations between nodes.
     * The string can then be added to the query's where clause.
     * @param alltables the tablenames to use
     * @return a condition as a <code>String</code>
     */
    protected String getRelationString(Vector alltables) {
        return getRelationString(alltables, SEARCH_EITHER, new HashMap());
    }

    /**
     * Creates a condition string which checks the relations between nodes.
     * The string can then be added to the query's where clause.
     * The method returns null if no validf condition can be made (i.e. the result would ALWAYS be an empty resultset)
     * @param alltables the tablenames to use
     * @param searchdir the directionality option to use
     * @return a condition as a <code>String</code>, or null id no valid condiiton can be made
     */
    protected String getRelationString(Vector alltables, int searchdir, HashMap roles) {
        StringBuffer result = new StringBuffer(40); // 40: reasonable size for the result
        TypeDef typedef = mmb.getTypeDef();
        TypeRel typerel = mmb.getTypeRel();
        InsRel insrel   = mmb.getInsRel();
        RelDef reldef   = mmb.getRelDef();
        int siz = alltables.size() - 2;

        // get basic object builder
        // if it exists, you can use 'object' in nodepaths
        int rootnr = mmb.getRootType();

        if (log.isDebugEnabled()) log.debug("SEARCHDIR=" + searchdir);


        for (int i = 0; i < siz; i += 2) {

            String relChar    = idx2char(i + 1);
            if (result.length() > 0) result.append(" AND ");

            boolean desttosrc = false; // Wether the relation must be followed from 'source' to 'destination' (first and second given node-typ)e
            boolean srctodest = false; // And from 'destination' to 'source'.
            { // determine desttosrc and srctodest

                // the typedef number of the source-type
                int s        = typedef.getIntValue(getTableName((String) alltables.elementAt(i)));
                // role ?
                Integer rnum = (Integer) roles.get((String) alltables.elementAt(i + 1));
                // the typdef number of the destination-type
                int d = typedef.getIntValue(getTableName((String) alltables.elementAt(i + 2)));

                // check if  a definite rnumber was requested...
                if (rnum != null) {
                    result.append(relChar + ".rnumber=" + rnum.intValue() + " AND ");
                    srctodest = (searchdir != SEARCH_SOURCE)      && typerel.reldefCorrect(s, d, rnum.intValue());
                    desttosrc = (searchdir != SEARCH_DESTINATION) && typerel.reldefCorrect(d, s, rnum.intValue());
                } else {
                    for (Enumeration e = typerel.getAllowedRelations(s, d); e.hasMoreElements(); ) {
                        // get the allowed relation definitions
                        MMObjectNode typenode = (MMObjectNode) e.nextElement();
                        desttosrc = (searchdir != SEARCH_DESTINATION) &&
                                    (desttosrc ||
                                        (d == rootnr) || // ignore root 'object' type
                                         typenode.getIntValue("snumber") == d
                                     );
                        srctodest = (searchdir != SEARCH_SOURCE) &&
                                    (srctodest ||
                                         (s == rootnr) || // ignore root 'object' type
                                          typenode.getIntValue("snumber") == s
                                    );
                        if (desttosrc && srctodest) break;
                    }
                }
            }

            if (desttosrc && srctodest && (searchdir == SEARCH_EITHER)) { // support old
                desttosrc = false;
            }

            if (desttosrc) {
                // check for directionality if supported
                String dirstring;
                if (InsRel.usesdir && (searchdir != SEARCH_ALL)) {
                    dirstring = " AND " + relChar + ".dir <> 1";
                } else {
                    dirstring = "";
                }
                // there is a typed relation from destination to src
                if (srctodest) {
                    String sourceNumber = numberOf(idx2char(i));
                    String destNumber   = numberOf(idx2char(i + 2));
                    // there is ALSO a typed relation from src to destination - make a more complex query
                    result.append(
                           "(("+ sourceNumber     + "=" + relChar + ".snumber AND "+
                                 destNumber       + "=" + relChar + ".dnumber ) OR ("+
                                 sourceNumber     + "=" + relChar + ".dnumber AND "+
                                 destNumber       + "=" + relChar + ".snumber" + dirstring + "))");
                } else {
                    // there is ONLY a typed relation from destination to src - optimized query
                    result.append(numberOf(idx2char(i))     + "=" + relChar + ".dnumber AND "+
                                  numberOf(idx2char(i + 2)) + "=" + relChar + ".snumber" + dirstring);
                }
            } else {
                if (srctodest) {
                    // there is no typed relation from destination to src (assume a relation between src and destination)  - optimized query
                    result.append(numberOf(idx2char(i))     + "=" + relChar + ".snumber AND "+
                              numberOf(idx2char(i + 2)) + "=" + relChar + ".dnumber");
                } else {
                    // no results possible
                    // terminate, return null!
                    log.warn("There are no relations possible (no typerel specified) between "+
                               getTableName((String) alltables.elementAt(i)) + " and "+
                               getTableName((String) alltables.elementAt(i + 2)) + " using "+
                               alltables.elementAt(i + 1)+ " in "+
                               getSearchDirString(searchdir) + " direction(s)"
                               );
                    return null;
                }
            }

        }
        return result.toString();
    }

    /**
     * Converts an index to a one-character string.
     * I.e. o becomes 'a', 1 becomes 'b', etc.
     * This is used to map the tables in a List to alternate names (using their index in the list).
     * @param idx the index
     * @return the one-letter name as a <code>String</code>
     * XXX: So, why does this not return simply a 'char'?
     */
    protected String idx2char(int idx) {
        return ""+new Character((char)('a'+idx));
    }

    private String getNodeString(String bstr,Vector snodes) {
        String snode,str;
        StringBuffer bb=new StringBuffer();
        snode=(String)snodes.elementAt(0);
        if (snodes.size()>1) {
            bb.append(bstr+"."+mmb.getDatabase().getNumberString()+" in ("+snode);
            for (int i=1;i<snodes.size();i++) {
                str=(String)snodes.elementAt(i);
                bb.append(","+str);
            }
            bb.append(")");
        } else {
            bb.append(bstr+"."+mmb.getDatabase().getNumberString()+"="+snode);
        }
        return bb.toString();
    }

    /**
     * Get text from a blob field.
     * The text is cut if it is to long.
     * @param fieldname name of the field
     * @param number number of the object in the table
     * @return a <code>String</code> containing the contents of a field as text
     */
    public String getShortedText(String fieldname,int number) {
        String buildername=getBuilderNameFromField(fieldname);
        if (buildername.length()>0) {
            MMObjectBuilder bul=mmb.getMMObject(buildername);
            return bul.getShortedText(getFieldNameFromField(fieldname),number);
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
    public byte[] getShortedByte(String fieldname,int number) {
        String buildername=getBuilderNameFromField(fieldname);
        if (buildername.length()>0) {
            MMObjectBuilder bul=mmb.getMMObject(buildername);
            return bul.getShortedByte(getFieldNameFromField(fieldname),number);
        }
        return null;
    }

    /**
     * Creates search query that selects all the objects that match the
     * searchkeys.
     *
     * @param snodes The numbers of the nodes to start the search with.
     *        These have to be present in the first table listed in the
     *        tables parameter.
     * @param fields The fieldnames to return.
     *        These should be formatted as <em>stepalias.field</em>.
     *        Examples: 'people.lastname'
     * @param pdistinct 'YES' indicates the records returned need to be
     *        distinct.
     *        Any other value indicates double values can be returned.
     * @param tables The builder chain.
     *        A list containing builder names.
     *        The search is formed by following the relations between
     *        successive builders in the list.
     *        It is possible to explicitly supply a relation builder by
     *        placing the name of the builder between two builders to search.
     *        Example: company,people or typedef,authrel,people.
     * @param where The contraint.
     *        This is in essence a SQL where clause, using the NodeManager
     *        names from the nodes as tablenames.
     *        The syntax is either sql (if preceded by "WHERE') or
     *        Examples: "WHERE people.email IS NOT NULL",
     *        "(authrel.creat=1) and (people.lastname='admin')"
     * @param sortFields the fieldnames on which you want to sort.
     * @param directions A list of values containing, for each field in the
     *        order parameter, a value indicating whether the sort is
     *        ascending (<code>UP</code>) or descending (<code>DOWN</code>).
     *        If less values are supplied then there are fields in order,
     *        the first value in the list is used for the remaining fields.
     *        Default value is <code>'UP'</code>.
     * @param searchDir Specifies in which direction relations are to be
     *        followed, this must be one of the values defined by this class.
     * @return the resulting search query.
     * @since MMBase-1.7
     */
    // TODO RvM: update javadoc
    public BasicSearchQuery getMultiLevelSearchQuery (
            List snodes, List fields, String pdistinct,
            List tables, String where, List sortFields, List directions,
            int searchdir) {
        String stables,relstring,select,order,basenodestring;
        Vector alltables,selectTypes;

        // Create the query.
        BasicSearchQuery query = new BasicSearchQuery();

        // Set the distinct property.
        boolean distinct =
            pdistinct != null && pdistinct.equalsIgnoreCase("YES");
        query.setDistinct(distinct);

        // Get ALL tables (including missing reltables)
        Map roles= new HashMap();
        Map fieldsByAlias = new HashMap();
        Map stepsByAlias
            = addSteps(query, tables, roles, !distinct, fieldsByAlias);

        // Add fields.
        Iterator iFields = fields.iterator();
        while (iFields.hasNext()) {
            addFields(
                query, (String) iFields.next(), stepsByAlias, fieldsByAlias);
        }

        // Add sortorders.
        addSortOrders(query, sortFields, directions, fieldsByAlias);

        // Supporting more then 1 source node or no source node at all
        // Note that node number -1 is seen as no source node
        if (snodes != null && snodes.size() > 0) {
            Integer nodeNumber = new Integer(-1);

            // Copy list, so the original list is not affected.
            snodes = new ArrayList(snodes);

            // Go trough the whole list of strings (each representing
            // either a nodenumber or an alias), convert all to Integer objects.
            // from last to first,,... since we want snode to be the one that
            // contains the first..
           for (int i=snodes.size() - 1 ; i >= 0 ; i--) {
                String str = (String)snodes.get(i);
                try {
                    nodeNumber= new Integer(str);
                } catch(NumberFormatException e) {
                    // maybe it was not an integer, hmm lets look in OAlias
                    // table then
                    nodeNumber = new Integer(mmb.OAlias.getNumber(str));
                    if (nodeNumber.intValue() < 0) {
                        nodeNumber = new Integer(0);
                    }
                }
                snodes.set(i, nodeNumber);
            }

            BasicStep nodesStep
                = getNodesStep(query.getSteps(), nodeNumber.intValue());
            if (nodesStep != null) {
                Iterator iNodeNumbers = snodes.iterator();
                while (iNodeNumbers.hasNext()) {
                    Integer number = (Integer) iNodeNumbers.next();
                    nodesStep.addNode(number.intValue());
                }
            }
        }

        addRelationDirections(query, searchdir, roles);

        // Add constraint for the where part.
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
    Map addSteps(BasicSearchQuery query, List tables,
            Map roles, boolean includeAllReference, Map fieldsByAlias) {

        Map stepsByAlias = new HashMap(); // Maps original table names to steps.
        Set tableAliases = new HashSet(); // All table aliases that are in use.

        Iterator iTables = tables.iterator();
        if (iTables.hasNext()) {
            // First table.
            String tableName = (String) iTables.next();
            MMObjectBuilder bul = getBuilder(tableName, roles);
            String tableAlias = getUniqueTableAlias(tableName, tableAliases, tables);
            BasicStep step = query.addStep(bul);
            step.setAlias(tableAlias);
            stepsByAlias.put(tableName, step);
            if (includeAllReference) {
                 // Add number field.
                addField(query, step, "number", fieldsByAlias);
            }
        }
        while (iTables.hasNext()) {
            String tableName;
            InsRel bul;
            String tableName2 = (String) iTables.next();
            MMObjectBuilder bul2 = getBuilder(tableName2, roles);
            BasicRelationStep relation;
            BasicStep step2;
            if (bul2 instanceof InsRel) {
                // Explicit relation step.
                tableName = tableName2;
                bul = (InsRel) bul2;
                tableName2 = (String) iTables.next();
                bul2 = getBuilder(tableName2, roles);
                relation = query.addRelationStep(bul, bul2);
                step2 = (BasicStep) relation.getNext();
                if (includeAllReference) {
                    // Add number fields.
                    relation.setAlias(tableName);
                    addField(query, relation, "number", fieldsByAlias);
                    step2.setAlias(tableName2);
                    addField(query, step2, "number", fieldsByAlias);
                 }
            } else {
                // Not a relation, relation step is implicit.
                tableName = "insrel";
                bul = mmb.getInsRel();
                relation = query.addRelationStep(bul, bul2);
                step2 = (BasicStep) relation.getNext();
                if (includeAllReference) {
                    // Add number field.
                    step2.setAlias(tableName2);
                    addField(query, step2, "number", fieldsByAlias);
                }
            }
            String tableAlias = getUniqueTableAlias(tableName, tableAliases, tables);
            String tableAlias2 = getUniqueTableAlias(tableName2, tableAliases, tables);
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
    MMObjectBuilder getBuilder(String tableAlias, Map roles) {
        String tableName = getTableName(tableAlias);
        // check builder - should throw exception if builder doesn't exist ?
        MMObjectBuilder bul = null;
        try {
            bul = mmb.getBuilder(tableName);
        } catch (BuilderConfigurationException e) {}
        if (bul == null) {
            // check if it is a role name. if so, use the builder of the
            // rolename and store a filter on rnumber.
            int rnumber = mmb.getRelDef().getNumberByName(tableName);
            if (rnumber == -1) {
                String msg = "Specified builder " + tableName + " does not exist.";
                log.error(msg);
                throw new IllegalArgumentException(msg);
            } else {
                bul = mmb.getRelDef().getBuilder(rnumber); // relation builder
                roles.put(tableAlias, new Integer(rnumber));
            }
        } else if (bul instanceof InsRel) {
            int rnumber = mmb.getRelDef().getNumberByName(tableName);
            if (rnumber!=-1) {
                roles.put(tableAlias, new Integer(rnumber));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Resolved table alias \"" + tableAlias
                + "\" to builder \"" + bul.getTableName() + "\"");
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
    String getUniqueTableAlias(String tableAlias, Set tableAliases,
            Collection originalAliases) {

        // If provided alias is not unique, try alternatives,
        // skipping alternatives that are already in originalAliases.
        if (tableAliases.contains(tableAlias)) {
            tableName = getTableName(tableAlias);

            tableAlias = tableName;
            char ch = '0';
            while (originalAliases.contains(tableAlias)
                    || tableAliases.contains(tableAlias)) {
                // Can't create more than 11 aliases for same tablename.
                if (ch > '9') {
                    throw new IndexOutOfBoundsException(
                        "Failed to create unique table alias, because there "
                        + "are already 11 aliases for this tablename: \""
                        + tableName + "\"");
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
    void addFields(BasicSearchQuery query,
            String expression, Map stepsByAlias, Map fieldsByAlias) {

        // TODO RvM: stripping functions is this (still) necessary?.
        // Strip function(s).
        int pos1 = expression.indexOf('(');
        int pos2 = expression.indexOf(')');
        if (pos1 != -1 ^ pos2 != -1) {
            // Parenthesis do not match.
            throw new IllegalArgumentException(
                "Parenthesis do not match in expression: \""
                    + expression + "\"");
        } else if (pos1 != -1) {
            // Function parameter list containing subexpression(s).
            String parameters = expression.substring(pos1 + 1, pos2);
            Iterator iParameters = getFunctionParameters(parameters).iterator();
            while (iParameters.hasNext()) {
                String parameter = (String) iParameters.next();
                addFields(
                    query, parameter, stepsByAlias, fieldsByAlias);
            }
        } else if (!Character.isDigit(expression.charAt(0))) {
            int pos = expression.indexOf('.');
            if (pos < 1 || pos == (expression.length() - 1)) {
                throw new IllegalArgumentException(
                    "Invalid fieldname: \"" + expression + "\"");
            }
            String stepAlias = expression.substring(0, pos);
            String fieldName = expression.substring(pos + 1);

            BasicStep step = (BasicStep) stepsByAlias.get(stepAlias);
            if (step == null) {
                throw new IllegalArgumentException(
                    "Invalid step alias: \"" + stepAlias + "\"");
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
    private void addField(BasicSearchQuery query, BasicStep step,
            String fieldName, Map fieldsByAlias) {

        // Fieldalias = stepalias.fieldname.
        // This value is used to store the field in fieldsByAlias.
        // The actual alias of the field is not set.
        String fieldAlias = step.getAlias() + "." + fieldName;
        if (fieldsByAlias.containsKey(fieldAlias)) {
            // Added already.
            return;
        }

        MMObjectBuilder builder = mmb.getBuilder(step.getTableName());
        FieldDefs fieldDefs = builder.getField(fieldName);
        if (fieldDefs == null) {
            throw new IllegalArgumentException(
                "Not a known field of builder " + step.getTableName()
                    + ": \"" + fieldName + "\"");
        }

        // Add the stepfield.
        BasicStepField stepField
            = query.addField(step, fieldDefs);
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
    void addSortOrders(BasicSearchQuery query, List fieldNames,
            List directions, Map fieldsByAlias) {

        // Test if fieldnames are specified.
        if (fieldNames == null || fieldNames.size() == 0) {
            return;
        }

        int defaultSortOrder = SortOrder.ORDER_ASCENDING;
        if (directions != null && directions.size() != 0) {
            if (((String) directions.get(0)).trim().equalsIgnoreCase("DOWN")) {
                defaultSortOrder = SortOrder.ORDER_DESCENDING;
            }
        }

        Iterator iFieldNames = fieldNames.iterator();
        Iterator iDirections = directions.iterator();
        while (iFieldNames.hasNext()) {
            String fieldName = (String) iFieldNames.next();
            StepField field = (BasicStepField) fieldsByAlias.get(fieldName);
            if (field == null) {
                // Field has not been added.
                field = ConstraintParser.getField(fieldName, query.getSteps());
            }
            if (field == null) {
                throw new IllegalArgumentException(
                "Invalid fieldname: \"" + fieldName + "\"");
            }

            // Add sort order.
            BasicSortOrder sortOrder = query.addSortOrder(field); // ascending

            // Change direction if needed.
            if (iDirections.hasNext()) {
                String direction = (String) iDirections.next();
                if (direction.trim().equalsIgnoreCase("DOWN")) {
                    sortOrder.setDirection(SortOrder.ORDER_DESCENDING);
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
    BasicStep getNodesStep(List steps, int nodeNumber) {
        if (nodeNumber < 0) {
            return null;
        }

        MMObjectNode node = getNode(nodeNumber);
        if (node == null) {
            return null;
        }

        MMObjectBuilder builder = node.parent;
        BasicStep result = null;
        do {
            // Find step corresponding to builder.
            Iterator iSteps = steps.iterator();
            while (iSteps.hasNext() && result == null) {
                BasicStep step = (BasicStep) iSteps.next();
                if (step.getTableName().equals(builder.tableName)) {
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
     * @param searchDir Specifies in which direction relations are to be
     *      followed, this must be one of the values defined by this class.
     * @param roles Map of tablenames mapped to <code>Integer</code> values,
     *        representing the nodenumber of the corresponing RelDef node.
     * @since MMBase-1.7
     */
    // package visibility!
    void addRelationDirections(BasicSearchQuery query, int searchdir, Map roles) {

        Iterator iSteps = query.getSteps().iterator();
        BasicStep sourceStep = (BasicStep) iSteps.next();
        BasicStep destinationStep = null;
        while (iSteps.hasNext()) {
            if (destinationStep != null) {
                sourceStep = destinationStep;
            }
            BasicRelationStep relationStep = (BasicRelationStep) iSteps.next();
            destinationStep = (BasicStep) iSteps.next();

            // Check directionality is requested and supported.
            if (searchdir != SEARCH_ALL && InsRel.usesdir) {
                relationStep.setCheckedDirectionality(true);
            }

            // Determine in what direction(s) this relation can be followed:
            boolean desttosrc = false; // From 'source' to 'destination'.
            boolean srctodest = false; // From 'destination' to 'source'.

            // Determine typedef number of the source-type.
            int srcType = mmb.getTypeDef().getIntValue(
                getTableName(sourceStep.getAlias()));
            // Determine reldef number of the role.
            Integer role = (Integer) roles.get(relationStep.getAlias());
            // Determine the typdef number of the destination-type.
            int destType = mmb.getTypeDef().getIntValue(
                getTableName(destinationStep.getAlias()));

            // check if  a definite rnumber was requested...
            if (role != null) {
                relationStep.setRole(role);
                srctodest = searchdir != SEARCH_SOURCE
                    && mmb.getTypeRel().reldefCorrect(srcType, destType, role.intValue());
                desttosrc = searchdir != SEARCH_DESTINATION
                    && mmb.getTypeRel().reldefCorrect(destType, srcType, role.intValue());
            } else {
                Enumeration e = mmb.getTypeRel().getAllowedRelations(srcType, destType);
                while (e.hasMoreElements()) {
                    // get the allowed relation definitions
                    MMObjectNode typenode = (MMObjectNode) e.nextElement();
                    desttosrc = (searchdir != SEARCH_DESTINATION) &&
                        (desttosrc || destType ==  mmb.getRootType() || // ignore root 'object' type
                            typenode.getIntValue("snumber") == destType);

                    srctodest = (searchdir != SEARCH_SOURCE) &&
                        (srctodest || srcType ==  mmb.getRootType() || // ignore root 'object' type
                            typenode.getIntValue("snumber") == srcType);
                    if (desttosrc && srctodest) break;
                }
            }

            if (desttosrc && srctodest && (searchdir == SEARCH_EITHER)) { // support old
                desttosrc = false;
            }

            if (desttosrc) {
                // there is a typed relation from destination to src
                if (srctodest) {
                    // there is ALSO a typed relation from src to destination - make a more complex query
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_BOTH);
                } else {
                    // there is ONLY a typed relation from destination to src - optimized query
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
                }
            } else {
                if (srctodest) {
                    // there is no typed relation from destination to src (assume a relation between src and destination)  - optimized query
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_DESTINATION);
                } else {
                    // no results possible...
                    relationStep.setDirectionality(RelationStep.DIRECTIONS_DESTINATION);
                    log.warn("No relation defined between "
                        + sourceStep.getAlias() + " and "
                        + destinationStep.getAlias() + " using "
                        + relationStep + " with direction(s) "
                        + getSearchDirString(searchdir));
                }
            }
        }
    }

}
