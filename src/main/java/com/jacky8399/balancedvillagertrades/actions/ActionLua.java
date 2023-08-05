package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.lua.ScriptRunner;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.luaj.vm2.LuaError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

public class ActionLua extends Action {
    private final String recipeName;
    public final String chunkName;
    public final String script;
    private ActionLua(String recipeName, String chunkName, String script) {
        this.recipeName = recipeName;
        this.chunkName = chunkName;
        this.script = script;
    }


    @Override
    public void accept(TradeWrapper wrapper) {
        try {
            ScriptRunner.runScriptInSandbox(script, chunkName,
                    ScriptRunner.createSandbox(globals -> {
                        globals.set("trade", ScriptRunner.wrapField(wrapper, Fields.ROOT_FIELD));
                        globals.set("__chunkName", chunkName);
                    }));
        } catch (LuaError ex) {
            Logger logger = BalancedVillagerTrades.LOGGER;
            logger.severe("""
                    An error occurred while running Lua script %s in recipe %s
                    Context: %s
                    Exception: %s""".formatted(chunkName, recipeName, wrapper, ex));
        }
    }

    public static ActionLua fromString(String recipeName, String string) {
        return new ActionLua(recipeName, "[inline script in " + recipeName + "]", string);
    }

    public static ActionLua fromFile(String recipeName, String path) {
        File dataFolder = BalancedVillagerTrades.INSTANCE.getDataFolder();
        File scriptFile = new File(dataFolder, path);
        if (!scriptFile.exists())
            throw new IllegalArgumentException(path + " doesn't exist!");
        // check extension
        String fileName = scriptFile.getName();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        if (!"lua".equalsIgnoreCase(extension))
            throw new IllegalArgumentException(fileName + " has invalid extension (" + extension + ")");

        try {
            String script = Files.readString(scriptFile.toPath());
            return new ActionLua(recipeName, scriptFile.getName(), script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Run Lua script " + chunkName;
    }
}
