package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class ComplexField<TOwner, TField> extends Field<TOwner, TField> {
    public ComplexField(Class<TField> clazz, Function<TOwner, TField> getter, BiConsumer<TOwner, TField> setter) {
        super(clazz, getter, setter);
    }

    @Nullable
    public abstract Field<TField, ?> getField(String fieldName);

    @Nullable
    public Collection<String> getFields(TOwner owner) {
        return null;
    }

    @Nullable
    public Field<TOwner, ?> getFieldWrapped(String fieldName) {
        Field<TField, ?> field = getField(fieldName);
        return field != null ? andThen(field) : null;
    }
}
