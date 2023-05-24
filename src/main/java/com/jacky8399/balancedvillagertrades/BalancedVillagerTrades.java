package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.utils.reputation.ReputationProvider;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public final class BalancedVillagerTrades extends JavaPlugin {

    @Override
    public void onEnable() {
        INSTANCE = this;
        LOGGER = getLogger();

        getCommand("balancedvillagertrades").setExecutor(new CommandBvt());

        ReputationProvider.loadMappings();

        Bukkit.getPluginManager().registerEvents(new Events(), this);

        saveDefaultConfig();
        reloadConfig();

        new Metrics(this, 11784);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        Config.reloadConfig();
    }

    @Override
    public void onDisable() {

    }

    public static BalancedVillagerTrades INSTANCE;
    public static Logger LOGGER;
    @Nullable
    public static ReputationProvider REPUTATION;
}
