package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
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
import org.luaj.vm2.LuaError;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bukkit.ChatColor.RED;

public class CommandBvt implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "You are using BalancedVillagerTrades v" + BalancedVillagerTrades.INSTANCE.getDescription().getVersion());
            sender.sendMessage(ChatColor.GREEN + "Using " + BalancedVillagerTrades.REPUTATION);
            sender.sendMessage(ChatColor.AQUA + "Loaded recipes: " + Recipe.RECIPES.size());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "reload" -> {
                BalancedVillagerTrades.INSTANCE.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration and recipes reloaded!");
                sender.sendMessage(ChatColor.GREEN + "Loaded " + Recipe.RECIPES.size() + " recipes.");
            }
            case "recipe" -> {
                if (args.length != 3) {
                    sender.sendMessage(RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                    return true;
                }
                Recipe recipe = Recipe.RECIPES.get(args[1]);
                if (recipe == null) {
                    sender.sendMessage(RED + "Can't find recipe by the name " + args[1]);
                    return true;
                }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "info" -> {
                        sender.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + args[1]);
                        sender.sendMessage(ChatColor.GREEN + "Status: " + (recipe.enabled ? ChatColor.GREEN + "enabled" : RED + "disabled"));
                        sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + recipe.desc);
                        sender.sendMessage(ChatColor.GREEN + "Conditions:");
                        sender.sendMessage(ChatColor.YELLOW + recipe.predicate.toString());
                        sender.sendMessage(ChatColor.GREEN + "Actions:");
                        sender.sendMessage(ChatColor.YELLOW + recipe.actions.stream()
                                .map(Action::toString)
                                .collect(Collectors.joining("\n")));
                    }
                    case "disable", "enable" -> {
                        boolean enable = args[2].equalsIgnoreCase("enable");
                        recipe.enabled = enable;
                        sender.sendMessage((enable ? ChatColor.GREEN + "Enabled" : RED + "Disabled") +
                                ChatColor.YELLOW + " " + args[1] + " temporarily");
                        sender.sendMessage(ChatColor.YELLOW + "Note: to disable this recipe permanently, comment it out or set enabled: false in recipes.yml");
                    }
                    default -> sender.sendMessage(RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                }
            }
            case "recipes" -> sender.sendMessage(ChatColor.AQUA + "Recipes: (do /bvt recipe <recipe> info for more info)" +
                    ChatColor.GREEN + String.join("\n", Recipe.RECIPES.keySet()));

            case "getfield" -> {
                if (args.length == 1) {
                    sender.sendMessage(RED + "Usage: /bvt getfield <villager> <");
                    Map<String, ? extends Field<TradeWrapper, ?>> fieldMap =
                            new TreeMap<>(Fields.listFields(null, null, null));
                    sender.sendMessage(ChatColor.GREEN + "All fields:");
                    fieldMap.forEach((key, value) -> sender.sendMessage(ChatColor.YELLOW + "  " + key + " (type: " + value.getFieldClass().getSimpleName() + ")"));
                    return true;
                }

                TradeWrapper wrapper;
                try {
                    Villager villager = selectVillager(sender, args[1]);
                    if (args.length >= 4) {
                        int recipeId = Integer.parseInt(args[3]);
                        wrapper = new TradeWrapper(villager, villager.getRecipe(recipeId), recipeId, false);
                    } else {
                        wrapper = new TradeWrapper(villager, null, -1, false);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                    sender.sendMessage(RED + "Invalid recipe index " + args[3]);
                    return true;
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ex.getMessage());
                    return true;
                }
                FieldProxy<TradeWrapper, ?, ?> field = Fields.findField(null, args[2], true);
                if (args.length == 5 && "debugaccessor".equals(args[4])) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Field lookup: " + field);
                }
                Object value = field.get(wrapper);
                sender.sendMessage(ChatColor.GREEN + args[2] + " is " + value + " (type=" + field.getFieldClass().getSimpleName() + ")");
                if (field.isComplex()) {
                    Collection<String> children = field.getFields(wrapper);
                    if (children != null)
                        sender.sendMessage(ChatColor.YELLOW + "  (contains fields: " + String.join(", ", children) + ")");
                    else
                        sender.sendMessage(ChatColor.YELLOW + "  (may contain more fields)");
                }
            }
            case "script" -> {
                if (args.length == 1) {
                    sender.sendMessage(RED + "Usage: /bvt script <script>");
                    return true;
                }
                String script = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                try {
                    var globals = ScriptUtils.createSandbox();
                    // try to redirect stdout to player chat
                    var baos = new ByteArrayOutputStream();
                    globals.STDOUT = new PrintStream(baos);
                    ScriptUtils.runScriptInSandbox(script, "[script]", globals);
                    globals.STDOUT.flush();
                    var output = baos.toString();
                    if (!output.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "[STDOUT] " + output);
                    }
                } catch (LuaError ex) {
                    sender.sendMessage(RED + "[Script Error] " + ex);
                    ex.printStackTrace();
                }
            }
            case "runscriptfile" -> {
                if (args.length < 4) {
                    sender.sendMessage(RED + "Usage: /bvt runscriptfile <villager> <recipeId> <scriptFile>");
                    return true;
                }
                try {
                    Villager villager = selectVillager(sender, args[1]);
                    int recipeId = Integer.parseInt(args[2]);
                    TradeWrapper trade = new TradeWrapper(villager, villager.getRecipe(recipeId), recipeId, false);

                    String fileName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    File file = new File(BalancedVillagerTrades.INSTANCE.getDataFolder(), fileName);
                    try (var reader = new FileReader(file)) {
                        var sandbox = ScriptUtils.createSandbox(globals ->
                                globals.set("trade", ScriptUtils.wrapField(trade, Fields.ROOT_FIELD))
                        );
                        var baos = new ByteArrayOutputStream();
                        sandbox.STDOUT = new PrintStream(baos);
                        ScriptUtils.runScriptInSandbox(reader, fileName, sandbox);
                        sandbox.STDOUT.flush();
                        var output = baos.toString();
                        if (!output.isEmpty()) {
                            sender.sendMessage(ChatColor.YELLOW + "[STDOUT] " + output);
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                    sender.sendMessage(RED + "Invalid recipe ID " + args[2]);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(RED + ex.getMessage());
                } catch (IOException ex) {
                    sender.sendMessage(RED + ex.toString());
                } catch (LuaError error) {
                    sender.sendMessage(RED + "[Script Error] " + error);
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "recipes", "recipe", "script", "runscriptfile", "getfield");
        } else if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                return new ArrayList<>(Recipe.RECIPES.keySet());
            } else if (args.length == 3) {
                return Arrays.asList("info", "enable", "disable");
            }
        } else if (args[0].equalsIgnoreCase("getfield") || args[0].equalsIgnoreCase("runscriptfile")) {
            boolean getField = args[0].equalsIgnoreCase("getfield");
            if (args.length == 2) {
                return completeVillager(sender);
            } else {
                try {
                    Villager villager = selectVillager(sender, args[1]);
                    // show recipe IDs for:
                    // /bvt getfield <villager> <field> recipeId
                    // /bvt runscriptfile <villager> recipeId
                    if ((getField && args.length == 4) || (!getField && args.length == 3)) {
                        return IntStream.range(0, villager.getRecipeCount())
                                .mapToObj(Integer::toString)
                                .collect(Collectors.toList());
                    } else if (getField && args.length == 3) {
                        var tempWrapper = new TradeWrapper(villager, null, -1, false);
                        return List.copyOf(Fields.listFields(null, null, tempWrapper).keySet());
                    }
                } catch (IllegalArgumentException ignored) {
                    if (getField && args.length == 3) {
                        return List.copyOf(Fields.listFields(null, null, null).keySet());
                    }
                }
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private static Villager selectVillager(CommandSender sender, String selector) throws IllegalArgumentException {
        List<Entity> entities;
        try {
            entities = Bukkit.selectEntities(sender, selector);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity selector " + selector);
        }
        if (entities.size() != 1) {
            throw new IllegalArgumentException("Expected 1 entity, got " + entities.size());
        }
        Entity entity = entities.get(0);
        if (!(entity instanceof Villager villager)) {
            throw new IllegalArgumentException("Expected villager, got " + entity.getType().name());
        }
        return villager;
    }

    private static List<String> completeVillager(CommandSender sender) {
        List<String> completions = new ArrayList<>(Arrays.asList("@e", "@e[sort=nearest,limit=1]"));
        // see if player is looking at a villager
        if (sender instanceof Player player) {
            RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(), player.getLocation().getDirection(),
                    5, e -> e instanceof Villager
            );
            if (rayTrace != null && rayTrace.getHitEntity() instanceof Villager villager) {
                completions.add(villager.getUniqueId().toString());
            }
        }
        return completions;
    }
}
