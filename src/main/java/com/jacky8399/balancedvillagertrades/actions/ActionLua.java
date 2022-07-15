package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.luaj.vm2.LuaError;

public class ActionLua extends Action {
    private final String script;
    private ActionLua(String script) {
        this.script = script;
    }


    @Override
    public void accept(TradeWrapper wrapper) {
        try {
            ScriptUtils.run(script, wrapper);
        } catch (LuaError ex) {
            BalancedVillagerTrades.LOGGER.severe("An error occurred while running script:");
            ex.printStackTrace();
            BalancedVillagerTrades.LOGGER.severe("The script:\n" + script);
        }
    }

    public static ActionLua fromString(String string) {
        return new ActionLua(string);
    }

    public static ActionLua fromFile() {
        // TODO
        return null;
    }
}
