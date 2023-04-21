package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.fields.ItemStackField.ItemStackWrapper;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EnchantmentsField implements ContainerField<ItemStackWrapper, ItemStackWrapper>, LuaProxy<ItemStackWrapper> {
    private static final Field<ItemStackWrapper, Integer> SIZE_FIELD = Field.readOnlyField(Integer.class, wrapper -> getEnchants(wrapper).size());
    @Override
    public @Nullable Field<ItemStackWrapper, ?> getField(String fieldName) {
        if ("size".equals(fieldName))
            return SIZE_FIELD;

        try {
            return getEnchantmentField(readEnchantment(fieldName));
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    @NotNull
    private static Enchantment readEnchantment(@NotNull String string) {
        NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(string.toLowerCase(Locale.ENGLISH)), "Invalid resource location" + string);
        return Objects.requireNonNull(Enchantment.getByKey(key), "Unknown enchantment " + string);
    }

    private static Field<ItemStackWrapper, Integer> getEnchantmentField(Enchantment enchantment) {
        return ItemStackField.metaField(Integer.class,
                itemMeta -> itemMeta instanceof EnchantmentStorageMeta storageMeta ?
                        storageMeta.getStoredEnchantLevel(enchantment) :
                        itemMeta.getEnchantLevel(enchantment),
                (itemMeta, newLevel) -> {
                    if (itemMeta instanceof EnchantmentStorageMeta storageMeta) {
                        if (newLevel <= 0)
                            storageMeta.removeStoredEnchant(enchantment);
                        else
                            storageMeta.addStoredEnchant(enchantment, newLevel, true);
                    } else {
                        if (newLevel <= 0)
                            itemMeta.removeEnchant(enchantment);
                        else
                            itemMeta.addEnchant(enchantment, newLevel, true);
                    }
                });
    }

    @Override
    public @Nullable Collection<String> getFields(@Nullable ItemStackWrapper wrapper) {
        if (wrapper == null) {
            return List.of("size");
        }
        List<String> fields = new ArrayList<>();
        for (Enchantment enchantment : getEnchants(wrapper).keySet()) {
            fields.add(enchantment.getKey().toString());
        }
        // list enchantment fields first
        fields.add("size");
        return fields;
    }

    @Override
    public ItemStackWrapper get(ItemStackWrapper itemMeta) {
        return itemMeta;
    }

    @Override
    public void set(ItemStackWrapper itemMeta, ItemStackWrapper value) {

    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Class<ItemStackWrapper> getFieldClass() {
        return ItemStackWrapper.class;
    }

    private static final Pattern CONTAINS_REGEX = Pattern.compile("^contains\\s+(\\S+)$");
    // see examples in FieldTest
    private static final Pattern CATEGORY_MATCHER_REGEX = Pattern.compile("^(all|none|any|some)\\s*(?:is|are)?\\s*(n'?t|not|non-?)?\\s*(treasure|curse)s?$");
    private static final Pattern CONFLICTS_WITH_REGEX = Pattern.compile("^conflicts with\\s+(\\S+)$");

    @Override
    public @NotNull BiPredicate<TradeWrapper, ItemStackWrapper> parsePredicate(@NotNull String input) {
        Matcher matcher;
        if ((matcher = CONTAINS_REGEX.matcher(input)).matches()) {
            Enchantment enchantment = readEnchantment(matcher.group(1));
            return (trade, wrapper) -> wrapper.meta() instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.hasStoredEnchant(enchantment) :
                    wrapper.meta().hasEnchant(enchantment);
        } else if ((matcher = CATEGORY_MATCHER_REGEX.matcher(input)).matches()) {
            MatchMode matchMode = MatchMode.valueOf(matcher.group(1).toUpperCase(Locale.ENGLISH));
            boolean negate = matcher.group(2) != null;
            String category = matcher.group(3);
            if ("treasure".equals(category)) {
                return checkMatch(Enchantment::isTreasure, matchMode, negate);
            } else if ("curse".equals(category)) {
                return checkMatch(Enchantment::isCursed, matchMode, negate);
            } else {
                throw new IllegalArgumentException("Invalid category " + category);
            }
        } else if ((matcher = CONFLICTS_WITH_REGEX.matcher(input)).matches()) {
            Enchantment enchantment = readEnchantment(matcher.group(1));
            return (trade, wrapper) -> wrapper.meta() instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.hasConflictingStoredEnchant(enchantment) :
                    wrapper.meta().hasConflictingEnchant(enchantment);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable LuaValue getProperty(ItemStackWrapper instance, LuaValue key) {
        if (key.isstring() && "entries".equals(key.checkjstring())) {
            Map<Enchantment, Integer> enchants = getEnchants(instance);
            return ScriptUtils.iterator(enchants.entrySet(),
                    entry -> LuaValue.varargsOf(LuaValue.valueOf(entry.getKey().getKey().toString()), LuaValue.valueOf(entry.getValue()), LuaValue.NIL));
        }
        return null;
    }

    private enum MatchMode {
        ALL(Stream::allMatch), NONE(Stream::noneMatch), ANY(Stream::anyMatch), SOME(Stream::anyMatch);

        final BiPredicate<Stream<Enchantment>, Predicate<Enchantment>> matchFunction;
        MatchMode(BiPredicate<Stream<Enchantment>, Predicate<Enchantment>> matchFunction) {
            this.matchFunction = matchFunction;
        }
    }
    private BiPredicate<TradeWrapper, ItemStackWrapper> checkMatch(Predicate<Enchantment> predicate, MatchMode matchMode, boolean negate) {
        return (trade, wrapper) -> {
            var enchantmentMap = getEnchants(wrapper);
            var stream = enchantmentMap.keySet().stream();

            return matchMode.matchFunction.test(stream, negate ? predicate.negate() : predicate); // XOR
        };
    }


    @Override
    public @NotNull BiFunction<TradeWrapper, ItemStackWrapper, ItemStackWrapper> parseTransformer(@Nullable String input) {
        return ContainerField.super.parseTransformer(input);
    }

    private static Map<Enchantment, Integer> getEnchants(ItemStackWrapper wrapper) {
        return getEnchants(wrapper.meta());
    }

    private static Map<Enchantment, Integer> getEnchants(ItemMeta meta) {
        return meta instanceof EnchantmentStorageMeta storageMeta ?
                storageMeta.getStoredEnchants() :
                meta.getEnchants();
    }
}
