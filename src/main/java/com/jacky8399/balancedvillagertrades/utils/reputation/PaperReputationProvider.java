package com.jacky8399.balancedvillagertrades.utils.reputation;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class PaperReputationProvider extends ReputationProvider {

    public PaperReputationProvider() {
        BalancedVillagerTrades.LOGGER.info("Using Paper villager reputation API");
    }

    @Override
    public void addGossip(Villager villager, UUID uuid, ReputationTypeWrapped reputationType, int amount) {
        Reputation reputation = villager.getReputation(uuid);
        if (reputation == null)
            reputation = new Reputation();
        ReputationType paperType = ReputationType.values()[reputationType.ordinal()];
        reputation.setReputation(paperType, reputation.getReputation(paperType) + amount);
        villager.setReputation(uuid, reputation);
    }
}
