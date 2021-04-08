package com.jacky8399.balancedvillagertrades.actions;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ActionSet<T> extends Action {
    public final Field<T> field;
    public final UnaryOperator<T> transformer;

    public ActionSet(Field<T> field, UnaryOperator<T> transformer) {
        this.field = field;
        this.transformer = transformer;
    }

    @Override
    public void accept(TradeWrapper tradeWrapper) {
        T newValue = transformer.apply(field.getter.apply(tradeWrapper));
        field.setter.accept(tradeWrapper, newValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<ActionSet<?>> parse(Map<String, Object> map) {
        return map.entrySet().stream()
                .map(entry -> {
                    Field<?> field = FIELDS.get(entry.getKey());
                    UnaryOperator<?> operator = getTransformer(field.clazz, entry.getValue().toString());
                    return new ActionSet(field, operator);
                })
                .collect(Collectors.toList());
    }

    private static final Pattern OPERATOR_PATTERN = Pattern.compile("^(>|>=|<|<=|=)\\s*?(\\d+)$");
    private static final Pattern FIELD_OPERATOR_PATTERN = Pattern.compile("^(amount)\\s*?(>|>=|<|<=|=)\\s*?(\\d+)$");
    @SuppressWarnings("unchecked")
    public static UnaryOperator<?> getTransformer(Class<?> clazz, String input) {
        String trimmed = input.trim();
        if (clazz == Boolean.class) {
            boolean bool = Boolean.parseBoolean(trimmed);
            return oldVal -> bool;
        } else if (clazz == Integer.class) {
            Matcher operator = OPERATOR_PATTERN.matcher(trimmed);
            if (operator.matches()) {
                Integer num = Ints.tryParse(operator.group(2));
                if (num == null)
                    throw new IllegalArgumentException(operator.group(2) + " is not a valid number");
                return oldInt -> OperatorUtils.getFunctionFromOperator(operator.group(1), ()->num);
            }
        } else if (clazz == ItemStack.class) {
            Matcher operator = FIELD_OPERATOR_PATTERN.matcher(trimmed);
            if (operator.matches()) {
                Integer num = Ints.tryParse(operator.group(3));
                if (num == null)
                    throw new IllegalArgumentException(operator.group(3) + " is not a valid number");
                IntUnaryOperator intOperator = OperatorUtils.getFunctionFromOperator(operator.group(2), ()->num);
                if ("amount".equals(operator.group(1))) {
                    return oldIs -> {
                        ItemStack stack = ((ItemStack) oldIs).clone();
                        stack.setAmount(intOperator.applyAsInt(stack.getAmount()));
                        return stack;
                    };
                }
            }
        }
        throw new IllegalArgumentException("Don't know how to handle " + input);
    }

    public static class Field<T> {
        public final Class<T> clazz;
        public final Function<TradeWrapper, T> getter;
        public final BiConsumer<TradeWrapper, T> setter;
        public Field(Class<T> clazz, Function<TradeWrapper, T> getter, BiConsumer<TradeWrapper, T> setter) {
            this.clazz = clazz;
            this.getter = getter;
            this.setter = setter;
        }
    }

    public static final ImmutableMap<String, Field<?>> FIELDS = ImmutableMap.<String, Field<?>>builder()
            .put("apply-discounts", new Field<>(Boolean.class,
                    trade -> trade.getRecipe().getPriceMultiplier() != 0,
                    (trade, bool) -> trade.getRecipe().setPriceMultiplier(bool ? 1 : 0)))
            .put("max-uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getMaxUses(),
                    (trade, maxUses) -> trade.getRecipe().setMaxUses(maxUses)))
            .put("uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getUses(),
                    (trade, maxUses) -> trade.getRecipe().setUses(maxUses)))
            .put("ingredient-0", new Field<>(ItemStack.class,
                    trade -> trade.getRecipe().getIngredients().get(0),
                    (trade, stack) -> setIngredient(0, trade, stack)))
            .put("ingredient-1", new Field<>(ItemStack.class,
                    trade -> trade.getRecipe().getIngredients().get(1),
                    (trade, stack) -> setIngredient(1, trade, stack)))
            .put("result", new Field<>(ItemStack.class,
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
