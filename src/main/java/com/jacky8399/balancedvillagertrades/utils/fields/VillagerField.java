package com.jacky8399.balancedvillagertrades.utils.fields;

import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class VillagerField extends ComplexField<TradeWrapper, Villager> {
    private final ImmutableMap<String, Field<Villager, ?>> FIELDS = ImmutableMap.<String, Field<Villager, ?>>builder()
            .put("type", Field.readOnlyField(String.class, villager -> villager.getVillagerType().name()))
            .put("profession", Field.readOnlyField(String.class, villager -> villager.getProfession().name()))
            .put("level", Field.readOnlyField(Integer.class, Villager::getVillagerLevel))
            .put("experience", Field.readOnlyField(Integer.class, Villager::getVillagerExperience))
            .put("recipe-count", Field.readOnlyField(Integer.class, Merchant::getRecipeCount))
            .put("inventory", new InventoryField())
            .build();

    public VillagerField() {
        super(Villager.class, TradeWrapper::getVillager, null);
    }

    @Override
    public @Nullable Field<Villager, ?> getField(String fieldName) {
        return FIELDS.get(fieldName);
    }

    @Override
    public @Nullable Collection<String> getFields(TradeWrapper tradeWrapper) {
        return FIELDS.keySet();
    }

    public static class InventoryField extends ComplexField<Villager, Inventory> {
        public InventoryField() {
            super(Inventory.class, Villager::getInventory, null);
        }

        @Override
        public @Nullable Field<Inventory, ?> getField(String fieldName) {
            if ("size".equals(fieldName)) {
                return Field.readOnlyField(Integer.class, Inventory::getSize);
            } else if ("empty".equals(fieldName)) {
                return Field.readOnlyField(Boolean.class, Inventory::isEmpty);
            }

            try {
                int slot = Integer.parseInt(fieldName);
                return new ItemStackField<>(inv -> {
                    ItemStack is = inv.getItem(slot);
                    return is != null ? is : new ItemStack(Material.AIR);
                }, (inv, is) -> inv.setItem(slot, is));
            } catch (NumberFormatException e) {
                Material material = Material.matchMaterial(fieldName);
                if (material == null) {
                    return null;
                }
                return new Field<>(Integer.class,
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
        public @Nullable Collection<String> getFields(Villager villager) {
            if (villager == null)
                return Arrays.asList("size", "empty");
            ItemStack[] items = villager.getInventory().getContents();
            return Stream.concat(
                    IntStream.range(0, items.length).mapToObj(Integer::toString),
                    Arrays.stream(items).map(ItemStack::getType).distinct().map(Material::name)
            ).collect(Collectors.toList());
        }
    }
}
