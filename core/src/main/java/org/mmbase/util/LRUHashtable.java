/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;
import org.mmbase.cache.CacheImplementationInterface;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A hashtable which has a maximum of entries.  Old entries are
 * removed when the maximum is reached.  This table is used mostly to
 * implement a simple caching system.
 *
 * @move consider moving to org.mmbase.cache
 * @author  Rico Jansen
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @see    org.mmbase.cache.Cache
 * @deprecated use org.mmbase.cache.implementation.LRUCache
 */
public class LRUHashtable<K, V> implements Cloneable, CacheImplementationInterface<K, V>, SizeMeasurable {

    private static final Logger log = Logging.getLoggerInstance(LRUHashtable.class);

    private final Hashtable<K, LRUEntry> backing;
    /**
     * First (virtual) element of the table.
     * The element that follows root is the oldest element in the table
     * (and thus first to be removed if size maxes out).
     */
    private final LRUEntry root     = new LRUEntry();
    /**
     * Last (virtual) element of the table.
     * The element that precedes dangling is the latest element in the table
     */
    private final LRUEntry dangling = new LRUEntry();

    /**
     * Maximum size (capacity) of the table
     */
    private int maxSize = 0;

    /**
     * Creates the URL Hashtable.
     * @param size the maximum capacity
     * @param cap the starting capacity (used to improve performance)
     * @param lf the amount with which current capacity frows
     */
    public LRUHashtable(int size, int cap, float lf) {
        backing = new Hashtable<K, LRUEntry>(cap, lf);
        root.next = dangling;
        dangling.prev = root;
        this.maxSize = size;
    }

    /**
     * Creates the URL Hashtable with growing capacity 0.75.
     * @param size the maximum capacity
     * @param cap the starting capacity (used to improve performance)
     */
    public LRUHashtable(int size, int cap) {
        this(size, cap, 0.75f);
    }

    /**
     * Creates the URL Hashtable with starting capacity 101 and
     * growing capacity 0.75.
     * @param size the maximum capacity
     */
    public LRUHashtable(int size) {
        this(size, 101, 0.75f);
    }

    /**
     * Creates the URL Hashtable with maximum capacity 100,
     * starting capacity 101, and growing capacity 0.75.
     */
    public LRUHashtable() {
        this(100, 101, 0.75f);
    }


