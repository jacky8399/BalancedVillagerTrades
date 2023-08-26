package com.jacky8399.balancedvillagertrades.utils.lua;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.EnchantmentUtils;
import net.md_5.bungee.api.ChatColor;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.awt.*;
import java.util.Random;

public class ScriptUtilities {

    public static void inject(Globals globals) {
        globals.set("enchantments", ENCHANTMENT_UTILS);
        globals.set("color", COLOR);
        globals.set("random", RANDOM);
    }

    public static final ScriptRunner.ReadOnlyLuaTable ENCHANTMENT_UTILS;

    public static final ScriptRunner.ReadOnlyLuaTable COLOR;

    // stolen from MathLib.random
    public static final LibFunction RANDOM = new LibFunction() {
        private final Random random = BalancedVillagerTrades.RANDOM;
        @Override
        public LuaValue call() {
            return valueOf(random.nextDouble());
        }

        @Override
        public LuaValue call(LuaValue a) {
            int m = a.checkint();
            if (m < 1)
                argerror(1, "interval is empty");
            return valueOf(1+random.nextInt(m));
        }

        @Override
        public LuaValue call(LuaValue a, LuaValue b) {
            int m = a.checkint();
            int n = b.checkint();
            if (n < m)
                argerror(2, "interval is empty");
            return valueOf(m+random.nextInt(n+1-m));
        }
    };

    static {
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

        ENCHANTMENT_UTILS = new ScriptRunner.ReadOnlyLuaTable(enchantments);

        var colors = new LuaTable();
        for (ChatColor color : ChatColor.values()) {
            colors.set(color.getName(), color.toString());
        }

        colors.set("of", new VarArgFunction() {
            @SuppressWarnings("deprecation")
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() == 3) {
                    return LuaValue.valueOf(ChatColor.of(new Color(args.arg1().checkint(), args.arg(2).checkint(), args.arg(3).checkint())).toString());
                } else {
                    LuaValue arg1 = args.arg1();
                    return LuaValue.valueOf((arg1.isint() ? ChatColor.of(new Color(arg1.toint())) : ChatColor.of(arg1.checkjstring())).toString());
                }
            }
        });

        COLOR = new ScriptRunner.ReadOnlyLuaTable(colors);

    }
}
