/*
 * This software is OSI Certified Open Source Software. OSI Certified is a
 * certification mark of the Open Source Initiative. The license (Mozilla
 * version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mmbase.core.event.*;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.storage.search.SearchQuery;
import org.mmbase.storage.search.SearchQueryWrapper;
import org.mmbase.storage.search.Step;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This cache provides a base implementation to cache the result of
 * SearchQuery's. Such a cache links a SearchQuery object to a list of
 * MMObjectNodes. A cache entry is automaticly invalidated if arbitrary node of
 * one of the types present in the SearchQuery is changed (,created or deleted).
 * This mechanism is not very subtle but it is garanteed to be correct. It means
 * though that your cache can be considerably less effective for queries
 * containing node types from which often nodes are edited.
 *
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @author Ernst Bunders
 * @version $Id$
 * @since MMBase-1.7
 * @see org.mmbase.storage.search.SearchQuery
 * @todo Perhaps we could put the 'typeCounter' stuff in a sub-class.
 */

abstract public class QueryResultCache extends Cache<SearchQuery, List<MMObjectNode>> implements NodeEventListener, RelationEventListener {

    private static final Logger log = Logging.getLoggerInstance(QueryResultCache.class);

    /**
     * This map contains the possible counts of queries grouped by type in this cache.
     * A query with multiple steps (types) will increase all counters.
     * A relation role name is considered a type
     * This cache will not invalidate when an event does not mention one of these types
     * The cache will be evaluated when a parent type is in this map.
     * @todo I think that nearly all query result caches contain queries with quite generic or
     * oftenly changed types. I doubt that the gain is worth the hassle.
     */
    private Map<String, Integer> typeCounters = new ConcurrentHashMap<>();


    /**
     * This is the default release strategy. Actually it is a container for any
     * number of 'real' release strategies
     *
     * @see ChainedReleaseStrategy
     */
    private final ChainedReleaseStrategy releaseStrategy;

    public QueryResultCache(int size) {
        super(size);
        releaseStrategy = new ChainedReleaseStrategy();
        log.service("Instantiated a " + this.getClass().getName() + " (" + releaseStrategy + ")"); // should happen limited number of times
        MMBase.getMMBase().addNodeRelatedEventsListener("object", this);
    }

    /**
     * @param strategies
     */
    public void addReleaseStrategies(List<ReleaseStrategy> strategies) {
        if (strategies != null) {
            for (ReleaseStrategy element : strategies) {
                if (log.isDebugEnabled()) {
                    log.debug(("adding strategy " + element.getName() + " to cache " + getName()));
                }
                addReleaseStrategy(element);
            }
        }
    }

    /**
     * This method lets you add a release strategy to the cache. It will in fact
     * be added to <code>ChainedReleaseStrategy</code>, which
     * is the default base release strategy.
     * @param releaseStrategy A releaseStrategy to add.
     */
    public void addReleaseStrategy(ReleaseStrategy releaseStrategy) {
        this.releaseStrategy.addReleaseStrategy(releaseStrategy);
    }

    /**
     * @return Returns the releaseStrategy.
     */
    public ChainedReleaseStrategy getReleaseStrategy() {
        return releaseStrategy;
    }

    /**
     * Puts a search result in this cache.
     */
    @Override
    public List<MMObjectNode> put(SearchQuery query, List<MMObjectNode> queryResult) {
        query.markUsed();
        while (query instanceof SearchQueryWrapper) {
            query = ((SearchQueryWrapper) query).unwrap();
        }
        if (!checkCachePolicy(query)) {
            return null;
        }
        increaseCounters(query, typeCounters);
        return super.put(query, queryResult);

    }

    /**
     * Removes an object from the cache. It alsos remove the watch from the
     * observers which are watching this entry.
     *
     * @param key A SearchQuery object.
     */
    @Override
    public List<MMObjectNode> remove(Object key) {
        while (key instanceof SearchQueryWrapper) {
            key = ((SearchQueryWrapper) key).unwrap();
        }

        List<MMObjectNode> result = super.remove(key);
        if (result != null) {
            decreaseCounters((SearchQuery) key, typeCounters);
        }
        return result;
    }

    private void increaseCounters(SearchQuery query, Map<String, Integer> counters) {
        for (Step step : query.getSteps()) {
            String stepName = step.getTableName();
            if (counters.containsKey(stepName)) {
                int count = counters.get(stepName);
                counters.put(stepName, count + 1);
            } else {
                counters.put(stepName, 1);
            }
        }
    }

    private void decreaseCounters(SearchQuery query, Map<String, Integer> counters) {
        for (Step step : query.getSteps()) {
            String stepName = step.getTableName();
            if (counters.containsKey(stepName)) {
                int count = counters.get(stepName);
                if (count > 1) {
                    counters.put(stepName, count - 1);
                } else {
                    counters.remove(stepName);
                }
            }
        }
    }
    /**
     *
     */
    @Override
    public double getAvarageValueLength() {
        double total = 0;
        for (List<MMObjectNode> result : values()) {
            total += result.size();
        }
        return total / size();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + getName();
    }

    /**
     * @see org.mmbase.core.event.RelationEventListener#notify(org.mmbase.core.event.RelationEvent)
     */
    public void notify(RelationEvent event) {
        if(containsType(event)) {
            nodeChanged(event);
        }
    }

