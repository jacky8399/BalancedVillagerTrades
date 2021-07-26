package com.jacky8399.balancedvillagertrades.predicates;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.*;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldPredicate extends TradePredicate {
    public final String desc;
    public final Field<TradeWrapper, ?> field;
    public final Predicate<?> predicate;
    public FieldPredicate(String desc, Field<TradeWrapper, ?> field, Predicate<?> predicate) {
        this.desc = desc;
        this.field = field;
        this.predicate = predicate;
    }

    @Override
    public String toString() {
        return "Test " + desc;
    }

    @Override
    public boolean test(Villager villager, MerchantRecipe recipe) {
        TradeWrapper wrapper = new TradeWrapper(villager, recipe);
        return test(wrapper);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        Object value = field.get(tradeWrapper);
        return value != null && ((Predicate) predicate).test(value);
    }

    @SuppressWarnings("unchecked")
    private static Stream<FieldPredicate> parse(@Nullable ComplexField<TradeWrapper, ?> base, @Nullable String baseName, Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    Field<TradeWrapper, ?> field;
                    try {
                        field = Fields.findField(base, fieldName, true);
                    } catch (IllegalArgumentException e) {
                        BalancedVillagerTrades.LOGGER.warning(e.getMessage() + "! Skipping.");
                        return Stream.empty();
                    }
                    fieldName = base != null ? baseName + "." + fieldName : fieldName; // for better error messages
                    if (value instanceof Map) {
                        if (!(field instanceof ComplexField)) { // complex fields only
                            BalancedVillagerTrades.LOGGER.warning("Field " + fieldName + " does not have inner fields! Skipping.");
                            return Stream.empty();
                        }
                        Map<String, Object> innerMap = (Map<String, Object>) value;
                        return parse((ComplexField<TradeWrapper, ?>) field, fieldName, innerMap);
                    } else {
                        try {
                            Predicate<?> predicate = getPredicate(field, value.toString());
                            return Stream.of(new FieldPredicate(fieldName + ": " + value, field, predicate));
                        } catch (IllegalArgumentException e) {
                            BalancedVillagerTrades.LOGGER.warning(e.getMessage() + "! Skipping");
                            return Stream.empty();
                        }
                    }
                });
    }

    private static final Pattern STRING_PATTERN = Pattern.compile("^(=|contains|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAP_PATTERN = Pattern.compile("^contains\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static Predicate<?> getPredicate(Field<TradeWrapper, ?> field, String input) {
        Class<?> clazz = field.clazz;
        String trimmed = input.trim();
        if (clazz == Boolean.class) {
            Boolean value = trimmed.toLowerCase(Locale.ROOT).contains("true");
            return obj -> obj == value;
        } else if (clazz == Integer.class) {
            IntPredicate predicate = OperatorUtils.fromInput(input);
            if (predicate != null)
                return obj -> predicate.test((Integer) obj);
        } else if (clazz == String.class) {
            Matcher matcher = STRING_PATTERN.matcher(input);
            if (!matcher.matches()) {
                return obj -> input.equalsIgnoreCase((String) obj);
            }
            String operation = matcher.group(1);
            String operand = matcher.group(2);
            if (operand.startsWith("\"") && operand.endsWith("\"")) {
                operand = operand.substring(1, operand.length() - 1);
            }
            final String finalOperand = operand;
            switch (operation.toLowerCase(Locale.ROOT)) {
                case "=":
                    return obj -> ((String) obj).equalsIgnoreCase(finalOperand);
                case "contains":
                    return obj -> ((String) obj).toLowerCase(Locale.ROOT).contains(finalOperand);
                case "matches":
                    Pattern pattern = Pattern.compile(operand);
                    return obj -> pattern.matcher((String) obj).matches();
            }
        } else if (field instanceof MapField) {
            Matcher matcher = MAP_PATTERN.matcher(input);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Can only check for keys in a map");
            }
            String key = matcher.group(1);
            Object translatedKey = ((MapField) field).translateKey(key);
            return obj -> ((Map) obj).containsKey(translatedKey);
        }
        throw new IllegalArgumentException("Don't know how to handle " + clazz.getSimpleName() + " fields");
    }

    public static List<FieldPredicate> parse(Map<String, Object> map) {
        return parse(null, null, map).collect(Collectors.toList());
    }
}
