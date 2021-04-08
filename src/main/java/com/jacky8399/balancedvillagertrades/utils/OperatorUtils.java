package com.jacky8399.balancedvillagertrades.utils;

import java.util.function.IntPredicate;

public class OperatorUtils {
    public static IntPredicate getFromOperator(String operator, int operand) {
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
}
