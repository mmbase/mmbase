/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext.builders;

import org.mmbase.security.implementation.cloudcontext.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.security.Rank;
import org.mmbase.security.SecurityException;
import org.mmbase.util.Encode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This MMObjectBuilder implementation belongs to the object type
 * 'mmbaseusers' It contains functionality to MD5 encode passwords,
 * and so on.
 * 
 * @author Michiel Meeuwissen
 * @version $Id: Ranks.java,v 1.5 2003-07-08 17:42:45 michiel Exp $
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
        mmb.addLocalObserver(getTableName(),  CacheInvalidator.getInstance());
        mmb.addRemoteObserver(getTableName(), CacheInvalidator.getInstance());
         Enumeration allRanks = search(null);  
         while (allRanks.hasMoreElements()) {
             MMObjectNode rank = (MMObjectNode) allRanks.nextElement();
             String name = rank.getStringValue("name");
             Rank r = Rank.getRank(name);
             if (r == null) {                  
                 Rank.createRank(rank.getIntValue("rank"), name);
             }
         }
         return res;
    }
    /**
     * If a rank is inserted, it must be registered
     */
    public int insert(String owner, MMObjectNode node) {
        int res = super.insert(owner, node);
        int rank = node.getIntValue("rank");
        String name  = node.getStringValue("name");
        Enumeration allRanks = search(null);  
        while (allRanks.hasMoreElements()) {
            MMObjectNode otherNode = (MMObjectNode) allRanks.nextElement();
            if (node.getNumber() == otherNode.getNumber()) continue;
            Rank r = getRank(otherNode);
            if(r.getInt() == rank) {
                // there is a unique key on rank so insert will have failed.
                // this tells us why.                
                throw new SecurityException("Cannot insert rank '" + name + "', because there is already is a rank with rank weight " + rank + " (" + r + ")");
            }
        }
        Rank.createRank(rank, name);      
        return res;
    }


    /**
     * A rank may only be removed if there are no users of that rank.
     *
     */
    public void removeNode(MMObjectNode node) {
        List users =  node.getRelatedNodes("mmbaseusers", ClusterBuilder.SEARCH_SOURCE);
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
     * Only the description of a rarnk may be changed.
     *
     */
    public boolean setValue(MMObjectNode node, String field, Object originalValue) {
        if (field.equals("name") || field.equals("rank")) {
            if ( (!node.getValue(field).equals(originalValue)) && (originalValue != null)) {
                throw new SecurityException("Cannot change " + field + " field of rank objects");
            }
        }
        return true; 
    }

    //javadoc inherited
    public void setDefaults(MMObjectNode node) {
        
    }

}
