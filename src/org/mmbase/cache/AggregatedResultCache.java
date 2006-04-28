/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import org.mmbase.util.logging.*;

/**
 * Cache for queries like  SELECT COUNT(number) AS number FROM mm_news news.
 *
 * @author Michiel Meeuwissen
 * @version $Id: AggregatedResultCache.java,v 1.2 2003-08-29 12:12:27 keesj Exp $
 * @see   org.mmbase.bridge.implementation.BasicCloud#getList
 * @todo  It is odd that this query cache is called in the bridge implementation and not in the core.
 * @since MMBase-1.7
 */


public class AggregatedResultCache extends QueryResultCache {

    private static Logger log = Logging.getLoggerInstance(AggregatedResultCache.class);

    // There will be only one multilevel cache, and here it is:
    private static AggregatedResultCache cache;

    public static AggregatedResultCache getCache() {
        return cache;
    }

    static {
        cache = new AggregatedResultCache(300);
        cache.putCache();
    }

    public String getName() {
        return "AggregatedResultCache";
    }
    public String getDescription() {
        return "Aggregating Query Results";
    }

    /**
     * Creates the cache.
     */
    private AggregatedResultCache(int size) {
        super(size);
    }
        
}
