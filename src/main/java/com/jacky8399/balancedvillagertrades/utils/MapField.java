package com.jacky8399.balancedvillagertrades.utils;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapField<T, K, V> extends ComplexField<T, Map<K, V>> {
    public final Function<String, K> keyTranslator;
    public final Class<V> valueType;
    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter, Function<String, K> keyTranslator, Class<V> valueType) {
        super((Class) Map.class, getter, setter);
        this.keyTranslator = keyTranslator;
        this.valueType = valueType;
    }

    public K translateKey(String key) {
        return keyTranslator.apply(key);
    }

    private static final Field<Map<?, ?>, Integer> SIZE_FIELD = new Field<>(Integer.class, Map::size, (map, newSize)->{});
    @Override
    public @Nullable Field<Map<K, V>, ?> getField(String fieldName) {
        if ("size".equals(fieldName))
            return (Field) SIZE_FIELD;
        K key = translateKey(fieldName);
        return key != null ? new Field<>(valueType, map -> map.get(key), (map, newValue) -> map.put(key, newValue)) : null;
    }
}
