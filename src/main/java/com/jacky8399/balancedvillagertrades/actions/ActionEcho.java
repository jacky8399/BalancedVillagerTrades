package com.jacky8399.balancedvillagertrades.actions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.ComplexField;
import com.jacky8399.balancedvillagertrades.utils.Field;
import com.jacky8399.balancedvillagertrades.utils.Fields;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActionEcho extends Action {
    public final String recipeName;
    public final Map<String, Field<TradeWrapper, ?>> fields;
    public ActionEcho(String name, Map<String, Field<TradeWrapper, ?>> fields) {
        this.recipeName = name;
        this.fields = ImmutableMap.copyOf(fields);
    }

    public static ActionEcho parse(String name, List<?> list) {
        return new ActionEcho(name, list.stream()
                .map(Object::toString)
                .map(fieldName -> {
                    try {
                        return Maps.immutableEntry(fieldName, Fields.findField(null, fieldName, true));
                    } catch (Exception e) {
                        BalancedVillagerTrades.LOGGER.warning(e.getMessage() + "! Skipping.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    @Override
    public void accept(TradeWrapper tradeWrapper) {
        BalancedVillagerTrades.LOGGER.info("Echo from recipe " + recipeName);
        fields.forEach((fieldName, field) -> {
            Object value = field.get(tradeWrapper);
            BalancedVillagerTrades.LOGGER.info("  " + fieldName + " : " + value);
            if (field instanceof ComplexField) {
                Collection<String> children = ((ComplexField<TradeWrapper, ?>) field).getFields(tradeWrapper);
                if (children != null)
                    BalancedVillagerTrades.LOGGER.info("  (contains fields: " + String.join(", ", children) + ")");
                else
                    BalancedVillagerTrades.LOGGER.info("  (may contain more fields)");
            }
        });
    }
}