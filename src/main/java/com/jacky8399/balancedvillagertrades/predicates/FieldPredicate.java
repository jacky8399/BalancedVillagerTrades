package com.jacky8399.balancedvillagertrades.predicates;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import com.jacky8399.balancedvillagertrades.utils.fields.*;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        Object value = field.get(tradeWrapper);
        return value != null && ((Predicate) predicate).test(value);
    }

    private static final Field<TradeWrapper, TradeWrapper> IDENTITY_FIELD = Field.readOnlyField(TradeWrapper.class, Function.identity());
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
                        if (field instanceof ItemStackField && innerMap.size() == 1 && innerMap.containsKey("matches")) {
                            // fallback to old ItemPredicates
                            if (warnOldItemSyntax) {
                                warnOldItemSyntax = false;
                                BalancedVillagerTrades.LOGGER.warning("Falling back to old item predicate for " + fieldName + ".");
                            }
                            TradePredicate predicate = TradePredicate.CONSTRUCTORS.get(fieldName).apply(innerMap);
                            return Stream.of(new FieldPredicate(predicate.toString(),
                                    IDENTITY_FIELD, obj -> predicate.test((TradeWrapper) obj)));
                        }
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

    private static final Pattern STRING_PATTERN = Pattern.compile("^(==?|contains|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAP_PATTERN = Pattern.compile("^contains\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static boolean warnOldItemSyntax = true;
    private static boolean warnOldVillagerSyntax = true;

    private static final Pattern LEGACY_VILLAGER_SYNTAX = Pattern.compile("^(profession|type)\\s*(=|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    @SuppressWarnings("unchecked")
    private static Predicate<?> getPredicate(Field<TradeWrapper, ?> field, String input) {
        Class<?> clazz = field.clazz;
        String trimmed = input.trim();
        if (clazz == Boolean.class) {
            Boolean value = trimmed.toLowerCase(Locale.ROOT).contains("true");
            return obj -> obj == value;
        } else if (clazz == Integer.class) {
            IntPredicate predicate = OperatorUtils.fromInput(input);
            if (predicate != null) {
                return obj -> predicate.test((Integer) obj);
            } else {
                try {
                    int number = Integer.parseInt(input);
                    return obj -> (Integer) obj == number;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid comparison expression or integer " + input);
                }
            }
        } else if (clazz == String.class) {
            Matcher matcher = STRING_PATTERN.matcher(input);
            if (!matcher.matches()) {
                return obj -> input.equalsIgnoreCase((String) obj);
            }
            String operation = matcher.group(1);
            String operand = matcher.group(2);
            boolean caseInsensitive = true;
            if (operand.startsWith("\"") && (operand.endsWith("\"") || operand.endsWith("\"i"))) {
                if (operand.endsWith("\"i")) {
                    operand = operand.substring(1, operand.length() - 2);
                } else if (operand.endsWith("\"")) {
                    caseInsensitive = false;
                    operand = operand.substring(1, operand.length() - 1);
                } else {
                    throw new IllegalArgumentException("Expected \" at the end");
                }
            }
            final String finalOperand = operand;
            switch (operation.toLowerCase(Locale.ROOT)) {
                case "=":
                    return caseInsensitive ?
                            obj -> ((String) obj).equalsIgnoreCase(finalOperand) :
                            obj -> obj.equals(finalOperand);
                case "contains":
                    return caseInsensitive ?
                            obj -> ((String) obj).toLowerCase(Locale.ROOT).contains(finalOperand) :
                            obj -> ((String) obj).contains(finalOperand);
                case "matches":
                    Pattern pattern = Pattern.compile(operand);
                    return obj -> pattern.matcher((String) obj).matches();
            }
        } else if (field instanceof MapField) {
            Matcher matcher = MAP_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Can only check for keys in a map");
            }
            String key = matcher.group(1);
            Object translatedKey = ((MapField<TradeWrapper, ?, ?>) field).translateKey(key);
            return translatedKey != null ? obj -> ((Map<?, ?>) obj).containsKey(translatedKey) : obj -> false;
        } else if (clazz == Villager.class) {
            Matcher matcher = LEGACY_VILLAGER_SYNTAX.matcher(trimmed);
            if (matcher.matches()) {
                if (warnOldVillagerSyntax) {
                    warnOldVillagerSyntax = false;
                    BalancedVillagerTrades.LOGGER.warning("Using 'villager: profession/type ...' to target villagers is obsolete.");
                    BalancedVillagerTrades.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Fields#villager-properties " +
                            "for a better way to target specific villagers.");
                }
                Field<TradeWrapper, ?> property = ((ComplexField<TradeWrapper, ?>) field).getFieldWrapped(matcher.group(1));
                //noinspection ConstantConditions
                return getPredicate(property, matcher.group(2) + matcher.group(3));
            }
        }
        throw new IllegalArgumentException("Don't know how to handle " + clazz.getSimpleName() + " fields");
    }

    public static List<FieldPredicate> parse(Map<String, Object> map) {
        return parse(null, null, map).collect(Collectors.toList());
    }
}
