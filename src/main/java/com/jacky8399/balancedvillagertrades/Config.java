package com.jacky8399.balancedvillagertrades;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public static void reloadConfig() {
        FileConfiguration config = BalancedVillagerTrades.INSTANCE.getConfig();
        nerfStickTrade = config.getBoolean("nerfs.stick-trade.enabled");
        nerfStickTradeMaxUses = config.getInt("nerfs.stick-trade.max-uses");
        nerfStickTradeMinAmount = clamp(config.getInt("nerfs.stick-trade.min-items"), 1, 128);
        nerfStickTradeMaxAmount = clamp(config.getInt("nerfs.stick-trade.max-items"), nerfStickTradeMinAmount, 128);
        nerfStickTradeDisableDiscounts = config.getBoolean("nerfs.stick-trade.disable-discounts");

        nerfBookshelvesExploit = config.getBoolean("nerfs.bookshelves-exploit.enabled");
        nerfBookshelvesExploitMinAmount = clamp(config.getInt("nerfs.bookshelves-exploit.min-items"), 1, 64);
        nerfBookshelvesExploitDisableDiscounts = config.getBoolean("nerfs.bookshelves-exploit.disable-discounts");

        nerfNegativeReputationOnKilled = config.getBoolean("nerfs.negative-reputation-on-killed.enabled");
        nerfNegativeReputationOnKilledRadius = config.getDouble("nerfs.negative-reputation-on-killed.radius");
        nerfNegativeReputationOnKilledReputationPenalty = clamp(config.getInt("nerfs.negative-reputation-on-killed.reputation-penalty"), 1, 100);
    }

    public static int clamp(int original, int min, int max) {
        return Math.max(Math.min(original, max), min);
    }

    public static boolean nerfStickTrade;
    public static boolean nerfStickTradeDisableDiscounts;
    public static int nerfStickTradeMaxUses;
    public static int nerfStickTradeMinAmount;
    public static int nerfStickTradeMaxAmount;

    public static boolean nerfBookshelvesExploit;
    public static int nerfBookshelvesExploitMinAmount;
    public static boolean nerfBookshelvesExploitDisableDiscounts;

    public static boolean nerfNegativeReputationOnKilled;
    public static double nerfNegativeReputationOnKilledRadius;
    public static int nerfNegativeReputationOnKilledReputationPenalty;
}
