/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;

import java.util.Collection;
import java.util.NoSuchElementException;
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * A list of relations
 *
 * @author Pierre van Rooden
 * @version $Id: BasicRelationList.java,v 1.11 2002-10-15 15:28:29 pierre Exp $
 */
public class BasicRelationList extends BasicNodeList implements RelationList {
    private static Logger log = Logging.getLoggerInstance(BasicRelationList.class.getName());

    /**
     * ...
     */
    BasicRelationList() {
        super();
    }

    /**
     * ...
     */
    BasicRelationList(Collection c, Cloud cloud) {
        super(c,cloud);
    }

    /**
     * ...
     */
    BasicRelationList(Collection c, NodeManager nodemanager) {
        super(c,nodemanager);
    }

    protected Object validate(Object o) throws ClassCastException,IllegalArgumentException {
        if (o instanceof MMObjectNode) {
            if (((MMObjectNode) o).getBuilder() instanceof org.mmbase.module.corebuilders.InsRel) {
                return o;
            } else {
                throw new IllegalArgumentException("requires a relation node");
            }
        } else {
            return (Relation)o;
        }
    }

    /**
     *
     */
    public Relation getRelation(int index) {
        return (Relation)get(index);
    }

    /**
     *
     */
    public RelationList subRelationList(int fromIndex, int toIndex) {
        return new BasicRelationList(subList(fromIndex, toIndex),nodeManager);
    }

    /**
     *
     */
    public RelationIterator relationIterator() {
        return new BasicRelationIterator(this);
    }

    public class BasicRelationIterator extends BasicNodeIterator implements RelationIterator {
        BasicRelationIterator(BasicList list) {
            super(list);
        }

        public Relation nextRelation() {
            return (Relation)next();
        }
    }
}
