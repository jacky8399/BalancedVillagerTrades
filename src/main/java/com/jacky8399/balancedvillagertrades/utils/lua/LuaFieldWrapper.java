package com.jacky8399.balancedvillagertrades.utils.lua;

import com.jacky8399.balancedvillagertrades.fields.item.EnchantmentsField;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.LuaProxy;
import org.bukkit.NamespacedKey;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class LuaFieldWrapper<T> extends ScriptRunner.ReadOnlyLuaTable {
    private final T trade;
    private final FieldProxy<T, ?, ?> field;

    public LuaFieldWrapper(T trade, FieldProxy<T, ?, ?> field) {
        this.trade = trade;
        this.field = field;
    }

    @Override
    public LuaValue get(LuaValue key) {
        if (key.isstring() && "children".equals(key.tojstring())) {
            var fields = field.getFields(trade);
            if (fields != null) {
                return ScriptRunner.iterator(fields, LuaValue::valueOf);
            } else {
                return error("Cannot iterate children of non-container field " + field);
            }
        } else if (field.child instanceof LuaProxy luaProxy) {
            Object instance = field.get(trade);
            var intercepted = luaProxy.getProperty(instance, key);
            if (intercepted != null)
                return intercepted;
        }

        String fieldName = key.checkjstring();
        var child = field.getFieldWrapped(fieldName);
        if (child == null && fieldName.indexOf('_') > -1) // access fields with hyphens
            child = field.getFieldWrapped(fieldName.replace('_', '-'));

        if (child == null) {
            return error("Field " + fieldName + " does not exist on " + field.fieldName);
        }

        Object value = child.get(trade);

        if (child.child instanceof LuaProxy proxy) {
            LuaValue intercepted = proxy.getLuaValue(value);
            if (intercepted != null)
                return intercepted;
        }

        if (value != null && child.isComplex()) {
            return new LuaFieldWrapper<>(trade, child);
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
        if (field.child instanceof LuaProxy proxy) {
            if (proxy.setProperty(field, trade, key, value))
                return;
        }

        String fieldName = key.checkjstring();

        var child = (FieldProxy) field.getFieldWrapped(fieldName);
        if (child == null && fieldName.indexOf('_') > -1) // access fields with hyphens
            child = field.getFieldWrapped(fieldName.replace('_', '-'));

        if (child == null) {
            error("Field " + fieldName + " does not exist on " + field.fieldName);
        }

        if (child.child instanceof LuaProxy proxy) {
            if (proxy.setLuaValue(child, trade, value))
                return;
        }

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

    static boolean warnNextDeprecation = true;
    static boolean warnNextDeprecationEnchantment = true;
    String[] children;

    @Override
    public Varargs next(LuaValue key) {
        if (warnNextDeprecationEnchantment && field.child instanceof EnchantmentsField) {
            ScriptRunner.LOGGER.warning("Using the built-in Lua functions next()/pairs() is deprecated for enchantments. " +
                    "Please use enchantments.entries() to get a key-value pair of enchantments.");
            ScriptRunner.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Lua-next-pairs#enchantments for more information.");
            if (ScriptRunner.runningScript.globals.debuglib != null)
                ScriptRunner.LOGGER.warning("Offending script: " + ScriptRunner.runningScript.globals.debuglib.traceback(1));
            else
                ScriptRunner.LOGGER.warning("Offending script: " + ScriptRunner.runningScript.globals.get("__chunkName").tojstring());
            warnNextDeprecationEnchantment = false;
        } else if (warnNextDeprecation) {
            ScriptRunner.LOGGER.warning("Using the built-in Lua functions next()/pairs() is deprecated for container fields. " +
                    "Please use field.children() to get a key set of properties.");
            ScriptRunner.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Lua-next-pairs#container-fields for more information.");
            if (ScriptRunner.runningScript.globals.debuglib != null)
                ScriptRunner.LOGGER.warning("Offending script: " + ScriptRunner.runningScript.globals.debuglib.traceback(1));
            else
                ScriptRunner.LOGGER.warning("Offending script: " + ScriptRunner.runningScript.globals.get("__chunkName").tojstring());
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
