package com.jacky8399.balancedvillagertrades;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandBvt implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                BalancedVillagerTrades.INSTANCE.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                return true;
            } else if (args[0].equalsIgnoreCase("recipes")) {
                sender.sendMessage(ChatColor.AQUA + "Recipes: (do /bvt recipe <recipe> info for more info)");
                for (String recipe : Recipe.RECIPES.keySet()) {
                    sender.sendMessage(ChatColor.GREEN + recipe);
                }
            }
        }
        sender.sendMessage(ChatColor.GREEN + "You are using BalancedVillagerTrades v" + BalancedVillagerTrades.INSTANCE.getDescription().getVersion());
        sender.sendMessage(ChatColor.AQUA + "Loaded recipes: " + Recipe.RECIPES.size());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "recipes", "recipe");
        } else if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                return new ArrayList<>(Recipe.RECIPES.keySet());
            } else if (args.length == 3) {
                return Collections.singletonList("info");
            }
        }
        return Collections.emptyList();
    }
}
