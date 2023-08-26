package com.jacky8399.balancedvillagertrades.utils;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OperatorUtils {
    public static final Pattern BASIC_COMPARISON = Pattern.compile("^(>|>=|<|<=|==?|<>|!=)\\s*(\\d+)$");
    public static final Pattern OPERATORS = Pattern.compile("^([+\\-*/%]=)\\s*(\\d+)$");
    public static final Pattern BETWEEN = Pattern.compile("^between\\s+(\\d+)\\s+and\\s+(\\d+)$");
    public static final Pattern RANDOM_BETWEEN = Pattern.compile("^random between\\s+(\\d+)\\s+and\\s+(\\d+)$");
    public static final Pattern IN_RANGE = Pattern.compile("^in\\s+(\\d+)(?:-|\\.{2,3})(\\d+)$");

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
            if (min > max)
                throw new IllegalArgumentException("Empty interval " + min + ".." + max);
            return i -> i >= min && i <= max;
        }
        return null;
    }

    @Nullable
    public static IntUnaryOperator getFunctionFromInput(String input) throws IllegalArgumentException {
        Matcher matcher;
        if ((matcher = BASIC_COMPARISON.matcher(input)).matches() || (matcher = OPERATORS.matcher(input)).matches()) {
            String operator = matcher.group(1);
            int operand = Integer.parseInt(matcher.group(2));
            return getFunctionFromOperator(operator, ()->operand);
        } else if ((matcher = BETWEEN.matcher(input)).matches() || (matcher = IN_RANGE.matcher(input)).matches()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            if (min > max)
                throw new IllegalArgumentException("Empty interval " + min + ".." + max);
            return i -> Math.min(Math.max(i, min), max);
        } else if ((matcher = RANDOM_BETWEEN.matcher(input)).matches()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2)) + 1; // inclusive
            if (min > max)
                throw new IllegalArgumentException("Empty interval " + min + ".." + max);
            return i -> BalancedVillagerTrades.RANDOM.nextInt(min, max);
        }
        return null;
    }

    public static IntPredicate getPredicateFromOperator(String operator, int operand) {
        return switch (operator) {
            case ">" -> i -> i > operand;
            case "<" -> i -> i < operand;
            case ">=" -> i -> i >= operand;
            case "<=" -> i -> i <= operand;
            case "=", "==" -> i -> i == operand;
            case "!=", "<>" -> i -> i != operand;
            default -> throw new IllegalArgumentException(operator + " is not a valid operator");
        };
    }

    public static IntUnaryOperator getFunctionFromOperator(String operator, IntSupplier operand) {
        return switch (operator) {
            case ">" -> i -> Math.max(i, operand.getAsInt() + 1);
            case "<" -> i -> Math.min(i, operand.getAsInt() - 1);
            case ">=" -> i -> Math.max(i, operand.getAsInt());
            case "<=" -> i -> Math.min(i, operand.getAsInt());
            case "=", "==" -> i -> operand.getAsInt();
            case "+=" -> i -> i + operand.getAsInt();
            case "-=" -> i -> i - operand.getAsInt();
            case "*=" -> i -> i * operand.getAsInt();
            case "/=" -> i -> i / operand.getAsInt();
            case "%=" -> i -> i % operand.getAsInt();
            default -> throw new IllegalArgumentException(operator + " is not a valid operator");
        };
    }
}
