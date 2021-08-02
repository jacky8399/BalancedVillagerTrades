package com.jacky8399.balancedvillagertrades.predicates;

import com.google.common.collect.ImmutableList;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class OrPredicate extends TradePredicate {
    private final ImmutableList<? extends TradePredicate> predicates;
    public OrPredicate(Collection<? extends TradePredicate> predicates) {
        this.predicates = ImmutableList.copyOf(predicates);
    }

    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        for (TradePredicate predicate : predicates) {
            if (predicate.test(tradeWrapper))
                return true;
        }
        return predicates.size() == 0; // always true if empty
    }

    @Override
    public String toString() {
        return predicates.stream()
                .map(TradePredicate::toString)
                .collect(Collectors.joining(("\n  OR\n")))
                .trim();
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
