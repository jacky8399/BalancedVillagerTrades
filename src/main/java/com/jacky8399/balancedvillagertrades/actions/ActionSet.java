package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ActionSet extends Action {
    /**
     * A description of the operation
     */
    public final String desc;
    public final FieldProxy<TradeWrapper, ?, ?> field;
    public final BiFunction<TradeWrapper, ?, ?> transformer;

    public ActionSet(String desc, FieldProxy<TradeWrapper, ?, ?> field, BiFunction<TradeWrapper, ?, ?> transformer) {
        this.desc = desc;
        this.field = field;
        this.transformer = transformer;
    }

    @Override
    public void accept(TradeWrapper tradeWrapper) {
        Object value = field.get(tradeWrapper);
        Object newValue;
        try {
            newValue = ((BiFunction) transformer).apply(tradeWrapper, value);
        } catch (Exception ex) {
            BalancedVillagerTrades.LOGGER.severe("Failed to transform value " + value + " for field " + field.fieldName);
            ex.printStackTrace();
            return;
        }
        try {
            ((Field) field).set(tradeWrapper, newValue);
        } catch (Exception ex) {
            BalancedVillagerTrades.LOGGER.severe("Failed to set field " + field.fieldName + " to " + newValue);
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Set " + desc;
    }

    private static Stream<ActionSet> parse(@Nullable ContainerField<TradeWrapper, ?> base, @Nullable String baseName, Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    FieldProxy<TradeWrapper, ?, ?> field;
                    try {
                        field = Fields.findField(base, fieldName, true);
                    } catch (IllegalArgumentException e) {
                        Config.addError(e.getMessage() + "! Skipping.");
                        return Stream.empty();
                    }
                    fieldName = base != null ? baseName + "." + fieldName : fieldName; // for better error messages
                    if (value instanceof Map) {
                        if (!field.isComplex()) { // complex fields only
                            Config.addError("Field " + fieldName + " does not have inner fields! Skipping.");
                            return Stream.empty();
                        }
                        Map<String, Object> innerMap = (Map<String, Object>) value;
                        return parse(field, fieldName, innerMap);
                    } else {
                        if (field.isReadOnly()) {
                            Config.addError("Field " + fieldName + " is read-only! Assigning new values to it will have no effect.");
                        }
                        BiFunction<TradeWrapper, ?, ?> transformer = getTransformer(field, value != null ? value.toString() : null);
                        return Stream.of(new ActionSet(fieldName + " to " + value, field, transformer));
                    }
                });
    }

    public static List<ActionSet> parse(Map<String, Object> map) {
        return parse(null, null, map).collect(Collectors.toList());
    }

    public static BiFunction<TradeWrapper, ?, ?> getTransformer(FieldProxy<TradeWrapper, ?, ?> field, @Nullable String input) {
        String trimmed = input != null ? input.trim() : "null";
        try {
            return field.parseTransformer(trimmed);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Don't know how to transform " + field
                    + "(type=" + field.getFieldClass().getSimpleName() + ") for input " + trimmed, ex);
        }
    }

}
