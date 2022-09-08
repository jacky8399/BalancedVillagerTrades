package utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DangerousMocks {
    public static ItemMeta mockEnchants(Map<Enchantment, Integer> enchants) {
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        when(meta.hasEnchants()).thenReturn(true);
        when(meta.hasEnchant(any()))
                .thenAnswer(invocation -> enchants.containsKey(invocation.getArgument(0, Enchantment.class)));
        when(meta.getEnchants()).thenReturn(enchants);
        when(meta.getEnchantLevel(any()))
                .thenAnswer(invocation -> enchants.getOrDefault(invocation.getArgument(0, Enchantment.class), 0));
        when(meta.hasConflictingEnchant(any()))
                .thenAnswer(invocation -> {
                    Enchantment other = invocation.getArgument(0, Enchantment.class);
                    return enchants.keySet().stream().anyMatch(other::conflictsWith);
                });

        return meta;
    }
    public static EnchantmentStorageMeta mockStoredEnchants(Map<Enchantment, Integer> enchants) {
        EnchantmentStorageMeta meta = Mockito.mock(EnchantmentStorageMeta.class);
        when(meta.hasStoredEnchants()).thenReturn(true);
        when(meta.hasStoredEnchant(any()))
                .thenAnswer(invocation -> enchants.containsKey(invocation.getArgument(0, Enchantment.class)));
        when(meta.getStoredEnchants()).thenReturn(enchants);
        when(meta.getStoredEnchantLevel(any()))
                .thenAnswer(invocation -> enchants.getOrDefault(invocation.getArgument(0, Enchantment.class), 0));
        when(meta.hasConflictingStoredEnchant(any()))
                .thenAnswer(invocation -> {
                    Enchantment other = invocation.getArgument(0, Enchantment.class);
                    return enchants.keySet().stream().anyMatch(other::conflictsWith);
                });

        return meta;
    }
}
