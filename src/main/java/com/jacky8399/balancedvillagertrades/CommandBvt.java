package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
                return true;
            } else if (args[0].equalsIgnoreCase("recipe")) {
                if (args.length != 3 || !args[2].equalsIgnoreCase("info")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bvt recipe <recipe> info");
                    return true;
                }
                Recipe recipe = Recipe.RECIPES.get(args[1]);
                if (recipe == null) {
                    sender.sendMessage(ChatColor.RED + "Can't find recipe by name " + args[1]);
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + recipe.desc);
                sender.sendMessage(ChatColor.GREEN + "Conditions:");
                sender.sendMessage(ChatColor.YELLOW + recipe.predicate.toString());
                sender.sendMessage(ChatColor.GREEN + "Actions:");
                sender.sendMessage(ChatColor.YELLOW + recipe.actions.stream()
                        .map(Action::toString)
                        .collect(Collectors.joining("\n")));
                return true;
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
