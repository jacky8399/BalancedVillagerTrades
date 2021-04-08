package com.jacky8399.balancedvillagertrades.actions;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;

public class ActionRemove extends Action {
    public boolean remove;
    @Override
    public void accept(TradeWrapper tradeWrapper) {
        tradeWrapper.setRemove(remove);
    }
}
