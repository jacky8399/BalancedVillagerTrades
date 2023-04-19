package com.jacky8399.balancedvillagertrades.fields;

import com.google.common.collect.Maps;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class Fields {
    public static final Map<String, Field<TradeWrapper, ?>> FIELDS = Map.ofEntries(
            entry("apply-discounts", new SimpleField<>(Boolean.class,
                    trade -> trade.getRecipe().getPriceMultiplier() != 0,
                    (trade, bool) -> trade.getRecipe().setPriceMultiplier(bool ? 1 : 0))),
            entry("max-uses", new SimpleField<>(Integer.class,
                    trade -> trade.getRecipe().getMaxUses(),
                    (trade, maxUses) -> trade.getRecipe().setMaxUses(maxUses))),
            entry("uses", new SimpleField<>(Integer.class,
                    trade -> trade.getRecipe().getUses(),
                    (trade, maxUses) -> trade.getRecipe().setUses(maxUses))),
            entry("award-experience", new SimpleField<>(Boolean.class,
                    trade -> trade.getRecipe().hasExperienceReward(),
                    (trade, awardXP) -> trade.getRecipe().setExperienceReward(awardXP))),
            entry("villager-experience", new SimpleField<>(Integer.class,
                    trade -> trade.getRecipe().getVillagerExperience(),
                    (trade, villagerXP) -> trade.getRecipe().setVillagerExperience(villagerXP))),
            entry("ingredient-0", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(0),
                    (trade, stack) -> setIngredient(0, trade, stack))),
            entry("ingredient-1", new ItemStackField<>(
                    trade -> trade.getRecipe().getIngredients().get(1),
                    (trade, stack) -> setIngredient(1, trade, stack))),
            entry("result", new ItemStackField<>(trade -> trade.getRecipe().getResult(), Fields::setResult)),
            entry("villager", new VillagerField()),
            entry("index", Field.readOnlyField(Integer.class, TradeWrapper::getIndex)),
            entry("is-new", Field.readOnlyField(Boolean.class, TradeWrapper::isNewRecipe))
    );

    public static final ContainerField<TradeWrapper, TradeWrapper> ROOT_FIELD =
            new SimpleContainerField<>(TradeWrapper.class, Function.identity(), null, FIELDS) {
                @Override
                public String toString() {
                    return "trade";
                }
            };

    @NotNull
    public static FieldProxy<TradeWrapper, ?, ?> findField(@Nullable ContainerField<TradeWrapper, ?> root, String path, boolean recursive) {
        if (root == null)
            root = ROOT_FIELD;

        if (!recursive) {
            FieldProxy<TradeWrapper, ?, ?> field = root.getFieldWrapped(path);
            if (field == null)
                throw new IllegalArgumentException("Can't access " + path + " because it does not exist");
            return field;
        }
        String[] paths = path.split("\\.");
        FieldProxy<TradeWrapper, ?, ?> field = FieldProxy.emptyAccessor(root);
        StringBuilder pathName = new StringBuilder("root");
        for (String child : paths) {
            FieldProxy<TradeWrapper, ?, ?> parent = field;
            if (field.isComplex()) {
                field = field.getFieldWrapped(child);
            } else {
                throw new IllegalArgumentException("Can't access " + path + " because " + pathName + " does not have fields");
            }
            if (field == null) {
                throw new IllegalArgumentException(pathName + "(" + parent + ") does not have field " + child);
            }
            pathName.append('.').append(child);
        }
        if (field == ROOT_FIELD) {
            throw new IllegalArgumentException(pathName + " does not have field " + path);
        }
        return field;
    }

    @NotNull
    public static Map<String, ? extends Field<TradeWrapper, ?>> listFields(@Nullable ContainerField<TradeWrapper, ?> root, @Nullable String path, @Nullable TradeWrapper context) {
        if (root == null) {
            return FIELDS.entrySet().stream()
                    .flatMap(entry -> {
                        Field<TradeWrapper, ?> field = entry.getValue();
                        if (field instanceof ContainerField) {
                            // noinspection unchecked
                            return listFields((ContainerField<TradeWrapper, ?>) field, entry.getKey(), context)
                                    .entrySet().stream();
                        }
                        return Stream.of(Maps.immutableEntry(entry.getKey(), field));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        Collection<String> fields = root.getFields(context);
        if (fields != null) {
            return fields.stream()
                    .flatMap(key -> {
                        FieldProxy<TradeWrapper, ?, ?> field = root.getFieldWrapped(key);
                        String childPath = path + "." + key;
                        var self = Stream.of(Map.entry(childPath, field));
                        if (field != null && field.isComplex()) {
                            try {
                                return Stream.concat(self,
                                        listFields(field, path + "." + key, context)
                                                .entrySet().stream());
                            } catch (Exception ignored) {
                            }
                        }
                        return self;
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Collections.singletonMap(path, root);
    }

    private static void setIngredient(int index, TradeWrapper trade, @Nullable final ItemStack stack) {
        if (stack == null) {
            throw new NullPointerException();
        }
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

    private static void setResult(TradeWrapper trade, ItemStack stack) {
        MerchantRecipe oldRecipe = trade.getRecipe();
        MerchantRecipe newRecipe = new MerchantRecipe(Objects.requireNonNull(stack),
                oldRecipe.getUses(), oldRecipe.getMaxUses(),
                oldRecipe.hasExperienceReward(), oldRecipe.getVillagerExperience(),
                oldRecipe.getPriceMultiplier(), oldRecipe.getDemand(), oldRecipe.getSpecialPrice());
        newRecipe.setIngredients(oldRecipe.getIngredients());
        trade.setRecipe(newRecipe);
    }
}
