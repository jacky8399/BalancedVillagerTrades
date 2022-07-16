package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;

public class NamespacedKeyField<TOwner> extends SimpleField<TOwner, NamespacedKey> {
    public NamespacedKeyField(Function<TOwner, NamespacedKey> getter, @Nullable BiConsumer<TOwner, NamespacedKey> setter) {
        super(NamespacedKey.class, getter, setter);
    }

    @Override
    public @NotNull BiPredicate<TradeWrapper, NamespacedKey> parsePredicate(@NotNull String input) throws IllegalArgumentException {
        Matcher matcher = STRING_PATTERN.matcher(input);
        if (!matcher.matches()) {
            String lowercase = input.toLowerCase(Locale.ENGLISH);
            NamespacedKey key = NamespacedKey.fromString(lowercase);
            if (key == null)
                throw new IllegalArgumentException("Invalid resource location " + lowercase);
            return (ignored, value) -> value.equals(key);
        }
        String operator = matcher.group(1);
        if (operator.equals("=") || operator.equals("==")) {
            String key = matcher.group(2).toLowerCase(Locale.ENGLISH);
            NamespacedKey operand = NamespacedKey.fromString(key);
            if (operand == null)
                throw new IllegalArgumentException("Invalid resource location " + key);
            return (ignored, value) -> value.equals(operand);
        }
        // support string predicates
        BiPredicate<TradeWrapper, String> stringPredicate = parseStringPredicate(input);
        return (ignored, value) -> stringPredicate.test(ignored, value.toString());
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, NamespacedKey, NamespacedKey> parseTransformer(@Nullable String input) throws IllegalArgumentException {
        if (input == null)
            return (ignored, old) -> null;
        NamespacedKey key = NamespacedKey.fromString(input);
        if (key == null)
            throw new IllegalArgumentException("Invalid resource location " + input);
        return (ignored, old) -> key;
    }
}
