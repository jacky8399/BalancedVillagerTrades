package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapField<T, K, V> extends ComplexField<T, Map<K, V>> {
    public final Function<String, K> keyTranslator;
    public final Class<V> valueType;
    public final Function<K, String> keyGetter;
    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter,
                    Function<String, @Nullable K> keyTranslator, Class<V> valueType) {
        this(getter, setter, keyTranslator, null, valueType);
    }

    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter,
                    Function<String, @Nullable K> keyTranslator, Function<K, String> keyGetter, Class<V> valueType) {
        super((Class) Map.class, getter, setter);
        this.keyTranslator = keyTranslator;
        this.valueType = valueType;
        this.keyGetter = keyGetter;
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
        try {
            int index = Integer.parseInt(fieldName);
            // allow numeric indices to get the key
            if (keyGetter != null && index >= 0) {
                return new Field<>(String.class, map -> {
                    if (map.size() <= index)
                        return null;
                    var iterator = map.entrySet().iterator();
                    for (int i = 0; i < index; i++) {
                        iterator.next();
                    }
                    return keyGetter.apply(iterator.next().getKey());
                }, (map, newValue) -> {});
            }
        } catch (NumberFormatException ignored) {}
        K key = translateKey(fieldName);
        return key != null ? new Field<>(valueType, map -> map.get(key), (map, newValue) -> map.put(key, newValue)) : null;
    }

    @Override
    public @Nullable Collection<String> getFields(T owner) {
        if (owner == null)
            return Collections.singletonList("size");
        return get(owner).keySet().stream()
                .map(keyGetter != null ? keyGetter : Objects::toString)
                .collect(Collectors.toList());
    }
}
