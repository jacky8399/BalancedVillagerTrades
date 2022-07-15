package com.jacky8399.balancedvillagertrades.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ContainerField<TOwner, TField> extends Field<TOwner, TField> {

    @Nullable
    Field<TField, ?> getField(String fieldName);

    @Nullable
    default Collection<String> getFields(@Nullable TOwner owner) {
        return null;
    }

    @Nullable
    default FieldProxy<TOwner, TField, ?> getFieldWrapped(String fieldName) {
        Field<TField, ?> field = getField(fieldName);
        return field != null ? new FieldProxy<>(this, field, fieldName) : null;
    }

    static <TOwner, TField> ContainerField<TOwner, TField> withFields(
            Class<TField> clazz,
            Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter,
            @NotNull Map<String, Field<TField, ?>> fields) {
        return new SimpleContainerField<>(clazz, getter, setter, fields);
    }
}
