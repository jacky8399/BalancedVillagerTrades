package com.jacky8399.balancedvillagertrades.predicates;

import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultPredicate extends ItemPredicate {
    public ResultPredicate(ItemStack stack, Set<ComplexItemMatcher> matchers, List<ItemMatcher> simpleMatchers) {
        super(stack, matchers, simpleMatchers);
    }

    @SuppressWarnings("unchecked")
    public static ResultPredicate parse(Object obj) throws IllegalArgumentException {
        if (!(obj instanceof Map<?, ?>))
            throw new IllegalArgumentException("Expected map");
        Map<String, Object> map = (Map<String, Object>) obj;
        Object matchers = map.get("matches");
        if (!(matchers instanceof List<?>)) {
            throw new IllegalArgumentException("Expected list at matches");
        }
        return new ResultPredicate(new ItemStack(Material.AIR), Collections.emptySet(),
                ItemPredicate.getFromInput((List<String>) matchers));
    }

    @Override
    public @Nullable ItemStack getStack(AbstractVillager villager, MerchantRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    public String toString() {
        return "result:\n" + super.toString();
    }
}
