package com.jacky8399.balancedvillagertrades.actions;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ActionSet extends Action {
    /**
     * A description of the operation
     */
    public final String desc;
    public final Field field;
    public final UnaryOperator transformer;

    public ActionSet(String desc, Field<TradeWrapper, ?> field, UnaryOperator<?> transformer) {
        this.desc = desc;
        this.field = field;
        this.transformer = transformer;
    }

    @Override
    public void accept(TradeWrapper tradeWrapper) {
        Object newValue = transformer.apply(field.getter.apply(tradeWrapper));
        field.setter.accept(tradeWrapper, newValue);
    }

    @Override
    public String toString() {
        return "Set " + desc;
    }

    private static Stream<ActionSet> parse(@Nullable ComplexField<TradeWrapper, ?> base, @Nullable String baseName, Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    Field<TradeWrapper, ?> field = base == null ? FIELDS.get(fieldName) : base.getFieldWrapped(fieldName);
                    fieldName = base != null ? baseName + "." + fieldName : fieldName; // for better error messages
                    if (field == null) {
                        BalancedVillagerTrades.LOGGER.warning("Can't set unknown field " + fieldName + "! Skipping.");
                        return Stream.empty();
                    }
                    if (value instanceof Map) {
                        if (!(field instanceof ComplexField)) { // complex fields only
                            BalancedVillagerTrades.LOGGER.warning("Field " + fieldName + " does not have inner fields! Skipping.");
                            return Stream.empty();
                        }
                        Map<String, Object> innerMap = (Map<String, Object>) value;
                        return parse((ComplexField<TradeWrapper, ?>) field, fieldName, innerMap);
                    } else {
                        UnaryOperator<?> operator = getTransformer(field.clazz, value.toString());
                        return Stream.of(new ActionSet(fieldName + " to " + value, field, operator));
                    }
                });
    }

    public static List<ActionSet> parse(Map<String, Object> map) {
        return parse(null, null, map).collect(Collectors.toList());
    }

    public static UnaryOperator<?> getTransformer(Class<?> clazz, String input) {
        String trimmed = input.trim();
        if (clazz == Boolean.class) {
            boolean bool = Boolean.parseBoolean(trimmed);
            return oldVal -> bool;
        } else if (clazz == String.class) {
            return oldVal -> trimmed;
        } else if (clazz == Integer.class) {
            IntUnaryOperator func = OperatorUtils.getFunctionFromInput(trimmed);
            if (func == null) {
                try {
                    int num = Integer.parseInt(trimmed);
                    return oldInt -> num;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid comparison expression or integer " + trimmed);
                }
            }
            return (UnaryOperator<Integer>) func::applyAsInt;
        } else if (clazz == ItemStack.class) {
            if (trimmed.startsWith("amount")) {
                String operatorStr = trimmed.substring(6).trim();
                IntUnaryOperator intOperator = OperatorUtils.getFunctionFromInput(operatorStr);
                if (intOperator == null) {
                    throw new IllegalArgumentException("Invalid comparison expression " + trimmed);
                }
                return oldIs -> {
                    ItemStack stack = ((ItemStack) oldIs).clone();
                    stack.setAmount(intOperator.applyAsInt(stack.getAmount()));
                    return stack;
                };
            }
        }
        throw new IllegalArgumentException("Don't know how to handle field of type " + clazz.getSimpleName());
    }

    public static class Field<TOwner, TField> {
        public final Class<TField> clazz;
        public final Function<TOwner, TField> getter;
        public final BiConsumer<TOwner, TField> setter;

        public Field(Class<TField> clazz, Function<TOwner, TField> getter, BiConsumer<TOwner, TField> setter) {
            this.clazz = clazz;
            this.getter = getter;
            this.setter = setter;
        }

        @Contract("!null -> !null")
        @Nullable
        public <TInner> Field<TOwner, TInner> andThen(Field<TField, TInner> field) {
            if (field == null)
                return null;
            Function<TOwner, TInner> newGetter = getter.andThen(field.getter);
            BiConsumer<TOwner, TInner> newSetter = (owner, newVal) -> {
                TField instance = getter.apply(owner);
                field.setter.accept(instance, newVal);
                setter.accept(owner, instance);
            };
            return field instanceof ComplexField ? new ComplexField<TOwner, TInner>(field.clazz, newGetter, newSetter) {
                @Override
                public @Nullable Field<TInner, ?> getField(String fieldName) {
                    return ((ComplexField<TField, TInner>) field).getField(fieldName);
                }
            } : new Field<>(field.clazz, newGetter, newSetter);
        }
    }

    public static abstract class ComplexField<TOwner, TField> extends Field<TOwner, TField> {
        public ComplexField(Class<TField> clazz, Function<TOwner, TField> getter, BiConsumer<TOwner, TField> setter) {
            super(clazz, getter, setter);
        }

        @Nullable
        public abstract Field<TField, ?> getField(String fieldName);

        public Field<TOwner, ?> getFieldWrapped(String fieldName) {
            Field<TField, ?> field = getField(fieldName);
            return field != null ? andThen(field) : null;
        }
    }

    public static class ItemStackField<T> extends ComplexField<T, ItemStack> {
        public ItemStackField(Function<T, ItemStack> getter, BiConsumer<T, ItemStack> setter) {
            super(ItemStack.class, getter, setter);
        }

        // I'm lazy
        private static final Field<ItemStack, ItemMeta> META_FIELD = new Field<>(ItemMeta.class, ItemStack::getItemMeta, ItemStack::setItemMeta);

        private static final Field<ItemStack, Map<Enchantment, Integer>> ENCHANTMENT_FIELD =
                META_FIELD.andThen(
                        new ComplexField<ItemMeta, Map<Enchantment, Integer>>((Class) Map.class, // horrible
                                meta -> new HashMap<>(
                                        meta instanceof EnchantmentStorageMeta ?
                                                ((EnchantmentStorageMeta) meta).getStoredEnchants() :
                                                meta.getEnchants()
                                ),
                                (meta, newMap) -> {
                                    boolean isEnchantedBook = meta instanceof EnchantmentStorageMeta;
                                    // remove enchantments
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
                                }) {
                            @Override
                            public @Nullable Field<Map<Enchantment, Integer>, Integer> getField(String fieldName) {
                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(fieldName));
                                return enchantment != null ? new Field<>(Integer.class,
                                        map -> map.getOrDefault(enchantment, 0),
                                        (map, lvl) -> {
                                            if (lvl <= 0)
                                                map.remove(enchantment);
                                            else
                                                map.put(enchantment, lvl);
                                        }
                                ) : null;
                            }
                        });

        public static final ImmutableMap<String, Field<ItemStack, ?>> ITEM_STACK_FIELDS = ImmutableMap.<String, Field<ItemStack, ?>>builder()
                .put("amount", new Field<>(Integer.class,
                        ItemStack::getAmount,
                        ItemStack::setAmount))
                .put("type", new Field<>(String.class,
                        is -> is.getType().getKey().toString(),
                        (is, newType) -> is.setType(Objects.requireNonNull(Material.matchMaterial(newType)))))
                .put("enchantments", ENCHANTMENT_FIELD)
                .put("damage", META_FIELD.andThen(new Field<>(Integer.class,
                        meta -> meta instanceof Damageable ? ((Damageable) meta).getDamage() : 0,
                        (meta, damage) -> {
                            if (meta instanceof Damageable)
                                ((Damageable) meta).setDamage(damage);
                        })))
                .put("name", META_FIELD.andThen(new Field<>(String.class,
                        ItemMeta::getDisplayName, ItemMeta::setDisplayName)))
                .put("unbreakable", META_FIELD.andThen(new Field<>(Boolean.class,
                        ItemMeta::isUnbreakable, ItemMeta::setUnbreakable)))
                .build();

        @Override
        public @Nullable Field<ItemStack, ?> getField(String fieldName) {
            return ITEM_STACK_FIELDS.get(fieldName);
        }
    }

    public static final ImmutableMap<String, Field<TradeWrapper, ?>> FIELDS = ImmutableMap.<String, Field<TradeWrapper, ?>>builder()
            .put("apply-discounts", new Field<>(Boolean.class,
                    trade -> trade.getRecipe().getPriceMultiplier() != 0,
                    (trade, bool) -> trade.getRecipe().setPriceMultiplier(bool ? 1 : 0)))
            .put("max-uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getMaxUses(),
                    (trade, maxUses) -> trade.getRecipe().setMaxUses(maxUses)))
            .put("uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getUses(),
                    (trade, maxUses) -> trade.getRecipe().setUses(maxUses)))
            .put("award-experience", new Field<>(Boolean.class,
                    trade -> trade.getRecipe().hasExperienceReward(),
                    (trade, awardXP) -> trade.getRecipe().setExperienceReward(awardXP)))
            .put("villager-experience", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getVillagerExperience(),
                    (trade, villagerXP) -> trade.getRecipe().setVillagerExperience(villagerXP)))
            .put("ingredient-0", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(0),
                    (trade, stack) -> setIngredient(0, trade, stack)))
            .put("ingredient-1", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(1),
                    (trade, stack) -> setIngredient(1, trade, stack)))
            .put("result", new ItemStackField<>(
                    trade -> trade.getRecipe().getResult(),
                    (trade, stack) -> {
                        MerchantRecipe oldRecipe = trade.getRecipe();
                        MerchantRecipe newRecipe = new MerchantRecipe(stack,
                                oldRecipe.getUses(), oldRecipe.getMaxUses(),
                                oldRecipe.hasExperienceReward(), oldRecipe.getVillagerExperience(),
                                oldRecipe.getPriceMultiplier());
                        newRecipe.setIngredients(oldRecipe.getIngredients());
                        trade.setRecipe(newRecipe);
                    }))
            .build();

    private static void setIngredient(int index, TradeWrapper trade, final ItemStack stack) {
        List<ItemStack> stacks = new ArrayList<>(trade.getRecipe().getIngredients());
        if (stack.getAmount() > stack.getMaxStackSize()) {
            ItemStack clone = stack.clone();
            if (index == 0) { // only split first ingredient
                int remainder = Math.min(clone.getAmount() - clone.getMaxStackSize(), clone.getMaxStackSize());
                clone.setAmount(clone.getMaxStackSize());
                ItemStack extra = stack.clone();
                extra.setAmount(remainder);
                stacks.set(0, clone);
                stacks.set(1, extra);
            } else {
                clone.setAmount(clone.getMaxStackSize());
                stacks.set(1, clone);
            }
        } else {
            stacks.set(index, stack);
        }
        trade.getRecipe().setIngredients(stacks);
    }

}
