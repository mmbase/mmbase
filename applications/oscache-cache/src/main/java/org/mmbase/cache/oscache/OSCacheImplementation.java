package org.mmbase.cache.oscache;

import com.opensymphony.oscache.base.Config;
import com.opensymphony.oscache.base.algorithm.AbstractConcurrentReadCache;
import com.opensymphony.oscache.base.persistence.PersistenceListener;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import org.mmbase.cache.CacheImplementationInterface;
import org.mmbase.util.SizeMeasurable;
import org.mmbase.util.SizeOf;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Implementation of the MMBase 'CacheImplementationInterface' interface, which
 * delegates all calls to an OSCache implementation.
 * This implementation accepts the following configuration properties:
 * <ul>
 *   <li>path</li>
 * </ul>
 * To use this implementation, you can use a construction like this in your 'caches.xml'
 * configuration file:
 * <pre>
 * &lt;cache name="NodeListCache"&gt;
 *   &lt;status&gt;active&lt;/status&gt;
 *   &lt;size&gt;3&lt;/size&gt;
 *   &lt;implementation&gt;
 *     &lt;class&gt;org.mmbase.cache.oscache.OSCacheImplementation&lt;/class&gt;
 *     &lt;param name="path"&gt;/tmp&lt;/param&gt;
 *   &lt;/implementation&gt;
 * &lt;/cache&gt;
 * </pre>
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 */
public class OSCacheImplementation<K, V> implements CacheImplementationInterface<K, V>, SizeMeasurable {
    private AbstractConcurrentReadCache cacheImpl;
    private static final String defaultClassName = com.opensymphony.oscache.base.algorithm.LRUCache.class.getName();
    private static final String defaultPersistenceClass = com.opensymphony.oscache.plugins.diskpersistence.DiskPersistenceListener.class.getName();
    private static final Logger log = Logging.getLoggerInstance(OSCacheImplementation.class);

    public OSCacheImplementation() {
    }

    /**
     * This method is called by MMBase to configure the cache using the given
     * map of configuration parameters.
     */
    public void config(Map<String, String> config) {
       try {
           String className = config.get("class");
           if (className == null) {
               className = defaultClassName;
           }

           cacheImpl = (AbstractConcurrentReadCache) Class.forName(className).newInstance();
           String persistanceclass = config.get("persistence");
           if (persistanceclass == null) {
               persistanceclass = defaultPersistenceClass;
           }
           if (! persistanceclass.isEmpty()) {
               PersistenceListener pl = (PersistenceListener) Class.forName(persistanceclass).newInstance();
               Config osconfig = new Config();
               osconfig.set("cache.path", config.get("path"));
               pl.configure(osconfig);
               cacheImpl.setPersistenceListener(pl);
               cacheImpl.setMemoryCaching(true);
               cacheImpl.setUnlimitedDiskCache(true);
               cacheImpl.setOverflowPersistence(true);
           }
        } catch (Exception e) {
            log.error("Exception while initializing cache: " + e);
        }
    }

    /**
     * Wrapper around the setMaxEntries() method of the cache implementation.
     */
    public void setMaxSize(int size) {
        cacheImpl.setMaxEntries(size);
    }

    /**
     * Wrapper around the getMaxEntries() method of the cache implementation.
     */
    public int maxSize() {
        return cacheImpl.getMaxEntries();
    }

    /**
     * Wrapper around the getCount() method of the cache implementation. (not implemented)
     */
    public int getCount(K key) {
        return -1; //not implemented
    }

    /**
     * Wrapper around the clear() method of the cache implementation.
     */
    public void clear() {
        cacheImpl.clear();
    }

    /**
     * Wrapper around the containsKey() method of the cache implementation.
     */
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return cacheImpl.containsKey(key);
        } else {
            return cacheImpl.containsKey(computeKey(key));
        }
    }

    /**
     * Wrapper around the containsValue() method of the cache implementation.
     */
    public boolean containsValue(Object value) {
        return cacheImpl.containsValue(value);
    }

    /**
     * Wrapper around the entrySet() method of the cache implementation.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return cacheImpl.entrySet();
    }

    /**
     * Wrapper around the equals() method of the cache implementation.
     */
    public boolean equals(Object o) {
        return cacheImpl.equals(o);
    }

    /**
     * Wrapper around the get() method of the cache implementation.
     */
    public V get(Object key) {
        if (key instanceof String) {
            return (V) cacheImpl.get(key);
        } else {
            return (V) cacheImpl.get(computeKey(key));
        }
    }

    /**
     * Wrapper around the hashCode() method of the cache implementation.
     */
    public int hashCode() {
        return cacheImpl.hashCode();
    }

    /**
     * Wrapper around the isEmpty() method of the cache implementation.
     */
    public boolean isEmpty() {
        return cacheImpl.isEmpty();
    }

    /**
     * Wrapper around the keySet() method of the cache implementation.
     */
    public Set<K> keySet() {
        return cacheImpl.keySet();
    }

    /**
     * Wrapper around the put() method of the cache implementation.
     */
    public V put(K key, V value) {
        V oldValue = get(key);
        // returning does not work because of bug CACHE-255 in oscache (http://jira.opensymphony.com/browse/CACHE-255)
        if (key instanceof String) {
            //return (V) cacheImpl.put(key, value);
            cacheImpl.put(key, value);
        } else {
            //return (V) cacheImpl.put(computeKey(key), value);
            cacheImpl.put(computeKey(key), value);

        }
        return oldValue;
    }

    /**
     * Wrapper around the putAll() method of the cache implementation.
     */
    public void putAll(Map t) {
        cacheImpl.putAll(t);
    }

    /**
     * Wrapper around the remove() method of the cache implementation.
     */
    public V remove(Object key) {
        if (key instanceof String) {
            return (V) cacheImpl.remove(key);
        } else {
            return (V) cacheImpl.remove(computeKey(key));
        }
    }

    /**
     * Wrapper around the size() method of the cache implementation.
     */
    public int size() {
        return cacheImpl.size();
    }

    /**
     * Wrapper around the values() method of the cache implementation.
     */
    public Collection<V> values() {
        return cacheImpl.values();
    }

    /**
     * Calculate an unique string key for an object that is not a 'String'.
     * The 'toString()' will not suffice, partly because it doesn't guarantee
     * a unique value, and also because the toString() method may generate
     * keys that are too long.
     * This method will concatenate the classname and the hashcode of the object,
     * this combination should be unique.
     */
    private static String computeKey(Object o) {
        return o.getClass().getName() + "_" + o.hashCode();
    }

    /**
     * @see org.mmbase.util.SizeMeasurable#getByteSize()
     */
    public int getByteSize() {
        throw new UnsupportedOperationException("Size is not available for OSCache");
    }

    /**
     * @see org.mmbase.util.SizeMeasurable#getByteSize(org.mmbase.util.SizeOf)
     */
    public int getByteSize(SizeOf sizeof) {
        throw new UnsupportedOperationException("Size is not available for OSCache");
    }

    /**
     * @todo This will also be used to lock 'get' iteration operations in QueryResultCache, but that
     * is not actually needed for this implementation. You cannot synchronize on null though, in java.
     */
    public Optional<ReadWriteLock> getLock() {
        return Optional.empty();
    }
}
