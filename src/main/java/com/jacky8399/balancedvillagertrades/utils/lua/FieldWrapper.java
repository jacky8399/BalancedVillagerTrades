package com.jacky8399.balancedvillagertrades.utils.lua;

import com.jacky8399.balancedvillagertrades.fields.EnchantmentsField;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.LuaProxy;
import org.bukkit.NamespacedKey;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class FieldWrapper<T> extends ScriptUtils.ReadOnlyLuaTable {
    private final T trade;
    private final FieldProxy<T, ?, ?> field;

    public FieldWrapper(T trade, FieldProxy<T, ?, ?> field) {
        this.trade = trade;
        this.field = field;
    }

    @Override
    public LuaValue get(LuaValue key) {
        if (key.isstring() && "children".equals(key.tojstring())) {
            var fields = field.getFields(trade);
            if (fields != null) {
                return ScriptUtils.iterator(fields, LuaValue::valueOf);
            } else {
                return error("Cannot iterate children of non-container field " + field);
            }
        } else if (field.child instanceof LuaProxy<?> luaProxy) {
            Object instance = field.get(trade);
            @SuppressWarnings({"rawtypes", "unchecked"})
            var intercepted = ((LuaProxy) luaProxy).getProperty(instance, key);
            if (intercepted != null)
                return intercepted;
        }

        String fieldName = key.checkjstring();
        var child = field.getFieldWrapped(fieldName);
        if (child == null && fieldName.indexOf('_') > -1) // access fields with hyphens
            child = field.getFieldWrapped(fieldName.replace('_', '-'));

        if (child == null) {
            return LuaValue.NIL;
        }

        Object value = child.get(trade);
        if (value != null && child.isComplex()) {
            return new FieldWrapper<>(trade, child);
        } else {
            if (value instanceof Integer num) {
                return LuaValue.valueOf(num);
            } else if (value instanceof Boolean bool) {
                return LuaValue.valueOf(bool);
            } else if (value == null) {
                return LuaValue.NIL;
            } else {
                return LuaValue.valueOf(value.toString());
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(LuaValue key, LuaValue value) {
        if (field.child instanceof LuaProxy<?> proxy) {
            Object instance = field.get(trade);
            if (((LuaProxy) proxy).setProperty(instance, key, value))
                return;
        }

        String fieldName = key.checkjstring();
        var child = (FieldProxy) field.getFieldWrapped(fieldName);
        if (child == null && fieldName.indexOf('_') > -1) // access fields with hyphens
            child = field.getFieldWrapped(fieldName.replace('_', '-'));

        if (child != null) {
            Class<?> clazz = child.getFieldClass();
            try {
                if (clazz == String.class) {
                    child.set(trade, value.checkjstring());
                } else if (clazz == Integer.class) {
                    child.set(trade, value.checkint());
                } else if (clazz == Boolean.class) {
                    child.set(trade, value.checkboolean());
                } else if (clazz == NamespacedKey.class) {
                    NamespacedKey namespacedKey = NamespacedKey.fromString(value.checkjstring());
                    if (namespacedKey == null)
                        argerror("NamespacedKey");
                    child.set(trade, namespacedKey);
                } else {
                    error("Don't know how to assign Lua type %s to %s"
                            .formatted(value.typename(), clazz.getSimpleName()));
                }
            } catch (LuaError e) {
                error("Failed to set %s to %s: ".formatted(value.tojstring(), child.fieldName) +
                        e.getMessage());
            }
        }
    }

    static boolean warnNextDeprecation = true;
    static boolean warnNextDeprecationEnchantment = true;
    String[] children;

    @Override
    public Varargs next(LuaValue key) {
        if (warnNextDeprecationEnchantment && field.child instanceof EnchantmentsField) {
            ScriptUtils.LOGGER.warning("Using the built-in Lua functions next()/pairs() is deprecated for enchantments. " +
                    "Please use enchantments.entries() to get a key-value pair of enchantments.");
            ScriptUtils.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Lua-next-pairs#enchantments for more information.");
            if (ScriptUtils.runningScript.globals.debuglib != null)
                ScriptUtils.LOGGER.warning("Offending script: " + ScriptUtils.runningScript.globals.debuglib.traceback(1));
            else
                ScriptUtils.LOGGER.warning("Offending script: " + ScriptUtils.runningScript.globals.get("__chunkName").tojstring());
            warnNextDeprecationEnchantment = false;
        } else if (warnNextDeprecation) {
            ScriptUtils.LOGGER.warning("Using the built-in Lua functions next()/pairs() is deprecated for container fields. " +
                    "Please use field.children() to get a key set of properties.");
            ScriptUtils.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Lua-next-pairs#container-fields for more information.");
            if (ScriptUtils.runningScript.globals.debuglib != null)
                ScriptUtils.LOGGER.warning("Offending script: " + ScriptUtils.runningScript.globals.debuglib.traceback(1));
            else
                ScriptUtils.LOGGER.warning("Offending script: " + ScriptUtils.runningScript.globals.get("__chunkName").tojstring());
            warnNextDeprecation = false;
        }


        if (children == null) {
            var childrenFields = field.getFields(trade);
            if (childrenFields == null)
                return LuaValue.NIL;
            children = childrenFields.toArray(new String[0]);
        }
        String stringKey = null;
        if (key.isnil()) {
            stringKey = children[0];
        } else {
            // find index of key
            String luaKey = key.checkjstring();
            for (int i = 0; i < children.length - 1; i++) {
                if (children[i].equals(luaKey)) {
                    stringKey = children[i + 1];
                    break;
                }
            }
        }
        if (stringKey != null) {
            var nextLuaKey = LuaValue.valueOf(stringKey);
            return LuaValue.varargsOf(nextLuaKey, get(nextLuaKey));
        }
        return LuaValue.NIL;
    }

    @Override
    public void set(int key, LuaValue value) {
        set(LuaValue.valueOf(key), value);
    }

    @Override
    public LuaValue tostring() {
        return LuaValue.valueOf("LuaWrapper{" + field.toString() + "}");
    }
}
