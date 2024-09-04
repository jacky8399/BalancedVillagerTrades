package com.jacky8399.balancedvillagertrades.utils;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.item.ItemStackField;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class VersionUtils {
    private static final boolean HAS_COMPONENTS;
    private static boolean shouldWarnNoComponents = true;
    static {
        boolean hasComponents = false;
        try {
            ItemMeta.class.getMethod("getAsComponentString");
            hasComponents = true;
        } catch (NoSuchMethodException ignored) {}
        HAS_COMPONENTS = hasComponents;
    }

    public static String getNbt(ItemStackField.ItemStackWrapper itemStackWrapper) {
        if (HAS_COMPONENTS) {
            return itemStackWrapper.meta().getAsComponentString();
        } else {
            return itemStackWrapper.meta().getAsString();
        }
    }

    public static void setMaxStackSize(ItemStackField.ItemStackWrapper itemStackWrapper, int maxStackSize) {
        if (HAS_COMPONENTS) {
            itemStackWrapper.meta().setMaxStackSize(maxStackSize);
        } else if (shouldWarnNoComponents) {
            shouldWarnNoComponents = false;
            BalancedVillagerTrades.LOGGER.warning("max-stack-size is not available.");
        }
    }

    public static int getMaxDamage(ItemStackField.ItemStackWrapper itemStackWrapper) {
        if (HAS_COMPONENTS && ((Damageable) itemStackWrapper.meta()).hasMaxDamage()) {
            Damageable damageable = (Damageable) itemStackWrapper.meta();
            return damageable.hasMaxDamage() ? damageable.getMaxDamage() : 0;
        } else {
            return 0;
        }
    }

    public static void setMaxDamage(ItemStackField.ItemStackWrapper itemStackWrapper, int maxDamage) {
        if (HAS_COMPONENTS) {
            ((Damageable) itemStackWrapper.meta()).setMaxDamage(maxDamage);
        } else if (shouldWarnNoComponents) {
            shouldWarnNoComponents = false;
            BalancedVillagerTrades.LOGGER.warning("max-damage is not available.");
        }
    }
}
