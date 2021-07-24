package com.jacky8399.balancedvillagertrades.actions;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ActionSet extends Action {
    /** A description of the operation */
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

    public static List<ActionSet> parse(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    TradeField<?> field = FIELDS.get(key);
                    if (field == null) {
                        BalancedVillagerTrades.LOGGER.warning("Unknown field " + key + " in set, skipping.");
                        return Stream.empty();
                    }
                    if (value instanceof Map) {
                        if (field.clazz != ItemStack.class) // only item stacks can have inner fields
                            return Stream.empty();
                        Map<String, Object> innerMap = (Map<String, Object>) value;
                        return innerMap.entrySet().stream()
                                .map(innerEntry -> {
                                    String innerKey = innerEntry.getKey();
                                    String innerValue = innerEntry.getValue().toString();
                                    ItemStackField<?> isField = ITEM_STACK_FIELDS.get(innerKey);
                                    if (isField == null) {
                                        BalancedVillagerTrades.LOGGER.warning("Unknown field " +
                                                key + "." + innerKey + " in set, skipping.");
                                        return null;
                                    }
                                    return new ActionSet(key + "." + innerKey + " to " + innerValue,
                                            ((TradeField<ItemStack>) field).andThen(isField),
                                            getTransformer(isField.clazz, innerValue));
                                })
                                .filter(Objects::nonNull);
                    } else {
                        UnaryOperator<?> operator = getTransformer(field.clazz, value.toString());
                        return Stream.of(new ActionSet(key + " to " + value, field, operator));
                    }
                })
                .collect(Collectors.toList());
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
        throw new IllegalArgumentException("Don't know how to handle " + input);
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

        public <TInner> Field<TOwner, TInner> andThen(Field<TField, TInner> field) {
            return new Field<TOwner, TInner>(field.clazz, getter.andThen(field.getter), (owner, newVal) -> {
                TField instance = getter.apply(owner);
                field.setter.accept(instance, newVal);
                setter.accept(owner, instance);
            });
        }
    }

    public static class TradeField<T> extends Field<TradeWrapper, T> {
        public TradeField(Class<T> clazz, Function<TradeWrapper, T> getter, BiConsumer<TradeWrapper, T> setter) {
            super(clazz, getter, setter);
        }
    }

    public static class ItemStackField<T> extends Field<ItemStack, T> {
        public ItemStackField(Class<T> clazz, Function<ItemStack, T> getter, BiConsumer<ItemStack, T> setter) {
            super(clazz, getter, setter);
        }
    }

    public static final ImmutableMap<String, TradeField<?>> FIELDS = ImmutableMap.<String, TradeField<?>>builder()
            .put("apply-discounts", new TradeField<>(Boolean.class,
                    trade -> trade.getRecipe().getPriceMultiplier() != 0,
                    (trade, bool) -> trade.getRecipe().setPriceMultiplier(bool ? 1 : 0)))
            .put("max-uses", new TradeField<>(Integer.class,
                    trade -> trade.getRecipe().getMaxUses(),
                    (trade, maxUses) -> trade.getRecipe().setMaxUses(maxUses)))
            .put("uses", new TradeField<>(Integer.class,
                    trade -> trade.getRecipe().getUses(),
                    (trade, maxUses) -> trade.getRecipe().setUses(maxUses)))
            .put("ingredient-0", new TradeField<>(ItemStack.class,
                    trade -> trade.getRecipe().getIngredients().get(0),
                    (trade, stack) -> setIngredient(0, trade, stack)))
            .put("ingredient-1", new TradeField<>(ItemStack.class,
                    trade -> trade.getRecipe().getIngredients().get(1),
                    (trade, stack) -> setIngredient(1, trade, stack)))
            .put("result", new TradeField<>(ItemStack.class,
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

    public static final ImmutableMap<String, ItemStackField<?>> ITEM_STACK_FIELDS = ImmutableMap.<String, ItemStackField<?>>builder()
            .put("amount", new ItemStackField<>(Integer.class,
                    ItemStack::getAmount,
                    ItemStack::setAmount))
            .put("type", new ItemStackField<>(String.class,
                    is -> is.getType().getKey().toString(),
                    (is, newType) -> is.setType(Objects.requireNonNull(Material.matchMaterial(newType)))))

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
