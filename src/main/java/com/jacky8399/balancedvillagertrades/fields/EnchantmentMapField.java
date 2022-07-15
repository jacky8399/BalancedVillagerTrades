package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.EnchantmentPair;
import com.jacky8399.balancedvillagertrades.utils.Pair;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EnchantmentMapField extends MapField<ItemStack, Enchantment, Integer>{

    public EnchantmentMapField(Function<ItemStack, Map<Enchantment, Integer>> getter, BiConsumer<ItemStack, Map<Enchantment, Integer>> setter) {
        super(Enchantment.class, Integer.class, getter, setter);
    }

    @Override
    public Enchantment getKeyByString(String stringKey) {
        return Enchantment.getByKey(NamespacedKey.fromString(stringKey));
    }

    @Override
    String getStringFromKey(Enchantment key) {
        return key.getKey().toString();
    }

    @Override
    SimpleField<Map<Enchantment, Integer>, ?> getKeyByIndexField(int index) {
        return new EnchantmentField<Map<Enchantment, Integer>>(
                map -> EnchantmentPair.fromPair(getPairByIndex(map, index)),
                (map, newValue) -> {
                    Pair<Enchantment, Integer> pair = getPairByIndex(map, index);

                    map.remove(pair.getKey());

                    map.put(newValue.getKey(), newValue.getValue());
                }
        );
    }

    @Override
    SimpleField<Map<Enchantment, Integer>, ?> getValueByStringField(String keyString) {
        return new EnchantmentField<Map<Enchantment, Integer>>(
                map -> {
                    if(keyString == null || keyString.isEmpty())
                        return new EnchantmentPair(null, 0, map);

                    Enchantment key = getKeyByString(keyString);
                    Integer value = map.get(key);

                    return new EnchantmentPair(key, value, map);
                },
                (map, newValue) -> {
                    if(keyString != null && !keyString.isEmpty()) {
                        Enchantment key = getKeyByString(keyString);

                        map.remove(key);

                        map.put(key, newValue.getValue());
                    }
                });
    }
}
