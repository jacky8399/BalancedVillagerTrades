package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.actions.ActionSet;
import com.jacky8399.balancedvillagertrades.utils.OperatorUtils;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleField<TOwner, TField> implements Field<TOwner, TField> {
    public final Class<TField> clazz;
    public final Function<TOwner, TField> getter;
    @Nullable
    public final BiConsumer<TOwner, TField> setter;

    public SimpleField(Class<TField> clazz, Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter) {
        this.clazz = clazz;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public TField get(TOwner owner) {
        return getter.apply(owner);
    }

    @Override
    public void set(TOwner owner, TField value) {
        if (setter != null)
            setter.accept(owner, value);
    }

    @Override
    public boolean isReadOnly() {
        return setter == null;
    }

    @Override
    public Class<TField> getFieldClass() {
        return clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull BiPredicate<TradeWrapper, TField> parsePredicate(String input) throws IllegalArgumentException {
        if (clazz == Boolean.class) {
            if (!(input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")))
                throw new IllegalArgumentException("booleans can only be true or false");
            Boolean value = input.equalsIgnoreCase("true");
            return (ignored, obj) -> obj == value;
        } else if (clazz == Integer.class) {
            IntPredicate predicate = OperatorUtils.fromInput(input);
            if (predicate != null) {
                return (ignored, obj) -> predicate.test((Integer) obj);
            } else {
                try {
                    Integer number = Integer.valueOf(input);
                    return (ignored, obj) -> number.equals(obj);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid comparison expression or integer " + input);
                }
            }
        } else if (clazz == String.class) {
            return (BiPredicate<TradeWrapper, TField>) parseStringPredicate(input);
        }
        throw new UnsupportedOperationException();
    }

    private static final Pattern STRING_PATTERN = Pattern.compile("^(==?|contains|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static BiPredicate<TradeWrapper, String> parseStringPredicate(String input) {
        Matcher matcher = STRING_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return (ignored, obj) -> input.equalsIgnoreCase(obj);
        }
        String operation = matcher.group(1);
        String operand = matcher.group(2);
        boolean caseInsensitive = true;
        if (operand.startsWith("\"")) {
            if (operand.endsWith("\"i")) {
                operand = operand.substring(1, operand.length() - 2);
            } else if (operand.endsWith("\"")) {
                caseInsensitive = false;
                operand = operand.substring(1, operand.length() - 1);
            } else {
                throw new IllegalArgumentException("Expected \" or \"i at the end, got " + operand.charAt(operand.length() - 1));
            }
        }
        final String finalOperand = operand;
        switch (operation.toLowerCase(Locale.ENGLISH)) {
            case "=", "==" -> {
                return caseInsensitive ?
                        (ignored, obj) -> finalOperand.equalsIgnoreCase(obj) :
                        (ignored, obj) -> finalOperand.equals(obj);
            }
            case "contains" -> {
                if (caseInsensitive) {
                    String lowerCaseOperand = operand.toLowerCase(Locale.ENGLISH);
                    return (ignored, obj) -> obj != null && obj.toLowerCase(Locale.ENGLISH).contains(lowerCaseOperand);
                } else {
                    return (ignored, obj) -> obj != null && obj.contains(finalOperand);
                }
            }
            case "matches" -> {
                Pattern pattern = Pattern.compile(operand, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
                return (ignored, obj) -> obj != null && pattern.matcher(obj).matches();
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, TField, TField> parseTransformer(String input) throws IllegalArgumentException {
        UnaryOperator<TField> transformer = (UnaryOperator<TField>) ActionSet.getTransformer(clazz, input);
        return (wrapper, old) -> transformer.apply(old);
    }

    @Override
    public String toString() {
        return "SimpleField{type=" + clazz.getSimpleName() + ", readonly=" + isReadOnly() + "}";
    }
}