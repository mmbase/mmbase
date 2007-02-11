/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;

import org.mmbase.core.event.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.storage.search.SearchQuery;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class will manage a collection of <code>ReleaseStrategy</code>
 * instances, and call them hierarchically. 
 *
 * @since MMBase-1.8
 * @author Ernst Bunders
 * @version $Id: ChainedReleaseStrategy.java,v 1.21 2007-02-11 19:21:11 nklasens Exp $
 */
public class ChainedReleaseStrategy extends ReleaseStrategy {

    private final List<ReleaseStrategy> releaseStrategies = new CopyOnWriteArrayList<ReleaseStrategy>();

    //this map is used to store the 'enabled' status of wrapped strategies when this one is being disabled
    //so the old settings can be returned when it is enabled again
    private final Map<String, Boolean> childStrategyMemory = new HashMap<String, Boolean>();

    public ChainedReleaseStrategy() {
    }



    /**
     * This method provides a way of globally switching off all strategies this one wraps.
     * When this strategy is set to 'disabled', the state of all wrapped strategies is being
     * preserved, so when it is being 'enabled' again, these settings are restored, in stead of
     * just setting all wrapped strategies to 'enabled'.
     */
    // MM: very nice. When is this useful?
    public void setEnabled(boolean newStatus) {
        if(newStatus != isEnabled()){
            super.setEnabled(newStatus);

            //if the strategy is enabled and we have recorded settings, we must put them back


            for(Iterator<ReleaseStrategy> i = iterator(); i.hasNext();){
                ReleaseStrategy strategy = i.next();

                //if it must be switched on, we must use the memeory if present
                if(newStatus == true){
                    Boolean memory = childStrategyMemory.get(strategy.getName());
                    strategy.setEnabled( memory == null ? true :  memory.booleanValue());
                } else {
                    //if it must switch of, we must record the status
                    childStrategyMemory.put(strategy.getName(), new Boolean(strategy.isEnabled()));
                    strategy.setEnabled(false);
                    strategy.clear();
                }
            }
        }
    }
    /**
     * Adds the strategy if it is not already there. Strategies should only
     * occur once.
     *
     * @param strategy
     */
    public void addReleaseStrategy(ReleaseStrategy strategy) {
        if (! releaseStrategies.contains(strategy)){
            releaseStrategies.add(strategy);
        }
    }

    public void removeStrategy(ReleaseStrategy strategy) {
        releaseStrategies.remove(strategy);
    }

    /**
     * removes all strategies 
     */
    public void removeAllStrategies(){
        for (Iterator<ReleaseStrategy> i = iterator(); i.hasNext(); ){
            removeStrategy(i.next());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mmbase.cache.ReleaseStrategy#getName()
     */
    public String getName() {
        return "Multi Release Strategy";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mmbase.cache.ReleaseStrategy#getDescription()
     */
    public String getDescription() {
        return "This is a wrapper for any number of strategies you would like to "
            + "combine. it is used as the base strategy for QueryResultCache subclasses.";
    }

    public Iterator<ReleaseStrategy> iterator() {
        return releaseStrategies.iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mmbase.cache.ReleaseStrategy#doEvaluate(org.mmbase.module.core.NodeEvent,
     *      org.mmbase.storage.search.SearchQuery, java.util.List)
     */
    protected final boolean doEvaluate(NodeEvent event, SearchQuery query, List<MMObjectNode> cachedResult) {
        // first do the 'basic' strategy that is allways there. (see constructor)
        Iterator<ReleaseStrategy> i = releaseStrategies.iterator();
        // while the outcome of getResult is true (the cache should be flushed), we have to keep trying.
        while (i.hasNext()) {
            ReleaseStrategy strategy = i.next();
            StrategyResult result = strategy.evaluate(event, query, cachedResult);
            if (! result.shouldRelease()) return false;
        }
        return true;
    }
    protected final boolean doEvaluate(RelationEvent event, SearchQuery query, List<MMObjectNode> cachedResult) {
        // first do the 'basic' strategy that is allways there. (see constructor)
        Iterator<ReleaseStrategy> i = releaseStrategies.iterator();
        // while the outcome of getResult is true (the cache should be flushed), we have to keep trying.
        while (i.hasNext()) {
            ReleaseStrategy strategy = i.next();
            StrategyResult result = strategy.evaluate(event, query, cachedResult);
            if (! result.shouldRelease()) return false;
        }
        return true;
    }

    public void clear(){
        super.clear();
        for(Iterator<ReleaseStrategy> i = iterator(); i.hasNext();){
            ReleaseStrategy rs = i.next();
            rs.clear();
        }
    }
    /**
     * @since MMBase-1.8.1
     */
    public int size() {
        return releaseStrategies.size();
    }

    public String toString() {
        return "" + releaseStrategies;
    }
}
