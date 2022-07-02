package com.jacky8399.balancedvillagertrades.utils.reputation;

import net.minecraft.world.entity.ai.gossip.GossipType;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftVillager;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class NMSNewReputationProvider extends ReputationProvider {
    @Override
    public void addGossip(Villager villager, UUID uuid, ReputationTypeWrapped reputationType, int amount) {
        net.minecraft.world.entity.npc.Villager nmsVillager = ((CraftVillager) villager).getHandle();
        nmsVillager.getGossips().add(uuid, GossipType.values()[reputationType.ordinal()], amount);
    }

    @Override
    public String toString() {
        return "Latest NMS version";
    }
}
