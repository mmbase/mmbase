/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;
import java.util.*;
import java.io.InputStream;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.mmbase.util.functions.Function;
import org.mmbase.util.functions.Parameters;

/**
 * Describes an object in the cloud.
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @version $Id: Node.java,v 1.54 2005-05-02 08:27:40 michiel Exp $
 */
public interface Node extends Comparable {

    /**
     * Returns the cloud this node belongs to.
     * @return the Cloud
     */
    public Cloud getCloud();

    /**
     * Returns the node manager for this node.
     * @return the node manager
     */
    public NodeManager getNodeManager();

    /**
     * Returns the unique number for this node. Every node has a unique number
     * which can be used to refer to it. In addition to this number a node can
     * have one or more aliases.
     * A value of -1 indicates an invalid number.
     * Other negative values may be used for temporary ids (but not true node numbers).
     * This may differ by implementation.
     *
     * @return the unique number for this node
     * @see    #createAlias(String alias)
     */
    public int getNumber();

    /**
     * Determine whether this Node is a Relation.
     * @since MMBase-1.6
     * @return <code>true</code> if this Node is a Relation.
     */
    public boolean isRelation();

    /**
     * Returns this as a Relation.
     * @since MMBase-1.6
     * @return a <code>Relation</code> object
     * @throws ClassCastException if the Node is not a Relation
     */
    public Relation toRelation();

    /**
     * Determine whether this Node is a NodeManager.
     * @since MMBase-1.6
     * @return <code>true</code> if this Node is a NodeManager.
     */
    public boolean isNodeManager();

    /**
     * Returns this as a NodeManager.
     * @since MMBase-1.6
     * @return a <code>NodeManager</code> object
     * @throws ClassCastException if the Node is not a NodeManager
     */
    public NodeManager toNodeManager();

    /**
     * Determine whether this Node is a RelationManager.
     * @since MMBase-1.6
     * @return <code>true</code> if this Node is a RelationManager.
     */
    public boolean isRelationManager();

    /**
     * Returns this as a RelationManager.
     * @since MMBase-1.6
     * @return a <code>NodeManager</code> object
     * @throws ClassCastException if the Node is not a RelationManager
     */
    public RelationManager toRelationManager();

    /**
     * Sets the value of the specified field using an object, but delegated to the right
     * set--Value depending on the type of the field.
     * For example a field of type <code>int</code> can be set using an
     * <code>Integer</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setValue(String fieldName, Object value);

    /**
     * Sets the value of the specified field using an object, but without dispatching to the right
     * type first.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     * @since MMBase-1.7
     */
    public void setObjectValue(String fieldName, Object value);

    /**
     * Sets the value of the specified field using an <code>boolean</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @since MMBase-1.6
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setBooleanValue(String fieldName, boolean value);

    /**
     * Sets the value of the specified field using an <code>Node</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @since MMBase-1.6
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setNodeValue(String fieldName, Node value);

    /**
     * Sets the value of the specified field using an <code>int</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setIntValue(String fieldName, int value);

    /**
     * Sets the value of the specified field using a <code>float</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setFloatValue(String fieldName, float value);

    /**
     * Sets the value of the specified field using a <code>double</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setDoubleValue(String fieldName, double value);

    /**
     * Sets the value of the specified field using a <code>byte array</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setByteValue(String fieldName, byte[] value);

    /**
     * Sets the value of the specified field using a <code>java.io.OutputStream</code>.
     */
    public void setInputStreamValue(String fieldName, InputStream value, long size);

    /**
     * Sets the value of the specified field using a <code>long</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setLongValue(String fieldName, long value);

    /**
     * Sets the value of the specified field using a <code>String</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     */
    public void setStringValue(String fieldName, String value);

    /**
     * Sets the value of the specified field using a <code>Date</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     * @since MMBase-1.8
     */
    public void setDateValue(String fieldName, Date value);


    /**
     * Sets the value of the specified field using a <code>List</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldName  the name of the field to be updated
     * @param value      the new value for the given field
     * // not yet working
     * @since MMBase-1.8
     */
    public void setListValue(String fieldName, List value);

