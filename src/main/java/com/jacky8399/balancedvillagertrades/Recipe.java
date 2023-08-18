package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import com.jacky8399.balancedvillagertrades.predicates.AndPredicate;
import com.jacky8399.balancedvillagertrades.predicates.TradePredicate;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.*;

public class Recipe {
    public static final HashMap<String, Recipe> RECIPES = new HashMap<>();

    public boolean ignoreRemoved = false;
    public TradePredicate predicate;
    public List<Action> actions;

    public final String name;
    public String desc;
    public boolean enabled = true;
    public Recipe(String name) {
        this.name = name;
    }

    private static final Set<String> VALID_CHILDREN = Set.of("desc", "enabled", "when", "do");

    @SuppressWarnings("unchecked")
    public void readFromMap(Map<String, Object> map) throws IllegalArgumentException {
        desc = String.valueOf(map.get("desc"));

        Object enabled = map.get("enabled");
        if (enabled instanceof Boolean bool) {
            this.enabled = bool;
        }

        Object whenMap = map.get("when");
        if (whenMap == null) {
            Config.addWarning("Recipe doesn't have a condition, and will run on every trade.");
            predicate = new AndPredicate(Collections.emptyList());
        } else {
            if (!(whenMap instanceof Map)) {
                throw new IllegalArgumentException("Expected map at 'when' section");
            }
            try {
                predicate = TradePredicate.getFromMap((Map<String, Object>) whenMap);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to load condition", e);
            }
        }

        Object doMap = map.get("do");
        if (doMap == null) {
            throw new IllegalArgumentException("No 'do' section to specify action");
        } else if (!(doMap instanceof Map)) {
            throw new IllegalArgumentException("Expected map at 'do' section");
        }
        actions = Action.getFromMap(name, (Map<String, Object>) doMap);

        for (String key : map.keySet()) {
            if (!VALID_CHILDREN.contains(key))
                Config.addWarning("Invalid section " + key + " in recipe");
        }
    }

    public boolean shouldHandle(TradeWrapper trade) {
        return enabled && predicate.test(trade);
    }

    public void handle(TradeWrapper trade) {
        for (Action action : actions) {
            action.accept(trade);
        }
    }
}
