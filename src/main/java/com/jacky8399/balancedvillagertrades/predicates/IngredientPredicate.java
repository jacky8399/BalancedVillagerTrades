package com.jacky8399.balancedvillagertrades.predicates;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IngredientPredicate extends ItemPredicate {

    public IngredientPredicate(ItemStack stack, Set<ComplexItemMatcher> matchers, List<ItemMatcher> simpleMatchers, int ingredient) {
        super(stack, matchers, simpleMatchers);
        Preconditions.checkArgument(ingredient == 0 || ingredient == 1, "Ingredient out of bounds");
        this.ingredient = ingredient;
    }

    @SuppressWarnings("unchecked")
    public static IngredientPredicate parse(int ingredient, Object obj) throws IllegalArgumentException {
        if (!(obj instanceof Map<?, ?>))
            throw new IllegalArgumentException("Expected map");
        Map<String, Object> map = (Map<String, Object>) obj;
        Object matchers = map.get("matches");
        if (!(matchers instanceof List<?>)) {
            throw new IllegalArgumentException("Expected list at matches");
        }
        return new IngredientPredicate(new ItemStack(Material.AIR), Collections.emptySet(),
                ItemPredicate.getFromInput((List<String>) matchers), ingredient);
    }

    public final int ingredient;

    @Override
    public @Nullable ItemStack getStack(Villager villager, MerchantRecipe recipe) {
        List<ItemStack> ingredients = recipe.getIngredients();
        if (ingredients.size() > ingredient) {
            return ingredients.get(ingredient);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ingredient " + ingredient + ":\n" + super.toString();
    }
}
