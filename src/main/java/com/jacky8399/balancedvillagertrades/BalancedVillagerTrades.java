package com.jacky8399.balancedvillagertrades;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class BalancedVillagerTrades extends JavaPlugin {

    @Override
    public void onEnable() {
        INSTANCE = this;
        LOGGER = getLogger();
        LOGGER.info("Enabling BalancedVillagerTrades " + getDescription().getVersion());

        getCommand("balancedvillagertrades").setExecutor(new CommandBvt());

        NMSUtils.loadMappings();

        Bukkit.getPluginManager().registerEvents(new Events(), this);


        saveDefaultConfig();
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        Config.reloadConfig();
    }

    @Override
    public void onDisable() {
        LOGGER.info("Disabling BalancedVillagerTrades " + getDescription().getVersion());
    }

    public static BalancedVillagerTrades INSTANCE;
    public static Logger LOGGER;
}
