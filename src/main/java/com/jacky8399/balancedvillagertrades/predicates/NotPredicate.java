package com.jacky8399.balancedvillagertrades.predicates;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

public class NotPredicate extends TradePredicate {
    private final TradePredicate predicate;
    public NotPredicate(TradePredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(Villager villager, MerchantRecipe recipe) {
        return !predicate.test(villager, recipe);
    }

    @Override
    public String toString() {
        return "not " + predicate;
    }

    @NotNull
    @Override
    public TradePredicate negate() {
        return predicate;
    }
}
