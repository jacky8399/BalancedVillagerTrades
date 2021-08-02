package com.jacky8399.balancedvillagertrades.predicates;

import com.google.common.collect.ImmutableList;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class AndPredicate extends TradePredicate {
    private final ImmutableList<TradePredicate> predicates;
    public AndPredicate(Collection<? extends TradePredicate> predicates) {
        this.predicates = ImmutableList.copyOf(predicates);
    }

    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        for (TradePredicate predicate : predicates) {
            if (!predicate.test(tradeWrapper)) {
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
                .collect(Collectors.joining("\n  AND\n"))
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
