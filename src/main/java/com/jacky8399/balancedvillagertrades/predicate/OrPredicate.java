package com.jacky8399.balancedvillagertrades.predicate;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class OrPredicate extends TradePredicate {
    private final ImmutableList<? extends TradePredicate> predicates;
    public OrPredicate(Collection<? extends TradePredicate> predicates) {
        this.predicates = ImmutableList.copyOf(predicates);
    }

    @Override
    public boolean test(Villager villager, MerchantRecipe recipe) {
        for (TradePredicate predicate : predicates) {
            if (predicate.test(villager, recipe))
                return true;
        }
        return predicates.size() == 0; // always true if empty
    }

    @Override
    public @NotNull TradePredicate or(@NotNull TradePredicate other) {
        ImmutableList.Builder<TradePredicate> predicates = ImmutableList.builder();
        predicates.addAll(this.predicates);
        if (other instanceof OrPredicate) {
            OrPredicate otherOr = (OrPredicate) other;
            predicates.addAll(otherOr.predicates);
        } else {
            predicates.add(other);
        }
        return new OrPredicate(predicates.build());
    }
}
