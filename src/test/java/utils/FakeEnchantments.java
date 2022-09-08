package utils;

import io.papermc.paper.enchantments.EnchantmentRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("ConstantConditions")
public class FakeEnchantments {

    public static final Enchantment SILK_TOUCH = createEnchantment("silk_touch", false, false, "fortune");
    public static final Enchantment FORTUNE = createEnchantment("fortune", false, false, "silk_touch");

    public static final Enchantment MENDING = createEnchantment("mending", true, false, "infinity");
    public static final Enchantment VANISHING_CURSE = createEnchantment("vanishing_curse", false, true);

    public static Enchantment createEnchantment(String key, boolean isTreasure, boolean isCurse, String... conflictingEnchantments) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        Set<NamespacedKey> conflicts = Arrays.stream(conflictingEnchantments)
                .map(NamespacedKey::fromString)
                .collect(Collectors.toSet());
        Enchantment enchantment = new Enchantment(namespacedKey) {
            @Override
            public @NotNull String getName() {
                return key;
            }

            @Override
            public int getMaxLevel() {
                return 5;
            }

            @Override
            public int getStartLevel() {
                return 1;
            }

            @Override
            public @NotNull EnchantmentTarget getItemTarget() {
                return null;
            }

            @Override
            public boolean isTreasure() {
                return isTreasure;
            }

            @Override
            public boolean isCursed() {
                return isCurse;
            }

            @Override
            public boolean conflictsWith(@NotNull Enchantment other) {
                return this == other || conflicts.contains(other.getKey());
            }

            @Override
            public boolean canEnchantItem(@NotNull ItemStack item) {
                return false;
            }

            @Override
            public @NotNull Component displayName(int level) {
                return null;
            }

            @Override
            public boolean isTradeable() {
                return false;
            }

            @Override
            public boolean isDiscoverable() {
                return false;
            }

            @Override
            public @NotNull EnchantmentRarity getRarity() {
                return null;
            }

            @Override
            public float getDamageIncrease(int level, @NotNull EntityCategory entityCategory) {
                return 0;
            }

            @Override
            public @NotNull Set<EquipmentSlot> getActiveSlots() {
                return null;
            }

            @Override
            public @NotNull String translationKey() {
                return null;
            }
        };
        Enchantment.registerEnchantment(enchantment);
        return enchantment;
    }
}
