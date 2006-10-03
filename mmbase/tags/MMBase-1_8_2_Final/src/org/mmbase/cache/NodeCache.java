/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;
import org.mmbase.core.event.*;
import org.mmbase.module.core.*;

/**
 * A cache for MMObjectNodes. 
 *
 * @author  Michiel Meeuwissen
 * @version $Id: NodeCache.java,v 1.6 2006-06-23 17:01:00 michiel Exp $
 */
public class NodeCache extends Cache implements NodeEventListener { 
    private static final int CACHE_SIZE = 4 * 1024;

    private static NodeCache cache;

    public static NodeCache getCache() {
        return cache;
    }

    static {
        cache = new NodeCache();
        cache.putCache();
    }

    public String getName() {
        return "Nodes";
    }
    public String getDescription() {
        return "Node number -> MMObjectNodes";
    }

    /**
     * Creates the MMBase ObjectNodes Cache.
     */
    private NodeCache() {
        super(CACHE_SIZE);
        // node cache is registered as a Listener in MMBase.java.
    }


    public Object remove(Object key) {
        RelatedNodesCache.getCache().removeNode((Integer) key);
        return super.remove(key);
    }


    public void notify(NodeEvent event) {
        int type = event.getType();
        if(type == Event.TYPE_DELETE || ((! event.isLocal()) && type == Event.TYPE_CHANGE)) {
            Integer changedNodeNumber = new Integer(event.getNodeNumber());
            remove(changedNodeNumber);
        }
    }
}
