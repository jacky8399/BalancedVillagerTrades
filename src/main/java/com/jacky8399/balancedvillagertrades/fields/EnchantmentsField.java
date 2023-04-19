package com.jacky8399.balancedvillagertrades.fields;

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
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EnchantmentsField implements ContainerField<ItemMeta, ItemMeta>, LuaProxy<ItemMeta> {
    private static final Field<ItemMeta, Integer> SIZE_FIELD = Field.readOnlyField(Integer.class,
            itemMeta -> itemMeta instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.getStoredEnchants().size() :
                    itemMeta.getEnchants().size());
    @Override
    public @Nullable Field<ItemMeta, ?> getField(String fieldName) {
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

    private static Field<ItemMeta, Integer> getEnchantmentField(Enchantment enchantment) {
        return Field.field(Integer.class,
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
    public @Nullable Collection<String> getFields(@Nullable ItemMeta itemMeta) {
        if (itemMeta == null) {
            return List.of("size");
        }
        List<String> fields = new ArrayList<>();
        fields.add("size");
        itemMeta.getEnchants().keySet().forEach(enchantment -> fields.add(enchantment.getKey().toString()));
        return fields;
    }

    @Override
    public ItemMeta get(ItemMeta itemMeta) {
        return itemMeta;
    }

    @Override
    public void set(ItemMeta itemMeta, ItemMeta value) {

    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Class<ItemMeta> getFieldClass() {
        return ItemMeta.class;
    }

    private static final Pattern CONTAINS_REGEX = Pattern.compile("^contains\\s+(\\S+)$");
    // see examples in FieldTest
    private static final Pattern CATEGORY_MATCHER_REGEX = Pattern.compile("^(all|none|any|some)\\s*(?:is|are)?\\s*(n'?t|not|non-?)?\\s*(treasure|curse)s?$");
    private static final Pattern CONFLICTS_WITH_REGEX = Pattern.compile("^conflicts with\\s+(\\S+)$");

    @Override
    public @NotNull BiPredicate<TradeWrapper, ItemMeta> parsePredicate(@NotNull String input) {
        Matcher matcher;
        if ((matcher = CONTAINS_REGEX.matcher(input)).matches()) {
            Enchantment enchantment = readEnchantment(matcher.group(1));
            return (trade, meta) -> meta instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.hasStoredEnchant(enchantment) :
                    meta.hasEnchant(enchantment);
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
            return (trade, meta) -> meta instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.hasConflictingStoredEnchant(enchantment) :
                    meta.hasConflictingEnchant(enchantment);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable LuaValue getProperty(ItemMeta instance, LuaValue key) {
        String fieldName = key.checkjstring();
        Supplier<Map<Enchantment, Integer>> enchantsGetter = instance instanceof EnchantmentStorageMeta storageMeta ?
                storageMeta::getStoredEnchants :
                instance::getEnchants;
        return switch (fieldName) {
            case "entries", "entrySet" -> ScriptUtils.getIteratorFor(enchantsGetter.get().entrySet(),
                    entry -> LuaValue.varargsOf(LuaValue.valueOf(entry.getKey().getKey().toString()), LuaValue.valueOf(entry.getValue()), LuaValue.NIL));
            case "keys", "keySet" -> ScriptUtils.getIteratorFor(enchantsGetter.get().keySet(),
                    ench -> LuaValue.valueOf(ench.getKey().toString()));
            case "values" -> ScriptUtils.getIteratorFor(enchantsGetter.get().values(), LuaValue::valueOf);
            default -> null;
        };
    }

    @Override
    public boolean setProperty(ItemMeta instance, LuaValue key, LuaValue value) {
        return false;
    }

    private enum MatchMode {
        ALL(Stream::allMatch), NONE(Stream::noneMatch), ANY(Stream::anyMatch), SOME(Stream::anyMatch);

        final BiPredicate<Stream<Enchantment>, Predicate<Enchantment>> matchFunction;
        MatchMode(BiPredicate<Stream<Enchantment>, Predicate<Enchantment>> matchFunction) {
            this.matchFunction = matchFunction;
        }
    }
    private BiPredicate<TradeWrapper, ItemMeta> checkMatch(Predicate<Enchantment> predicate, MatchMode matchMode, boolean negate) {
        return (trade, meta) -> {
            var enchantmentMap = meta instanceof EnchantmentStorageMeta storageMeta ?
                    storageMeta.getStoredEnchants() :
                    meta.getEnchants();
            var stream = enchantmentMap.keySet().stream();

            return matchMode.matchFunction.test(stream, negate ? predicate.negate() : predicate); // XOR
        };
    }


    @Override
    public @NotNull BiFunction<TradeWrapper, ItemMeta, ItemMeta> parseTransformer(@Nullable String input) {
        return ContainerField.super.parseTransformer(input);
    }
}
