package com.jacky8399.balancedvillagertrades.predicates;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.*;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldPredicate extends TradePredicate {
    public final String desc;
    public final Field<TradeWrapper, ?> field;
    public final BiPredicate<TradeWrapper, ?> predicate;
    public FieldPredicate(String desc, Field<TradeWrapper, ?> field, BiPredicate<TradeWrapper, ?> predicate) {
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
        return value != null && ((BiPredicate) predicate).test(tradeWrapper, value);
    }

    private static final Field<TradeWrapper, TradeWrapper> IDENTITY_FIELD = Field.readOnlyField(TradeWrapper.class, Function.identity());
    @SuppressWarnings("unchecked")
    private static Stream<FieldPredicate> parse(@Nullable ContainerField<TradeWrapper, ?> base, @Nullable String baseName, Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    FieldProxy<TradeWrapper, ?, ?> field;
                    try {
                        field = Fields.findField(base, fieldName, true);
                    } catch (IllegalArgumentException e) {
                        BalancedVillagerTrades.LOGGER.warning(e.getMessage() + "! Skipping.");
                        return Stream.empty();
                    }
                    fieldName = base != null ? baseName + "." + fieldName : fieldName; // for better error messages
                    if (value instanceof Map) {
                        if (!field.isComplex()) { // complex fields only
                            BalancedVillagerTrades.LOGGER.warning("Field " + fieldName + " does not have inner fields! Skipping.");
                            return Stream.empty();
                        }
                        Map<String, Object> innerMap = (Map<String, Object>) value;
                        if (field.child instanceof ItemStackField && innerMap.size() == 1 && innerMap.containsKey("matches")) {
                            // fallback to old ItemPredicates
                            if (warnOldItemSyntax) {
                                warnOldItemSyntax = false;
                                BalancedVillagerTrades.LOGGER.warning("Falling back to old item predicate for " + fieldName + ".");
                            }
                            TradePredicate predicate = TradePredicate.CONSTRUCTORS.get(fieldName).apply(innerMap);
                            return Stream.of(new FieldPredicate(predicate.toString(),
                                    IDENTITY_FIELD, (tradeWrapper, obj) -> predicate.test(tradeWrapper)));
                        }
                        return parse(field, fieldName, innerMap);
                    } else {
                        try {
                            BiPredicate<TradeWrapper, ?> predicate;
                            if (value != null)
                                predicate = getPredicate(field, value.toString());
                            else
                                predicate = (tradeWrapper, obj) -> obj == null;
                            return Stream.of(new FieldPredicate(fieldName + ": " + value, field, predicate));
                        } catch (IllegalArgumentException e) {
                            BalancedVillagerTrades.LOGGER.warning(e.getMessage() + "! Skipping");
                            e.printStackTrace();
                            return Stream.empty();
                        }
                    }
                });
    }

    private static boolean warnOldItemSyntax = true;

    public static BiPredicate<TradeWrapper, ?> getPredicate(FieldProxy<TradeWrapper, ?, ?> field, String input) {
        String trimmed = input.trim();
        try {
            return field.parsePredicate(trimmed);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Don't know how to test against " + field
                    + " (type=" + field.getFieldClass().getSimpleName() + ") for input " + trimmed, ex);
        }
    }

    public static List<FieldPredicate> parse(Map<String, Object> map) {
        return parse(null, null, map).collect(Collectors.toList());
    }
}
