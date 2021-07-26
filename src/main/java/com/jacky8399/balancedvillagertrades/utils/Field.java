package com.jacky8399.balancedvillagertrades.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Field<TOwner, TField> {
    public final Class<TField> clazz;
    public final Function<TOwner, TField> getter;
    public final BiConsumer<TOwner, TField> setter;

    public Field(Class<TField> clazz, Function<TOwner, TField> getter, BiConsumer<TOwner, TField> setter) {
        this.clazz = clazz;
        this.getter = getter;
        this.setter = setter;
    }

    public TField get(TOwner owner) {
        return this.getter.apply(owner);
    }

    public void set(TOwner owner, TField value) {
        this.setter.accept(owner, value);
    }

    @Contract("!null -> !null")
    @Nullable
    public <TInner> Field<TOwner, TInner> andThen(Field<TField, TInner> field) {
        if (field == null)
            return null;
        Function<TOwner, TInner> newGetter = getter.andThen(field.getter);
        BiConsumer<TOwner, TInner> newSetter = (owner, newVal) -> {
            TField instance = get(owner);
            field.set(instance, newVal);
            set(owner, instance);
        };
        return field instanceof ComplexField ? new ComplexField<TOwner, TInner>(field.clazz, newGetter, newSetter) {
            @Override
            public @Nullable Field<TInner, ?> getField(String fieldName) {
                return ((ComplexField<TField, TInner>) field).getField(fieldName);
            }
        } : new Field<>(field.clazz, newGetter, newSetter);
    }

}
