package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
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
import java.util.ListIterator;

public class Events implements Listener {
//
//    public static void fixTrade(Villager villager, MerchantRecipe trade) {
//        // stick trade
//        if (Config.nerfStickTrade && villager.getProfession() == Villager.Profession.FLETCHER) {
//            ItemStack item1 = trade.getIngredients().get(0), item2 = trade.getIngredients().get(1);
//            // maybe we modified it before, so we need to check both stacks
//            if (item1.getType() != Material.STICK || (item2.getType() != Material.STICK && item2.getType() != Material.AIR))
//                return;
//            // set ingredients
//            int originalAmount = trade.getIngredients().stream().mapToInt(ItemStack::getAmount).sum();
//            int amount = Config.clamp(originalAmount, Config.nerfStickTradeMinAmount, Config.nerfStickTradeMaxAmount);
//            int amount1 = Math.min(amount, 64), amount2 = amount - amount1;
//            List<ItemStack> newIngredients = new ArrayList<>();
//            newIngredients.add(new ItemStack(Material.STICK, amount1));
//            if (amount2 > 0)
//                newIngredients.add(new ItemStack(Material.STICK, amount2));
//            trade.setIngredients(newIngredients);
//            trade.setMaxUses(Config.nerfStickTradeMaxUses);
//            if (Config.nerfStickTradeDisableDiscounts && trade.getPriceMultiplier() != 0)
//                trade.setPriceMultiplier(0);
//        }
//        // bookshelves & book trade
//        if (Config.nerfBookshelvesExploit && villager.getProfession() == Villager.Profession.LIBRARIAN &&
//                trade.getIngredients().get(0).getType() == Material.BOOK && trade.getIngredients().get(1).getType() == Material.AIR) {
//            int originalPrice = trade.getIngredients().get(0).getAmount();
//            if (originalPrice < Config.nerfBookshelvesExploitMinAmount)
//                // does this work idk
//                trade.setIngredients(Collections.singletonList(new ItemStack(Material.BOOK, Config.nerfBookshelvesExploitMinAmount)));
//            if (Config.nerfBookshelvesExploitDisableDiscounts)
//                trade.setPriceMultiplier(0);
//        }
//    }

    // patch trades
    @EventHandler(ignoreCancelled = true)
    public void onNewTrade(VillagerAcquireTradeEvent e) {
        if (e.getEntity() instanceof Villager) {
            TradeWrapper trade = new TradeWrapper((Villager) e.getEntity(), e.getRecipe());
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
        if (e.getRightClicked() instanceof Villager) {
            Villager villager = (Villager) e.getRightClicked();
            List<MerchantRecipe> newRecipes = new ArrayList<>(villager.getRecipes());
            for (ListIterator<MerchantRecipe> iterator = newRecipes.listIterator(); iterator.hasNext();) {
                TradeWrapper trade = new TradeWrapper(villager, iterator.next());
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
            if (NMSUtils.REPUTATION_ADD_REPUTATION == null)
                return;
            Villager villager = (Villager) e.getEntity();
            // find players in radius
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld() == villager.getWorld() && player.getLocation()
                        .distance(villager.getLocation()) <= Config.nerfNegativeReputationOnKilledRadius) {
                    // add gossips
                    NMSUtils.addGossip(villager, player.getUniqueId(),
                            // major positives are permanent
                            NMSUtils.ReputationTypeWrapped.MAJOR_NEGATIVE, Config.nerfNegativeReputationOnKilledReputationPenalty
                    );
                    // show angry particles
                    player.spawnParticle(Particle.VILLAGER_ANGRY, villager.getLocation().add(0, villager.getHeight() + 0.5, 0), 4, 0.5, 0.5, 0.5);
                }
            }
//            ZombieVillager zombieVillager = (ZombieVillager) e.getTransformedEntity();
//            // copy nbt to zombie villager
//            NMSUtils.copyGossipsFrom(zombieVillager, gossips);
        }
    }
}
