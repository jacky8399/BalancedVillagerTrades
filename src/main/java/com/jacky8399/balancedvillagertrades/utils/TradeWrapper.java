package com.jacky8399.balancedvillagertrades.utils;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

public class TradeWrapper {
    public TradeWrapper(Villager villager, MerchantRecipe recipe, int index) {
        this.villager = villager;
        this.recipe = recipe;
        this.index = index;
    }

    private final Villager villager;
    private MerchantRecipe recipe;
    private int index;
    private boolean remove = false;

    public Villager getVillager() {
        return villager;
    }

    public MerchantRecipe getRecipe() {
        return recipe;
    }

    public int getIndex(){ return index; }
    public void setIndex(int index){ this.index = index; }

    public void setRecipe(MerchantRecipe recipe) {
        this.recipe = recipe;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }
}
