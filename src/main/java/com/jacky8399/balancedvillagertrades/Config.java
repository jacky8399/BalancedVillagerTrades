package com.jacky8399.balancedvillagertrades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Config {
    public static void reloadConfig() {
        FileConfiguration config = BalancedVillagerTrades.INSTANCE.getConfig();
        Logger logger = BalancedVillagerTrades.LOGGER;

        nerfNegativeReputationOnKilled = config.getBoolean("nerfs.negative-reputation-on-killed.enabled");
        if (nerfNegativeReputationOnKilled && BalancedVillagerTrades.REPUTATION == null) {
            logger.warning("Negative reputation on killed is not supported on Spigot.");
        }

        nerfNegativeReputationOnKilledRadius = config.getDouble("nerfs.negative-reputation-on-killed.radius");
        nerfNegativeReputationOnKilledReputationPenalty = clamp(config.getInt("nerfs.negative-reputation-on-killed.reputation-penalty"), 1, 100);

        luaAllowIO = config.getBoolean("lua.allow-io");
        if (luaAllowIO) {
            logger.warning("allow-io enabled. Lua scripts may read files.");
        }
        luaMaxInstructions = config.getInt("lua.max-instructions");

        parseRecipes();
    }

    private static String currentRecipe = null;
    private record Report(List<String> errors, List<String> warnings) {
        Report() {
            this(new ArrayList<>(), new ArrayList<>());
        }
    }
    private static final Map<String, Report> reports = new HashMap<>();
    public static void parseRecipes() {
        Recipe.RECIPES.clear();
        reports.clear();
        BalancedVillagerTrades plugin = BalancedVillagerTrades.INSTANCE;
        Logger logger = plugin.getLogger();
        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists())
            plugin.saveResource("recipes.yml", false); // save default recipes.yml
        try (FileInputStream stream = new FileInputStream(recipesFile)) {
            Map<String, Object> map = (new Yaml()).load(stream);
            if (map == null) {
                logger.info("Loaded 0 recipes");
                return;
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String name = entry.getKey();
                currentRecipe = name;
                Object obj = entry.getValue();
                if (!(obj instanceof Map<?, ?>)) {
                    addError("Expected map in %s, skipping", name);
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entryMap = (Map<String, Object>) obj;
                Recipe recipe = new Recipe(name);
                try {
                    recipe.readFromMap(entryMap);
                    Recipe.RECIPES.put(name, recipe);
                } catch (Exception e) {
                    addError(e.toString());
                }
            }
            logger.info("Loaded " + Recipe.RECIPES.size() + " recipes");
            sendReport(Bukkit.getConsoleSender(), false);
        } catch (IOException e) {
            logger.severe("Failed to read recipes.yml!");
            e.printStackTrace();
        }
        currentRecipe = null;
    }

    public static void addWarning(String warning, Object... args) {
        if (currentRecipe == null) return;
        if (args.length != 0)
            warning = warning.formatted(args);
        reports.computeIfAbsent(currentRecipe, ignored -> new Report()).warnings.add(warning);
    }
    public static void addError(String error, Object... args) {
        if (currentRecipe == null) return;
        if (args.length != 0)
            error = error.formatted(args);
        reports.computeIfAbsent(currentRecipe, ignored -> new Report()).errors.add(error);
    }

    @SuppressWarnings("deprecation") // thank you Paper, very cool
    public static void sendReport(CommandSender sender, boolean showWarnings) {
        if (reports.size() == 0)
            return;
        List<String> sortedKeys = new ArrayList<>(reports.keySet());
        sortedKeys.sort(null);
        List<String> lines = new ArrayList<>();
        List<String> fileLines = new ArrayList<>();
        int warnings = 0;
        for (String file : sortedKeys) {
            fileLines.clear();
            Report report = reports.get(file);
            if (report.errors.size() != 0) {
                for (String error : report.errors) {
                    fileLines.add(ChatColor.RED + "  " + error);
                }
            }
            warnings += report.warnings.size();
            if (showWarnings && report.warnings.size() != 0) {
                for (String warning : report.warnings) {
                    fileLines.add(ChatColor.YELLOW + "  " + warning);
                }
            }

            if (fileLines.size() != 0) {
                lines.add(ChatColor.AQUA + "[" + file + "]");
                lines.addAll(fileLines);
            }
        }
        if (!showWarnings && warnings != 0) {
            sender.sendMessage(ChatColor.YELLOW + "There were " + warnings + " warnings. To see them, run /bvt warnings.");
        }
        sender.sendMessage(String.join("\n", lines));
    }

    public static int clamp(int original, int min, int max) {
        return Math.max(Math.min(original, max), min);
    }

    public static boolean luaAllowIO;
    public static boolean luaDebug;
    public static int luaMaxInstructions;

    public static boolean nerfNegativeReputationOnKilled;
    public static double nerfNegativeReputationOnKilledRadius;
    public static int nerfNegativeReputationOnKilledReputationPenalty;
}
