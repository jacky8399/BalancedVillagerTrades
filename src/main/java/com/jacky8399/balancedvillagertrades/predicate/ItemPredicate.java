package com.jacky8399.balancedvillagertrades.predicate;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ItemPredicate extends TradePredicate {

    public ItemPredicate(ItemStack stack, Set<ItemMatcher> matchers) {
        this.stack = stack.clone();
        this.matchers = ImmutableSet.copyOf(matchers);
    }

    public final ItemStack stack;
    public final ImmutableSet<ItemMatcher> matchers;

    public abstract ItemStack getStack(Villager villager, MerchantRecipe recipe);


    @Override
    public boolean test(Villager villager, MerchantRecipe merchantRecipe) {
        ItemStack stack1 = getStack(villager, merchantRecipe);
        for (ItemMatcher matcher : matchers) {
            if (!matcher.test(stack, stack1)) {
                return false;
            }
        }
        return true;
    }

    public static class ItemMatcher implements BiPredicate<ItemStack, ItemStack> {
        private ItemMatcher(String name, BiPredicate<ItemStack, ItemStack> predicate) {
            this.name = name;
            this.predicate = predicate;
            matchers.put(name, this);
        }
        private static final HashMap<String, ItemMatcher> matchers = new HashMap<>();

        public final String name;
        private final BiPredicate<ItemStack, ItemStack> predicate;

        @Override
        public boolean test(ItemStack stack1, ItemStack stack2) {
            return predicate.test(stack1, stack2);
        }

        static <T> ItemMatcher compareProperty(String name, Function<ItemStack, T> mapper, BiPredicate<T, T> predicate) {
            return new ItemMatcher(name, (i1, i2) -> {
                T t1 = mapper.apply(i1), t2 = mapper.apply(i2);
                return predicate.test(t1, t2);
            });
        }

        static <T> ItemMatcher compareMeta(String name, Predicate<ItemMeta> precondition, Function<ItemMeta, T> mapper, BiPredicate<T, T> predicate) {
            return compareProperty(name, ItemStack::getItemMeta, (m1, m2) -> {
                if (!precondition.test(m1) || !precondition.test(m2))
                    return false;
                T t1 = mapper.apply(m1), t2 = mapper.apply(m2);
                return predicate.test(t1, t2);
            });
        }

        public static final ItemMatcher SIMILAR = new ItemMatcher("similar", ItemStack::isSimilar);
        public static final ItemMatcher AMOUNT = compareProperty("amount", ItemStack::getAmount, Integer::equals);
        public static final ItemMatcher TYPE = compareProperty("type", ItemStack::getType, Material::equals);

    }
}
