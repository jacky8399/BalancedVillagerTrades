package com.jacky8399.balancedvillagertrades.utils;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.EnchantmentsField;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.LuaProxy;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseIoLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.*;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

// some code adapted from https://github.com/luaj/luaj/blob/master/examples/jse/SampleSandboxed.java
public class ScriptUtils {
    private static final Logger LOGGER = BalancedVillagerTrades.LOGGER;
    private static Globals scriptCompiler;

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

        injectUtils(globals);

        return globals;
    }

    public static Globals createSandbox(Consumer<Globals> consumer) {
        Globals globals = createSandbox();
        consumer.accept(globals);
        return globals;
    }

    private static final LuaValue hookFunction = new ZeroArgFunction() {
        @Override
        public LuaValue call() {
            throw new Error("Script exceeded max-instruction in config.yml");
        }
    };
    public static LuaValue runScriptInSandbox(Reader scriptReader, String chunkName, Globals globals) {
        if (scriptCompiler == null) {
            scriptCompiler = new Globals();
            scriptCompiler.load(new JseBaseLib());
            scriptCompiler.load(new PackageLib());
            scriptCompiler.load(new StringLib());
            scriptCompiler.load(new JseMathLib());
            LoadState.install(scriptCompiler);
            LuaC.install(scriptCompiler);
            LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);
        }

        LuaValue chunk = scriptCompiler.load(scriptReader, chunkName, globals);
        LuaThread thread = new LuaThread(globals, chunk);

        if (Config.luaMaxInstructions > 0) {
            globals.load(new DebugLib());
            var sethookFunction = globals.get("debug").get("sethook");
            globals.set("debug", LuaValue.NIL);
            sethookFunction.invoke(LuaValue.varargsOf(new LuaValue[]{
                    thread, hookFunction, LuaValue.EMPTYSTRING, LuaValue.valueOf(Config.luaMaxInstructions)
            }));
        }

        // propagate errors from the protected call
        Varargs varargs = thread.resume(LuaValue.NIL);
        if (!varargs.arg1().checkboolean()) {
            throw new LuaError(varargs.arg(2).checkjstring());
        }
        return varargs.arg(2);

    }

    public static LuaValue runScriptInSandbox(String script, String chunkName, Globals globals) {
        try (var stringReader = new StringReader(script)) {
            return runScriptInSandbox(stringReader, chunkName, globals);
        }
    }

    public static <T> LuaValue wrapField(T trade, ContainerField<T, ?> field) {
        return new FieldWrapper<T>(trade, field instanceof FieldProxy proxy ? proxy : FieldProxy.emptyAccessor(field));
    }

    public static <T> LuaFunction getIteratorFor(Collection<? extends T> collection, Function<T, Varargs> deconstructor) {
        var iterator = collection.iterator();
        var iteratorFunction = new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (!iterator.hasNext())
                    return LuaValue.NIL;
                T next = iterator.next();
                return deconstructor.apply(next);
            }
        };
        // return something like an iterable
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return iteratorFunction;
            }
        };
    }

    static class FieldWrapper<T> extends LuaTable {
        private final T trade;
        private final FieldProxy<T, ?, ?> field;
        FieldWrapper(T trade, FieldProxy<T, ?, ?> field) {
            this.trade = trade;
            this.field = field;
        }

        @Override
        public LuaValue rawget(LuaValue key) {
            if (key.isstring() && "children".equals(key.tojstring())) {
                var fields = field.getFields(trade);
                if (fields != null) {
                    return getIteratorFor(fields, LuaValue::valueOf);
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
            if (field.child instanceof LuaProxy<?> proxy) {
                Object instance = field.get(trade);
                if (((LuaProxy) proxy).setProperty(instance, key, value))
                    return;
            }

            String fieldName = key.checkjstring().replace('_', '-');
            var child = (FieldProxy) field.getFieldWrapped(fieldName);
            if (child != null) {
                Class<?> clazz = child.getFieldClass();
                if (clazz == String.class) {
                    child.set(trade, value.checkjstring());
                } else if (clazz == Integer.class) {
                    child.set(trade, value.checkint());
                } else if (clazz == Boolean.class) {
                    child.set(trade, value.checkboolean());
                } else {
                    throw new LuaError("Cannot assign " + value.typename() + " to field " + child.fieldName + " (" + clazz.getSimpleName() + ")");
                }
            }
        }

        static boolean warnNextDeprecation = true;
        static boolean warnNextDeprecationEnchantment = true;
        String[] children;
        @Override
        public Varargs next(LuaValue key) {
            if (warnNextDeprecationEnchantment && field.child instanceof EnchantmentsField) {
                LOGGER.warning("Using the built-in Lua function next() is deprecated for enchantments. " +
                        "Please use enchantments:entries() to get a key-value pair of enchantments.");
                warnNextDeprecationEnchantment = false;
            } else if (warnNextDeprecation) {
                LOGGER.warning("Using the built-in Lua function next() is deprecated for container fields. " +
                        "Please use field:children() to get a key-value pair of properties.");
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
                return LuaValue.varargsOf(nextLuaKey, rawget(nextLuaKey));
            }
            return LuaValue.NIL;
        }

        @Override
        public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
        public void set(int key, LuaValue value) { error("table is read-only"); }
        public void rawset(int key, LuaValue value) { error("table is read-only"); }
        public LuaValue remove(int pos) { return error("table is read-only"); }

        @Override
        public LuaValue tostring() {
            return LuaValue.valueOf("LuaWrapper{" + field.toString() + "}");
        }
    }

    static class ReadOnlyLuaTable extends LuaTable {
        public ReadOnlyLuaTable(LuaValue table) {
            presize(table.length(), 0);
            for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table
                    .next(n.arg1())) {
                LuaValue key = n.arg1();
                LuaValue value = n.arg(2);
                super.rawset(key, value.istable() ? new ReadOnlyLuaTable(value) : value);
            }
        }
        public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
        public void set(int key, LuaValue value) { error("table is read-only"); }
        public void rawset(int key, LuaValue value) { error("table is read-only"); }
        public void rawset(LuaValue key, LuaValue value) { error("table is read-only"); }
        public LuaValue remove(int pos) { return error("table is read-only"); }
    }
}
