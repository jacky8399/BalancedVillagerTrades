package com.jacky8399.balancedvillagertrades.fields;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
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

public class ItemStackField<T> extends SimpleField<T, ItemStackField.ItemStackWrapper> implements ContainerField<T, ItemStackField.ItemStackWrapper> {
    public record ItemStackWrapper(ItemStack stack, ItemMeta meta) {
        static ItemStackWrapper fromStack(ItemStack stack) {
            return new ItemStackWrapper(stack, stack.getItemMeta());
        }

    }

    public ItemStackField(Function<T, ItemStackWrapper> getter, BiConsumer<T, ItemStackWrapper> setter) {
        super(ItemStackWrapper.class,
                getter,
                setter != null ? (t, wrapper) -> {
                    if (wrapper != null && wrapper.stack != null) {
                        wrapper.stack.setItemMeta(wrapper.meta);
                        setter.accept(t, wrapper);
                    } else {
                        setter.accept(t, null);
                    }
                } : null);
    }


    public static <T> ItemStackField<T> create(Function<T, ItemStack> getter, BiConsumer<T, ItemStack> setter) {
        return new ItemStackField<>(getter.andThen(ItemStackWrapper::fromStack),
                setter != null ? (t, wrapper) -> {
                    if (wrapper != null && wrapper.stack != null) {
                        wrapper.stack.setItemMeta(wrapper.meta);
                        setter.accept(t, wrapper.stack);
                    } else {
                        setter.accept(t, null);
                    }
                } : null);
    }

    static <T> Field<ItemStackWrapper, T> stackField(Class<T> clazz, Function<ItemStack, T> getter, @Nullable BiConsumer<ItemStack, T> setter) {
        return new SimpleField<>(clazz,
                getter.compose(ItemStackWrapper::stack),
                setter != null ? (wrapper, t) -> setter.accept(wrapper.stack, t) : null);
    }
    static <T> Field<ItemStackWrapper, T> metaField(Class<T> clazz, Function<ItemMeta, T> getter, @Nullable BiConsumer<ItemMeta, T> setter) {
        return new SimpleField<>(clazz,
                getter.compose(ItemStackWrapper::meta),
                setter != null ? (wrapper, t) -> setter.accept(wrapper.meta, t) : null);
    }

    public static final ImmutableMap<String, Field<ItemStackWrapper, ?>> ITEM_STACK_FIELDS = ImmutableMap.<String, Field<ItemStackWrapper, ?>>builder()
            .put("amount", stackField(Integer.class,
                    ItemStack::getAmount,
                    ItemStack::setAmount))
            .put("type", new NamespacedKeyField<>(
                    is -> is.stack.getType().getKey(),
                    (is, key) -> {
                        Objects.requireNonNull(key, "resource location must not be null");
                        is.stack.setType(Objects.requireNonNull(Registry.MATERIAL.get(key), "Invalid item " + key));
                    }))
            .put("enchantments", new EnchantmentsField(null))
            .put("safe_enchantments", new EnchantmentsField(false))
            .put("unsafe_enchantments", new EnchantmentsField(true))
            .put("damage", metaField(Integer.class,
                    meta -> meta instanceof Damageable damageable ? damageable.getDamage() : 0,
                    (meta, damage) -> {
                        if (meta instanceof Damageable damageable)
                            damageable.setDamage(damage);
                    }))
            .put("name", metaField(String.class,
                    meta -> meta.hasDisplayName() ? meta.getDisplayName() : null,
                    ItemMeta::setDisplayName))
            .put("unbreakable", metaField(Boolean.class,
                    ItemMeta::isUnbreakable, ItemMeta::setUnbreakable))
            .build();

    @Override
    public @Nullable Field<ItemStackWrapper, ?> getField(String fieldName) {
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
    public @NotNull BiPredicate<TradeWrapper, ItemStackWrapper> parsePredicate(@NotNull String input) throws IllegalArgumentException {
        // legacy syntax handled by FieldPredicate, since it used a map
        try {
            ItemStack parsed = Bukkit.getItemFactory().createItemStack(input);
            return (ignored, old) -> parsed.isSimilar(old.stack);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid item stack", ex);
        }
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, ItemStackWrapper, ItemStackWrapper> parseTransformer(@Nullable String input) throws IllegalArgumentException {
        if (input == null)
            return (ignored, old) -> null;
        if (input.startsWith("amount")) {
            String operatorStr = input.substring(6).trim();
            IntUnaryOperator intOperator = OperatorUtils.getFunctionFromInput(operatorStr);
            if (intOperator == null) {
                throw new IllegalArgumentException("Invalid comparison expression " + operatorStr);
            }
            return (ignored, oldIs) -> {
                ItemStack stack = oldIs.stack.clone();
                stack.setAmount(intOperator.applyAsInt(stack.getAmount()));
                return ItemStackWrapper.fromStack(stack);
            };
        }
        try {
            ItemStack parsed = Bukkit.getItemFactory().createItemStack(input);
            return (ignored, old) -> ItemStackWrapper.fromStack(parsed.clone());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid item stack", ex);
        }
    }
}
