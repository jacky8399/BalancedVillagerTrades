package com.jacky8399.balancedvillagertrades.utils.fields;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Fields {
    public static final ImmutableMap<String, Field<TradeWrapper, ?>> FIELDS = ImmutableMap.<String, Field<TradeWrapper, ?>>builder()
            .put("apply-discounts", new Field<>(Boolean.class,
                    trade -> trade.getRecipe().getPriceMultiplier() != 0,
                    (trade, bool) -> trade.getRecipe().setPriceMultiplier(bool ? 1 : 0)))
            .put("max-uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getMaxUses(),
                    (trade, maxUses) -> trade.getRecipe().setMaxUses(maxUses)))
            .put("uses", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getUses(),
                    (trade, maxUses) -> trade.getRecipe().setUses(maxUses)))
            .put("award-experience", new Field<>(Boolean.class,
                    trade -> trade.getRecipe().hasExperienceReward(),
                    (trade, awardXP) -> trade.getRecipe().setExperienceReward(awardXP)))
            .put("villager-experience", new Field<>(Integer.class,
                    trade -> trade.getRecipe().getVillagerExperience(),
                    (trade, villagerXP) -> trade.getRecipe().setVillagerExperience(villagerXP)))
            .put("ingredient-0", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(0),
                    (trade, stack) -> setIngredient(0, trade, stack)))
            .put("ingredient-1", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(1),
                    (trade, stack) -> setIngredient(1, trade, stack)))
            .put("result", new ItemStackField<>(
                    trade -> trade.getRecipe().getResult(),
                    (trade, stack) -> {
                        MerchantRecipe oldRecipe = trade.getRecipe();
                        MerchantRecipe newRecipe = new MerchantRecipe(stack,
                                oldRecipe.getUses(), oldRecipe.getMaxUses(),
                                oldRecipe.hasExperienceReward(), oldRecipe.getVillagerExperience(),
                                oldRecipe.getPriceMultiplier());
                        newRecipe.setIngredients(oldRecipe.getIngredients());
                        trade.setRecipe(newRecipe);
                    }))
            .put("villager", new VillagerField())
            .build();

    @SuppressWarnings("unchecked")
    @NotNull
    public static Field<TradeWrapper, ?> findField(@Nullable ComplexField<TradeWrapper, ?> root, String path, boolean recursive) {
        if (!recursive) {
            Field<TradeWrapper, ?> field = root != null ? root.getFieldWrapped(path) : FIELDS.get(path);
            if (field == null)
                throw new IllegalArgumentException("Can't access " + path + " because it does not exist");
            return field;
        }
        String[] paths = path.split("\\.");
        Field<TradeWrapper, ?> field = root;
        StringBuilder pathName = new StringBuilder("root");
        for (String child : paths) {
            if (field == null) {
                field = FIELDS.get(child);
            } else if (field instanceof ComplexField) {
                field = ((ComplexField<TradeWrapper, ?>) field).getFieldWrapped(child);
            } else {
                throw new IllegalArgumentException("Can't access " + path + " because " + pathName + " does not have fields");
            }
            if (field == null) {
                throw new IllegalArgumentException(pathName + " does not have field " + child);
            }
            pathName.append('.').append(child);
        }
        if (field == null) {
            throw new IllegalArgumentException(pathName + " does not have field " + path);
        }
        return field;
    }

    private static void setIngredient(int index, TradeWrapper trade, final ItemStack stack) {
        List<ItemStack> stacks = new ArrayList<>(trade.getRecipe().getIngredients());
        if (stack.getAmount() > stack.getMaxStackSize()) {
            ItemStack clone = stack.clone();
            if (index == 0) { // only split first ingredient
                int remainder = Math.min(clone.getAmount() - clone.getMaxStackSize(), clone.getMaxStackSize());
                clone.setAmount(clone.getMaxStackSize());
                ItemStack extra = stack.clone();
                extra.setAmount(remainder);
                stacks.set(0, clone);
                stacks.set(1, extra);
            } else {
                clone.setAmount(clone.getMaxStackSize());
                stacks.set(1, clone);
            }
        } else {
            stacks.set(index, stack);
        }
        trade.getRecipe().setIngredients(stacks);
    }
}