    /**
     * Store an element in the table.
     * @param key the key of the element
     * @param value the value of the element
     * @return the original value of the element if it existed, <code>null</code> if it could not be found
     */
    public  V put(K key, V value) {
        synchronized(backing) {
            LRUEntry work = backing.get(key);
            V rtn;
            if (work != null) {
                rtn = work.value;
                work.value = value;
                removeEntry(work);
                appendEntry(work);
            } else {
                rtn = null;
                work = new LRUEntry(key, value);
                backing.put(key, work);
                appendEntry(work);
                if (backing.size() > maxSize) {
                    K remove = root.next.key;
                    Object was =  remove(remove);
                    assert was != null;
                    if (was == null) {
                        log.warn("Nothing was removed, while that was expected " + remove + " should have been removed");
                    }
                }
            }
            return rtn;

        }
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        for (Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }
    public boolean containsValue(Object o) {
        return values().contains(o);
    }
    public boolean containsKey(Object o) {
        return backing.containsKey(o);
    }
    public boolean isEmpty() {
        return backing.isEmpty();
    }
    /**
     * Retrieves the count of the object with a certain key.
     * @param key the key of the element
     * @return the times the key has been requested
     */
    public int getCount(Object key) {
        LRUEntry work = backing.get(key);
        if (work != null) {
            return work.requestCount;
        } else {
            return -1;
        }
    }

    /**
     * Retrieves an element from the table.
     * @param key the key of the element
     * @return the value of the element, or <code>null</code> if it could not be found
     */
    public V get(Object key) {
        synchronized(backing) {
            LRUEntry work =  backing.get(key);
            if (work != null) {
                work.requestCount++;
                V rtn = work.value;
                removeEntry(work);
                appendEntry(work);
                return rtn;
            } else {
                return null;
            }
        }
    }

    /**
     * Remove an element from the table.
     * @param key the key of the element
     * @return the original value of the element if it existed, <code>null</code> if it could not be found
     */
    public V remove(Object key) {
        synchronized(backing) {
            LRUEntry work = backing.remove(key);
            if (work != null) {
                V rtn = work.value;
                removeEntry(work);
                return rtn;
            } else {
                return null;
            }
        }
    }


    /**
     * You should only remove entries from LRUHashtable using the 'remove' function, or using the
     * iterator of entrySet() otherwise the linked list gets messed up.  The keySet of LRUHashtable
     * therefore returns an unmodifiable set.
     * @since MMBase-1.6.3
     */
    public Set<K> keySet() {
        return Collections.unmodifiableSet(backing.keySet());
    }

    /**
     * Returns the entries of this Map. Modification are reflected.
     *
     * @since MMBase-1.6.3
     */
    public Set<Map.Entry<K, V>> entrySet() {
        //throw new UnsupportedOperationException();
        return new LRUEntrySet();
    }

    /**
     * @see   #keySet
     * @since MMBase-1.6.3
     */
    public Collection<V> values() {
        return new LRUValues();
    }

    /**
     * Return the current size of the table
     */
    public int size() {
        return backing.size();
    }

    /**
     * Change the maximum size of the table.
     * This may result in removal of entries in the table.
     * @param size the new desired size
     */
    public void setMaxSize(int size) {
        if (size < 0 ) throw new IllegalArgumentException("Cannot set size of LRUHashtable to negative value");
        if (size < maxSize) {
            while(size() > maxSize) {
                remove(root.next.key);
            }
        }
        maxSize = size;
    }

    /**
     * Return the maximum size of the table
     */
    public int maxSize() {
        return maxSize;
    }

    /**
     * Append an entry to the end of the list.
     */
    private void appendEntry(LRUEntry wrk) {
        dangling.prev.next = wrk;
        wrk.prev = dangling.prev;
        wrk.next = dangling;
        dangling.prev = wrk;
    }

    /**
     * remove an entry from the list.
     */
    private void removeEntry(LRUEntry wrk) {
        wrk.next.prev = wrk.prev;
        wrk.prev.next = wrk.next;
        wrk.next = null;
        wrk.prev = null;
    }

    /**
     * Returns a description of the table.
     * The information shown includes current size, maximum size, ratio of misses and hits,
     * and a description of the underlying hashtable
     */
    public String toString() {
        return "Size=" + size() + ", Max=" + maxSize;
    }

    /**
     * Returns a description of the table.
     * The information shown includes current size, maximum size, ratio of misses and hits,
     * and either a description of the underlying hashtable, or a list of all stored values.
     * @param which if <code>true</code>, the stored values are described.
     * @return a description of the table.
     */
    public String toString(boolean which) {
        if (which) {
            StringBuilder b = new StringBuilder();
            b.append("Size " + size() + ", Max " + maxSize + " : ");
            b.append(super.toString());
            return b.toString();
        } else {
            return toString();
        }
    }

    /**
     * Clears the table.
     */
    public void clear() {
        synchronized(backing) {
            while (root.next != dangling) removeEntry(root.next);
            backing.clear();
        }
    }

    /**
     * NOT IMPLEMENTED
     */
    public Object clone() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an <code>Enumeration</code> on the table's element values.
     */
    public Enumeration<V> elements() {
        return new LRUHashtableEnumeration();
    }



    /**
     * @deprecated use getOrderedEntries
     */
    public Enumeration<V> getOrderedElements() {
        return getOrderedElements(-1);
    }

    /**
     * @deprecated use getOrderedEntries
     */
    public Enumeration<V> getOrderedElements(int maxnumber) {
        List<V> results = new ArrayList<V>();
        LRUEntry current = root.next;
        if (maxnumber != -1) {
            int i = 0;
            while (current!=null && current!=dangling && i<maxnumber) {
                results.add(0, current.value);
                current = current.next;
                i++;
            }
        } else {
            while (current!=null && current!=dangling) {
                results.add(0, current.value);
                current = current.next;
            }
        }
        return Collections.enumeration(results);
    }

    /**
     * Returns an ordered list of Map.Entry's.
     *
     * @since MMBase-1.6
     */

    public List<? extends Map.Entry<K, V>> getOrderedEntries() {
        return getOrderedEntries(-1);
    }

    /**
     * Returns an ordered list of Map.Entry's. This can be used to
     * present the contents of the LRU Map.
     *
     * @since MMBase-1.6
     */

    public List<? extends Map.Entry<K, V>> getOrderedEntries(int maxNumber) {
        List<Map.Entry<K,V>> results = new ArrayList<Map.Entry<K,V>>();
        LRUEntry current = root.next;
        int i = 0;
        while (current != null && current != dangling && (maxNumber < 0 || i < maxNumber)) {
            results.add(0, current);
            current = current.next;
            i++;
        }
        return Collections.unmodifiableList(results);
    }


    public void config(Map<String, String> map) {
        // lru needs no configuration.
    }



    public int getByteSize() {
        return getByteSize(new SizeOf());
    }
    public int getByteSize(SizeOf sizeof) {
        int len = 4 * SizeOf.SZ_REF + (30 + 5 * SizeOf.SZ_REF) * size();  // 30:overhead of Hashtable, 5*SZ_REF: overhead of LRUEntry
        LRUEntry current = root.next;
        while (current != null && current != dangling) {
            current = current.next;
            len += sizeof.sizeof(current.key);
            len += sizeof.sizeof(current.value);
        }
        return len;
    }

    /**
     * Enumerator for the LRUHashtable.
     */
    private class LRUHashtableEnumeration implements Enumeration<V> {
        private Enumeration<V> superior;

        LRUHashtableEnumeration() {
            superior = LRUHashtable.this.elements();
        }

        public boolean hasMoreElements() {
            return superior.hasMoreElements();
        }

        public V nextElement() {
            LRUEntry entry = (LRUEntry) superior.nextElement();
            return entry.value;
        }
    }


    /**
     * Element used to store information from the LRUHashtable.
     */
    public class LRUEntry implements Map.Entry<K, V>, SizeMeasurable {
        /**
         * The element value
         */
        protected V value;
        /**
         * The next, newer, element
         */
        protected LRUEntry next;
        /**
         * The previous, older, element
         */
        protected LRUEntry prev;
        /**
         * The element key
         */
        protected K key;
        /**
         * the number of times this
         * entry has been requested
         */
        protected int requestCount = 0;

        LRUEntry() {
            this(null, null);
        }
        LRUEntry(K key, V val) {
            this(key, val, null, null);
        }

        LRUEntry(K key, V value, LRUEntry prev, LRUEntry next) {
            this.value = value;
            this.next  = next;
            this.prev  = prev;
            this.key   = key;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V o) {
            throw new UnsupportedOperationException("Cannot change values in LRU Hashtable");
        }

        public int getByteSize() {
            return new SizeOf().sizeof(value);
        }
        public int getByteSize(SizeOf sizeof) {
            return 20 + // 5 references
                sizeof.sizeof(value);
        }
        public String toString() {
            return  key + "=" + (value == LRUHashtable.this ? "[this lru]" : String.valueOf(value));
        }

    }
    /**
     * Used by 'entrySet' implementation, to make the Map modifiable.
     * @since MMBase-1.7.2
     */
    protected class LRUEntrySet extends AbstractSet<Map.Entry<K, V>> {
        Set<Map.Entry<K, LRUEntry>> set;
        LRUEntrySet() {
            set = LRUHashtable.this.backing.entrySet();
        }
        public int size() {
            return set.size();
        }
        public Iterator<Map.Entry<K, V>> iterator() {
            return new LRUEntrySetIterator(set.iterator());
        }
    }

    /**
     * Used by 'entrySet' implementation, to make the Map modifiable.
     * @since MMBase-1.7.2
     */
    protected class LRUEntrySetIterator implements Iterator<Map.Entry<K, V>> {
        final Iterator<Map.Entry<K, LRUEntry>> it;
        LRUEntry work;
        LRUEntrySetIterator(Iterator<Map.Entry<K, LRUEntry>> i ) {
            it = i;
        }
        public boolean 	hasNext() {
            return it.hasNext();
        }
        public Map.Entry<K, V> next() {
            Map.Entry<K, LRUEntry> entry =  it.next();
            work = entry.getValue();
            return work;
        }
        public void remove() {
            it.remove();
            if (work != null) {
                LRUHashtable.this.removeEntry(work);
            }
        }
    }
    /**
     * @since MMBase-1.9
     */
    protected class LRUValues extends AbstractCollection<V> {
        final Collection<LRUEntry> col;
        LRUValues() {
            col = LRUHashtable.this.backing.values();
        }
        public int size() {
            return col.size();
        }
        public Iterator<V> iterator() {
            final Iterator<LRUEntry> i = col.iterator();
            return new Iterator<V>() {
                LRUEntry work;
                public boolean hasNext() {
                    return i.hasNext();
                }
                public V next() {
                    work = i.next();
                    return work.getValue();
                }
                public void remove() {
                    i.remove();
                    if (work != null) {
                        LRUHashtable.this.removeEntry(work);
                    }
                }
            };
        }
    }


}
