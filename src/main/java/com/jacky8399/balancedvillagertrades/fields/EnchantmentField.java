package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.EnchantmentPair;
import com.jacky8399.balancedvillagertrades.utils.Pair;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Map.entry;

public class EnchantmentField<T> extends SimpleContainerField<T, EnchantmentPair>{
    public EnchantmentField(Function<T, EnchantmentPair> getter, @Nullable BiConsumer<T, EnchantmentPair> setter) {
        super(EnchantmentPair.class, getter, setter, Map.ofEntries(
                entry("id", new SimpleField<>(String.class,
                                pair -> pair.getKey().getKey().toString(),
                                (pair, string) -> pair.putKey(Enchantment.getByKey(NamespacedKey.fromString(string)))
                        )
                ),
                entry("level", new SimpleField<>(Integer.class,
                        Pair::getValue,
                        Pair::putValue)),
                entry("max-level", Field.readOnlyField(Integer.class, pair -> pair.getKey().getMaxLevel())),
                entry("start-level", Field.readOnlyField(Integer.class, pair -> pair.getKey().getStartLevel())),
                entry("is-curse", Field.readOnlyField(Boolean.class, pair -> pair.getKey().isCursed())),
                entry("is-treasure", Field.readOnlyField(Boolean.class, pair -> pair.getKey().isTreasure())),
                entry("is-discoverable", Field.readOnlyField(Boolean.class, pair -> pair.getKey().isDiscoverable())),
                entry("rarity", Field.readOnlyField(String.class, pair -> pair.getKey().getRarity().toString()))
        ));
    }
}
