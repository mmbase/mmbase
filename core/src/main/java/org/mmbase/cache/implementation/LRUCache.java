/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache.implementation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.mmbase.cache.CacheImplementationInterface;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A cache implementation backed by a {@link java.util.LinkedHashMap}, in access-order mode, and
 * restricted maximal size ('Least Recently Used' cache algorithm).
 *
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @see    org.mmbase.cache.Cache
 * @since MMBase-1.8.6
 */
public class LRUCache<K, V> implements CacheImplementationInterface<K, V> {

    private static final Logger log = Logging.getLoggerInstance(LRUCache.class);

    public int maxSize = 100;
    private final Map<K, V> backing;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final LinkedHashMap<K, Long> accessOrder;

    public LRUCache() {
        this(100);
    }

    public LRUCache(int size) {
        maxSize = size;
        // Use ConcurrentHashMap for non-blocking reads
        backing = new ConcurrentHashMap<>(size);
        // Track access order separately for LRU eviction
        accessOrder = new LinkedHashMap<K, Long>(size, 0.75f, true) {
            private static final long serialVersionUID = 0L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    public int getCount(K key) {
        return -1;
    }

    /**
     * Change the maximum size of the table.
     * This may result in removal of entries in the table.
     * @param size the new desired size
     */
    public void setMaxSize(int size) {
        if (size < 0 ) {
            throw new IllegalArgumentException("Cannot set size to negative value " + size);
        }
        rwLock.writeLock().lock();
        try {
            maxSize = size;
            evictExcessEntries();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Evict entries when cache exceeds maxSize.
     * Must be called with write lock held.
     */
    private void evictExcessEntries() {
        while (backing.size() > maxSize && !accessOrder.isEmpty()) {
            try {
                Iterator<K> i = accessOrder.keySet().iterator();
                if (i.hasNext()) {
                    K eldest = i.next();
                    i.remove();
                    backing.remove(eldest);
                }
            } catch (Exception e) {
                log.warn("Exception during eviction", e);
                break;
            }
        }
    }


    public final int maxSize() {
        return maxSize;
    }

    /**
     * Returns size, maxSize.
     */
    @Override
    public String toString() {
        return "Size=" + size() + ", Max=" + maxSize;
    }


    public void config(Map<String, String> map) {
        // needs no configuration.
    }

    public Object getLock() {
        return rwLock;
    }

    // wrapping for thread-safety with non-blocking reads
    public int size() {
        return backing.size();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    public V get(Object key) {
        // Non-blocking read from ConcurrentHashMap
        V value = backing.get(key);
        // Optionally update access order asynchronously (best effort, may be skipped under contention)
        if (value != null && key != null && rwLock.writeLock().tryLock()) {
            try {
                accessOrder.put((K) key, System.nanoTime());
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        return value;
    }

    public V put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            accessOrder.put(key, System.nanoTime());
            V result = backing.put(key, value);
            // Check if eldest entry should be removed
            if (accessOrder.size() > maxSize) {
                evictExcessEntries();
            }
            return result;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public V remove(Object key) {
        rwLock.writeLock().lock();
        try {
            accessOrder.remove(key);
            return backing.remove(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        rwLock.writeLock().lock();
        try {
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            backing.clear();
            accessOrder.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Set<K> keySet() {
        return backing.keySet();
    }

    public Set<Map.Entry<K,V>> entrySet() {
        return backing.entrySet();
    }

    public Collection<V> values() {
        return backing.values();
    }


}
