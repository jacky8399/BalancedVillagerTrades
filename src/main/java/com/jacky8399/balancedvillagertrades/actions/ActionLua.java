package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

public class ActionLua extends Action {
    private final String script;
    private ActionLua(String script) {
        this.script = script;
    }


    @Override
    public void accept(TradeWrapper wrapper) {
        ScriptUtils.run(script, wrapper);
    }

    public static ActionLua fromString(String string) {
        return new ActionLua(string);
    }

    public static ActionLua fromFile() {
        // TODO
        return null;
    }
}
