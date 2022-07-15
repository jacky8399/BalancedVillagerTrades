package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.Pair;
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
public abstract class MapField<T, K, V> implements ContainerField<T, Map<K, V>> {
    private final Function<T, Map<K, V>> getter;
    private final BiConsumer<T, Map<K, V>> setter;
    private final Class<V> valueType;
    private final Class<K> keyType;

    public MapField(Class<K> keyType, Class<V> valueType, Function<T, Map<K, V>> getter, BiConsumer<T, Map<K, V>> setter) {
        this.getter = getter;
        this.setter = setter;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public abstract K getKeyByString(String stringKey);
    abstract String getStringFromKey(K key);

    SimpleField<Map<K, V>, Integer> getSizeField(){
        return Field.readOnlyField(Integer.class, Map::size);
    }

    SimpleField<Map<K,V>,?> getKeyByIndexField(int index){
        return new SimpleField<>(keyType,
                map -> getPairByIndex(map, index).getKey()
                , (map, newValue) -> {
        });
    }
    SimpleField<Map<K,V>,V> getValueField(K key){
        return new SimpleField<>(valueType,
                map -> map.get(key),
                (map, newValue) -> map.put(key, newValue));
    }

    Pair<K,V> getPairByIndex(Map<K,V> map, int index){
        if (map.size() <= index)
            return null;
        var iterator = map.entrySet().iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        K key = iterator.next().getKey();

        return new Pair<>(key, map.get(key), map);
    }

    @Override
    public @Nullable SimpleField<Map<K, V>, ?> getField(String fieldName) {
        if ("size".equals(fieldName))
            return getSizeField();
        try {
            int index = Integer.parseInt(fieldName);
            // allow numeric indices to get the key
            if (index >= 0) {
                return getKeyByIndexField(index);
            }
        } catch (NumberFormatException ignored) {}

        K key = getKeyByString(fieldName);
        return key != null ? getValueField(key) : null;
    }

    @Override
    public @Nullable Collection<String> getFields(T owner) {
        if (owner == null)
            return Collections.singletonList("size");
        return get(owner).keySet().stream()
                .map(this::getStringFromKey)
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
        K translatedKey = getKeyByString(key);
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
