package com.jacky8399.balancedvillagertrades.utils;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.Field;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseIoLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.*;
import java.util.logging.Logger;

public class ScriptUtils {
    private static final Logger LOGGER = BalancedVillagerTrades.LOGGER;

    private static void redirectOutput(Globals globals) {
        // TODO fix
        globals.STDOUT = new PrintStream(new ByteArrayOutputStream(), true) {
            @Override
            public void flush() {
                String str = out.toString();
                LOGGER.info("[Script] " + str);
                out = new ByteArrayOutputStream();
            }
        };
        globals.STDERR = new PrintStream(new ByteArrayOutputStream(), true) {
            @Override
            public void flush() {
                String str = out.toString();
                LOGGER.severe("[Script] " + str);
                out = new ByteArrayOutputStream();
            }
        };
    }

    private static void injectUtils(Globals globals) {
        var enchantments = new LuaTable();
        enchantments.set("is_treasure", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(EnchantmentUtils.isTreasure(arg.checkjstring()));
            }
        });
        enchantments.set("get_cost", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var range = EnchantmentUtils.getEnchantmentPrice(args.arg(1).checkint(), args.arg(2).checkboolean());
                return LuaValue.varargsOf(LuaValue.valueOf(range.from()), LuaValue.valueOf(range.to()));
            }
        });

        globals.set("enchantments", enchantments);
    }

    public static Globals createSandbox() {
        var globals = new Globals();
        globals.load(new BaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        if (!Config.luaAllowIO) {
            globals.finder = filename -> null;
        } else {
            var dataFolder = BalancedVillagerTrades.INSTANCE.getDataFolder();
            globals.finder = filename -> {
                File file = new File(dataFolder, filename);
                try {
                    return file.exists() ? new FileInputStream(file) : null;
                } catch (IOException ignored) {
                    return null;
                }
            };
            globals.load(new JseIoLib());
        }
        LoadState.install(globals);
        LuaC.install(globals);

        injectUtils(globals);

        return globals;
    }

    public static LuaTable wrapField(TradeWrapper trade, ContainerField<TradeWrapper, ?> field) {
        return new FieldWrapper(trade, field);
    }

    static class FieldWrapper extends LuaTable {
        private final TradeWrapper trade;
        private final ContainerField<TradeWrapper, ?> field;
        FieldWrapper(TradeWrapper trade, ContainerField<TradeWrapper, ?> field) {
            this.trade = trade;
            this.field = field;
        }

        @Override
        public LuaValue rawget(LuaValue key) {
            String fieldName = key.checkjstring();
            var child = field.getFieldWrapped(fieldName);
            if (child == null && fieldName.indexOf('_') > -1) // access fields with hyphens
                child = field.getFieldWrapped(fieldName.replace('_', '-'));

            if (child == null) {
                return LuaValue.NIL;
            }

            Object value = child.get(trade);
            // null cannot have fields... right?
            // this will totally not cause problems in the future
            if (value != null && child.isComplex()) {
                return new FieldWrapper(trade, child);
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
        public void rawset(LuaValue key, LuaValue value) {
            String fieldName = key.checkjstring().replace('_', '-');
            var child = (Field) field.getFieldWrapped(fieldName);
            if (child != null) {
                Class<?> clazz = child.getFieldClass();
                if (clazz == String.class) {
                    child.set(trade, value.checkjstring());
                } else if (clazz == Integer.class) {
                    child.set(trade, value.checkint());
                } else if (clazz == Boolean.class) {
                    child.set(trade, value.checkboolean());
                } else {
                    throw new LuaError("Cannot assign " + value.typename() + " to " + clazz.getSimpleName());
                }
            }
        }

        String[] children;
        @Override
        public Varargs next(LuaValue key) {
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
                return LuaValue.varargsOf(nextLuaKey, rawget(nextLuaKey));
            }
            return LuaValue.NIL;
        }

        @Override
        public LuaValue setmetatable(LuaValue metatable) {
            return this;
        }

        @Override
        public LuaValue tostring() {
            return LuaValue.valueOf("LuaWrapper{" + field.toString() + "}");
        }
    }
}
