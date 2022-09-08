package com.jacky8399.balancedvillagertrades.fields;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.*;

public class ItemStackField<T> extends SimpleField<T, ItemStack> implements ContainerField<T, ItemStack> {
    public ItemStackField(Function<T, ItemStack> getter, BiConsumer<T, ItemStack> setter) {
        super(ItemStack.class, getter, setter);
    }

    // I'm lazy
    private static final Field<ItemStack, ItemMeta> META_FIELD = FieldProxy.emptyAccessor(new SimpleField<>(ItemMeta.class, ItemStack::getItemMeta, ItemStack::setItemMeta));
    private static <T> Field<ItemStack, T> metaField(Field<ItemMeta, T> field) {
        return new FieldProxy<>(META_FIELD, field, "[intermediate]") {
            @Override
            protected String formatField() {
                return child.toString();
            }

            @Override
            public String toString() {
                return child.toString();
            }
        };
    }

    public static final ImmutableMap<String, Field<ItemStack, ?>> ITEM_STACK_FIELDS = ImmutableMap.<String, Field<ItemStack, ?>>builder()
            .put("amount", new SimpleField<>(Integer.class,
                    ItemStack::getAmount,
                    ItemStack::setAmount))
            .put("type", new NamespacedKeyField<>(
                    is -> is.getType().getKey(),
                    (is, key) -> {
                        Objects.requireNonNull(key, "resource location must not be null");
                        is.setType(Objects.requireNonNull(Registry.MATERIAL.get(key), "Invalid item " + key));
                    }))
            .put("enchantments", metaField(new EnchantmentsField()))
            .put("damage", metaField(new SimpleField<>(Integer.class,
                    meta -> meta instanceof Damageable damageable ? damageable.getDamage() : 0,
                    (meta, damage) -> {
                        if (meta instanceof Damageable damageable)
                            damageable.setDamage(damage);
                    })))
            .put("name", metaField(new SimpleField<>(String.class,
                    meta -> meta.hasDisplayName() ? meta.getDisplayName() : null,
                    ItemMeta::setDisplayName)))
            .put("unbreakable", metaField(new SimpleField<>(Boolean.class,
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

    @Override
    public String toString() {
        return "ItemStackField";
    }

    @Override
    public @NotNull BiPredicate<TradeWrapper, ItemStack> parsePredicate(@NotNull String input) throws IllegalArgumentException {
        // legacy syntax handled by FieldPredicate, since it used a map
        try {
            ItemStack parsed = Bukkit.getItemFactory().createItemStack(input);
            return (ignored, old) -> parsed.isSimilar(old);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid item stack", ex);
        }
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, ItemStack, ItemStack> parseTransformer(@Nullable String input) throws IllegalArgumentException {
        if (input == null)
            return (ignored, old) -> null;
        if (input.startsWith("amount")) {
            String operatorStr = input.substring(6).trim();
            IntUnaryOperator intOperator = OperatorUtils.getFunctionFromInput(operatorStr);
            if (intOperator == null) {
                throw new IllegalArgumentException("Invalid comparison expression " + operatorStr);
            }
            return (ignored, oldIs) -> {
                ItemStack stack = oldIs.clone();
                stack.setAmount(intOperator.applyAsInt(stack.getAmount()));
                return stack;
            };
        }
        try {
            ItemStack parsed = Bukkit.getItemFactory().createItemStack(input);
            return (ignored, old) -> parsed.clone();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid item stack", ex);
        }
    }
}
