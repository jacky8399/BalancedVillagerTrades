package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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

    public boolean isComplex() {
        return child instanceof ContainerField;
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
    public @NotNull BiPredicate<TradeWrapper, TField> parsePredicate(String input) {
        return child.parsePredicate(input);
    }

    @Override
    public @NotNull BiFunction<TradeWrapper, TField, TField> parseTransformer(String input) {
        return child.parseTransformer(input);
    }

    @Override
    public String toString() {
        return "FieldProxy{parent=" + parent + ", child=" + child + ", fieldName=" + fieldName + "}";
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