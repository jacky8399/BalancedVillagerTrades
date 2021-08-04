package com.jacky8399.balancedvillagertrades.utils.reputation;

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
}
