/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;

import org.mmbase.util.*;
import org.mmbase.cache.implementation.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A base class for all Caches. Extend this class for other caches.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
abstract public class Cache<K, V> implements SizeMeasurable, Map<K, V>, CacheMBean {

    private static final Logger log = Logging.getLoggerInstance(Cache.class);

    private boolean active = true;
    protected int maxEntrySize = -1; // no maximum/ implementation does not support;

    /**
     * @since MMBase-1.8
     */
    private CacheImplementationInterface<K, V> implementation;

    protected volatile Object lock;

    /**
     * The number of times an element was succesfully retrieved from this cache.
     */
    private long hits = 0;

    /**
     * The number of times an element could not be retrieved from this cache.
     */
    private long misses = 0;

    /**
     * The number of times an element was committed to this cache.
     */
    private long puts = 0;

    public Cache(int size) {
        // See: http://www.mmbase.org/jira/browse/MMB-1486
        implementation = CacheManager.getInstance().createDefaultImplementation(size);
        lock           = implementation.getLock();
        //implementation = new LRUHashtable<K, V>(size);

        log.service("Creating cache " + getName() + ": " + getDescription());
    }

    void setImplementation(int size, String clazz, Map<String,String> configValues) {
        synchronized(lock) {
            clear();
            try {
                if (implementation == null || (! clazz.equals(implementation.getClass().getName()))) {
                    log.info("Setting implementation of " + this + " to " + clazz);
                    implementation = CacheManager.getInstance().createImplementation(size, clazz, configValues);
                    lock = implementation.getLock();
                }
            } catch (RuntimeException iae) {
                log.error("For cache " + this + " " + iae.getClass().getName() + ": " + iae.getMessage());
            }
        }
    }

    /**
     * If you want to structurally modify this cache, synchronize on this object.
     *
     * @since MMBase-1.8.6
     */
    public final Object getLock() {
        return lock;
    }

    /**
     * Returns a name for this cache type. Default it is the class
     * name, but this normally will be overriden.
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * Gives a description for this cache type. This can be used in
     * cache overviews.
     */
    public String getDescription() {
        return "An all purpose Cache";
    }



    /**
     * Return the maximum entry size for the cache in bytes.  If the
     * cache-type supports it (default no), then no values bigger then
     * this will be stored in the cache.
     */
    public int getMaxEntrySize() {
        if (getDefaultMaxEntrySize() > 0) {
            return maxEntrySize;
        } else {
            return -1;
        }
    }

