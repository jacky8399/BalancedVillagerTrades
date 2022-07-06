package com.jacky8399.balancedvillagertrades.utils.fields;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemStackField<T> extends ComplexField<T, ItemStack> {
    public ItemStackField(Function<T, ItemStack> getter, BiConsumer<T, ItemStack> setter) {
        super(ItemStack.class, getter, setter);
    }

    // I'm lazy
    private static final Field<ItemStack, ItemMeta> META_FIELD = FieldAccessor.emptyAccessor(new Field<>(ItemMeta.class, ItemStack::getItemMeta, ItemStack::setItemMeta));

    private static final MapField<ItemStack, Enchantment, Integer> ENCHANTMENT_FIELD = new MapField<>(
            is -> {
                ItemMeta meta = is.getItemMeta();
                return new LinkedHashMap<>(
                        meta instanceof EnchantmentStorageMeta ?
                                ((EnchantmentStorageMeta) meta).getStoredEnchants() :
                                meta.getEnchants()
                );
            },
            (is, newMap) -> {
                ItemMeta meta = is.getItemMeta();
                boolean isEnchantedBook = meta instanceof EnchantmentStorageMeta;
                // remove enchantments
                newMap.values().removeIf(num -> num <= 0);
                Map<Enchantment, Integer> oldEnch = isEnchantedBook ? ((EnchantmentStorageMeta) meta).getStoredEnchants() : meta.getEnchants();
                if (!oldEnch.keySet().equals(newMap.keySet())) {
                    Set<Enchantment> oldEnchSet = new HashSet<>(oldEnch.keySet());
                    oldEnchSet.removeAll(newMap.keySet());
                    oldEnchSet.forEach(isEnchantedBook ? ((EnchantmentStorageMeta) meta)::removeStoredEnchant : meta::removeEnchant);
                }
                newMap.forEach((ench, lvl) -> {
                    if (isEnchantedBook)
                        ((EnchantmentStorageMeta) meta).addStoredEnchant(ench, lvl, true);
                    else
                        meta.addEnchant(ench, lvl, true);
                });
                is.setItemMeta(meta);
            },
            string -> Enchantment.getByKey(NamespacedKey.fromString(string)),
            enchantment -> enchantment.getKey().toString(), Integer.class
    );

    public static final ImmutableMap<String, Field<ItemStack, ?>> ITEM_STACK_FIELDS = ImmutableMap.<String, Field<ItemStack, ?>>builder()
            .put("amount", new Field<>(Integer.class,
                    ItemStack::getAmount,
                    ItemStack::setAmount))
            .put("type", new Field<>(String.class,
                    is -> is.getType().getKey().toString(),
                    (is, newType) -> is.setType(Objects.requireNonNull(Material.matchMaterial(newType)))))
            .put("enchantments", ENCHANTMENT_FIELD)
            .put("damage", META_FIELD.chain(new Field<>(Integer.class,
                    meta -> meta instanceof Damageable ? ((Damageable) meta).getDamage() : 0,
                    (meta, damage) -> {
                        if (meta instanceof Damageable)
                            ((Damageable) meta).setDamage(damage);
                    })))
            .put("name", META_FIELD.chain(new Field<>(String.class,
                    meta -> meta.hasDisplayName() ? meta.getDisplayName() : null, ItemMeta::setDisplayName)))
            .put("unbreakable", META_FIELD.chain(new Field<>(Boolean.class,
                    ItemMeta::isUnbreakable, ItemMeta::setUnbreakable)))
            .build();

    @Override
    public @Nullable Field<ItemStack, ?> getField(String fieldName) {
        return ITEM_STACK_FIELDS.get(fieldName);
    }

    @Override
    public @Nullable Collection<String> getFields(T t) {
        return ITEM_STACK_FIELDS.keySet();
    }
}
