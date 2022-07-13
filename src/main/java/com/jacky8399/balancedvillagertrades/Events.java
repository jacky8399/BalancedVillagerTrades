package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import com.jacky8399.balancedvillagertrades.utils.reputation.ReputationProvider;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class Events implements Listener {

    // patch trades
    @EventHandler(ignoreCancelled = true)
    public void onNewTrade(VillagerAcquireTradeEvent e) {
        if (e.getEntity() instanceof Villager villager) {
            TradeWrapper trade = new TradeWrapper(villager, e.getRecipe(), villager.getRecipeCount(), true);
            for (Recipe recipe : Recipe.RECIPES.values()) {
                if (recipe.ignoreRemoved && trade.isRemove())
                    continue;
                if (recipe.shouldHandle(trade))
                    recipe.handle(trade);
            }
            if (trade.isRemove()) {
                e.setCancelled(true);
                return;
            }
            e.setRecipe(trade.getRecipe());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Villager villager) {
            List<MerchantRecipe> newRecipes = new ArrayList<>(villager.getRecipes());
            for (var iterator = newRecipes.listIterator(); iterator.hasNext();) {
                int index = iterator.nextIndex();
                TradeWrapper trade = new TradeWrapper(villager, iterator.next(), index, false);
                for (Recipe recipe : Recipe.RECIPES.values()) {
                    if (recipe.ignoreRemoved && trade.isRemove())
                        continue;
                    if (recipe.shouldHandle(trade))
                        recipe.handle(trade);
                }
                if (trade.isRemove()) {
                    iterator.remove();
                    continue;
                }
                iterator.set(trade.getRecipe());
            }
            villager.setRecipes(newRecipes);
        }
    }

    // negative reputation
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent e) {
        if (Config.nerfNegativeReputationOnKilled &&
                e.getTransformReason() == EntityTransformEvent.TransformReason.INFECTION) {
            // check if mappings is loaded
            if (BalancedVillagerTrades.REPUTATION == null)
                return;
            Villager villager = (Villager) e.getEntity();
            // find players in radius
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld() == villager.getWorld() && player.getLocation()
                        .distance(villager.getLocation()) <= Config.nerfNegativeReputationOnKilledRadius) {
                    // add gossips
                    BalancedVillagerTrades.REPUTATION.addGossip(villager, player.getUniqueId(),
                            // major positives are permanent
                            ReputationProvider.ReputationTypeWrapped.MAJOR_NEGATIVE, Config.nerfNegativeReputationOnKilledReputationPenalty
                    );
                    // show angry particles
                    player.spawnParticle(Particle.VILLAGER_ANGRY, villager.getLocation().add(0, villager.getHeight() + 0.5, 0), 4, 0.5, 0.5, 0.5);
                }
            }
        }
    }
}
