package com.jacky8399.balancedvillagertrades;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;

import java.lang.reflect.Method;
import java.util.UUID;

// wow what a mess
public class NMSUtils {
    public static final ImmutableMap<String, String[]> MAPPINGS = ImmutableMap.<String, String[]>builder()
            // example
            .put("mapping version", new String[]{"EntityZombieVillager.setGossips", "EntityVillager.getGossips", "DynamicOpsNBT.INSTANCE", "Reputation.store", "Reputation.add"})
            // mappings
            .put("v1_16_R1", new String[]{"a", "fj", "a", "a", "a"})
            .put("v1_16_R2", new String[]{"a", "fj", "a", "a", "a"})
            .put("v1_16_R3", new String[]{"a", "fj", "a", "a", "a"})
            .build();
    public static String mappingsVersion;

    static {
        Class<? extends Server> clazz = Bukkit.getServer().getClass();
        // org.bukkit.craftbukkit.v1_16_R3.CraftServer
        mappingsVersion = clazz.getName().split("\\.")[3];
    }

    public static void loadMappings() {
        if (!MAPPINGS.containsKey(mappingsVersion)) {
            BalancedVillagerTrades.LOGGER.warning("Version " + Bukkit.getVersion() + " (mappings: " + mappingsVersion + ") is not tested!");
            BalancedVillagerTrades.LOGGER.warning("Some features may not work!");
            guessMappings();
        } else {
            loadMappingsSupported();
            if (NMSUtils.REPUTATION_ADD_REPUTATION != null)
                BalancedVillagerTrades.LOGGER.info("Loaded mappings for " + NMSUtils.mappingsVersion);
            else
                BalancedVillagerTrades.LOGGER.severe("Something went wrong!");
        }
    }

    public static boolean isSupportedVersion() {
        return MAPPINGS.containsKey(mappingsVersion);
    }

    public static void copyGossipsFrom(ZombieVillager zombieVillager, Object gossips) {
        Object nms = getHandle(zombieVillager);
        try {
            Object dynamic = REPUTATION_STORE.invoke(gossips, DYNAMIC_OPS_NBT_INSTANCE);
            Object nbt = DYNAMIC_GET_VALUE.invoke(dynamic);
            ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS.invoke(nms, nbt);
        } catch (Exception ignored) {}
    }

    public static void addGossip(Object gossips, UUID uuid, ReputationTypeWrapped reputationType, int amount) {
        try {
            REPUTATION_ADD_REPUTATION.invoke(gossips, uuid, reputationType.nmsCopy, amount);
        } catch (Exception ignored) {}
    }

    public static Object getGossips(Villager villager) {
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

    public static Class<?> getNMSClazz(String name) {
        String clazzName = "net.minecraft.server." + mappingsVersion + "." + name;
        try {
            return Class.forName(clazzName);
        } catch (Exception e) {
            if (!isSupportedVersion()) {
                BalancedVillagerTrades.LOGGER.warning("Class " + clazzName + " not found!");
                return null;
            } else {
                // shouldn't happen
                throw new Error(e);
            }
        }
    }

    // utilities
    public static Class<?> NBT_BASE_CLAZZ = getNMSClazz("NBTBase");
    public static Class<?> DYNAMIC_CLAZZ; // what the hell is a dynamic
    public static Method DYNAMIC_GET_VALUE;
    public static Class<?> DYNAMIC_OPS_CLAZZ;
    public static Class<?> DYNAMIC_OPS_NBT_CLAZZ = getNMSClazz("DynamicOpsNBT");
    public static Object DYNAMIC_OPS_NBT_INSTANCE;

    static {
        try {
            DYNAMIC_CLAZZ = Class.forName("com.mojang.serialization.Dynamic");
            DYNAMIC_GET_VALUE = DYNAMIC_CLAZZ.getMethod("getValue");
            DYNAMIC_OPS_CLAZZ = Class.forName("com.mojang.serialization.DynamicOps");
        } catch (Exception ignored) {}
    }

    public static Class<?> ENTITY_ZOMBIE_VILLAGER_CLAZZ = getNMSClazz("EntityZombieVillager");
    public static Class<?> ENTITY_VILLAGER_CLAZZ = getNMSClazz("EntityVillager");
    public static Class<?> REPUTATION_CLAZZ = getNMSClazz("Reputation");
    public static Class<?> REPUTATION_TYPE_CLAZZ = getNMSClazz("ReputationType");

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

    public static Method ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS;
    public static Method ENTITY_VILLAGER_GET_GOSSIPS;
    public static Method REPUTATION_STORE;
    public static Method REPUTATION_ADD_REPUTATION;


    public static void guessMappings() {
        // TODO guess mappings by method signature?
    }

    public static void loadMappingsSupported() {
        String[] mappings = MAPPINGS.get(mappingsVersion);
        if (mappings == null) {
            BalancedVillagerTrades.LOGGER.severe("Mappings for " + mappingsVersion + " was null?");
            return;
        }
        try {
            ENTITY_ZOMBIE_VILLAGER_SET_GOSSIPS = ENTITY_ZOMBIE_VILLAGER_CLAZZ.getMethod(mappings[0], NBT_BASE_CLAZZ);
            ENTITY_VILLAGER_GET_GOSSIPS = ENTITY_VILLAGER_CLAZZ.getMethod(mappings[1]);
            DYNAMIC_OPS_NBT_INSTANCE = DYNAMIC_OPS_NBT_CLAZZ.getField(mappings[2]).get(null);
            REPUTATION_STORE = REPUTATION_CLAZZ.getMethod(mappings[3], DYNAMIC_OPS_CLAZZ);
            REPUTATION_ADD_REPUTATION = REPUTATION_CLAZZ.getMethod(mappings[4], UUID.class, REPUTATION_TYPE_CLAZZ, int.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
