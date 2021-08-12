package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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

    public <TInner> Field<TOwner, TInner> andThen(Field<TField, TInner> field) {
        if (field == null)
            return null;
        Function<TOwner, TInner> newGetter = getter.andThen(field.getter);
        BiConsumer<TOwner, TInner> newSetter = field.setter != null ? (owner, newVal) -> {
            TField instance = get(owner);
            field.set(instance, newVal);
            set(owner, instance);
        } : null;
        return field instanceof ComplexField ? new ComplexField<TOwner, TInner>(field.clazz, newGetter, newSetter) {
            @Override
            public @Nullable Field<TInner, ?> getField(String fieldName) {
                return ((ComplexField<TField, TInner>) field).getField(fieldName);
            }

            @Override
            public @Nullable Collection<String> getFields(@Nullable TOwner owner) {
                return ((ComplexField<TField, TInner>) field).getFields(owner != null ? Field.this.get(owner) : null);
            }
        } : new Field<>(field.clazz, newGetter, newSetter);
    }

}
