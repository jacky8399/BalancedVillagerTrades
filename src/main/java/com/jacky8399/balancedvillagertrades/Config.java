package com.jacky8399.balancedvillagertrades;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class Config {
    public static void reloadConfig() {
        FileConfiguration config = BalancedVillagerTrades.INSTANCE.getConfig();
        Logger logger = BalancedVillagerTrades.LOGGER;

        nerfNegativeReputationOnKilled = config.getBoolean("nerfs.negative-reputation-on-killed.enabled");
        nerfNegativeReputationOnKilledRadius = config.getDouble("nerfs.negative-reputation-on-killed.radius");
        nerfNegativeReputationOnKilledReputationPenalty = clamp(config.getInt("nerfs.negative-reputation-on-killed.reputation-penalty"), 1, 100);

//        luaAllowIO = config.getBoolean("lua.allow-io");
        if (luaAllowIO) {
            logger.warning("allow-io enabled. Lua scripts may read files.");
        }
        luaMaxInstructions = config.getInt("lua.max-instructions");

        parseRecipes();
    }

    public static void parseRecipes() {
        Recipe.RECIPES.clear();
        BalancedVillagerTrades plugin = BalancedVillagerTrades.INSTANCE;
        Logger logger = plugin.getLogger();
        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists())
            plugin.saveResource("recipes.yml", false); // save default recipes.yml
        try (FileInputStream stream = new FileInputStream(recipesFile);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, Object> map = (new Yaml()).load(reader);
            if (map == null) {
                logger.info("Loaded 0 recipes");
                return;
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String name = entry.getKey();
                Object obj = entry.getValue();
                if (!(obj instanceof Map<?, ?>)) {
                    logger.severe("Expected map in " + name + ", skipping");
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entryMap = (Map<String, Object>) obj;
                Recipe recipe = new Recipe(name);
                try {
                    recipe.readFromMap(entryMap);
                    Recipe.RECIPES.put(name, recipe);
                } catch (Exception e) {
                    logger.severe("Error while loading recipe " + name + ", skipping");
                    e.printStackTrace();
                }
            }
            logger.info("Loaded " + Recipe.RECIPES.size() + " recipes");
        } catch (IOException e) {
            logger.severe("Failed to read recipes.yml!");
            e.printStackTrace();
        }
    }

    public static int clamp(int original, int min, int max) {
        return Math.max(Math.min(original, max), min);
    }

    public static boolean luaAllowIO;
    public static int luaMaxInstructions;

    public static boolean nerfNegativeReputationOnKilled;
    public static double nerfNegativeReputationOnKilledRadius;
    public static int nerfNegativeReputationOnKilledReputationPenalty;
}
