package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    public static final List<Recipe> RECIPES = new ArrayList<>();

    public boolean ignoreRemoved = false;

    public boolean shouldHandle(TradeWrapper trade) {
        return true;
    }

    public void handle(TradeWrapper trade) {

    }
}
