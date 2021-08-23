package com.jacky8399.balancedvillagertrades.utils.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class ComplexField<TOwner, TField> extends Field<TOwner, TField> {
    public ComplexField(Class<TField> clazz, Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter) {
        super(clazz, getter, setter);
    }

    @Nullable
    public abstract Field<TField, ?> getField(String fieldName);

    @Nullable
    public Collection<String> getFields(@Nullable TOwner owner) {
        return null;
    }

    @Nullable
    public FieldAccessor<TOwner, TField, ?> getFieldWrapped(String fieldName) {
        Field<TField, ?> field = getField(fieldName);
        return field != null ? new FieldAccessor<>(this, field, fieldName) : null;
    }

    @Override
    public String toString() {
        return "ComplexField{type=" + clazz.getSimpleName() + "}";
    }

    public static <TOwner, TField> ComplexField<TOwner, TField> withFields(
            Class<TField> clazz, Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter,
            @NotNull Map<String, Field<TField, ?>> fields) {
        return new ComplexField<TOwner, TField>(clazz, getter, setter) {
            @Override
            public @Nullable Field<TField, ?> getField(String fieldName) {
                return fields.get(fieldName);
            }

            @Override
            public @Nullable Collection<String> getFields(@Nullable TOwner tOwner) {
                return fields.keySet();
            }
        };
    }
}
