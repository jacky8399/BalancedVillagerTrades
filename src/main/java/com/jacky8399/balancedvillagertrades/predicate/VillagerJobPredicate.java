package com.jacky8399.balancedvillagertrades.predicate;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.Locale;
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
