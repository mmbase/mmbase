/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.corebuilders;

import java.util.Iterator;
import org.mmbase.cache.Cache;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The OAlias builder is an optional corebuilder used to associate aliases with nodes.
 * Each OAlias object contains a name field (the alias), and a destination field (the number of
 * the object referenced).
 * This builder is not used directly. If you add aliases, use {@link MMObjectBuilder#createAlias} instead
 * of the builder's insert method.
 * MMBase will run without this builder, but most applications use aliases.
 *
 * @author Rico Jansen
 * @author Michiel Meeuwissen
 * @version $Id: OAlias.java,v 1.17 2004-09-02 10:40:56 pierre Exp $
 */

public class OAlias extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(OAlias.class);

    // alias -> node-number (Integer)
    private Cache numberCache = new Cache(128) {
        public String getName()        { return "AliasCache"; }
        public String getDescription() { return "Cache for node aliases"; }
        };

    private static final Integer NOT_FOUND = new Integer(-1);

    public OAlias() {
        numberCache.putCache();
    }

    public boolean init() {
        boolean res = super.init();
        if (res) checkAddTmpField("_destination");
        return res;
    }

    /**
     * Obtain the number of a node through its alias
     * @param alias the alias of the desired node
     * @return the number of the node, or -1 if the alias does not exist
     * @see #getAliasedNode
     */
    public int getNumber(String name) {
        if (log.isDebugEnabled()) {
            log.debug("Finding oalias node '" + name + "'");
        }

        Integer nodeNumber = (Integer) numberCache.get(name);
        if (nodeNumber == null) {
            try {
                NodeSearchQuery query = new NodeSearchQuery(this);
                BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(query.getField(getField("name")), name);
                query.setConstraint(constraint);
                Iterator i = getNodes(query).iterator();
                if (i.hasNext()) {
                    MMObjectNode node = (MMObjectNode) i.next();
                    int rtn = node.getIntValue("destination");
                    numberCache.put(name, new Integer(rtn));
                    return rtn;
                } else {
                    numberCache.put(name, NOT_FOUND);
                    return -1;
                }
            } catch (SearchQueryException sqe) {
                log.error(sqe.toString());
                return -1;
            }
        } else {
            return nodeNumber.intValue();
        }
    }

    /**
     * Obtain the alias of a node. If a node has more aliases, it returns only one.
     * Which one is not specified.
     * @param number the number of the node
     * @return the alias of the node, or null if it does not exist
     * @see #getNumber
     * @todo No caching here?
     */
    public String getAlias(int number) {
        NodeSearchQuery query = new NodeSearchQuery(this);
        BasicFieldValueConstraint constraint = new BasicFieldValueConstraint(query.getField(getField("destination")), new Integer(number));
        query.setConstraint(constraint);
        try {
            Iterator i = getNodes(query).iterator();
            if (i.hasNext()) {
                MMObjectNode node = (MMObjectNode)i.next();
                return node.getStringValue("name");
            } else {
                return null;
            }
        } catch (SearchQueryException sqe) {
            log.error(sqe.toString());
            return null;

        }
    }

    /**
     * Obtain a node from the cloud through its alias
     * @param alias the alias of the desired node
     * @return the node, or null if the alias does not exist
     * @throws RuntimeException if the alias exists but the node itself doesn't (this indicates
     *                          an inconsistency in the database)
     * @see #getNumber
     */
    public MMObjectNode getAliasedNode(String alias) {
        MMObjectNode node = null;
        int nr = getNumber(alias);
        if (nr > 0) {
            try {
                node = getNode(nr);
            } catch (RuntimeException e) {
                log.error("Alias '" + alias + "' points to non-existing node with number " + nr);
                throw e;
            }
        }
        return node;
    }
    /**
     * Creates an alias for the node with the given number, and updates the alias cache.
     *
     * @since MMBase-1.7
     */

    public void createAlias(String alias, int number) {
        MMObjectNode node = getNewNode("system");
        node.setValue("name", alias);
        node.setValue("destination", number);
        node.insert("system");
        numberCache.remove(alias);

    }

    /**
     * Remove a node from the cloud and update the cache
     * @param node The node to remove.
     */
    public void removeNode(MMObjectNode node) {
        String name = node.getStringValue("name");
        super.removeNode(node);
        numberCache.remove(name);
    }

    /**
     * Called when a remote node is changed.
     * If a node is changed or newly created, this adds the new or updated alias to the
     * cache.
     * @todo Old aliasses are currently not cleared or removed - which means that they may remain
     * useable for some time after the actual alias is deleted or renamed.
     * This is because old alias information is no longer available when this call is made.
     * @since MMBase-1.7
     * @param machine Name of the machine that changed the node.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return always <code>true</code>
     */
    public boolean nodeRemoteChanged(String machine,String number,String builder,String ctype) {
        if (machine.equals(getTableName())) {
            if (ctype.equals("c") || ctype.equals("n")) {
                // should remove aliasses referencing this number from numberCache here
                MMObjectNode node = getNode(number);
                numberCache.put(node.getStringValue("name"), node.getIntegerValue("destination"));
            } else if (ctype.equals("d")) {
                // should remove aliasses referencing this number from numberCache here
            }
       }
       return super.nodeRemoteChanged(machine, number, builder, ctype);
    }

}
