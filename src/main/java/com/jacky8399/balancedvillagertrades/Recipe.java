package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.predicates.AndPredicate;
import com.jacky8399.balancedvillagertrades.predicates.TradePredicate;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.*;

public class Recipe {
    public static final HashMap<String, Recipe> RECIPES = new HashMap<>();

    public boolean ignoreRemoved = false;
    public TradePredicate predicate;

    public final String name;
    public String desc;
    public Recipe(String name) {
        this.name = name;
    }

    public void readFromMap(Map<String, Object> map) throws IllegalArgumentException {
        desc = String.valueOf(map.get("desc"));

        Object whenMap = map.get("when");
        if (whenMap == null) {
            BalancedVillagerTrades.LOGGER.warning("Recipe " + name + " without condition? This will run on every villager trade!");
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
        // TODO parse action
    }

    public boolean shouldHandle(TradeWrapper trade) {
        return predicate.test(trade);
    }

    public void handle(TradeWrapper trade) {
        // TODO actions
    }
}
