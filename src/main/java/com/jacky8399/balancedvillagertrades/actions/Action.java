package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Action implements Consumer<TradeWrapper> {
    @SuppressWarnings("unchecked")
    public static List<Action> getFromMap(String name, Map<String, Object> map) throws IllegalArgumentException {
        List<Action> actions = new ArrayList<>();
        Object setMap = map.get("set");
        if (setMap != null) {
            if (!(setMap instanceof Map))
                throw new IllegalArgumentException("Expected map at 'set' section");
            actions.addAll(ActionSet.parse((Map<String, Object>) setMap));
        }
        Object removeBool = map.get("remove");
        if (removeBool != null) {
            if (!(removeBool instanceof Boolean))
                throw new IllegalArgumentException("Expected boolean at 'remove' section");
            actions.add(new ActionRemove((Boolean) removeBool));
        }
        Object echoList = map.get("echo");
        if (echoList != null) {
            if (!(echoList instanceof List))
                throw new IllegalArgumentException("Expected list at 'echo' section");
            actions.add(ActionEcho.parse(name, (List<?>) echoList));
        }
        Object luaString = map.get("lua");
        if (luaString != null) {
            if (!(luaString instanceof String string))
                throw new IllegalArgumentException("Expected string at 'lua' section");
            actions.add(ActionLua.fromString(name, string));
        }
        Object luaFileString = map.get("lua-file");
        if (luaFileString != null) {
            if (!(luaFileString instanceof String string))
                throw new IllegalArgumentException("Expected string at 'lua-file' section");
            actions.add(ActionLua.fromFile(name, string));
        }
        if (actions.size() == 0)
            throw new IllegalArgumentException("Empty action");
        return actions;
    }
}
