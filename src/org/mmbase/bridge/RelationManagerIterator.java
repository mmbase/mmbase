/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;

/**
 * A list of nodes
 *
 * @author Pierre van Rooden
 * @version $Id: RelationManagerIterator.java,v 1.4 2003-03-04 09:19:05 pierre Exp $
 */
public interface RelationManagerIterator extends NodeManagerIterator {

    /**
     * Returns the next element in the iterator as a RelationManager
     */
    public RelationManager nextRelationManager();

    /**
     * Returns the previous element in the iterator as a RelationManager
     */
    public RelationManager previousRelationManager();

}