    /**
     * Whether the value fo the speficied field is <code>null</code>. This avoids acquiring the
     * complete value if you only want to check if for emptyness.
     * @since MMBase-1.8
     */
    public boolean isNull(String fieldName);

    /**
     * Returns the value of the specified field as an object. For example a
     * field of type <code>int</code> is returned as an <code>Integer</code>.
     * The object type may vary and is dependent on how data was stored in a field.
     * I.e. It may be possible for an Integer field to return it's value as a String
     * if it was stored that way in the first place.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public Object getValue(String fieldName);

    /**
     * Returns the field's value as an object. It is not delegated to the right get--Value.
     * @param fieldName name of the field
     * @return object value
     * @since MMBase-1.7
     */
    public Object getObjectValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>boolean</code>.
     * If the actual value is numeric, this call returns <code>true</code>
     * if the value is a positive, non-zero, value. In other words, values '0'
     * and '-1' are considered <code>false</code>.
     * If the value is a string, this call returns <code>true</code> if
     * the value is "true" or "yes" (case-insensitive).
     * In all other cases (including calling byte fields), <code>false</code>
     * is returned.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public boolean getBooleanValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>Node</code>.
     * If the value is not itself a Node, this call attempts to convert the
     * original field value to a Node, by trying to retrieve a Node using
     * the field value as a Node number or alias.<br />
     * For instance, getNodeValue("destination"), when run on a OAlias object,
     * returns the referenced destination node (instead of the number, which is
     * what it normally holds).<br />
     * Mostly, this call is used in cluster nodes (nodes retrieved by using the
     * Cloud.getList method. A cluster node returns one of its compound nodes
     * when an appropriate nodemanager name (name from the nodepath) is specified.
     * I.e. getNodeValue("people") will return the people-node in the cluster.
     * If this fails, the method returns <code>null</code>.
     * <br />
     * Notes: the behavior of getNodeValue when called on a field that is not
     * intended to be a node reference is currently undefined and is not
     * encouraged.
     * <br />
     * Calling this method with field "number" or <code>null</code> lets the
     * Node return a reference to itself, regardless of the actual value of the
     * number field or status of the Node.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     * @see Cloud#getList(String, String, String, String, String, String, String, boolean)
     */
    public Node getNodeValue(String fieldName);

    /**
     * Returns the value of the specified field as an <code>int</code>.
     * Numeric fields are simply converted. Double and float values may be truncated.
     * For Node values, the numeric key is returned.
     * Long values return -1 of the value is too large.
     * Boolean fields return 0 if false, and 1 if true.
     * String fields are parsed.
     * If a parsed string contains an error, ot the field value is not of a type that can be converted
     * (i.e. a byte array), this function returns -1
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public int getIntValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>float</code>.
     * This function attempts to convert the value to a float.
     * Numeric fields are simply converted.
     * Boolean fields return 0.0 if false, and 1.0 if true.
     * String fields are parsed.
     * If a parsed string contains an error, ot the field value is not of a type that can be converted
     * (i.e. a byte array), this function returns -1.0.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public float getFloatValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>long</code>.
     * This function attempts to convert the value to a long.
     * Numeric fields are simply converted. Double and float values may be truncated.
     * Boolean fields return 0 if false, and 1 if true.
     * String fields are parsed.
     * If a parsed string contains an error, ot the field value is not of a type that can be converted
     * (i.e. a byte array), this function returns -1
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public long getLongValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>double</code>.
     * This function attempts to convert the value to a double.
     * Numeric fields are simply converted. Double may be truncated.
     * Boolean fields return 0.0 if false, and 1.0 if true.
     * String fields are parsed.
     * If a parsed string contains an error, ot the field value is not of a type that can be converted
     * (i.e. a byte array), this function returns -1.0.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public double getDoubleValue(String fieldName);

    /**
     * Returns the value of the specified field as a <code>byte array</code>.
     * This function returns either the value of a byte field, or the byte value of a string
     * (converted using the default encoding, i.e. UTF8)
     * Other types of values return an empty byte-array.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public byte[] getByteValue(String fieldName);
    

    /**
     * Returns the value of the specified field as a <code>java.io.InputStream</code> This is
     * especially usefull for large byte-array fields. By this you can avoid them to be completely
     * stored in memory.
     */        
    public InputStream getInputStreamValue(String fieldName);


    /**
     * Returns the value of the specified field as a <code>String</code>.
     * Byte arrays are converted to string using the default encoding (UTF8).
     * Node values return a string representation of their numeric key.
     * For other values the result is calling the toString() method on the actual object.
     *
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     */
    public String getStringValue(String fieldName);

    /**
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     * @since MMBase-1.8
     */
    public Date getDateValue(String fieldName);

    /**
     * @param fieldName  the name of the field to be returned
     * @return           the value of the specified field
     * not yet working
     * @since MMBase-1.8
     */
    public List getListValue(String fieldName);


    /**
     * Returns the value of the specified field as a <code>FieldValue</code> object.
     *
     * @since MMBase-1.6
     * @param fieldName  the name of the field whose value to return
     * @return           the value of the specified field
     * @throws NotFoundException is the field does not exist
     */
    public FieldValue getFieldValue(String fieldName) throws NotFoundException;

    /**
     * Returns the value of the specified field as a <code>FieldValue</code> object.
     *
     * @since MMBase-1.6
     * @param field  the Field object whose value to return
     * @return       the value of the specified field
     */
    public FieldValue getFieldValue(Field field);

    /**
    * Commit the node to the database.
    * Makes this node and/or the changes made to this node visible to the cloud.
    * If this method is called for the first time on this node it will make
    * this node visible to the cloud, otherwise the modifications made to
    * this node using the set methods will be made visible to the cloud.
    * This action fails if the current node is not in edit mode.
    * If the node is in a transaction, nothing happens - actual committing occurs through the transaction.
    */
    public void commit();

    /**
     * Cancel changes to a node
     * This fails if the current node is not in edit mode.
     * If the node is in a transaction, nothing happens - actual committing occurs through the transaction.
     */
    public void cancel();

    /**
     * Removes the Node. Throws an exception if still has relations. Like delete(false).
     */
    public void delete();

    /**
     * Whether this Node is new (not yet commited).
     * @since MMBase-1.8
     */    
    public boolean isNew();

    /**
     * Removes the Node.
     * @param deleteRelations a boolean. If true, then first all
     * existing relations with this node will be removed.
     */

    public void delete(boolean deleteRelations);

    /**
     * Converts the node to a string
     * @return string representation of a node
     */
    public String toString();

    /**
     * Returns the value of the specified field as a <code>dom.Document</code>
     * If the node value is not itself a Document, the method attempts to
     * attempts to convert the String value into an XML.
     * This included the empty string, but not the 'null' value.
     * If the value is null, this method returns <code>null</code>
     *
     * @param fieldName  the name of the field to be returned
     * @return the value of the specified field as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException if the value cannot be converted to xml.
     * @since MMBase-1.6
     */
    public Document getXMLValue(String fieldName) throws IllegalArgumentException;

    /**
     * Returns the value of the specified field as a <code>dom.Element</code>
     * If the node value is not itself a Document, the method attempts to
     * attempts to convert the String value into an XML.
     * This method fails (throws a IllegalArgumentException) if the Field is not of type TYPE_XML.
     * If the value cannot be converted, this method returns <code>null</code>
     *
     * @param fieldName  the name of the field to be returned
     * @param tree the DOM Document to which the Element is added
     *             (as the document root element)
     * @return  the value of the specified field as a DOM Element or <code>null</code>
     * @throws  IllegalArgumentException  if the value cannot be converted to xml.
     * @since MMBase-1.6
     */

    public Element getXMLValue(String fieldName, Document tree) throws IllegalArgumentException;

    /**
     * Set's the value of the specified field as a <code>dom.Element</code>
     *
     * @param fieldName  the name of the field to be returned
     * @param value      the DOM Document to has to be set, if not correct doc-type,
     *                   system will try to convert it to the wanted type.
     * @since MMBase-1.6
     */
    public void setXMLValue(String fieldName, Document value);

    /**
     * Checks whether this node has any relations.
     *
     * @return <code>true</code> if the node has relations
     */
    public boolean hasRelations();

    /**
     * Removes all relation nodes attached to this node.
     */
    public void deleteRelations();

    /**
      * Removes all relation nodes with a certain relation manager that are
     * attached to this node.
     *
     * @param relationManager  the name of the relation manager the removed
     *                         relation nodes should have
     */
    public void deleteRelations(String relationManager);

    /**
     * Returns all relation nodes attached to this node.
     *
     * @return a list of relation nodes
     */
    public RelationList getRelations();

    /**
     * Returns all relation nodes attached to this node that have a specific
     * role
     *
     * @param role  the name of the role the returned
     *              relation nodes should have
     * @return      a list of relation nodes
     */
    public RelationList getRelations(String role);

    /**
     * Returns all relation nodes attached to this node that have a specific
     * role, or refer a node from a specific nodemanager
     *
     * @param role  the name of the role the returned
     *              relation nodes should have
     * @param nodeManager  the name of the nodemanager for the nodes the returned
     *                     relation nodes should have a relation to
     * @return      a list of relation nodes
     */
    public RelationList getRelations(String role, String nodeManager);

    /**
     * Returns all relation nodes attached to this node that have a specific
     * role, or refer a node from a specific nodemanager
     *
     * @param role  the name of the role the returned
     *              relation nodes should have
     * @param nodeManager  the nodemanager for the nodes the returned
     *                     relation nodes should have a relation to (can be null)
     * @return      a list of relation nodes
     */
    public RelationList getRelations(String role, NodeManager nodeManager);


    /**
     * @param role forward role of a relation
     * @param nodeManager node manager on the other side of the relation
     * @param searchDir the direction of the relation
     * @return List of relations
     * @since MMBase-1.7
     */
    public RelationList getRelations(String role, NodeManager nodeManager, String searchDir);

    /**
     * Returns the number of relations this node has with other nodes.
     *
     * @return the number of relations this node has with other nodes
     */
    public int countRelations();

    /**
     * Returns the number of relation nodes attached to this node that have a
     * specific relation manager.
     * @param relationManager relation manager
     * @return the number of relation nodes attached to this node that have a
     *         specific relation manager
     */
    public int countRelations(String relationManager);


    /**
     * Returns all related nodes.
     * The returned nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @return a list of all related nodes
     */
    public NodeList getRelatedNodes();

    /**
     * Returns all related nodes that have a specific node manager.
     * The returned nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @param nodeManager  the name of the node manager the returned nodes
     *                     should have
     * @return             a list of related nodes
     */
    public NodeList getRelatedNodes(String nodeManager);

    /**
     * Returns all related nodes that have a specific node manager.
     * The returned nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @param nodeManager  the node manager the returned nodes should have, can be null
     * @return             a list of related nodes
     */
    public NodeList getRelatedNodes(NodeManager nodeManager);

    /**
     * Returns all related nodes that have a specific node manager and role.
     * The returned nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @since MMBase-1.6
     * @param nodeManager  the name of the node manager the returned nodes
     *                     should have
     * @param role         the role of the relation
     * @param searchDir    the direction of the relation
     * @return             a list of related nodes
     */
    public NodeList getRelatedNodes(String nodeManager, String role, String searchDir);

    /**
     * Returns all related nodes that have a specific node manager and role.
     * The returned nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @since MMBase-1.6
     * @param nodeManager  the node manager the returned nodes should have
     * @param role         the role of the relation
     * @param searchDir    the direction of the relation
     * @return             a list of related nodes
     */
    public NodeList getRelatedNodes(NodeManager nodeManager, String role, String searchDir);



    /**
     * Returns a query to reretrieve this node. It is not very usefull 'as is' because you already
     * have the node. The result can however be changed (with addRelationsStep), to find 'related nodes'.
     *
     * @since MMBase-1.7.
     * @see   NodeManager#getList
     * @see   Query#addRelationStep
     */
    //public Query    createQuery();

    /**
     * Returns the number of related nodes that have a specific node manager.
     * The counted nodes are not the nodes directly attached to this node (the
     * relation nodes) but the nodes attached to the relation nodes of this
     * node.
     *
     * @param nodeManager  the name of the node manager the counted nodes
     *                     should have
     * @return             the number of related nodes that have a specific node
     *                     manager
     */
    public int countRelatedNodes(String nodeManager);


    /**
     * @param otherNodeManager the node manager the nodes should have
     * @param role the role of the relation
     * @param searchDir the direction of the relation
     * @return number of related nodes
     * @since MMBase-1.7
     */
    public int countRelatedNodes(NodeManager otherNodeManager, String role, String searchDir);



    /**
     * Returns all aliases for this node.
     *
     * @return a list of alias names for this node
     */
    public StringList getAliases();

    /**
     * Create an alias for this node. An alias can be used to refer to a node in
     * addition to his number.
     *
     * @param alias             the alias to be created for this node
     * @throws BridgeException  if the alias allready exists
     */
    public void createAlias(String alias);

    /**
     * Delete an alias for this node.
     *
     * @param alias  the alias to be removed for this node
     */
    public void deleteAlias(String alias);

    /**
     * Adds a relation between this node and a specified node to the cloud.
     *
     * @param destinationNode   the node to which you want to relate this node
     * @param relationManager   the relation manager you want to use
     * @return                  the added relation
     * @throws BridgeException  if the relation manager is not the right one
     *                          for this type of relation
     */
    public Relation createRelation(Node destinationNode,
                                   RelationManager relationManager);

    /**
     * Sets the security context of this Node (AKA the 'owner' field)
     *
     * @param context	    	   The security context to which this node should belong,
     * @throws SecurityException   When appropriate rights to perform this are lacking (write / change context rights)
     */
    public void setContext(String context);

    /**
     * Get the security context of the current Node
     *
     * @return the current context of the node (as a String)
     * @throws SecurityException   When appropriate rights to perform this are lacking (read rights)
     */
    public String getContext();

    /**
     * Contacts the security implementation to find out to which other possible contexts the
     * context of this node may be set.
     *
     * @return A StringList containing the contexts which can be used in setContext on this node.
     * @throws SecurityException   When appropriate rights to perform this are lacking (read rights)
     */
    public StringList getPossibleContexts();

    /**
     * Check write rights on this node.
     *
     * @return                      Whether the node may be changed by the current user
     */

    public boolean mayWrite();

    /**
     * Check delete rights on this node.
     *
     * @return                      Whether the node may be deleted by the current user
     */

    public boolean mayDelete();


    /**
     * Check context-change rights on this node.
     *
     * @return                      Whether the current user may change the context of this node
     */
    public boolean mayChangeContext();

    /**
     * Returns all the Function objects of this Module.
     *
     * @since MMBase-1.8
     * @return a Set of {@link org.mmbase.util.functions.Function} objects.
     */
    public Set getFunctions();

    /**
     * Returns a Fuction object.
     * The object returned is a {@link org.mmbase.util.functions.Function} object.
     * You need to explixitly cast the result to this object, since not all bridge
     * implementations (i.e. the RMMCI) support this class.
     *
     * @since MMBase-1.8
     * @param functionName name of the function
     * @return a {@link org.mmbase.util.functions.Function} object.
     * @throws NotFoundException if the function does not exist
     */
    public Function getFunction(String functionName);

    /**
     * Creates a parameter list for a function.
     * The list can be filled with parameter values by either using the List set(int, Object) method, to
     * set values for parameters by psoition, or by using the set(String, Object) method to
     * set parameters by name.<br />
     * This object can then be passed to the getFunctionValue method.
     * Note that adding extra parameters (with the add(Object) method) won't work and may cause exceptions.<br />
     * @since MMBase-1.8
     * @param functionName name of the function
     * @return a {@link org.mmbase.util.functions.Parameters} object.
     * @throws NotFoundException if the function does not exist
     */
    public Parameters createParameters(String functionName);

    /**
     * Returns the value of the specified function on the node.  A
     * function normally has arguments, which can be supplied with a
     * List.
     * @since MMBase-1.6
     * @param functionName name of the function
     * @param parameters list with parameters for the fucntion
     * @return the result value of executing the function
     * @throws NotFoundException if the function does not exist
     */
    public FieldValue getFunctionValue(String functionName, List parameters);

}
