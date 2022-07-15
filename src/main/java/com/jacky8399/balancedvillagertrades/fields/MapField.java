package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapField<T, K, V> implements ContainerField<T, Map<K, V>> {
    private final Function<T, Map<K, V>> getter;
    private final BiConsumer<T, Map<K, V>> setter;
    private final Function<String, K> keyTranslator;
    private final Class<V> valueType;
    private final Function<K, String> keyGetter;
    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter,
                    Function<String, @Nullable K> keyTranslator, Class<V> valueType) {
        this(getter, setter, keyTranslator, null, valueType);
    }

    public MapField(Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter,
                    Function<String, @Nullable K> keyTranslator, Function<K, String> keyGetter, Class<V> valueType) {
        this.getter = getter;
        this.setter = setter;
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
    public @Nullable SimpleField<Map<K, V>, ?> getField(String fieldName) {
        if ("size".equals(fieldName))
            return (SimpleField) SIZE_FIELD;
        try {
            int index = Integer.parseInt(fieldName);
            // allow numeric indices to get the key
            if (keyGetter != null && index >= 0) {
                return new SimpleField<>(String.class, map -> {
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
        return key != null ? new SimpleField<>(valueType, map -> map.get(key), (map, newValue) -> map.put(key, newValue)) : null;
    }

    @Override
    public @Nullable Collection<String> getFields(T owner) {
        if (owner == null)
            return Collections.singletonList("size");
        return get(owner).keySet().stream()
                .map(keyGetter != null ? keyGetter : Objects::toString)
                .collect(Collectors.toList());
    }
    @Override
    public Map<K, V> get(T t) {
        return getter.apply(t);
    }

    @Override
    public void set(T t, Map<K, V> value) {
        setter.accept(t, value);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Class<Map<K, V>> getFieldClass() {
        return (Class) Map.class;
    }


    private static final Pattern MAP_PATTERN = Pattern.compile("^contains\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    @Override
    public @NotNull BiPredicate<TradeWrapper, Map<K, V>> parsePredicate(String input) throws IllegalArgumentException {
        Matcher matcher = MAP_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can only check for keys in a map");
        }
        String key = matcher.group(1);
        K translatedKey = translateKey(key);
        if (translatedKey == null)
            throw new IllegalArgumentException("Invalid key " + key);
        return (ignored, map) -> map.containsKey(translatedKey);
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, Map<K, V>, Map<K, V>> parseTransformer(String input) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot modify a map");
    }

    @Override
    public String toString() {
        return "MapField";
    }
}
