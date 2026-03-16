/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache.implementation;

import org.mmbase.cache.CacheImplementationInterface;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.mmbase.util.ReadWriteLockAbstractCollection;
import org.mmbase.util.ReadWriteLockAbstractSet;
import org.mmbase.util.logging.*;

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

    public LRUCache() {
        this(100);
    }

    public LRUCache(int size) {
        maxSize = size;
        // Access-order LinkedHashMap: get() is a structural modification
        // All access must be protected by the ReadWriteLock
        backing = new LinkedHashMap<K, V>(size, 0.75f, true) {
            private static final long serialVersionUID = 0L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                int overSized = size() - LRUCache.this.maxSize;
                if (overSized <= 0) {
                    return false;
                } else if (overSized == 1) {
                    // Using iterator to manualy remove the eldest rather then return true to make absolutely sure that one
                    // disappears, because that seems to fail sometimes for QueryResultCache.

                    final Iterator<K> i = keySet().iterator();
                    K actualEldest = i.next();
                    i.remove();
                    overSized = size() - LRUCache.this.maxSize;
                    while (overSized > 0) {
                        // if for some reason a key changed in the cache, even 1 i.remove may not
                        // shrink the cache.
                        log.warn("cache didn't shrink (a)" + eldest.getKey() + " [" + eldest.getKey().getClass() + "] [" + eldest.getKey().hashCode() + "]");
                        log.warn("cache didn't shrink (b)" + actualEldest + " [" + actualEldest.getClass() + "] [" + actualEldest.hashCode() + "]");
                        actualEldest = i.next();
                        i.remove();
                        overSized = size() - LRUCache.this.maxSize;
                    }
                    assert overSized <= 0;
                    return false;
                } else {
                    log.warn("How is this possible? Oversized: " + overSized);
                    log.debug("because", new Exception());
                    if (overSized > 10) {
                        log.error("For some reason this cache grew much too big (" + size() + " >> " + LRUCache.this.maxSize + "). This must be some kind of bug. Resizing now.");
                        clear();
                    }
                    return false;
                }
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
            while (backing.size() > maxSize) {
                try {
                    Iterator<K> i = backing.keySet().iterator();
                    i.next();
                    i.remove();
                } catch (Exception e) {
                    log.warn(e);
                    // ConcurentModification?
                }
            }
        } finally {
            rwLock.writeLock().unlock();
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

    public Optional<ReadWriteLock> getLock() {
        return Optional.of(rwLock);
    }

    // All methods need synchronization via ReadWriteLock.
    // Because the backing LinkedHashMap uses access-order mode,
    // even get() is a structural modification and requires a write lock.
    public int size() {
        rwLock.readLock().lock();
        try {
            return backing.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public boolean isEmpty() {
        rwLock.readLock().lock();
        try {
            return backing.isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public boolean containsKey(Object key) {
        rwLock.readLock().lock();
        try {
            return backing.containsKey(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public boolean containsValue(Object value) {
        rwLock.readLock().lock();
        try {
            return backing.containsValue(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    // get() modifies access order in this LinkedHashMap, so it needs a write lock
    public V get(Object key) {
        rwLock.writeLock().lock();
        try {
            return backing.get(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public V put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            return backing.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public V remove(Object key) {
        rwLock.writeLock().lock();
        try {
            return backing.remove(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void putAll(Map<? extends K, ? extends V> map) {
        rwLock.writeLock().lock();
        try {
            backing.putAll(map);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void clear() {
        rwLock.writeLock().lock();
        try {
            backing.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public Set<K> keySet() {
        return new ReadWriteLockAbstractSet<>(rwLock.writeLock(), rwLock.writeLock(), backing.keySet());
    }
    public Set<Map.Entry<K,V>> entrySet() {
        return new ReadWriteLockAbstractSet<>(rwLock.writeLock(), rwLock.writeLock(),  backing.entrySet());
    }
    public Collection<V> values() {
        return new ReadWriteLockAbstractCollection<>(rwLock.writeLock(), rwLock.writeLock(), backing.values());
    }


}
