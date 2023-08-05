package com.jacky8399.balancedvillagertrades.utils.lua;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
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
public class ScriptRunner {
    static final Logger LOGGER = BalancedVillagerTrades.LOGGER;
    private static Globals scriptCompiler;


    public static Globals createSandbox() {
        var globals = new Globals();
        globals.load(new BaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        globals.load(new LuaOsLib());
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
        globals.set("__chunkName", "?");
        ScriptUtilities.inject(globals);

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
    static LuaThread runningScript;
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
        if (Config.luaMaxInstructions > 0)
            globals.load(new DebugLib());
        if (Config.luaMaxInstructions > 0) {
            var sethookFunction = globals.get("debug").get("sethook");
            globals.set("debug", LuaValue.NIL);
            sethookFunction.invoke(LuaValue.varargsOf(new LuaValue[]{
                    thread, hookFunction, LuaValue.EMPTYSTRING, LuaValue.valueOf(Config.luaMaxInstructions)
            }));
        }

        runningScript = thread;
        // propagate errors from the protected call
        Varargs varargs = thread.resume(LuaValue.NIL);
        runningScript = null;
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

    public static <T> LuaFunction iterator(Collection<? extends T> collection, Function<T, Varargs> deconstructor) {
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

    static class ReadOnlyLuaTable extends LuaTable {

        public ReadOnlyLuaTable() {

        }

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
