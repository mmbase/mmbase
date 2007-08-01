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
 * @version $Id: LRUCache.java,v 1.1 2007-08-01 06:33:15 michiel Exp $
 * @see    org.mmbase.cache.Cache
 * @since MMBase-1.9
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
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LRUCache.this.maxSize;
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
        if (size < 1 ) throw new IllegalArgumentException("Cannot set size of  to non-positive value");
        maxSize = size;
        while (size() > maxSize()) {
            try {
                Iterator i = entrySet().iterator();
                i.next();
                i.remove();
            } catch (Exception e) {
                // ConcurentModification?
            }
        }
    }
 

    public int maxSize() {
        return maxSize;
    }
 
    /**
     * Returns size, maxSize.
     */
    public String toString() {
        return "Size=" + size() + ", Max=" + maxSize;
    }


    public void config(Map<String, String> map) {
        // needs no configuration.
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
