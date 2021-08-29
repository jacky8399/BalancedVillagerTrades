package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import com.jacky8399.balancedvillagertrades.utils.fields.Field;
import com.jacky8399.balancedvillagertrades.utils.fields.FieldAccessor;
import com.jacky8399.balancedvillagertrades.utils.fields.Fields;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CommandBvt implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                BalancedVillagerTrades.INSTANCE.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration and recipes reloaded!");
                sender.sendMessage(ChatColor.GREEN + "Loaded " + Recipe.RECIPES.size() + " recipes.");
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
            } else if (args[0].equalsIgnoreCase("getfield")) {
                if (args.length == 1) {
                    Map<String, ? extends Field<TradeWrapper, ?>> fieldMap =
                            new TreeMap<>(Fields.listFields(null, null, null));
                    sender.sendMessage(ChatColor.GREEN + "Fields:");
                    fieldMap.forEach((key, value) -> sender.sendMessage(ChatColor.YELLOW + "  " + key + " (type: " + value.clazz.getSimpleName() + ")"));
                    return true;
                }

                List<Entity> entities;
                try {
                    entities = Bukkit.selectEntities(sender, args[1]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Expected entity selector (" + e.getMessage() + ")");
                    return true;
                }
                if (entities.size() != 1) {
                    sender.sendMessage(ChatColor.RED + "Expected 1 entity, got " + entities.size());
                    return true;
                }
                Entity entity = entities.get(0);
                if (!(entity instanceof Villager)) {
                    sender.sendMessage(ChatColor.RED + "Expected villager, got " + entity.getType().name());
                    return true;
                }
                Villager villager = (Villager) entity;
                TradeWrapper wrapper;
                if (args.length >= 4) {
                    try {
                        int recipeId = Integer.parseInt(args[3]);
                        wrapper = new TradeWrapper(villager, villager.getRecipe(recipeId));
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid recipe index " + args[3]);
                        return true;
                    }
                } else {
                    wrapper = new TradeWrapper(villager, null);
                }
                FieldAccessor<TradeWrapper, ?, ?> field = Fields.findField(null, args[2], true);
                // debug accessor
                if (args.length == 5 && "debugaccessor".equals(args[4])) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Field lookup: " + field);
                }
                Object value = field.get(wrapper);
                sender.sendMessage(ChatColor.GREEN + args[2] + " is " + value + " (type=" + value.getClass().getSimpleName() + ")");
                return true;
            }
        }
        sender.sendMessage(ChatColor.GREEN + "You are using BalancedVillagerTrades v" + BalancedVillagerTrades.INSTANCE.getDescription().getVersion());
        sender.sendMessage(ChatColor.GREEN + "Using " + BalancedVillagerTrades.REPUTATION);
        sender.sendMessage(ChatColor.AQUA + "Loaded recipes: " + Recipe.RECIPES.size());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "recipes", "recipe"/*, "getfield"*/);
        } else if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                return new ArrayList<>(Recipe.RECIPES.keySet());
            } else if (args.length == 3) {
                return Arrays.asList("info", "enable", "disable");
            }
        } else if (args[0].equalsIgnoreCase("getfield")) {
            switch (args.length) {
                case 2: {
                    // see if player is looking at a villager
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                                player.getEyeLocation(), player.getLocation().getDirection(),
                                5, e -> e instanceof Villager);
                        if (rayTrace != null && rayTrace.getHitEntity() != null) {
                            return Arrays.asList(rayTrace.getHitEntity().getUniqueId().toString(), "@e");
                        }
                    }
                    return Arrays.asList("@e", "@e[sort=nearest,limit=1]");
                }
                case 3: {
                    try {
                        List<Entity> entities = Bukkit.selectEntities(sender, args[1]);
                        if (entities.size() == 1 && entities.get(0) instanceof Villager) {
                            return new ArrayList<>(Fields.listFields(null, null, new TradeWrapper((Villager) entities.get(0), null)).keySet());
                        }
                    } catch (Exception ignored) {}
                    return new ArrayList<>(Fields.listFields(null, null, null).keySet());
                }
                case 4: {
                    try {
                        List<Entity> entities = Bukkit.selectEntities(sender, args[1]);
                        if (entities.size() == 1 && entities.get(0) instanceof Villager) {
                            Villager villager = (Villager) entities.get(0);
                            return IntStream.range(0, villager.getRecipeCount())
                                    .mapToObj(Integer::toString)
                                    .collect(Collectors.toList());
                        }
                    } catch (Exception ignored) {}
                    return Collections.singletonList("0");
                }
                case 5:
                    return Collections.singletonList("debugaccessor");
                default:
                    return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
