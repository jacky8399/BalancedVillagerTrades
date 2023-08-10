package com.jacky8399.balancedvillagertrades.fields.item;

import com.jacky8399.balancedvillagertrades.fields.ContainerField;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.LuaProxy;
import com.jacky8399.balancedvillagertrades.fields.SimpleField;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ItemLoreField implements ContainerField<ItemStackField.ItemStackWrapper, List<String>>, LuaProxy<List<String>> {

    public static final SimpleField<List<String>, String> NEW_LINE_FIELD = new SimpleField<>(String.class, ignored -> "", List::add);
    public static final SimpleField<List<String>, Integer> SIZE_FIELD = Field.readOnlyField(Integer.class, List::size);

    @Override
    public @Nullable Field<List<String>, ?> getField(String fieldName) {
        if ("new-line".equals(fieldName))
            return NEW_LINE_FIELD; // new lines are always empty
        else if ("size".equals(fieldName))
            return SIZE_FIELD;

        int lineNo;
        try {
            lineNo = Integer.parseInt(fieldName) - 1;
            if (lineNo < 0)
                return null;
            return new SimpleField<>(String.class,
                    list -> lineNo < list.size() ? list.get(lineNo) : "",
                    (list, newLine) -> {
                        if (lineNo < list.size()) {
                            list.set(lineNo, newLine);
                        } else {
                            for (int i = list.size(), end = lineNo - 1; i < end; i++) {
                                list.add("");
                            }
                            list.add(newLine);
                        }
                    });
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public @Nullable Collection<String> getFields(ItemStackField.@Nullable ItemStackWrapper itemStackWrapper) {
        List<String> lore;
        if (itemStackWrapper == null || (lore = itemStackWrapper.meta().getLore()) == null || lore.isEmpty())
            return List.of("new-line", "size");

        var names = new ArrayList<String>(2 + lore.size());
        names.add("new-line");
        names.add("size");
        for (int i = 1; i <= lore.size(); i++) {
            names.add(Integer.toString(i));
        }
        return names;
    }

    @Override
    public List<String> get(ItemStackField.ItemStackWrapper itemStackWrapper) {
        var lore = itemStackWrapper.meta().getLore();
        return lore != null ? new ArrayList<>(lore) : new ArrayList<>(0);
    }

    @Override
    public void set(ItemStackField.ItemStackWrapper itemStackWrapper, List<String> value) {
        var meta = itemStackWrapper.meta();
        meta.setLore(value);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Class<List<String>> getFieldClass() {
        return (Class) List.class;
    }

    // replicate string functionality in YAML
    @Override
    public @NotNull BiPredicate<TradeWrapper, List<String>> parsePredicate(@NotNull String input) {
        var stringPredicate = SimpleField.parsePrimitivePredicate(String.class, input);
        return (t, list) -> {
            String lore = String.join("\n", list);
            return stringPredicate.test(t, lore);
        };
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, List<String>, List<String>> parseTransformer(@Nullable String input) {
        var stringTransformer = SimpleField.parsePrimitiveTransformer(String.class, input);
        return (t, list) -> {
            String lore = String.join("\n", list);
            String newLore = stringTransformer.apply(t, lore);
            if (!newLore.equals(lore)) {
                return new ArrayList<>(Arrays.asList(newLore.split("\n")));
            } else {
                return list;
            }
        };
    }

    @Override
    public @Nullable LuaValue getLuaValue(List<String> instance) {
        return LuaValue.valueOf(String.join("\n", instance));
    }

    @Override
    public <TOwner> boolean setLuaValue(Field<TOwner, List<String>> field, TOwner parent, LuaValue value) {
        if (!value.isstring())
            throw new LuaError("Cannot set item lore to " + value.typename());
        List<String> string = new ArrayList<>(Arrays.asList(value.tojstring().split("\n")));
        field.set(parent, string);
        return true;
    }
}
