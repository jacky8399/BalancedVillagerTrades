package com.jacky8399.balancedvillagertrades.fields;

import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;

public interface LuaProxy<T> {
    @Nullable
    default LuaValue getProperty(T instance, LuaValue key) {
        return null;
    }

    @Deprecated
    default <TOwner> boolean setProperty(Field<TOwner, T> field, TOwner parent, LuaValue key, LuaValue value) {
        return false;
    }

    // methods to intercept conversion to/from Lua types
    @Nullable
    default LuaValue getLuaValue(T instance) {
        return null;
    }

    default <TOwner> boolean setLuaValue(Field<TOwner, T> field, TOwner parent, LuaValue value) {
        return false;
    }

}
