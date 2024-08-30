package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.utils.reputation.ReputationProvider;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

public class BalancedVillagerTrades extends JavaPlugin {

    public BalancedVillagerTrades() {
        super();
    }

    @SuppressWarnings("removal")
    protected BalancedVillagerTrades(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        LOGGER = getLogger();
        RANDOM = new Random();

        getCommand("balancedvillagertrades").setExecutor(new CommandBvt());

        ReputationProvider.loadMappings();

        Bukkit.getPluginManager().registerEvents(new Events(), this);

        saveDefaultConfig();
        reloadConfig();

        // stolen from slimefun, don't run metrics if running unit tests
        if (!getClassLoader().getClass().getPackageName().startsWith("be.seeseemelk.mockbukkit")) {
            new Metrics(this, 11784);
        }
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
    public static Random RANDOM;
    @Nullable
    public static ReputationProvider REPUTATION;
}
