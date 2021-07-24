package com.jacky8399.balancedvillagertrades.utils;

import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OperatorUtils {
    public static final Pattern BASIC_COMPARISON = Pattern.compile("^(>|>=|<|<=|=|<>)\\s*(\\d+)$");
    public static final Pattern BETWEEN = Pattern.compile("^between\\s+(\\d+)\\s+and(\\d+)$");
    public static final Pattern IN_RANGE = Pattern.compile("^in\\s+(\\d+)(?:-|.{2,3})(\\d+)$");

    @Nullable
    public static IntPredicate fromInput(String input) throws IllegalArgumentException {
        Matcher matcher;
        if ((matcher = BASIC_COMPARISON.matcher(input)).matches()) {
            String operator = matcher.group(1);
            int operand = Integer.parseInt(matcher.group(2));
            return getPredicateFromOperator(operator, operand);
        } else if ((matcher = BETWEEN.matcher(input)).matches() || (matcher = IN_RANGE.matcher(input)).matches()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            return i -> i >= min && i <= max;
        }
        return null;
    }

    @Nullable
    public static IntUnaryOperator getFunctionFromInput(String input) throws IllegalArgumentException {
        Matcher matcher;
        if ((matcher = BASIC_COMPARISON.matcher(input)).matches()) {
            String operator = matcher.group(1);
            int operand = Integer.parseInt(matcher.group(2));
            return getFunctionFromOperator(operator, ()->operand);
        } else if ((matcher = BETWEEN.matcher(input)).matches() || (matcher = IN_RANGE.matcher(input)).matches()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            return i -> Math.min(Math.max(i, min), max);
        }
        return null;
    }

    public static IntPredicate getPredicateFromOperator(String operator, int operand) {
        switch (operator) {
            case ">":
                return i -> i > operand;
            case "<":
                return i -> i < operand;
            case ">=":
                return i -> i >= operand;
            case "<=":
                return i -> i <= operand;
            case "=":
            case "==":
                return i -> i == operand;
            case "<>":
                return i -> i != operand;
            default:
                throw new IllegalArgumentException(operator + " is not a valid operator");
        }
    }

    public static IntUnaryOperator getFunctionFromOperator(String operator, IntSupplier operand) {
        switch (operator) {
            case ">":
                return i -> Math.max(i, operand.getAsInt() + 1);
            case "<":
                return i -> Math.min(i, operand.getAsInt() - 1);
            case ">=":
                return i -> Math.max(i, operand.getAsInt());
            case "<=":
                return i -> Math.min(i, operand.getAsInt());
            case "=":
            case "==":
                return i -> operand.getAsInt();
            default:
                throw new IllegalArgumentException(operator + " is not a valid operator");
        }
    }
}
