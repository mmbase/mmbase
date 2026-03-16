/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache.implementation;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.mmbase.cache.CacheImplementationInterface;
import org.mmbase.util.ReadWriteLockAbstractCollection;
import org.mmbase.util.ReadWriteLockAbstractSet;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A cache implementation backed by a {@link LinkedHashMap}, with
 * restricted maximal size ('Least Recently Used' cache algorithm).
 *
 * <p>This implementation uses a ReadWriteLock to provide non-blocking concurrent reads.
 * Read operations (get, containsKey, etc.) use read locks allowing multiple concurrent readers.
 * Write operations (put, remove, clear) use write locks.</p>
 *
 * <p>Note: To maintain non-blocking reads, the get() operation does NOT update access order.
 * Therefore, LRU eviction is based on insertion/update order rather than true access order.
 * This provides better performance under high read concurrency at the cost of approximate LRU semantics.</p>
 *
 * @author  Michiel Meeuwissen
 * @see    org.mmbase.cache.Cache
 * @since MMBase-1.9.7
 */
public class FIFOCache<K, V> implements CacheImplementationInterface<K, V> {

    private static final Logger log = Logging.getLoggerInstance(FIFOCache.class);

    public int maxSize = 100;
    private final LinkedHashMap<K, V> backing;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public FIFOCache() {
        this(100);
    }

    public FIFOCache(int size) {
        maxSize = size;
        // Use LinkedHashMap in insertion-order mode (not access-order) for LRU behavior
        // Access-order would require write locks on get(), preventing non-blocking reads
        // Protected by ReadWriteLock for non-blocking concurrent reads
        backing = new LinkedHashMap<K, V>(size, 0.75f, false) {
            private static final long serialVersionUID = 0L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return this.size() > FIFOCache.this.maxSize;
            }
        };
    }

    @Override
    public int getCount(K key) {
        return -1;
    }

    /**
     * Change the maximum size of the table.
     * This may result in removal of entries in the table.
     * @param size the new desired size
     */
    @Override
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
        while (backing.size() > maxSize) {
            try {
                Iterator<K> i = backing.keySet().iterator();
                if (i.hasNext()) {
                    i.next();
                    i.remove();
                } else {
                    break;
                }
            } catch (Exception e) {
                log.warn("Exception during eviction", e);
                break;
            }
        }
    }


    @Override
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


    @Override
    public void config(Map<String, String> map) {
        // needs no configuration.
        if (! map.isEmpty()) {
            log.warn("Unknown configuration parameters: " + map);
        }

    }

    @Override
    public Optional<ReadWriteLock> getLock() {
        return Optional.of(rwLock);
    }

    // wrapping for thread-safety with read/write locks
    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return backing.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        rwLock.readLock().lock();
        try {
            return backing.isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        rwLock.readLock().lock();
        try {
            return backing.containsKey(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        rwLock.readLock().lock();
        try {
            return backing.containsValue(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public V get(Object key) {
        rwLock.readLock().lock();
        try {
            return backing.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            V result = backing.put(key, value);
            // LinkedHashMap's removeEldestEntry handles eviction automatically
            return result;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public V remove(Object key) {
        rwLock.writeLock().lock();
        try {
            return backing.remove(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        rwLock.writeLock().lock();
        try {
            backing.putAll(map);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        rwLock.writeLock().lock();
        try {
            backing.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        return new ReadWriteLockAbstractSet<>(rwLock, backing.keySet());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ReadWriteLockAbstractSet<>(rwLock, backing.entrySet());
    }

    @Override
    public Collection<V> values() {
        return new ReadWriteLockAbstractCollection<>(rwLock, backing.values());
    }


}
