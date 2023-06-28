package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/** Utility class to enable chained access while retaining property field implementation */
public class FieldProxy<TOwner, T, TField> implements ContainerField<TOwner, TField> { // generic types are a mess but eh
    public final Field<TOwner, T> parent;
    public final Field<T, TField> child;
    public final String fieldName;
    public FieldProxy(Field<TOwner, T> parent, Field<T, TField> field, @Nullable String fieldName) {
        this.parent = parent;
        this.child = field;
        this.fieldName = fieldName;
    }

    public boolean isComplex() {
        return isComplex(child);
    }

    public static boolean isComplex(Field<?, ?> field) {
        return field instanceof ContainerField &&
                !(field instanceof FieldProxy<?,?,?> childProxy && !childProxy.isComplex()); // check for complex chains

    }

    @Nullable
    public String getSimpleName() {
        if (fieldName == null)
            return null;
        int lastDot = fieldName.lastIndexOf('.');
        if (lastDot == -1)
            return fieldName;
        return fieldName.substring(lastDot + 1);
    }

    @Override
    public TField get(TOwner tOwner) {
        T intermediate = parent.get(tOwner);
        return child.get(intermediate);
    }

    @Override
    public void set(TOwner tOwner, TField value) {
        T intermediate = parent.get(tOwner);
        child.set(intermediate, value);
        parent.set(tOwner, intermediate);
    }

    @Override
    public <TInner> FieldProxy<TOwner, TField, TInner> andThen(Field<TField, TInner> field) {
        return new FieldProxy<>(this, field, fieldName);
    }

    @Override
    public boolean isReadOnly() {
        return child.isReadOnly();
    }

    @Override
    public Class<TField> getFieldClass() {
        return child.getFieldClass();
    }

    @Override
    public @Nullable Collection<String> getFields(@Nullable TOwner tOwner) {
        if (child instanceof ContainerField) {
            if (tOwner == null)
                return ((ContainerField<T, TField>) child).getFields(null);

            T intermediate = parent.get(tOwner);
            return (((ContainerField<T, TField>) child).getFields(intermediate));
        }
        return null;
    }

    @Override
    public @Nullable Field<TField, ?> getField(String fieldName) {
        if (child instanceof ContainerField) {
            return ((ContainerField<T, TField>) child).getField(fieldName);
        }
        return null;
    }

    @Override
    public @Nullable FieldProxy<TOwner, TField, ?> getFieldWrapped(String fieldName) {
        Field<TField, ?> field = getField(fieldName);
        if (field != null)
            return new FieldProxy<>(this, field, (this.fieldName != null ? this.fieldName + "." : "") + fieldName);
        return null;
    }

    @Override
    public @NotNull BiPredicate<TradeWrapper, TField> parsePredicate(@NotNull String input) {
        return child.parsePredicate(input);
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, TField, TField> parseTransformer(@Nullable String input) {
        return child.parseTransformer(input);
    }

    protected String formatField() {
        return getSimpleName() + " (" + child.toString() + ")";
    }

    @Override
    public String toString() {
        // print the entire access chain along with their types
        Deque<String> parents = new ArrayDeque<>();
        parents.addFirst(formatField());
        Field<?, ?> parent = this.parent;
        while (parent instanceof FieldProxy parentProxy) {
            parents.addFirst(parentProxy.formatField());
            parent = parentProxy.parent;
        }
        if (parent != null)
            parents.addFirst(parent.toString());

        String chain = String.join(" -> ", parents);
        return "FieldProxy{" + chain + "}";
    }


    public static <TOwner, TField> FieldProxy<TOwner, TOwner, TField> emptyAccessor(Field<TOwner, TField> field) {
        return new FieldProxy<>(null, field, null) {
            @Override
            public TField get(TOwner o) {
                return field.get(o);
            }

            @Override
            public void set(TOwner o, TField value) {
                field.set(o, value);
            }

            @Override
            public boolean isReadOnly() {
                return field.isReadOnly();
            }

            @Override
            public String toString() {
                return "EmptyFieldProxy{field=" + field + "}";
            }

            @Override
            public <TInner> FieldProxy<TOwner, TField, TInner> andThen(Field<TField, TInner> field1) {
                return new FieldProxy<>(field, field1, null);
            }

            @SuppressWarnings("unchecked")
            @Override
            public @Nullable FieldProxy<TOwner, TField, ?> getFieldWrapped(String fieldName) {
                return field instanceof ContainerField complex ? complex.getFieldWrapped(fieldName) : null;
            }
        };
    }
}
