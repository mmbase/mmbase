/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache.implementation;

import org.mmbase.cache.CacheImplementationInterface;
import java.util.*;
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

    public LRUCache() {
        this(100);
    }

    public LRUCache(int size) {
        maxSize = size;
        // caches can typically be accessed/modified by multipible thread, so we need to synchronize
        backing = Collections.synchronizedMap(new LinkedHashMap<K, V>(size, 0.75f, true) {
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
        });
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
        maxSize = size;
        synchronized(backing) {
            while (size() > maxSize) {
                try {
                    Iterator<K> i = keySet().iterator();
                    i.next();
                    i.remove();
                } catch (Exception e) {
                    log.warn(e);
                    // ConcurentModification?
                }
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
        return backing;
    }

    // wrapping for synchronization
    public int size() { return backing.size(); }
    public boolean isEmpty() { return backing.isEmpty();}
    public boolean containsKey(Object key) { return backing.containsKey(key);}
    public boolean containsValue(Object value){ return backing.containsValue(value);}
    public V get(Object key) { return backing.get(key);}
    public V put(K key, V value) { return backing.put(key, value);}
    public V remove(Object key) { return backing.remove(key);}
    public void putAll(Map<? extends K, ? extends V> map) { backing.putAll(map); }
    public void clear() { backing.clear(); }
    public Set<K> keySet() { return backing.keySet(); }
    public Set<Map.Entry<K,V>> entrySet() { return backing.entrySet(); }
    public Collection<V> values() { return backing.values();}


}
