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

    public V getValue() {
        return value;
    }
}