    /**
     *
     */
    private boolean containsType(RelationEvent event) {
        try {
            if (typeCounters.containsKey("object")) {
                return true;
            }
            if (typeCounters.containsKey(event.getRelationSourceType())
                || typeCounters.containsKey(event.getRelationDestinationType())) {
                return true;
            }
            MMBase mmb = MMBase.getMMBase();
            String roleName = mmb.getRelDef().getBuilderName(Integer.valueOf(event.getRole()));
            if (typeCounters.containsKey(roleName)) {
                return true;
            }
            MMObjectBuilder srcbuilder = mmb.getBuilder(event.getRelationSourceType());
            if (srcbuilder == null) {
                return false;
            }
            for (MMObjectBuilder parent : srcbuilder.getAncestors()) {
                if (typeCounters.containsKey(parent.getTableName())) {
                    return true;
                }
            }
            MMObjectBuilder destbuilder = mmb.getBuilder(event.getRelationDestinationType());
            if (destbuilder == null) {
                return false;
            }
            for (MMObjectBuilder parent : destbuilder.getAncestors()) {
                if (typeCounters.containsKey(parent.getTableName())) {
                    return true;
                }
            }
            return false;
        } finally {
        }

    }


    /**
     * @see org.mmbase.core.event.NodeEventListener#notify(org.mmbase.core.event.NodeEvent)
     */
    public void notify(NodeEvent event) {
        if (containsType(event)) {
            nodeChanged(event);
        }
    }

    private boolean containsType(NodeEvent event) {
        try {
            if (typeCounters.containsKey("object")) {
                return true;
            }
            if (typeCounters.containsKey(event.getBuilderName())) {
                return true;
            }
            MMBase mmb = MMBase.getMMBase();
            MMObjectBuilder destBuilder = mmb.getBuilder(event.getBuilderName());
            if (destBuilder == null) {  // builder is not even available
                return false;
            }
            for (MMObjectBuilder parent : destBuilder.getAncestors()) {
                if (typeCounters.containsKey(parent.getTableName())) {
                    return true;
                }
            }
            return false;
        } finally {
        }
    }

    protected int nodeChanged(Event event) throws IllegalArgumentException{
        if (log.isDebugEnabled()) {
            log.debug("Considering " + event);
        }
        Set<SearchQuery> cacheKeys = new HashSet<>(keySet());
        Map<String, Integer> oldTypeCounters =  new HashMap<String, Integer>(typeCounters);

        Set<SearchQuery>     removeKeys        = new HashSet<SearchQuery>();
        Map<String, Integer> foundTypeCounters = new HashMap<String, Integer>();

        evaluate(event, cacheKeys, removeKeys, foundTypeCounters);

        try {
            for (SearchQuery q : removeKeys) {
                remove(q);
            }

            for (String type : typeCounters.keySet()) {
                if (foundTypeCounters.containsKey(type)) {
                    if (oldTypeCounters.containsKey(type)) {
                        // adjust counter
                        int oldValue = oldTypeCounters.get(type);
                        int guessedValue = typeCounters.get(type);
                        int foundValue = foundTypeCounters.get(type);
                        if (guessedValue - oldValue > 0) {
                            int newValue = foundValue + (guessedValue - oldValue);
                            foundTypeCounters.put(type, newValue);
                        }
                    } else {
                        int guessedValue = typeCounters.get(type);
                        int foundValue = foundTypeCounters.get(type);
                        int newValue = foundValue + guessedValue;
                        foundTypeCounters.put(type, newValue);
                    }
                } else {
                    Integer guessedValue = typeCounters.get(type);
                    foundTypeCounters.put(type, guessedValue);
                }
            }
            typeCounters = foundTypeCounters;
        } finally {

        }
        return removeKeys.size();
    }
    /**
     * @javadoc
     */
    private void evaluate(Event event, Set<SearchQuery> cacheKeys, Set<SearchQuery> removeKeys, Map<String, Integer> foundTypeCounters) {
        int evaluatedResults = cacheKeys.size();
        long startTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("Considering " + cacheKeys.size() + " objects in " + QueryResultCache.this.getName() + " for flush because of " + event);
        }
        for (SearchQuery key : cacheKeys) {

            boolean shouldRelease;
            if(releaseStrategy.isEnabled()){
                if(event instanceof NodeEvent){
                    shouldRelease = releaseStrategy.evaluate((NodeEvent)event, key, get(key)).shouldRelease();
                } else if (event instanceof RelationEvent){
                    shouldRelease = releaseStrategy.evaluate((RelationEvent)event, key, get(key)).shouldRelease();
                } else {
                    log.error("event " + event.getClass() + " " + event + " is of unsupported type");
                    shouldRelease = false;
                }
            } else {
                shouldRelease = true;
            }

            if (shouldRelease) {
                removeKeys.add(key);
            } else {
                increaseCounters(key, foundTypeCounters);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(QueryResultCache.this.getName() + ": event analyzed in " + (System.currentTimeMillis() - startTime)  + " milisecs. evaluating " + evaluatedResults + ". Flushed " + removeKeys.size());
        }
    }


    @Override
    public void clear(){
        try {
            super.clear();
            releaseStrategy.clear();
        } finally {
        }
    }
}
