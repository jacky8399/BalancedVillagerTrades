package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.luaj.vm2.LuaError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ActionLua extends Action {
    private final String recipeName;
    private final String chunkName;
    private final String script;
    private ActionLua(String recipeName, String chunkName, String script) {
        this.recipeName = recipeName;
        this.chunkName = chunkName;
        this.script = script;
    }


    @Override
    public void accept(TradeWrapper wrapper) {
        try {
            var globals = ScriptUtils.createSandbox();
            globals.set("trade", ScriptUtils.wrapField(wrapper, Fields.ROOT_FIELD));

            var chunk = globals.load(script, chunkName);
            chunk.call();
        } catch (LuaError ex) {
            BalancedVillagerTrades.LOGGER.severe("An error occurred while running script in recipe " + recipeName + ":");
            ex.printStackTrace();
        }
    }

    public static ActionLua fromString(String recipeName, String string) {
        return new ActionLua(recipeName, "[inline script in " + recipeName + "]", string);
    }

    public static ActionLua fromFile(String recipeName, String path) {
        File dataFolder = BalancedVillagerTrades.INSTANCE.getDataFolder();
        File scriptFile = new File(dataFolder, path);
        if (!scriptFile.exists())
            throw new IllegalStateException(path + " doesn't exist!");

        try {
            String script = Files.readString(scriptFile.toPath());
            return new ActionLua(recipeName, scriptFile.getName(), script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
