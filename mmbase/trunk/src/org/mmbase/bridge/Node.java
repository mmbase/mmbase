/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;
import org.mmbase.module.core.*;
import java.util.List;

/**
 * Describes an object in the cloud.
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 */
public interface Node {

  	/**
     * Returns the cloud this node belongs to.
     */
    public Cloud getCloud();

	/**
     * Returns the node manager for this node.
     */
    public NodeManager getNodeManager();
	
	/**
     * Returns the unique number for this node. Every node has a unique number
     * wich can be used to refer to it. In addition to this number a node can
     * have one or more aliases.
     *
     * @return the unique number for this node
     * @see    #addAlias(String alias)
     */
    public int getNumber();
	
	/** 
     * Sets the value of the specified field using an object.
     * For example a field of type <code>int</code> can be set using an
     * <code>Integer</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setValue(String fieldname, Object value); 

	/** 
     * Sets the value of the specified field using an <code>int</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setIntValue(String fieldname, int value); 

	/** 
     * Sets the value of the specified field using a <code>float</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setFloatValue(String fieldname, float value); 

	/** 
     * Sets the value of the specified field using a <code>double</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setDoubleValue(String fieldname, double value); 

	/** 
     * Sets the value of the specified field using a <code>byte array</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setByteValue(String fieldname, byte[] value); 

	/** 
     * Sets the value of the specified field using a <code>long</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
	 * @param fieldname  the name of the field to be updated
	 * @param value      the new value for the given field
	 */
	public void setLongValue(String fieldname, long value); 

    /** 
     * Sets the value of the specified field using a <code>String</code>.
     * This change will not be visible to the cloud until the commit method is
     * called.
     *
     * @param fieldname  the name of the field to be updated
     * @param value      the new value for the given field
     */
	public void setStringValue(String fieldname, String value); 

    /** 
     * Returns the value of the specified field as an object. For example a
     * field of type <code>int</code> is returned as an <code>Integer</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public Object getValue(String fieldname);

    /** 
     * Returns the value of the specified field as an <code>int</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public int getIntValue(String fieldname);

    /** 
     * Returns the value of the specified field as a <code>float</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public float getFloatValue(String fieldname);

    /** 
     * Returns the value of the specified field as a <code>long</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public long getLongValue(String fieldname);

    /** 
     * Returns the value of the specified field as a <code>double</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public double getDoubleValue(String fieldname);

    /** 
     * Returns the value of the specified field as a <code>byte array</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public byte[] getByteValue(String fieldname);

    /** 
     * Returns the value of the specified field as a <code>String</code>.
     *
     * @param fieldname  the name of the field to be returned
     * @return           the value of the specified field
     */
	public String getStringValue(String fieldname);

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
	 * Removes the Node
	 */
	public void remove(); 

	/**
	 * Converts the node to a string
	 */
	 public String toString();

	/**
	 * Checks whether this node has any relations.
     *
	 * @return <code>true</code> if the node has relations
	 */
	public boolean hasRelations();
		
	/**
	 * Removes all relation nodes attached to this node.
	 */
	public void removeRelations();

	/**
 	 * Removes all relation nodes with a certain relation manager that are
     * attached to this node.
     *
	 * @param relationManager  the name of the relation manager the removed
     *                         relation nodes should have
	 */
	public void removeRelations(String relationManager);

	/**
     * Returns all relation nodes attached to this node.
     *
	 * @return a list of relation nodes
	 */
	public RelationList getRelations();

	/**
     * Returns all relation nodes attached to this node that have a specific
     * relation manager.
     *
	 * @param relationManager  the name of the relation manager the returned
     *                         relation nodes should have
	 * @return                 a list of relation nodes
	 */
	public RelationList getRelations(String relationManager);
	
	/**
	 * Returns the number of relations this node has with other nodes.
     *
	 * @return the number of relations this node has with other nodes
	 */
	public int countRelations();

	/**
     * Returns the number of relation nodes attached to this node that have a
     * specific relation manager.
     *
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
	public int countRelatedNodes(String type);
	
	/**
     * Returns all aliases for this node.
     *
     * @return a list of alias names for this node
     */
    public List getAliases();

	/**
     * Adds an alias for this node. An alias can be used to refer to a node in
     * addition to his number.
     *
     * @param alias             the alias to be created for this node
     * @throws BridgeException  if the alias allready exists
     */
    public void addAlias(String alias);

    /**
     * Remove an alias for this node.
     *
     * @param alias  the alias to be removed for this node
     */
    public void removeAlias(String alias);

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
}
