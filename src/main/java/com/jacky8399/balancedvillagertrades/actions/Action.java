package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Action implements Consumer<TradeWrapper> {
    @SuppressWarnings("unchecked")
    public static List<? extends Action> getFromMap(Map<String, Object> map) throws IllegalArgumentException {
        Object setMap = map.get("set");
        if (setMap != null) {
            if (!(setMap instanceof Map))
                throw new IllegalArgumentException("Expected map at 'set' section");
            return ActionSet.parse((Map<String, Object>) setMap);
        }
        Object removeBool = map.get("remove");
        if (removeBool != null) {
            if (!(removeBool instanceof Boolean))
                throw new IllegalArgumentException("Expected boolean at 'remove' section");
            return Collections.singletonList(new ActionRemove((Boolean) removeBool));
        }
        throw new IllegalArgumentException("Empty action");
    }
}
