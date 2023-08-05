package com.jacky8399.balancedvillagertrades.utils.lua;

import org.luaj.vm2.lib.OsLib;

public class LuaOsLib extends OsLib {
    @Override
    protected void exit(int code) {
        throw new IllegalStateException("Not permitted");
    }

    @Override
    protected String getenv(String varname) {
        throw new IllegalStateException("Not permitted");
    }
}
