/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering;

import java.util.Iterator;
import java.util.Map;

import org.mmbase.core.event.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;

/**
 * This utility class contains the methods for broadcasting/registering changes on nodes. It is
 * available as 'getChangeManager()' from the StorageManagerFactory.
 *
 * @author Pierre van Rooden
 * @version $Id: ChangeManager.java,v 1.11 2005-12-22 10:13:22 ernst Exp $
 * @see org.mmbase.storage.StorageManagerFactory#getChangeManager
 */
public final class ChangeManager {

     /**
     * Commit all changes stored in a Changes map.
     * Clears the change status of all changed nodes, then broadcasts changes to the
     * nodes' builders.
     * @param changes a map with node/change value pairs
     */
    public void commit(Map changes) {
        for (Iterator i = changes.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            MMObjectNode node = (MMObjectNode)e.getKey();
            String change = (String)e.getValue();
            commit(node, change);
            i.remove();
        }
    }

    /**
     * Commits the change to a node.
     * Fires the node change events through the EventManager.
     * Then clears 'changed' state on the node.
     * @param node the node to commit the change of
     * @param change the type of change: "n": new, "c": commit, "d": delete, "r" : relation changed
     */
    public void commit(MMObjectNode node, String change) {
        MMObjectBuilder builder = node.getBuilder();
        if (builder.broadcastChanges()) {
            //create a new local node event
            NodeEvent event = NodeEventHelper.createNodeEventInstance(node, NodeEvent.oldTypeToNewType(change), null);

            //regardless of wether this is a relatione event we fire a node event first
            EventManager.getInstance().propagateEvent(event);

           
            //if the changed node is a relation, we fire a relation event as well
            if(builder instanceof InsRel) {
                RelationEvent relEvent = NodeEventHelper.createRelationEventInstance(node, NodeEvent.oldTypeToNewType(change), null);

                //the relation event broker will make shure that listeners
                //for node-relation changes to a specific builder, will be
                //notified if this builder is either source or destination type
                //in the relation event
                EventManager.getInstance().propagateEvent(relEvent);
            }

        }
        node.clearChanged();
    }
}