    /**
     * @since MMBase-1.9.2
     */
    public void setMaxEntrySize(int i) {
        if (getDefaultMaxEntrySize() > 0) {
            maxEntrySize = i;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the average 'length' of the values in the cache. Whatever that may mean. For a {@link
     * QueryResultCache} the length obviously is the length of the cached lists.
     *
     * May return <code>NaN</code> if unknown or undetermined.
     * @since MMBase-1.9.2
     */
    public double getAvarageValueLength() {
        return Double.NaN;
    }

    /**
     * This has to be overridden by Caches which support max entry size.
     */

    protected int getDefaultMaxEntrySize() {
        return -1;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        if (! active) {
            return new HashSet<Map.Entry<K,V>>();
        }
        return implementation.entrySet();
    }

    /**
     * @since MMBase-1.8.6
     */
    public Class<?> getImplementation() {
        return implementation.getClass();
    }

    /**
     * Checks whether the key object should be cached.
     * This method returns <code>false</code> if either the current cache is inactive, or the object to cache
     * has a cache policy associated that prohibits caching of the object.
     * @param key the object to be cached
     * @return <code>true</code> if the object can be cached
     * @since MMBase-1.8
     */
    protected boolean checkCachePolicy(Object key) {
        CachePolicy policy = null;
        if (active) {
            if (key instanceof Cacheable) {
                policy = ((Cacheable)key).getCachePolicy();
                if (policy != null) {
                    return policy.checkPolicy(key);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Like 'get' of Maps but considers if the cache is active or not,  and the cache policy of the key.
     */
    public  V get(Object key) {
        if (!checkCachePolicy(key)) {
            return null;
        }
        V res = implementation.get(key);
        if (res != null) {
            hits++;
        } else {
            misses++;
        }
        return res;
    }

    /**
     * Like 'put' of LRUHashtable but considers if the cache is active or not.
     *
     */
    public V put(K key, V value) {
        if (!checkCachePolicy(key)) {
            return null;
        }
        puts++;
        return implementation.put(key, value);
    }

    /**
     * Returns the number of times an element was succesfully retrieved
     * from the table.
     */
    public long getHits() {
        return hits;
    }

    /**
     * Returns the number of times an element cpould not be retrieved
     * from the table.
     */
    public long getMisses() {
        return misses;
    }

    /**
     * Returns the number of times an element was committed to the table.
     */
    public long getPuts() {
        return puts;
    }

    /**
     * Reset 'puts', 'misses' and 'puts' to 0.
     * @since MMBase-1.9.2
     */
    public void reset() {
        hits = 0; misses = 0; puts = 0;
    }

    public  void setMaxSize(int size) {
        implementation.setMaxSize(size);
    }
    public  int maxSize() {
        return implementation.maxSize();
    }
    public int getMaxSize() {
        return maxSize();
    }

    /**
     * @see java.util.Map#size()
     */
    public  int size() {
        return implementation.size();
    }

    public int getSize() {
        return size();
    }
    public  boolean contains(Object key) {
        return implementation.containsKey(key);
    }

    public int getCount(K key) {
        return implementation.getCount(key);
    }

    /**
     * Returns the ratio of hits and misses.
     * The higher the ratio, the more succesfull the table retrieval is.
     * A value of 1 means every attempt to retrieve data is succesfull,
     * while a value nearing 0 means most times the object requested it is
     * not available.
     * Generally a high ratio means the table can be shrunk, while a low ratio
     * means its size needs to be increased.
     *
     * @return A double between 0 and 1 or NaN.
     */
    public double getRatio() {
        return ((double) hits) / (  hits + misses );
    }


    /**
     * Returns statistics on this table.
     * The information shown includes number of accesses, ratio of misses and hits,
     * current size, and number of puts.
     */
    public String getStats() {
        return "Access "+ (hits + misses) + " Ratio " + getRatio() + " Size " + size() + " Puts " + puts;
    }


    /**
     * Sets this cache to active or passive.
     * TODO: Writing back to caches.xml if necessary (if this call was nog caused by change of caches.xml itself)
     */
    public void setActive(boolean a) {
        active = a;
        if (! active) {
            implementation.clear();
        }
        // inactive caches cannot contain anything
        // another option would be to override also the 'contains' methods (which you problable should not use any way)
    }

    @Override
    public String toString() {
        return "Cache " + getName() + ", Ratio: " + getRatio() + " " + implementation;
    }

    /**
     * Wether this cache is active or not.
     */
    public final boolean isActive() {
        return active;
    }

    public int getByteSize() {
        return getByteSize(new SizeOf());
    }

    public int getByteSize(SizeOf sizeof) {
        int size = 26;
        if (implementation instanceof SizeMeasurable) {
            size += ((SizeMeasurable) implementation).getByteSize(sizeof);
        } else {
            // sizeof.sizeof(implementation) does not work because this.equals(implementation)
            synchronized(lock) {
                for (Map.Entry<K, V> entry : implementation.entrySet()) {
                    size += sizeof.sizeof(entry.getKey());
                    size += sizeof.sizeof(entry.getValue());
                }
            }
        }
        return size;
    }

    /**
     * Returns the sum of bytesizes of every key and value. This may count too much, because objects
     * (like Nodes) may occur in more then one value, but this is considerably cheaper then {@link
     * #getByteSize()}, which has to keep a Collection of every counted object.
     * @since MMBase-1.8
     */
    public int getCheapByteSize() {
        int size = 0;
        SizeOf sizeof = new SizeOf();
        synchronized(lock) {
            for (Map.Entry<K, V> entry : implementation.entrySet()) {
                size += sizeof.sizeof(entry.getKey());
                size += sizeof.sizeof(entry.getValue());
                sizeof.clear();
            }
        }
        return size;
    }


    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        implementation.clear();
    }


    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return implementation.containsKey(key);
    }


    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return implementation.containsValue(value);
    }


    /**
     * @see java.util.Map#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        // odd, but this is accordinding to javadoc of Map.
        if (o == this)
            return true;

        if (!(o instanceof Cache))
            return false;
        Cache<?,?> c = (Cache<?,?>) o;
        if (!c.getName().equals(getName())) {
            return false;
        }
        return implementation.equals(o);
    }


    /**
     * @see java.util.Map#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = getName().hashCode();
        hash = HashCodeUtil.hashCode(hash, implementation.hashCode());
        return hash;
    }


    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return implementation.isEmpty();
    }


    /**
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return implementation.keySet();
    }


    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K,? extends V> t) {
        implementation.putAll(t);
    }


    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(Object key) {
        return implementation.remove(key);
    }


    /**
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        return implementation.values();
    }


    /**
     * Puts this cache in the caches repository.
     * @see CacheManager#putCache(Cache)
     */

    public Cache<K,V> putCache() {
        return CacheManager.putCache(this);
    }

}
