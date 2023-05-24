package com.jacky8399.balancedvillagertrades.utils.reputation;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import org.bukkit.entity.Villager;

import java.util.UUID;

public abstract class ReputationProvider {
    public enum ReputationTypeWrapped {
        MAJOR_NEGATIVE,
        MINOR_NEGATIVE,
        MINOR_POSITIVE,
        MAJOR_POSITIVE,
        TRADING
    }

    public abstract void addGossip(Villager villager, UUID uuid, ReputationTypeWrapped reputationType, int amount);


    public static void loadMappings() {
        try {
            Class.forName("com.destroystokyo.paper.entity.villager.Reputation");
            BalancedVillagerTrades.REPUTATION = new PaperReputationProvider();
        } catch (ClassNotFoundException ignored) {

        }

    }
}
