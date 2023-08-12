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
    public static final SimpleField<List<String>, String> TEXT_FIELD = new SimpleField<>(String.class,
            list -> String.join("\n", list),
            (list, newText) -> {
                list.clear();
                list.addAll(Arrays.asList(newText.split("\n")));
            }
    );

    @Override
    public @Nullable Field<List<String>, ?> getField(String fieldName) {
        switch (fieldName) {
            case "new-line" -> {
                return NEW_LINE_FIELD;
            }
            case "size" -> {
                return SIZE_FIELD;
            }
            case "text" -> {
                return TEXT_FIELD;
            }
        }

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
                            for (int i = list.size(); i < lineNo; i++) {
                                list.add("");
                            }
                            list.add(newLine);
                        }
                    });
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final List<String> DEFAULT_FIELDS = List.of("new-line", "size", "text");
    @Override
    public @Nullable Collection<String> getFields(ItemStackField.@Nullable ItemStackWrapper itemStackWrapper) {
        List<String> lore;
        if (itemStackWrapper == null || (lore = itemStackWrapper.meta().getLore()) == null || lore.isEmpty())
            return DEFAULT_FIELDS;

        var fields = new ArrayList<String>(DEFAULT_FIELDS.size() + lore.size());
        fields.addAll(DEFAULT_FIELDS);
        for (int i = 1; i <= lore.size(); i++) {
            fields.add(Integer.toString(i));
        }
        return fields;
    }

    @Override
    public List<String> get(ItemStackField.ItemStackWrapper itemStackWrapper) {
        var lore = itemStackWrapper.meta().getLore();
        return lore != null ? new ArrayList<>(lore) : new ArrayList<>(0);
    }

    @Override
    public void set(ItemStackField.ItemStackWrapper itemStackWrapper, List<String> value) {
        var meta = itemStackWrapper.meta();
        // remove trailing empty lines
        int end = value.size();
        for (; end > 0 && value.get(end - 1).isEmpty(); end--) {}

        meta.setLore(value.subList(0, end));
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
    public <TOwner> boolean setLuaValue(Field<TOwner, List<String>> field, TOwner parent, LuaValue value) {
        if (!value.isstring())
            throw new LuaError("Cannot set item lore to " + value.typename());
        List<String> string = new ArrayList<>(Arrays.asList(value.tojstring().split("\n")));
        field.set(parent, string);
        return true;
    }
}
