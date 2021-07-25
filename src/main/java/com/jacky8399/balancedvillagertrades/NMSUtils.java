package com.jacky8399.balancedvillagertrades;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Villager;

import java.lang.reflect.Method;
import java.util.UUID;

// wow what a mess
public class NMSUtils {
    public static String mappingsVersion;

    static {
        Class<? extends Server> clazz = Bukkit.getServer().getClass();
        // org.bukkit.craftbukkit.v1_16_R3.CraftServer
        mappingsVersion = clazz.getName().split("\\.")[3];
    }

    private static final String NMS_PACKAGE = "net.minecraft.server." + mappingsVersion + ".";
    public static final ImmutableMap<String, String[]> MAPPINGS = ImmutableMap.<String, String[]>builder()
            // example
            .put("mapping version", new String[]{
                    "EntityVillager.class", "EntityVillager.getGossips", "Reputation.class", "Reputation.add", "ReputationType.class"
            })
            // mappings
            .put("v1_16_R1", new String[]{NMS_PACKAGE + "EntityVillager", "fj", NMS_PACKAGE + "Reputation", "a", NMS_PACKAGE + "ReputationType"})
            .put("v1_16_R2", new String[]{NMS_PACKAGE + "EntityVillager", "fj", NMS_PACKAGE + "Reputation", "a", NMS_PACKAGE + "ReputationType"})
            .put("v1_16_R3", new String[]{NMS_PACKAGE + "EntityVillager", "fj", NMS_PACKAGE + "Reputation", "a", NMS_PACKAGE + "ReputationType"})
            // i think they repackaged NMS once??
            .put("v_16_R3_Repackaged", new String[]{"net.minecraft.world.entity.npc.EntityVillager", "fj", "net.minecraft.world.entity.ai.gossip.Reputation", "a", "net.minecraft.world.entity.ai.gossip.ReputationType"})
            .put("v1_17_R1", new String[]{"net.minecraft.world.entity.npc.EntityVillager", null, "net.minecraft.world.entity.ai.gossip.Reputation", "a", "net.minecraft.world.entity.ai.gossip.ReputationType"})
            .build();

    public static void loadMappings() {
        if (!MAPPINGS.containsKey(mappingsVersion)) {
            BalancedVillagerTrades.LOGGER.warning("Version " + Bukkit.getVersion() + " (mappings: " + mappingsVersion + ") is not supported!");
            BalancedVillagerTrades.LOGGER.warning("Negative reputation when villagers are killed will not work.");
        } else {
            loadMappingsSupported();
            if (NMSUtils.REPUTATION_ADD_REPUTATION != null) {
                BalancedVillagerTrades.LOGGER.info("Loaded mappings for " + NMSUtils.mappingsVersion);
            } else if ("v1_16_R3".equals(mappingsVersion)) {
                mappingsVersion = "v_16_R3_Repackaged";
                loadMappingsSupported();
            } else {
                BalancedVillagerTrades.LOGGER.severe("Something went wrong! (mappings: " + mappingsVersion + ")");
                BalancedVillagerTrades.LOGGER.warning("Negative reputation when villagers are killed will not work.");
            }
        }
    }

    //    public static void copyGossipsFrom(ZombieVillager zombieVillager, Object gossips) {
//        Object nms = getHandle(zombieVillager);
//        try {
//            Object dynamic = REPUTATION_STORE.invoke(gossips, DYNAMIC_OPS_NBT_INSTANCE);
//            Object nbt = DYNAMIC_GET_VALUE.invoke(dynamic);
//            ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS.invoke(nms, nbt);
//        } catch (Exception ignored) {}
//    }

    public static void addGossip(Villager villager, UUID uuid, ReputationTypeWrapped reputationType, int amount) {
        try {
            Object gossips = getGossips(villager);
            REPUTATION_ADD_REPUTATION.invoke(gossips, uuid, reputationType.nmsCopy, amount);
        } catch (Exception ignored) {}
    }

