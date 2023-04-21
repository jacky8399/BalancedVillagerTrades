package com.jacky8399.balancedvillagertrades.fields;

import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;

public interface LuaProxy<T> {
    @Nullable
    LuaValue getProperty(T instance, LuaValue key);

    // TODO fix signature
    @Deprecated
    default boolean setProperty(T instance, LuaValue key, LuaValue value) {
        return false;
    }
}
