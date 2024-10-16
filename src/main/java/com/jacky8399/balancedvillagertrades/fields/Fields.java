package com.jacky8399.balancedvillagertrades.fields;

import com.google.common.collect.Maps;
import com.jacky8399.balancedvillagertrades.fields.item.ItemStackField;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
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
            entry("ingredient-0", ItemStackField.create(
                    trade -> getIngredient(0, trade),
                    (trade, stack) -> setIngredient(0, trade, stack))),
            entry("ingredient-1", ItemStackField.create(
                    trade -> getIngredient(1, trade),
                    (trade, stack) -> setIngredient(1, trade, stack))),
            entry("result", ItemStackField.create(trade -> trade.getRecipe().getResult(), Fields::setResult)),
            entry("villager", VillagerField.INSTANCE),
            entry("world", VillagerField.INSTANCE.andThen(VillagerField.WORLD_FIELD)),
            entry("index", Field.readOnlyField(Integer.class, TradeWrapper::getIndex)),
            entry("is-new", Field.readOnlyField(Boolean.class, TradeWrapper::isNewRecipe)),
            entry("merchant-type", new NamespacedKeyField<>(
                    tradeWrapper -> tradeWrapper.getVillager().getType().getKey(), null
            ))
    );

    public static final ContainerField<TradeWrapper, TradeWrapper> ROOT_FIELD =
            new SimpleContainerField<>(TradeWrapper.class, Function.identity(), null, FIELDS) {
                @Override
                public String toString() {
                    return "trade";
                }
            };
    public static final ItemStack EMPTY_STACK = new ItemStack(Material.AIR, 0);

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

    private static boolean shouldCombineIngredients(List<ItemStack> ingredients) {
        @SuppressWarnings("SizeReplaceableByIsEmpty")
        ItemStack slot0 = ingredients.size() > 0 ? ingredients.get(0) : EMPTY_STACK;
        ItemStack slot1 = ingredients.size() > 1 ? ingredients.get(1) : EMPTY_STACK;
        return slot0.isSimilar(slot1) ||
                (slot1.getType() == Material.AIR || slot1.getAmount() == 0);
    }

    private static ItemStack getIngredient(int index, TradeWrapper trade) {
        List<ItemStack> ingredients = trade.getRecipe().getIngredients();
        if (index < ingredients.size()) {
            if (shouldCombineIngredients(ingredients)) {
                // collapse both stacks into 1
                if (index == 0 && !ingredients.isEmpty()) {
                    ItemStack clone = ingredients.get(0);
                    if (ingredients.size() > 1) {
                        clone.setAmount(clone.getAmount() + ingredients.get(1).getAmount());
                    }
                    return clone;
                } else {
                    return EMPTY_STACK;
                }
            }
            return ingredients.get(index);
        }
        return EMPTY_STACK;
    }

    private static void setIngredient(int index, TradeWrapper trade, ItemStack stack) {
        Objects.requireNonNull(stack);
        // ensure that the list always has 2 elements
        @NotNull ItemStack[] stacks = {EMPTY_STACK, EMPTY_STACK};
        var ingredients = trade.getRecipe().getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            stacks[i] = ingredients.get(i);
        }
        if (index == 0) {
            int amount = stack.getAmount();
            ItemStack slot0 = stack.clone();
            int maxStackSize = slot0.getMaxStackSize();
            int remaining = Math.max(0, Math.min(amount - maxStackSize, maxStackSize));
            slot0.setAmount(Math.min(amount, maxStackSize));
            stacks[0] = slot0;
            if (shouldCombineIngredients(ingredients) || stack.isSimilar(stacks[1])) {
                ItemStack slot1 = stack.clone();
                slot1.setAmount(remaining);
                stacks[1] = slot1;
            }
        } else {
            stacks[index] = stack;
        }
        // what
        List<ItemStack> actualList = new ArrayList<>(2);
        if (stacks[0].getType() != Material.AIR && stacks[0].getAmount() != 0)
            actualList.add(stacks[0]);
        if (stacks[1].getType() != Material.AIR && stacks[1].getAmount() != 0)
            actualList.add(stacks[1]);
        trade.getRecipe().setIngredients(actualList);
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
