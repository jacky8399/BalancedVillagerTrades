package com.jacky8399.balancedvillagertrades.utils;

import io.papermc.paper.configuration.type.fallback.FallbackValue;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

public class EnchantmentPair extends Pair<Enchantment, Integer> {
    public EnchantmentPair(Enchantment key, Integer value, Map<Enchantment, Integer> map) {
        super(key, value, map);
    }

    public static EnchantmentPair fromPair(Pair<Enchantment, Integer> pair){
        if(pair == null)
            return null;
        return new EnchantmentPair(pair.getKey(), pair.getValue(), pair.getMap());
    }
}
