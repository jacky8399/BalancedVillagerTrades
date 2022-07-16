package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class VillagerField extends SimpleContainerField<TradeWrapper, Villager> {
    private static <T> Field<Villager, T> field(Class<T> clazz, Function<Villager, T> function) {
        return Field.readOnlyField(clazz, function);
    }

    private static final InventoryField INVENTORY_FIELD = new InventoryField();
    private static final Field<Villager, World> WORLD_FIELD = ContainerField.withFields(World.class, Villager::getWorld, null, Map.of(
            "name", Field.readOnlyField(String.class, World::getName),
            "environment", Field.readOnlyField(String.class, world -> world.getEnvironment().name()),
            "time", Field.readOnlyField(Integer.class, world -> (int) world.getTime()),
            "full-time", Field.readOnlyField(Integer.class, world -> (int) world.getFullTime()),
            "is-day-time", Field.readOnlyField(Boolean.class, World::isDayTime),
            "weather", Field.readOnlyField(String.class,
                    world -> world.isThundering() ? "thunder" : world.hasStorm() ? "rain" : "clear"))
    );

    private static final Map<String, Field<Villager, ?>> FIELDS = Map.of(
            "type", field(String.class, villager -> villager.getVillagerType().name()),
            "profession", field(String.class, villager -> villager.getProfession().name()),
            "level", field(Integer.class, Villager::getVillagerLevel),
            "experience", field(Integer.class, Villager::getVillagerExperience),
            "recipe-count", field(Integer.class, Merchant::getRecipeCount),
            "inventory", INVENTORY_FIELD,
            "world", WORLD_FIELD
    );

    public VillagerField() {
        super(Villager.class, TradeWrapper::getVillager, null, FIELDS);
    }

    private static boolean warnOldVillagerSyntax = true;

    private static final Pattern LEGACY_VILLAGER_SYNTAX = Pattern.compile("^(profession|type)\\s*(=|matches)\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public @NotNull BiPredicate<TradeWrapper, Villager> parsePredicate(@NotNull String input) throws IllegalArgumentException {
        Matcher matcher = LEGACY_VILLAGER_SYNTAX.matcher(input);
        if (matcher.matches()) {
            if (warnOldVillagerSyntax) {
                warnOldVillagerSyntax = false;
                BalancedVillagerTrades.LOGGER.warning("Using 'villager: profession/type ...' to target villagers is obsolete.");
                BalancedVillagerTrades.LOGGER.warning("See https://github.com/jacky8399/BalancedVillagerTrades/wiki/Fields#villager-properties " +
                        "for a better way to target specific villagers.");
            }
            String fieldName = matcher.group(1);
            Field<Villager, String> field = getFieldUnsafe(fieldName);
            var predicate = field.parsePredicate(matcher.group(2) + matcher.group(3));
            return (tradeWrapper, value) -> {
                var intermediate = field.get(value);
                return predicate.test(tradeWrapper, intermediate);
            };
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, Villager, Villager> parseTransformer(@Nullable String input) throws IllegalArgumentException {
        return super.parseTransformer(input);
    }

    public static class InventoryField extends SimpleField<Villager, Inventory> implements ContainerField<Villager, Inventory> {
        public InventoryField() {
            super(Inventory.class, Villager::getInventory, null);
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
                return new ItemStackField<>(inv -> {
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
