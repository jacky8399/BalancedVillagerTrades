package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/** Utility class to enable chained access while retaining property field implementation */
public class FieldAccessor<TOwner, T, TField> extends ComplexField<TOwner, TField> { // generic types are a mess but eh
    public final Field<TOwner, T> parent;
    public final Field<T, TField> child;
    public final String fieldName;
    public FieldAccessor(Field<TOwner, T> parent, Field<T, TField> field, @Nullable String fieldName) {
        super(field.clazz, null, field.setter != null ? ((tOwner, tField) -> {}) : null);
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
    public <TInner> FieldAccessor<TOwner, TField, TInner> andThen(Field<TField, TInner> field) {
        return new FieldAccessor<>(this, field, fieldName);
    }

    public boolean isComplex() {
        return child instanceof ComplexField;
    }

    @Override
    public @Nullable Collection<String> getFields(@Nullable TOwner tOwner) {
        if (child instanceof ComplexField) {
            T intermediate = parent.get(tOwner);
            return (((ComplexField<T, TField>) child).getFields(intermediate));
        }
        return null;
    }

    @Override
    public @Nullable Field<TField, ?> getField(String fieldName) {
        if (child instanceof ComplexField) {
            return ((ComplexField<T, TField>) child).getField(fieldName);
        }
        return null;
    }

    @Override
    public @Nullable FieldAccessor<TOwner, TField, ?> getFieldWrapped(String fieldName) {
        Field<TField, ?> field = getField(fieldName);
        if (field != null)
            return new FieldAccessor<>(this, field, (this.fieldName != null ? this.fieldName + "." : "") + fieldName);
        return null;
    }

    @Override
    public String toString() {
        return "FieldAccessor{parent=" + parent + ", child=" + child + ", fieldName=" + fieldName + "}";
    }


    public static <TOwner, TField> FieldAccessor<TOwner, TOwner, TField> emptyAccessor(Field<TOwner, TField> field) {
        return new FieldAccessor<TOwner, TOwner, TField>(null, field, null) {
            @Override
            public TField get(TOwner o) {
                return field.get(o);
            }

            @Override
            public void set(TOwner o, TField value) {
                field.set(o, value);
            }

            @Override
            public String toString() {
                return "EmptyFieldAccessor{field=" + field + "}";
            }

            @Override
            public <TInner> FieldAccessor<TOwner, TField, TInner> andThen(Field<TField, TInner> field1) {
                return field.andThen(field1);
            }

            @Override
            public @Nullable FieldAccessor<TOwner, TField, ?> getFieldWrapped(String fieldName) {
                return field instanceof ComplexField ? ((ComplexField<TOwner, TField>) field).getFieldWrapped(fieldName) : null;
            }
        };
    }
}
