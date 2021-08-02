package com.jacky8399.balancedvillagertrades.predicates;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;

public class NotPredicate extends TradePredicate {
    private final TradePredicate predicate;
    public NotPredicate(TradePredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        return !predicate.test(tradeWrapper);
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
