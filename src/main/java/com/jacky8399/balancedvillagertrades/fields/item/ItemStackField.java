package com.jacky8399.balancedvillagertrades.fields.item;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.NamespacedKeyField;
import com.jacky8399.balancedvillagertrades.fields.SimpleField;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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
    public static class ItemStackWrapper {
        ItemStack stack;
        ItemMeta meta;

        public ItemStackWrapper(ItemStack stack, ItemMeta meta) {
            this.stack = stack;
            this.meta = meta;
        }

        public ItemStack stack() {
            return stack;
        }

        public ItemMeta meta() {
            return meta;
        }

        public void setMeta(ItemMeta meta) {
            this.meta = meta;
            stack.setItemMeta(meta);
        }

        static ItemStackWrapper fromStack(ItemStack stack) {
            return new ItemStackWrapper(stack, stack.getItemMeta());
        }

    }

    public ItemStackField(Function<T, ItemStackWrapper> getter, BiConsumer<T, ItemStackWrapper> setter) {
        super(ItemStackWrapper.class,
                getter,
                setter != null ? (t, wrapper) -> {
                    if (wrapper != null && wrapper.stack != null) {
                        wrapper.setMeta(wrapper.meta);
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

    static <T> Field<ItemStackWrapper, T> metaField(Class<T> clazz, Function<ItemMeta, T> getter, @Nullable BiConsumer<ItemMeta, T> setter) {
        return new SimpleField<>(clazz,
                getter.compose(ItemStackWrapper::meta),
                setter != null ? (wrapper, t) -> setter.accept(wrapper.meta, t) : null);
    }

    public static final ImmutableMap<String, Field<ItemStackWrapper, ?>> ITEM_STACK_FIELDS = ImmutableMap.<String, Field<ItemStackWrapper, ?>>builder()
            .put("amount", new SimpleField<>(Integer.class,
                    stack -> stack.stack.getAmount(),
                    (wrapper, amount) -> wrapper.stack.setAmount(amount)))
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
            .put("unbreakable", metaField(Boolean.class, ItemMeta::isUnbreakable, ItemMeta::setUnbreakable))
            .put("lore", new ItemLoreField())
            .put("nbt", new SimpleField<>(String.class,
                    wrapper -> wrapper.meta.getAsString(),
                    (wrapper, newNbt) -> {
                        NamespacedKey key = wrapper.stack.getType().getKey();
                        try {
                            ItemStack newIs = Bukkit.getItemFactory().createItemStack(key + newNbt);
                            ItemMeta meta = newIs.getItemMeta();
                            wrapper.setMeta(meta);
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException("Invalid NBT data: " + newNbt, ex);
                        }
                    }
            ))
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
