package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

public class ActionRemove extends Action {
    public final boolean remove;

    public ActionRemove(boolean remove) {
        this.remove = remove;
    }

    @Override
    public void accept(TradeWrapper tradeWrapper) {
        tradeWrapper.setRemove(remove);
    }

    @Override
    public String toString() {
        return "Set removal status to " + remove;
    }
}
