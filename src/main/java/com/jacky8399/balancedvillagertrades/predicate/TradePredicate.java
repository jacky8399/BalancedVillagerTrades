package com.jacky8399.balancedvillagertrades.predicate;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.function.BiPredicate;

public abstract class TradePredicate implements BiPredicate<Villager, MerchantRecipe> {

}
