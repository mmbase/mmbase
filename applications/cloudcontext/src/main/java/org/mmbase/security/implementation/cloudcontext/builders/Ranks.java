/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext.builders;

import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.security.Rank;
import org.mmbase.security.SecurityException;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This MMObjectBuilder implementation belongs to the object type
 * 'mmbaseusers' It contains functionality to MD5 encode passwords,
 * and so on.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public class Ranks extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(Ranks.class);

    public Ranks() {
        super();
    }

    /**
     * Returns the Ranks builder.
     */
    public static Ranks getBuilder() {
        return (Ranks) MMBase.getMMBase().getBuilder("mmbaseranks");
    }

    // javadoc inherited
    public boolean init() {
        boolean res = super.init();
        try {
            Iterator<MMObjectNode> i = getNodes(new NodeSearchQuery(this)).iterator();
            while (i.hasNext()) {
                MMObjectNode rank = i.next();
                String name = rank.getStringValue("name");
                Rank r = Rank.getRank(name);
                if (r == null) {
                    Rank.createRank(rank.getIntValue("rank"), name);
                }
            }
        } catch (SearchQueryException sqe) {
            log.error(sqe + Logging.stackTrace(sqe));
        }
         return res;
    }
    /**
     * If a rank is inserted, it must be registered
     */
    @Override
    public int insert(String owner, MMObjectNode node) {
        int res = super.insert(owner, node);
        int rank = node.getIntValue("rank");
        String name  = node.getStringValue("name");
        try {
            Iterator<MMObjectNode> i = getNodes(new NodeSearchQuery(this)).iterator();
            while (i.hasNext()) {
                MMObjectNode otherNode = i.next();
                if (node.getNumber() == otherNode.getNumber()) continue;
                Rank r = getRank(otherNode);
                if(r.getInt() == rank) {
                    // there is a unique key on rank so insert will have failed.
                    // this tells us why.
                    throw new SecurityException("Cannot insert rank '" + name + "', because there is already is a rank with rank weight " + rank + " (" + r + ")");
                }
                if(r.toString().equals(name)) {
                    // there is a unique key on name so insert will have failed.
                    // this tells us why.
                    throw new SecurityException("Cannot insert rank '" + name + "', because there is already a rank with that name");
                }

                // TODO, fix core!  peculiar checks, only because core give unclear messages!!
            }
        } catch (SearchQueryException sqe) {
            log.error(sqe + Logging.stackTrace(sqe));
        }
        Rank.createRank(rank, name);
        return res;
    }


    /**
     * A rank may only be removed if there are no users of that rank.
     *
     */
    @Override
    public void removeNode(MMObjectNode node) {
        List<MMObjectNode> users =  node.getRelatedNodes("mmbaseusers", RelationStep.DIRECTIONS_SOURCE);
        if (users.size() > 1) {
            // cannot happen?
            throw new SecurityException("Rank " + node + " cannot be removed because there are users with this rank: " + users);
        }
        String name = node.getStringValue("name");
        Rank.deleteRank(name);
        super.removeNode(node);
    }



    /**
     * Converts this MMObjectNode to a real rank.
     */
    public Rank getRank(MMObjectNode node) {
        int rank = node.getIntValue("rank");
        if (rank == -1) {
            throw new SecurityException("odd rank " + rank);
        } else {
            String name = node.getStringValue("name");
            Rank r = Rank.getRank(name);
            return r;
        }
    }

    /**
     * Gets the rank node with given rank, or if no such node, the node with the highest rank
     * smaller than given rank.
     * @since MMBase-1.8
     */
    public MMObjectNode getRankNode(Rank rank) {
        NodeSearchQuery q = new NodeSearchQuery(this);
        org.mmbase.core.CoreField rankFieldDefs = getField("rank");
        StepField rankField = q.getField(rankFieldDefs);
        BasicFieldValueConstraint cons = new BasicFieldValueConstraint(rankField, new Integer(rank.getInt()));
        cons.setOperator(FieldCompareConstraint.LESS_EQUAL);
        BasicSortOrder s = q.addSortOrder(rankField);
        s.setDirection(SortOrder.ORDER_DESCENDING);
        q.setConstraint(cons);
        q.setMaxNumber(1);
        try {
            Iterator<MMObjectNode> i = getNodes(q).iterator();
            if (i.hasNext()) {
                return  i.next();
            } else {
                return null;
            }
        } catch (org.mmbase.storage.search.SearchQueryException sqe) {
            log.error(sqe);
            return null;
        }
    }


    /**
     * Only the description of a rarnk may be changed.
     *
     */
    @Override
    public boolean setValue(MMObjectNode node, String field, Object originalValue) {
        if (field.equals("name") || field.equals("rank")) {
            if ( (!node.getValue(field).equals(originalValue)) && (originalValue != null)) {
                throw new SecurityException("Cannot change " + field + " field of rank objects");
            }
        }
        return true;
    }

    //javadoc inherited
    @Override
    public void setDefaults(MMObjectNode node) {
        // does not work because setValue disallowes changing
        //setUniqueValue(node, "name", "rank");
        //setUniqueValue(node, "rank", 200);
    }


}
