package org.mmbase.cache.implementation;

import java.io.Serializable;
import java.util.*;

import org.mmbase.cache.CacheImplementationInterface;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache backed by <a href="https://github.com/ben-manes/caffeine/wiki/Eviction#size-based">Caffeine</a> cache.
 * This is very well optimized.
 * @since MMBase-1.9.7
 */
@SuppressWarnings("unchecked")
public class CaffeineCache<K, V> implements CacheImplementationInterface<K, V> {

    com.github.benmanes.caffeine.cache.Cache<K, Value<V>> backing ;
    Map<K, V> backingAsMap ;
    private int maxSize;

    public CaffeineCache() {
        this(100);
    }

    public CaffeineCache(int size) {
        this.maxSize = size;
        buildNew();
    }

    @Override
    public void setMaxSize(int size) {
        this.maxSize = size;
        buildNew();
    }

    void buildNew() {
        this.backing = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .build();
        this.backingAsMap = new AbstractMap<K, V>() {
            private final Map<K, Value<V>> wrapped = backing.asMap();

            @Override
            public boolean containsKey(Object key) {
                return wrapped.containsKey(key);
            }
            @Override
            public Set<K> keySet() {
                return wrapped.keySet();
            }
            @Override
            public V put(K key, V value) {
                Value<V> v = wrapped.put(key, new Value<>(value));
                return v == null ? null : v.value;
            }
            @Override
            public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<Entry<K, V>>() {
                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        final Iterator<Entry<K, Value<V>>> i = wrapped.entrySet().iterator();
                        return new Iterator<Entry<K, V>>() {
                            @Override
                            public boolean hasNext() {
                                return i.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {
                                Map.Entry<K, Value<V>> e = i.next();
                                return new SimpleEntry<>(e.getKey(), e.getValue().value);
                            }
                            @Override
                            public void remove() {
                                i.remove();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return wrapped.size();
                    }
                };
            }
        };
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public int getCount(K key) {
        return -1;
    }

    @Override
    public void config(Map<String, String> configuration) {

    }

    @Override
    public int size() {
        return backingAsMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingAsMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backingAsMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backingAsMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        Value<V> v = backing.getIfPresent((K) key);
        if (v == null) {
            return null;
        }
        return v.value;
    }

    @Override
    public V put(K key, V value) {
        V containingValue = get(key);
        backing.put(key, new Value<>(value));
        return containingValue;
    }

    @Override
    public V remove(Object key) {
        V containingValue = get(key);
        backing.invalidate((K)key);
        return  containingValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        backing.invalidateAll();
    }

    @Override
    public Set<K> keySet() {
        return backingAsMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return backingAsMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return backingAsMap.entrySet();
    }

    public String toString() {
        return "CaffeineCache " + maxSize + " ";
    }

    public static class Value<V> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final V value;

        public Value(V value) {
            this.value = value;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Value)) return false;

            Value<?> value1 = (Value<?>) o;
            return Objects.equals(value, value1.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }
}
