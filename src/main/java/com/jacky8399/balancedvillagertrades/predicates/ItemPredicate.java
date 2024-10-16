package com.jacky8399.balancedvillagertrades.predicates;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ItemPredicate extends TradePredicate {

    public ItemPredicate(ItemStack stack, Set<ComplexItemMatcher> matchers, List<ItemMatcher> simpleMatchers) {
        this.stack = stack.clone();
//        this.matchers = ImmutableSet.copyOf(matchers);
        this.simpleMatchers = ImmutableList.copyOf(simpleMatchers);
    }

    @NotNull
    public final ItemStack stack;
//    @NotNull
//    public final ImmutableSet<ComplexItemMatcher> matchers;
    @NotNull
    public final ImmutableList<ItemMatcher> simpleMatchers;

    @Nullable
    public abstract ItemStack getStack(AbstractVillager villager, MerchantRecipe recipe);

    @Override
    public boolean test(TradeWrapper tradeWrapper) {
        ItemStack toTest = getStack(tradeWrapper.getVillager(), tradeWrapper.getRecipe());
        if (toTest == null)
            return false;
        for (ItemMatcher simpleMatcher : simpleMatchers) {
            if (!simpleMatcher.test(toTest)) {
                return false;
            }
        }
//        for (ComplexItemMatcher matcher : matchers) {
//            if (!matcher.test(stack, toTest)) {
//                return false;
//            }
//        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
//        if (stack.getType() != Material.AIR) {
//            builder.append("Match against ").append(stack).append(" by:\n");
//            for (ComplexItemMatcher matcher : matchers) {
//                builder.append("- ").append(matcher.name).append('\n');
//            }
//        }
        builder.append("Match by:\n");
        for (ItemMatcher matcher : simpleMatchers) {
            builder.append("- ").append(matcher.pattern).append('\n');
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    public static class ComplexItemMatcher implements BiPredicate<ItemStack, ItemStack> {
        private ComplexItemMatcher(String name, BiPredicate<ItemStack, ItemStack> predicate) {
            this.name = name;
            this.predicate = predicate;
            matchers.put(name, this);
        }
        private static final HashMap<String, ComplexItemMatcher> matchers = new HashMap<>();

        public final String name;
        private final BiPredicate<ItemStack, ItemStack> predicate;

        @Override
        public boolean test(ItemStack stack1, ItemStack stack2) {
            return predicate.test(stack1, stack2);
        }

//        static <T> ComplexItemMatcher compareProperty(String name, Function<ItemStack, T> mapper, BiPredicate<T, T> predicate) {
//            return new ComplexItemMatcher(name, (i1, i2) -> {
//                T t1 = mapper.apply(i1), t2 = mapper.apply(i2);
//                return predicate.test(t1, t2);
//            });
//        }
//
//        static <T> ComplexItemMatcher compareMeta(String name, Predicate<ItemMeta> precondition, Function<ItemMeta, T> mapper, BiPredicate<T, T> predicate) {
//            return compareProperty(name, ItemStack::getItemMeta, (m1, m2) -> {
//                if (!precondition.test(m1) || !precondition.test(m2))
//                    return false;
//                T t1 = mapper.apply(m1), t2 = mapper.apply(m2);
//                return predicate.test(t1, t2);
//            });
//        }

//        public static final ComplexItemMatcher SIMILAR = new ComplexItemMatcher("similar", ItemStack::isSimilar);
//        public static final ComplexItemMatcher AMOUNT = compareProperty("amount", ItemStack::getAmount, Integer::equals);
//        public static final ComplexItemMatcher TYPE = compareProperty("type", ItemStack::getType, Material::equals);
//        public static final ComplexItemMatcher NAME = compareMeta("name", ItemMeta::hasDisplayName, ItemMeta::getDisplayName, String::equals);
    }

    public static abstract class ItemMatcher implements Predicate<ItemStack> {
        public final String pattern;
        public ItemMatcher(String pattern) {
            this.pattern = pattern;
        }
    }

    private static final Pattern TYPE_REGEX = Pattern.compile("^type\\s*(=|matches)\\s*(.+)$");
    private static final Pattern AMOUNT_REGEX = Pattern.compile("^(?:amount|count)\\s*(.+)$");
    private static final Pattern ENCHANTMENT_REGEX = Pattern.compile("^enchantments?\\s+contains?\\s+([A-Za-z_:]+)$");
    private static final Pattern ENCHANTMENT_LEVEL_REGEX = Pattern.compile("^enchantments?\\s+([A-Za-z_:]+)\\s+(.+)$");
    /** lazy implementation */
    public static ItemMatcher getFromInput(String str) {
        String trimmed = str.trim();
        Matcher matcher;
        if ((matcher = TYPE_REGEX.matcher(trimmed)).matches()) {
            String type = matcher.group(1);
            String pattern = matcher.group(2).trim();
            if (type.equals("=")) {
                Material mat = Material.matchMaterial(pattern);
                Preconditions.checkNotNull(mat, "Can't find type by name " + pattern);
                return new ItemMatcher(trimmed) {
                    @Override
                    public boolean test(ItemStack stack) {
                        return stack.getType().equals(mat);
                    }
                };
            } else { // regex oh god
                Pattern innerPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                return new ItemMatcher(trimmed) {
                    @Override
                    public boolean test(ItemStack stack) {
                        return innerPattern.matcher(stack.getType().name()).matches();
                    }
                };
            }
        } else if ((matcher = AMOUNT_REGEX.matcher(trimmed)).matches()) {
            String operator = matcher.group(1);
            IntPredicate predicate;
            predicate = OperatorUtils.fromInput(operator);
            if (predicate == null) {
                throw new IllegalArgumentException("Invalid comparison statement " + operator);
            }
            return new ItemMatcher(trimmed) {
                @Override
                public boolean test(ItemStack stack) {
                    return predicate.test(stack.getAmount());
                }
            };
        } else if ((matcher = ENCHANTMENT_REGEX.matcher(trimmed)).matches()) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(matcher.group(1)));
            if (enchantment == null) {
                throw new IllegalArgumentException("Invalid enchantment key " + matcher.group(1));
            }
            return new ItemMatcher(trimmed) {
                @Override
                public boolean test(ItemStack stack) {
                    ItemMeta meta = stack.getItemMeta();
                    return meta instanceof EnchantmentStorageMeta ?
                            ((EnchantmentStorageMeta) meta).hasStoredEnchant(enchantment) :
                            meta != null && meta.hasEnchant(enchantment);
                }
            };
        } else if ((matcher = ENCHANTMENT_LEVEL_REGEX.matcher(trimmed)).matches()) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(matcher.group(1)));
            if (enchantment == null) {
                throw new IllegalArgumentException("Invalid enchantment key " + matcher.group(1));
            }
            String operator = matcher.group(2);
            IntPredicate predicate;
            predicate = OperatorUtils.fromInput(operator);
            if (predicate == null) {
                throw new IllegalArgumentException("Invalid comparison expression " + operator);
            }
            return new ItemMatcher(trimmed) {
                @Override
                public boolean test(ItemStack stack) {
                    ItemMeta meta = stack.getItemMeta();
                    return meta instanceof EnchantmentStorageMeta ?
                            predicate.test(((EnchantmentStorageMeta) meta).getStoredEnchantLevel(enchantment)) :
                            meta != null && predicate.test(meta.getEnchantLevel(enchantment));
                }
            };
        }
        throw new IllegalArgumentException(str + " is not a valid matcher");
    }

    public static List<ItemMatcher> getFromInput(List<String> str) {
        return str.stream().map(ItemPredicate::getFromInput).collect(Collectors.toList());
    }
}
