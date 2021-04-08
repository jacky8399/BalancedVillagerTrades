package com.jacky8399.balancedvillagertrades.predicate;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class VillagerJobPredicate extends TradePredicate {
    public VillagerJobPredicate(Target target, MatchMode matchMode, String pattern) {
        this.target = target;
        this.matchMode = matchMode;
        if (matchMode == MatchMode.REGEX) {
            this.pattern = pattern;
            try {
                regexPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex", e);
            }
        } else {
            this.pattern = pattern.toLowerCase(Locale.ROOT);
            regexPattern = null;
        }
    }

    private static final Pattern REGEX = Pattern.compile("^(profession|type)\\s*?(=|matches)\\s*?(.+)$", Pattern.CASE_INSENSITIVE);
    public static VillagerJobPredicate parse(Object obj) {
        if (!(obj instanceof String))
            throw new IllegalArgumentException("Expected string");
        Matcher matcher = REGEX.matcher(obj.toString());
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid format");
        Target target = Target.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
        MatchMode mode = matcher.group(2).equals("=") ? MatchMode.TEXT : MatchMode.REGEX;
        String pattern = matcher.group(3);
        return new VillagerJobPredicate(target, mode, pattern);
    }

    public final Target target;
    public final MatchMode matchMode;
    public final String pattern;
    private final Pattern regexPattern;

    @Override
    public boolean test(Villager villager, MerchantRecipe merchantRecipe) {
        String target = (this.target == Target.PROFESSION ? villager.getProfession() : villager.getVillagerType())
                .name().toLowerCase(Locale.ROOT);
        if (matchMode == MatchMode.TEXT)
            return target.equals(pattern);
        else
            return regexPattern.matcher(target).matches();
    }

    public enum MatchMode {
        TEXT, REGEX
    }

    public enum Target {
        PROFESSION, TYPE;
    }
}
