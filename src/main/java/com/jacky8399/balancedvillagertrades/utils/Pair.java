package com.jacky8399.balancedvillagertrades.utils;

import java.util.Map;

public class Pair<K,V> {
    K key;
    V value;
    Map<K,V> map;

    public Pair(K key, V value, Map<K,V> map) {
        this.key = key;
        this.value = value;
        this.map = map;
    }

    public K getKey() {
        return key;
    }

    public void putKey(K key){
        if(this.key != null)
            map.remove(this.key);
        map.put(key, this.value);
        this.key = key;
    }

    public void putValue(V value){
        if(this.key != null)
            map.put(this.key, value);
        this.value = value;
    }

    public Map<K, V> getMap() {
        return map;
    }

    public V getValue() {
        return value;
    }
}