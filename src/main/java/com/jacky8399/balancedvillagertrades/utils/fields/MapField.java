package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapField<T, K, V> extends ComplexField<T, Map<K, V>> {
    public final Function<String, K> keyTranslator;
    public final Class<V> valueType;
    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter, Function<String, @Nullable K> keyTranslator, Class<V> valueType) {
        super((Class) Map.class, getter, setter);
        this.keyTranslator = keyTranslator;
        this.valueType = valueType;
    }

    @Nullable
    public K translateKey(String key) {
        return keyTranslator.apply(key);
    }

    private static final Field<Map<?, ?>, Integer> SIZE_FIELD = Field.readOnlyField(Integer.class, Map::size);
    @Override
    public @Nullable Field<Map<K, V>, ?> getField(String fieldName) {
        if ("size".equals(fieldName))
            return (Field) SIZE_FIELD;
        K key = translateKey(fieldName);
        return key != null ? new Field<>(valueType, map -> map.get(key), (map, newValue) -> map.put(key, newValue)) : null;
    }

    @Override
    public @Nullable Collection<String> getFields(T owner) {
        return Stream.concat(Stream.of("size"), get(owner).keySet().stream().map(Objects::toString))
                .collect(Collectors.toList());
    }
}