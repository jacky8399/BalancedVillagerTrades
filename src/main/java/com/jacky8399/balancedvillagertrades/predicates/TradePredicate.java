package com.jacky8399.balancedvillagertrades.predicates;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class TradePredicate implements BiPredicate<Villager, MerchantRecipe>, Predicate<TradeWrapper> {
    @NotNull
    public TradePredicate and(@NotNull TradePredicate other) {
        return new AndPredicate(Arrays.asList(this, other));
    }

    @NotNull
    @Override
    public TradePredicate negate() {
        return new NotPredicate(this);
    }

    @NotNull
    public TradePredicate or(@NotNull TradePredicate other) {
        return new OrPredicate(Arrays.asList(this, other));
    }

    @Override
    public boolean test(final TradeWrapper tradeWrapper) {
        return test(tradeWrapper.getVillager(), tradeWrapper.getRecipe());
    }

    public static final HashMap<String, Function<Object, TradePredicate>> CONSTRUCTORS = new HashMap<>();
    private static void initConstructors() {
        CONSTRUCTORS.put("ingredient-0", map -> IngredientPredicate.parse(0, map));
        CONSTRUCTORS.put("ingredient-1", map -> IngredientPredicate.parse(1, map));
        CONSTRUCTORS.put("result", ResultPredicate::parse);
        CONSTRUCTORS.put("villager", VillagerJobPredicate::parse);
    }
    @SuppressWarnings("unchecked")
    public static TradePredicate getFromMap(Map<String, Object> map) throws IllegalArgumentException {
        if (!CONSTRUCTORS.containsKey("result"))
            initConstructors();

        List<TradePredicate> predicates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String str = entry.getKey();
            Object val = entry.getValue();
            if (str.equalsIgnoreCase("and") || str.equalsIgnoreCase("or")) {
                Function<List<? extends TradePredicate>, TradePredicate> constructor =
                        str.equalsIgnoreCase("and") ? AndPredicate::new : OrPredicate::new;
                if (!(val instanceof List<?>)) {
                    throw new IllegalArgumentException("Expected a list inside " + str);
                }
                List<TradePredicate> list = new ArrayList<>();
                for (Object obj : (List<?>) val) {
                    if (!(obj instanceof Map<?, ?>)) {
                        throw new IllegalArgumentException("Expected a map inside list " + str);
                    }
                    Map<String, Object> innerMap = (Map<String, Object>) obj;
                    try {
                        list.add(getFromMap(innerMap));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("An exception occurred inside list " + str, e);
                    }
                }

                predicates.add(constructor.apply(list));
            } else if (str.equalsIgnoreCase("not")) {
                if (!(val instanceof Map<?, ?>))
                    throw new IllegalArgumentException("Expected a map inside " + str);
                try {
                    predicates.add(getFromMap((Map<String, Object>) val).negate());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("An exception occurred inside " + str, e);
                }
            }
            Function<Object, TradePredicate> constructor = CONSTRUCTORS.get(str);
            if (constructor == null)
                throw new IllegalArgumentException("Don't know how to match " + str);
            try {
                predicates.add(constructor.apply(val));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("An exception occurred inside " + str, e);
            }
        }
        if (predicates.size() == 1)
            return predicates.get(0);
        else
            return new AndPredicate(predicates);
    }
}
