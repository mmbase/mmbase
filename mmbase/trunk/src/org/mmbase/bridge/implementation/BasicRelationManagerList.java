/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;

import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * A list of relation managers
 *
 * @author Pierre van Rooden
 * @version $Id: BasicRelationManagerList.java,v 1.11 2003-03-04 09:19:07 pierre Exp $
 */
public class BasicRelationManagerList extends BasicNodeManagerList implements RelationManagerList {
    private static Logger log = Logging.getLoggerInstance(BasicRelationManagerList.class.getName());

    /**
     * ...
     */
    BasicRelationManagerList() {
        super();
    }

    BasicRelationManagerList(Collection c, Cloud cloud) {
        super(c,cloud);
    }

    protected Object validate(Object o) throws ClassCastException,IllegalArgumentException {
        if (o instanceof MMObjectNode) {
            MMObjectBuilder bul=((MMObjectNode) o).getBuilder();
            if ((bul instanceof org.mmbase.module.corebuilders.TypeRel) ||
                (bul instanceof org.mmbase.module.corebuilders.RelDef)) {
                return o;
            } else {
                throw new IllegalArgumentException("requires a relationmanager (typerel or reldef) node");
            }
        } else {
            return (RelationManager)o;
        }
    }

    /**
    *
    */
    public RelationManager getRelationManager(int index) {
        return (RelationManager) get(index);
    }

    /**
    *
    */
    public RelationManagerIterator relationManagerIterator() {
        return new BasicRelationManagerIterator(this);
    };

    /**
     *
     */
    public RelationManagerList subRelationManagerList(int fromIndex, int toIndex) {
        return new BasicRelationManagerList(subList(fromIndex, toIndex),cloud);
    }

    public class BasicRelationManagerIterator extends BasicNodeManagerIterator implements RelationManagerIterator {

        BasicRelationManagerIterator(BasicList list) {
            super(list);
        }

        public RelationManager nextRelationManager() {
            return (RelationManager)next();
        }

        public RelationManager previousRelationManager() {
            return (RelationManager)previous();
        }
    }

}
