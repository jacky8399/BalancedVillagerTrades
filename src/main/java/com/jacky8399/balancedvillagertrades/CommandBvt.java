package com.jacky8399.balancedvillagertrades;

import com.jacky8399.balancedvillagertrades.actions.Action;
import com.jacky8399.balancedvillagertrades.actions.ActionLua;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.lua.ScriptRunner;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaError;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bukkit.ChatColor.*;

public class CommandBvt implements TabExecutor {

    private void doField(@NotNull CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(RED + "Usage: /bvt " + args[0] + " <villager> <recipeId>|-1 <field>");
            Map<String, ? extends Field<TradeWrapper, ?>> fieldMap =
                    new TreeMap<>(Fields.listFields(null, null, null));
            sender.sendMessage(GREEN + "All fields:");
            fieldMap.forEach((key, value) -> sender.sendMessage(YELLOW + "  " + key + " (type: " + value.getFieldClass().getSimpleName() + ")"));
            return;
        }

        TradeWrapper wrapper;
        try {
            AbstractVillager villager = selectVillager(sender, args[1]);
            int recipeId = Integer.parseInt(args[2]);
            wrapper = new TradeWrapper(villager, recipeId != -1 ?
                    villager.getRecipe(recipeId) : null, recipeId, false);
        } catch (NumberFormatException ex) {
            sender.sendMessage(RED + "Invalid recipe index " + args[2]);
            return;
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ex.getMessage());
            return;
        }
        FieldProxy<TradeWrapper, ?, ?> field;
        try {
            field = Fields.findField(null, args[3], true);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(RED + ex.getMessage());
            return;
        }
        if (args.length == 5 && "debugaccessor".equals(args[4])) {
            sender.sendMessage(LIGHT_PURPLE + "Field lookup: " + field);
        }
        Object value = field.get(wrapper);
        sender.sendMessage(GREEN + args[3] + " is " + value + " (type=" + field.getFieldClass().getSimpleName() + ")");
        if (field.isComplex()) {
            Collection<String> children = field.getFields(wrapper);
            if (children != null)
                sender.sendMessage(YELLOW + "  (contains fields: " + String.join(", ", children) + ")");
            else
                sender.sendMessage(YELLOW + "  (may contain more fields)");
        }

        // setfield/testfield
        if (!args[0].equalsIgnoreCase("getfield") && args.length > 4) {
            String opString = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            doFieldHelper(sender, wrapper, field, args[0].equalsIgnoreCase("setfield"), opString);
        }
    }
    // helper method to deal with wildcards
    private <T, TField> void doFieldHelper(CommandSender sender, TradeWrapper wrapper,
                                           FieldProxy<TradeWrapper, T, TField> field,
                                           boolean isSetField, String opString) {
        TField value = field.get(wrapper);
        try {
            if (isSetField) {
                var transformer = field.parseTransformer(opString);
                TField newValue = transformer.apply(wrapper, value);
                field.set(wrapper, newValue);
                TField realValue = field.get(wrapper); // check if field.set worked
                sender.sendMessage(AQUA + "Changed field to " + realValue + " using transformer \"" + opString + "\"");
            } else {
                var predicate = field.parsePredicate(opString);
                boolean result = predicate.test(wrapper, value);
                sender.sendMessage((result ? GREEN : RED) + "Field evaluated to " + result + " using predicate \"" + opString + "\"");
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(DARK_RED + "String \"" + opString + "\" is not a valid " + (isSetField ? "transformer" : "predicate") + ": " + ex.getMessage());
        }
    }

    private static final String EXPECTED_SCRIPT_TOKENS = "Expected villager/index/new/script/file/recipe, got ";
    private void doScript(@NotNull CommandSender sender, String label, String[] args) {
        int index = 1;
        String lastToken = null;
        AbstractVillager villager = null;
        int recipeIndex = -1;
        boolean isNew = false;
        String chunkName = null;
        String chunk = null;

        try {
            loop:
            while (true) {
                String token = args[index++];
                if (lastToken == null) {
                    switch (token) {
                        case "villager", "index" -> lastToken = token;
                        case "new" -> isNew = true;
                        case "script" -> {
                            // consume all input immediately
                            String rest = String.join(" ", Arrays.copyOfRange(args, index, args.length));
                            if (rest.isBlank())
                                throw new IllegalArgumentException("Expected inline script after 'script', got nothing");

                            chunkName = "inline script";
                            chunk = rest;
                            break loop;
                        }
                        case "file" -> {
                            String rest = String.join(" ", Arrays.copyOfRange(args, index, args.length));
                            if (rest.isBlank())
                                throw new IllegalArgumentException("Expect file after 'file', got nothing");

                            Path base = BalancedVillagerTrades.INSTANCE.getDataFolder().toPath().toAbsolutePath();
                            try {
                                Path dest = base.resolve(rest).toAbsolutePath();
                                if (!dest.startsWith(base)) throw new IllegalArgumentException("Invalid path");

                                chunkName = rest;
                                chunk = Files.readString(dest);
                                break loop;
                            } catch (Exception ex) {
                                if (ex instanceof IllegalArgumentException iae)
                                    throw iae;
                                throw new IllegalArgumentException(ex.getMessage());
                            }
                        }
                        case "recipe" -> {
                            String recipeName = args[index];
                            Recipe recipe = Recipe.RECIPES.get(recipeName);
                            if (recipe == null)
                                throw new IllegalArgumentException("Recipe " + recipeName + " not found");

                            for (Action action : recipe.actions) {
                                if (action instanceof ActionLua actionLua) {
                                    chunk = actionLua.script;
                                    chunkName = actionLua.chunkName;
                                    break;
                                }
                            }

                            if (chunk == null)
                                throw new IllegalArgumentException("Recipe " + recipeName + " does not have a do.lua block");
                            break loop;
                        }
                        default -> throw new IllegalArgumentException(EXPECTED_SCRIPT_TOKENS + token);
                    }
                } else {
                    if (lastToken.equals("villager")) {
                        villager = selectVillager(sender, token);
                    } else { // if (lastToken.equals("index") {
                        recipeIndex = Integer.parseInt(token);
                    }
                    lastToken = null;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            String message;
            if (lastToken == null) {
                message = EXPECTED_SCRIPT_TOKENS + "nothing";
            } else {
                message = switch (lastToken) {
                    case "villager" -> "Expected entity selector after 'villager', got nothing";
                    case "index" -> "Expected index to recipe after 'index', got nothing";
                    default -> EXPECTED_SCRIPT_TOKENS + "nothing\nDid you mean /" + label + " script script " + lastToken + "?";
                };
            }
            message += "\nUsage: /" + label + " script [villager <villager>] [index <index>] [new] script|file|recipe <script...|file...|recipe>";
            sender.sendMessage(RED + message);
            return;
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(RED + ex.getMessage());
            return;
        }

        if (chunk == null) return;

        var sandbox = ScriptRunner.createSandbox();
        sandbox.set("__chunkName" , chunkName);
        if (villager != null && recipeIndex != -1) {
           var trade = new TradeWrapper(villager, villager.getRecipe(recipeIndex), recipeIndex, isNew);
           sandbox.set("trade", ScriptRunner.wrapField(trade, Fields.ROOT_FIELD));
        }


        var baos = new ByteArrayOutputStream();
        sandbox.STDOUT = new PrintStream(baos);
        try {
            var retVal = ScriptRunner.runScriptInSandbox(chunk, chunkName, sandbox);
            sandbox.STDOUT.flush();
            var output = baos.toString();
            if (!output.isEmpty()) {
                sender.sendMessage(YELLOW + "[STDOUT] " + output);
            }
            if (!retVal.isnil()) {
                sender.sendMessage(YELLOW + "[Return Value] " + retVal.tojstring());
            }
        } catch (LuaError error) {
            sender.sendMessage(RED + "[Script Error] " + error.getMessage());
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(GREEN + "You are using BalancedVillagerTrades v" + BalancedVillagerTrades.INSTANCE.getDescription().getVersion());
            sender.sendMessage(GREEN + "Using " + BalancedVillagerTrades.REPUTATION);
            sender.sendMessage(AQUA + "Loaded recipes: " + Recipe.RECIPES.size());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "reload" -> {
                BalancedVillagerTrades.INSTANCE.reloadConfig();
                sender.sendMessage(GREEN + "Configuration and recipes reloaded!");
                sender.sendMessage(GREEN + "Loaded " + Recipe.RECIPES.size() + " recipes.");
                Config.sendReport(sender, false);
            }
            case "recipe" -> {
                String recipeName = args[1];
                if (args.length != 3) {
                    sender.sendMessage(RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                    return true;
                }
                Recipe recipe = Recipe.RECIPES.get(recipeName);
                if (recipe == null) {
                    sender.sendMessage(RED + "Can't find recipe by the name " + recipeName);
                    return true;
                }
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "info" -> {
                        sender.sendMessage("" + GOLD + BOLD + recipeName);
                        sender.sendMessage(GREEN + "Status: " + (recipe.enabled ? GREEN + "enabled" : RED + "disabled"));
                        sender.sendMessage(GREEN + "Description: " + YELLOW + recipe.desc);
                        sender.sendMessage(GREEN + "Conditions:");
                        sender.sendMessage(YELLOW + recipe.predicate.toString());
                        sender.sendMessage(GREEN + "Actions:");
                        sender.sendMessage(YELLOW + recipe.actions.stream()
                                .map(Action::toString)
                                .collect(Collectors.joining("\n")));
                    }
                    case "disable", "enable" -> {
                        boolean enable = args[2].equalsIgnoreCase("enable");
                        recipe.enabled = enable;
                        sender.sendMessage((enable ? GREEN + "Enabled" : RED + "Disabled") +
                                YELLOW + " " + recipeName + " temporarily");
                        sender.sendMessage(YELLOW + "Note: to disable this recipe permanently, comment it out or set enabled: false in recipes.yml");
                    }
                    default -> sender.sendMessage(RED + "Usage: /bvt recipe <recipe> <info/enable/disable>");
                }
            }
            case "recipes" -> sender.sendMessage(AQUA + "Recipes: (do /bvt recipe <recipe> info for more info)" +
                    GREEN + String.join("\n", Recipe.RECIPES.keySet()));

            case "getfield", "setfield", "testfield" -> doField(sender, args);

            case "script" -> doScript(sender, label, args);

            case "warnings" -> Config.sendReport(sender, true);
            default -> sender.sendMessage(RED + "Usage: /bvt ...");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        String input = args[args.length - 1];
        Collection<String> collection = tabComplete(sender, args);
        var list = new ArrayList<String>(collection.size());
        for (String string : collection) {
            if (string.regionMatches(true, 0, input, 0, input.length())) {
                list.add(string);
            }
        }
        return list;
    }

    private static final Set<String> FIELD_COMMANDS = Set.of("getfield", "setfield", "testfield");
    private static final List<String> SCRIPT_OPTIONS = List.of("villager", "index", "recipe", "script", "file");
    private Collection<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "recipes", "recipe", "script", "getfield", "setfield", "testfield", "warnings");
        } else if (args[0].equalsIgnoreCase("recipe")) {
            if (args.length == 2) {
                return Recipe.RECIPES.keySet();
            } else if (args.length == 3) {
                return List.of("info", "enable", "disable");
            }
        } else if (FIELD_COMMANDS.contains(args[0].toLowerCase(Locale.ENGLISH))) {
            boolean getField = true;
            if (args.length == 2) {
                return completeVillager(sender);
            } else {
                try {
                    AbstractVillager villager = selectVillager(sender, args[1]);
                    // show recipe IDs for:
                    // /bvt getfield <villager> recipeId
                    // /bvt runscriptfile <villager> recipeId
                    if (args.length == 3) {
                        List<String> list = new ArrayList<>();
                        list.add("-1");
                        for (int i = 0; i < villager.getRecipeCount(); i++) {
                            list.add(Integer.toString(i));
                        }
                        return list;
                    } else if (args.length == 4) {
                        int recipeId = Integer.parseInt(args[2]);
                        var tempWrapper = new TradeWrapper(villager, recipeId != -1 ? villager.getRecipe(recipeId) : null, recipeId, false);
                        return completeFieldNames(args[args.length - 1], tempWrapper);
                    }
                } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                    if (args.length == 3) {
                        return completeFieldNames(args[args.length - 1], null);
                    }
                }
            }
            return List.of();
        } else if (args[0].equals("script")) {
            int lastIndex = args.length - 2;
            if (lastIndex == 0)
                return SCRIPT_OPTIONS;
            String lastToken = args[lastIndex];
            return switch (lastToken) {
                case "villager" -> completeVillager(sender);
                case "index" -> {
                    // find villager
                    for (int i = 1; i < args.length - 2; i++) {
                        if (args[i].equals("villager")) {
                            AbstractVillager villager = selectVillager(sender, args[i + 1]);
                            yield IntStream.range(0, villager.getRecipeCount()).mapToObj(Integer::toString).toList();
                        }
                    }
                    yield List.of(); // not found
                }
                case "recipe" -> Recipe.RECIPES.keySet();
                case "file", "script" -> List.of();
                default -> SCRIPT_OPTIONS;
            };
        }
        return List.of();
    }

    private static AbstractVillager selectVillager(CommandSender sender, String selector) throws IllegalArgumentException {
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
        if (!(entity instanceof AbstractVillager villager)) {
            throw new IllegalArgumentException("Expected villager or wandering_trader, got " + entity.getType().name());
        }
        return villager;
    }

    private static List<String> completeVillager(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        completions.add("@e");
        completions.add("@e[type=villager,sort=nearest,limit=1]");
        // see if player is looking at a villager
        if (sender instanceof Player player) {
            RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(), player.getLocation().getDirection(),
                    5, e -> e instanceof Villager
            );
            if (rayTrace != null && rayTrace.getHitEntity() instanceof AbstractVillager villager) {
                completions.add(villager.getUniqueId().toString());
            }
        }
        return completions;
    }

    // Only list requested fields
    // For example, given input "villager", returns "villager." (to indicate that it has children fields),
    // and other sibling fields.
    // Given input "villager.", returns all children fields under "children"
    private static List<String> completeFieldNames(String input, TradeWrapper context) {
        List<String> fields = new ArrayList<>();
        int dot = input.lastIndexOf('.');
        Collection<String> children;
        FieldProxy<TradeWrapper, ?, ?> parent;
        String parentPath;
        if (dot == -1) {
            parent = FieldProxy.emptyAccessor(Fields.ROOT_FIELD);
            children = Fields.FIELDS.keySet();
            parentPath = "";
        } else {
            parent = Fields.findField(null, input.substring(0, dot), true);
            children = parent.getFields(context);
            parentPath = input.substring(0, dot + 1);
        }

        if (children != null) {
            for (String child : children) {
                String childPath = parentPath + child;
                if (FieldProxy.isComplex(parent.getField(child))) {
                    fields.add(childPath + ".");
                } else {
                    fields.add(childPath);
                }
            }
        }
        return fields;
    }
}