    private static Object getGossips(Villager villager) {
        Object nms = getHandle(villager);
        try {
            return ENTITY_VILLAGER_GET_GOSSIPS.invoke(nms);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getHandle(Object bukkit) {
        try {
            return bukkit.getClass().getMethod("getHandle").invoke(bukkit);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // utilities
//    public static Class<?> NBT_BASE_CLAZZ = getNMSClazz("NBTBase");
//    public static Class<?> DYNAMIC_CLAZZ; // what the hell is a dynamic
//    public static Method DYNAMIC_GET_VALUE;
//    public static Class<?> DYNAMIC_OPS_CLAZZ;
//    public static Class<?> DYNAMIC_OPS_NBT_CLAZZ = getNMSClazz("DynamicOpsNBT");
//    public static Object DYNAMIC_OPS_NBT_INSTANCE;

//    static {
//        try {
//            DYNAMIC_CLAZZ = Class.forName("com.mojang.serialization.Dynamic");
//            DYNAMIC_GET_VALUE = DYNAMIC_CLAZZ.getMethod("getValue");
//            DYNAMIC_OPS_CLAZZ = Class.forName("com.mojang.serialization.DynamicOps");
//        } catch (Exception ignored) {}
//    }

//    public static Class<?> ENTITY_ZOMBIE_VILLAGER_CLAZZ = getNMSClazz("EntityZombieVillager");
    public static Class<?> ENTITY_VILLAGER_CLAZZ;
    public static Class<?> REPUTATION_CLAZZ;
    public static Class<?> REPUTATION_TYPE_CLAZZ;

    public enum ReputationTypeWrapped {
        MAJOR_NEGATIVE,
        MINOR_NEGATIVE,
        MINOR_POSITIVE,
        MAJOR_POSITIVE,
        TRADING;
        public final Object nmsCopy;
        @SuppressWarnings({"unchecked", "rawtypes"})
        ReputationTypeWrapped() {
            nmsCopy = Enum.valueOf((Class) REPUTATION_TYPE_CLAZZ, name());
        }
    }

//    public static Method ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS;
    public static Method ENTITY_VILLAGER_GET_GOSSIPS;
//    public static Method REPUTATION_STORE;
    public static Method REPUTATION_ADD_REPUTATION;


    public static void loadMappingsSupported() {
        String[] mappings = MAPPINGS.get(mappingsVersion);
        if (mappings == null) {
            BalancedVillagerTrades.LOGGER.severe("Mappings for " + mappingsVersion + " was null?");
            return;
        }
        try {
//            ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS = ENTITY_ZOMBIE_VILLAGER_CLAZZ.getMethod(mappings[0], NBT_BASE_CLAZZ);
            ENTITY_VILLAGER_CLAZZ = Class.forName(mappings[0]);
            REPUTATION_CLAZZ = Class.forName(mappings[2]);
            if (mappings[1] != null) {
                ENTITY_VILLAGER_GET_GOSSIPS = ENTITY_VILLAGER_CLAZZ.getMethod(mappings[1]);
            } else { // to work around 1.17 perhaps
                for (Method method : ENTITY_VILLAGER_CLAZZ.getMethods()) {
                    if (method.getReturnType() == REPUTATION_CLAZZ) {
                        ENTITY_VILLAGER_GET_GOSSIPS = method;
                    }
                }
                if (ENTITY_VILLAGER_GET_GOSSIPS == null)
                    throw new IllegalStateException("Can't get reputation");
            }
//            DYNAMIC_OPS_NBT_INSTANCE = DYNAMIC_OPS_NBT_CLAZZ.getField(mappings[2]).get(null);
//            REPUTATION_STORE = REPUTATION_CLAZZ.getMethod(mappings[3], DYNAMIC_OPS_CLAZZ);
            REPUTATION_TYPE_CLAZZ = Class.forName(mappings[4]);
            REPUTATION_ADD_REPUTATION = REPUTATION_CLAZZ.getMethod(mappings[3], UUID.class, REPUTATION_TYPE_CLAZZ, int.class);
        } catch (Exception e) {
            BalancedVillagerTrades.LOGGER.severe(e.toString());
        }
    }
}
