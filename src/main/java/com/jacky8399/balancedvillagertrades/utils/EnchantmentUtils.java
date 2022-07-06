package com.jacky8399.balancedvillagertrades.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.Objects;

public class EnchantmentUtils {
    private static Enchantment getEnchantment(String enchantmentKey) {
        var key = Objects.requireNonNull(NamespacedKey.fromString(enchantmentKey), "Invalid key " + enchantmentKey);
        return Objects.requireNonNull(Registry.ENCHANTMENT.get(key), "Invalid enchantment " + enchantmentKey);
    }

    public record Range(int from, int to) {}

    // Minecraft wiki
    public static Range getEnchantmentPrice(int level, boolean isTreasure) {
        var range = switch (level) {
            case 1 -> new Range(5, 19);
            case 2 -> new Range(8, 32);
            case 3 -> new Range(11, 45);
            case 4 -> new Range(14, 58);
            case 5 -> new Range(17, 71);
            default -> throw new IllegalArgumentException("level");
        };
        return isTreasure ? new Range(range.from() * 2, range.to() * 2) : range;
    }

    public static boolean isTreasure(String enchantmentKey) {
        var enchantment = getEnchantment(enchantmentKey);
        return enchantment.isTreasure();
    }
}
