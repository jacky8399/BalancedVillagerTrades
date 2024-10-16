package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.fields.item.ItemStackField;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VillagerField extends SimpleContainerField<TradeWrapper, AbstractVillager> {

    private static final InventoryField INVENTORY_FIELD = new InventoryField();

    public static final Field<AbstractVillager, World> WORLD_FIELD = new WorldField();

    private static final Field<AbstractVillager, Villager> REQUIRE_VILLAGER =
            Field.readOnlyField(Villager.class, abstractVillager -> abstractVillager instanceof Villager villager ? villager : null);
    private static final Map<String, Field<AbstractVillager, ?>> FIELDS = Map.of(
            "recipe-count", Field.readOnlyField(Integer.class, Merchant::getRecipeCount),
            "inventory", INVENTORY_FIELD,
            "world", WORLD_FIELD,
            // the following fields are only available on villagers
            "type", REQUIRE_VILLAGER.andThen(new NamespacedKeyField<>(
                    villager -> villager != null ? villager.getVillagerType().getKey() : null, null)),
            "profession", REQUIRE_VILLAGER.andThen(new NamespacedKeyField<>(
                    villager -> villager != null ? villager.getProfession().getKey() : null, null)),
            "level", REQUIRE_VILLAGER.andThen(Field.readOnlyField(Integer.class,
                    villager -> villager != null ? villager.getVillagerLevel() : 0)),
            "experience", REQUIRE_VILLAGER.andThen(Field.readOnlyField(Integer.class,
                    villager -> villager != null ? villager.getVillagerExperience() : 0))
    );

    private VillagerField() {
        super(AbstractVillager.class, TradeWrapper::getVillager, null, FIELDS);
    }

    public static VillagerField INSTANCE = new VillagerField();

    private static final Pattern LEGACY_VILLAGER_SYNTAX = Pattern.compile("^(profession|type)\\s*(=|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public @NotNull BiPredicate<TradeWrapper, AbstractVillager> parsePredicate(@NotNull String input) throws IllegalArgumentException {
        Matcher matcher = LEGACY_VILLAGER_SYNTAX.matcher(input);
        if (matcher.matches()) {
            Config.addWarning("Using 'villager: profession/type ...' to target villagers is deprecated.");
            String fieldName = matcher.group(1);
            Field<AbstractVillager, String> field = getFieldUnsafe(fieldName);
            var predicate = field.parsePredicate(matcher.group(2) + matcher.group(3));
            return (tradeWrapper, value) -> {
                var intermediate = field.get(value);
                return predicate.test(tradeWrapper, intermediate);
            };
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, AbstractVillager, AbstractVillager> parseTransformer(@Nullable String input) throws IllegalArgumentException {
        return super.parseTransformer(input);
    }

    public static class InventoryField extends SimpleField<AbstractVillager, Inventory> implements ContainerField<AbstractVillager, Inventory> {
        public InventoryField() {
            super(Inventory.class, AbstractVillager::getInventory, null);
        }

        @Override
        public @Nullable SimpleField<Inventory, ?> getField(String fieldName) {
            if ("size".equals(fieldName)) {
                return Field.readOnlyField(Integer.class, Inventory::getSize);
            } else if ("empty".equals(fieldName)) {
                return Field.readOnlyField(Boolean.class, Inventory::isEmpty);
            }

            try {
                int slot = Integer.parseInt(fieldName);
                return ItemStackField.create(inv -> {
                    ItemStack is = inv.getItem(slot);
                    return is != null ? is : new ItemStack(Material.AIR);
                }, (inv, is) -> inv.setItem(slot, is));
            } catch (NumberFormatException e) {
                Material material = Material.matchMaterial(fieldName);
                if (material == null) {
                    return null;
                }
                return new SimpleField<>(Integer.class,
                        inv -> Arrays.stream(inv.getContents())
                                .filter(Objects::nonNull)
                                .filter(is -> is.getType() == material)
                                .mapToInt(ItemStack::getAmount)
                                .sum(),
                        (inv, newAmount) -> {
                            int amountRemaining = newAmount;
                            for (ItemStack stack : inv) {
                                int amount = stack.getAmount();
                                if (amountRemaining >= amount) {
                                    amountRemaining -= amount;
                                } else {
                                    stack.setAmount(amountRemaining);
                                    amountRemaining = 0;
                                    break;
                                }
                            }

                            if (amountRemaining != 0) { // try to add new ItemStacks
                                int maxSize = material.getMaxStackSize();
                                ItemStack[] newItems = new ItemStack[(int) Math.ceil((double) amountRemaining / maxSize)];
                                for (int i = 0; i < newItems.length; i++) {
                                    newItems[i] = new ItemStack(material, Math.min(amountRemaining - i * maxSize, maxSize));
                                }
                                inv.addItem(newItems);
                            }
                        });
            }
        }

        @Override
        public @Nullable Collection<String> getFields(AbstractVillager villager) {
            if (villager == null)
                return Arrays.asList("size", "empty");
            List<String> fields = new ArrayList<>();
            fields.add("size");
            fields.add("empty");
            // slot accessors
            ItemStack[] items = villager.getInventory().getContents();
            for (int i = 0; i < items.length; i++) {
                fields.add(Integer.toString(i));
            }
            // item accessors
            Set<Material> seen = new HashSet<>();
            for (ItemStack item : items) {
                if (item != null && seen.add(item.getType())) {
                    fields.add(item.getType().getKey().toString());
                }
            }
            return fields;
        }
    }
}
