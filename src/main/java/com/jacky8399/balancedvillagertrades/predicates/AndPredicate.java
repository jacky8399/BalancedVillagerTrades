package com.jacky8399.balancedvillagertrades.predicates;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class AndPredicate extends TradePredicate {
    private final ImmutableList<TradePredicate> predicates;
    public AndPredicate(Collection<? extends TradePredicate> predicates) {
        this.predicates = ImmutableList.copyOf(predicates);
    }

    @Override
    public boolean test(Villager villager, MerchantRecipe recipe) {
        for (TradePredicate predicate : predicates) {
            if (!predicate.test(villager, recipe)) {
//                BalancedVillagerTrades.LOGGER.info("And predicate failed at " + predicate);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return predicates.stream()
                .map(TradePredicate::toString)
                .collect(Collectors.joining("\n  ", "all match:\n  ", ""))
                .trim();
    }

    @Override
    public @NotNull TradePredicate and(@NotNull TradePredicate other) {
        ImmutableList.Builder<TradePredicate> predicates = ImmutableList.builder();
        predicates.addAll(this.predicates);
        if (other instanceof AndPredicate) {
            AndPredicate otherAnd = (AndPredicate) other;
            predicates.addAll(otherAnd.predicates);
        } else {
            predicates.add(other);
        }
        return new AndPredicate(predicates.build());
    }
}
