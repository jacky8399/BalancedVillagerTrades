package com.jacky8399.balancedvillagertrades.utils;

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

public class OperatorUtils {
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
                return i -> operand.getAsInt();
            default:
                throw new IllegalArgumentException(operator + " is not a valid operator");
        }
    }
}
