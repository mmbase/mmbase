/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseObserver;
import org.mmbase.util.logging.*;

import org.mmbase.storage.search.*;

/**
 * Query result cache used for getNodes from MMObjectBuilder. So it contains only simple nodes (no
 * clusternodes)
 *
 * @author Michiel Meeuwissen
 * @version $Id: NodeListCache.java,v 1.2 2003-07-17 17:01:17 michiel Exp $
 * @see   org.mmbase.module.core.MMObjectBuilder#getNodes
 * @since MMBase-1.7
 */


public class NodeListCache extends QueryResultCache {

    private static Logger log = Logging.getLoggerInstance(NodeListCache.class);

    // There will be only one list cache, and here it is:
    private static NodeListCache nodeListCache;

    public static NodeListCache getCache() {
        return nodeListCache;
    }

    static {
        nodeListCache = new NodeListCache(300);
        nodeListCache.putCache();
    }

    public String getName() {
        return "NodeListCache";
    }
    public String getDescription() {
        return "List Results";
    }

    /**
     * Creates the Node list cache.
     */
    private NodeListCache(int size) {
        super(size);
    }
        
}
