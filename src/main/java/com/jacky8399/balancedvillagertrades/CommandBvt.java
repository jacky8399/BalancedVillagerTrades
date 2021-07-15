package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommandBvt implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                    return true;
                }
                Recipe recipe = Recipe.RECIPES.get(args[1]);
                if (recipe == null) {
                    sender.sendMessage(ChatColor.RED + "Can't find recipe by name " + args[1]);
                    return true;
                }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "info": {
                        sender.sendMessage(""+ChatColor.GOLD + ChatColor.BOLD + args[1]);
                        sender.sendMessage(ChatColor.GREEN + "Status: " + (recipe.enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
                        sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + recipe.desc);
                        sender.sendMessage(ChatColor.GREEN + "Conditions:");
                        sender.sendMessage(ChatColor.YELLOW + recipe.predicate.toString());
                        sender.sendMessage(ChatColor.GREEN + "Actions:");
                        sender.sendMessage(ChatColor.YELLOW + recipe.actions.stream()
                                .map(Action::toString)
                                .collect(Collectors.joining("\n")));
                        break;
                    }
                    case "disable":
                    case "enable": {
                        boolean enable = args[2].equalsIgnoreCase("enable");
                        recipe.enabled = enable;
                        sender.sendMessage((enable ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled") + ChatColor.YELLOW + " " + args[1] + " temporarily");
                        sender.sendMessage(ChatColor.YELLOW + "Note: to disable this recipe permanently, comment it out or set enabled: false in recipes.yml");
                    }
                    default: {
                        sender.sendMessage(ChatColor.RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                        break;
                    }
                }
                return true;
            }
        }
        sender.sendMessage(ChatColor.GREEN + "You are using BalancedVillagerTrades v" + BalancedVillagerTrades.INSTANCE.getDescription().getVersion());
        sender.sendMessage(ChatColor.AQUA + "Loaded recipes: " + Recipe.RECIPES.size());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "recipes", "recipe");
        } else if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                return new ArrayList<>(Recipe.RECIPES.keySet());
            } else if (args.length == 3) {
                return Arrays.asList("info", "enable", "disable");
            }
        }
        return Collections.emptyList();
    }
}
