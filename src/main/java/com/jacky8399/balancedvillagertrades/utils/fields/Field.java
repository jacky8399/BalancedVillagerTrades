package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Field<TOwner, TField> {
    public final Class<TField> clazz;
    public final Function<TOwner, TField> getter;
    @Nullable
    public final BiConsumer<TOwner, TField> setter;

    public Field(Class<TField> clazz, Function<TOwner, TField> getter, BiConsumer<TOwner, TField> setter) {
        this.clazz = clazz;
        this.getter = getter;
        this.setter = setter;
    }

    public static <TOwner, TField> Field<TOwner, TField> readOnlyField(Class<TField> clazz, Function<TOwner, TField> getter) {
        return new Field<>(clazz, getter, null);
    }

    public TField get(TOwner owner) {
        return getter.apply(owner);
    }

    public void set(TOwner owner, TField value) {
        if (setter != null)
            setter.accept(owner, value);
    }

    public <TInner> FieldAccessor<TOwner, TField, TInner> andThen(Field<TField, TInner> field) {
        if (field == null)
            return null;
        return new FieldAccessor<>(this, field, null);
    }

    @Override
    public String toString() {
        return "Field{type=" + clazz.getSimpleName() + "}";
    }
}
