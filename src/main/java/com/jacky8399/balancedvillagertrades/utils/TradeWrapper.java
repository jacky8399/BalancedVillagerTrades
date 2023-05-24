package com.jacky8399.balancedvillagertrades.utils;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

public class TradeWrapper {
    public TradeWrapper(Villager villager, MerchantRecipe recipe, int index, boolean newRecipe) {
        this.villager = villager;
        this.recipe = recipe;
        this.index = index;
        this.newRecipe = newRecipe;
    }

    private final Villager villager;
    private MerchantRecipe recipe;
    private final int index;
    private final boolean newRecipe;
    private boolean remove = false;

    public Villager getVillager() {
        return villager;
    }

    public MerchantRecipe getRecipe() {
        return recipe;
    }

    public int getIndex() {
        return index;
    }

    public boolean isNewRecipe() {
        return newRecipe;
    }

    public void setRecipe(MerchantRecipe recipe) {
        this.recipe = recipe;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    @Override
    public String toString() {
        String recipeInfo = recipe.getIngredients() + " -> " + recipe.getResult();
        String villager = getVillager().getCustomName();
        return "Trade [%s] (index=%d) from villager %s (%s)"
                .formatted(recipeInfo, getIndex(),
                        villager != null ? villager : "", getVillager().getUniqueId());
    }
}
