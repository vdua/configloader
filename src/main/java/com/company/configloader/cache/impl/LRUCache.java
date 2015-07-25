package com.company.configloader.cache.impl;

import com.company.configloader.cache.Cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class LRUCache implements Cache {

    private class LRULinkedHashMap<E,V> extends LinkedHashMap<E,V> {
        private int maximumSize;

        public LRULinkedHashMap() {
            this(initialSize);
        }

        public LRULinkedHashMap(int size) {
            super(initialSize, 0.75f, true);
            maximumSize = size;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maximumSize;
        }

    }

    private static int initialSize = 100;
    private Map<String, Object> hashMap;

    public LRUCache() {
        this(initialSize);
    }

    public LRUCache(int size) {
        hashMap = Collections.synchronizedMap(new LRULinkedHashMap<String, Object>(size));
    }

    @Override
    public Object get(String key) {
        return hashMap.get(key);
    }

    @Override
    public void put(String key, Object obj) {
        hashMap.put(key, obj);
    }
}