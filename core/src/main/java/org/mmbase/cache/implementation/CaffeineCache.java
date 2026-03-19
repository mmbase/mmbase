package org.mmbase.cache.implementation;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.*;
import org.mmbase.cache.CacheImplementationInterface;

public class CaffeineCache<K, V> implements CacheImplementationInterface<K, V> {

    com.github.benmanes.caffeine.cache.Cache<K, V> backing ;
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
        this.backingAsMap = backing.asMap();
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
        return backing.getIfPresent((K) key);
    }

    @Override
    public V put(K key, V value) {
        return backingAsMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return backingAsMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        backing.putAll(m);
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
}
