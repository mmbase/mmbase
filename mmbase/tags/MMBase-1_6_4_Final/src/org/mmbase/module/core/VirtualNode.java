/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;

import org.mmbase.util.logging.*;

/**
 * VirtualNode is a representation of a virtual objectnode.
 * Virtual Object nodes are nodes that are not stored in a databasetable.
 * Note that a temporary node is not virtual.
 * This class captures a number of methods that would normally require datbase
 * access, such as obtaining relations or determining age of a node.
 *
 * @author Pierre van Rooden
 * @version 26 Apr 2001
 */
public class VirtualNode extends MMObjectNode {
    /**
     * Logger routine
     */
    private static Logger log = Logging.getLoggerInstance(VirtualNode.class.getName());

    /**
     * Main contructor.
     * @param parent the node's parent, generally (but not necessarily) an instance
     * of a virtual builder.
     */
    public VirtualNode(MMObjectBuilder parent) {
        super(parent);
        virtual=true;
    }

     /**
      * commit : commits the node to the database or other storage system.
      * Generally, commiting a virtual node has no effect, so the basic
      * implementation returns false.
      * @return <code>false</code>
      */
    public boolean commit() {
      return false;
    }

    /**
     *  Insert is not implemented on a virtual node.
     *  @return nothing, throws an exception
     *  @throw UnsupportedOperationException
     */
    public int insert(String userName) {
        throw new UnsupportedOperationException("Method insert is not implemented on a virtual node.");
    }

    /**
     * Returns whether this node has relations.
     * A virtual node never has relations.
     * @return <code>false</code>
     */
    public boolean hasRelations() {
        return false;
    }

    /**
     * Return the relations of this node.
     * A virtual node never has relations.
     * @return empty <code>Enumeration</code>
     */
    public Enumeration getRelations() {    
        return new java.util.Vector(0).elements();
    }

    /**
     * Returns the number of relations of this node.
     * A virtual node never has relations.
     * @return 0
     */
    public int getRelationCount() {
        return 0;
    }

    /**
     * Return the number of relations of this node, filtered on a specified type.
     * A virtual node never has relations.
     * @param wantedtype the 'type' of related nodes (NOT the relations!).
     * @return An <code>int</code> indicating the number of nodes found
     */
    public int getRelationCount(String wantedtype) {
        return 0;
    }

    /**
     * Returns the node's age
     * A virtual node is always new (0)
     * @return the age in days (0)
     */
    public int getAge() {
        return 0;
    }

}
