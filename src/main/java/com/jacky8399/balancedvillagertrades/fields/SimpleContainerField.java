package com.jacky8399.balancedvillagertrades.fields;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SimpleContainerField<TOwner, TField> extends SimpleField<TOwner, TField> implements ContainerField<TOwner, TField> {

    private final Map<String, Field<TField, ?>> fields;

    public SimpleContainerField(Class<TField> clazz,
                                Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter,
                                @NotNull Map<String, Field<TField, ?>> fields) {
        super(clazz, getter, setter);
        this.fields = Map.copyOf(fields);
    }

    @Override
    public TField get(TOwner tOwner) {
        return getter.apply(tOwner);
    }

    @Override
    public @Nullable Field<TField, ?> getField(String fieldName) {
        return fields.get(fieldName);
    }

    @SuppressWarnings("unchecked")
    protected <T, C extends Field<TField, T>> C getFieldUnsafe(String fieldName) {
        return (C) fields.get(fieldName);
    }

    @Override
    public @Nullable Collection<String> getFields(@Nullable TOwner tOwner) {
        return fields.keySet();
    }

    @Override
    public String toString() {
        return "SimpleContainerField{type=" + clazz.getSimpleName() + ", readonly=" + isReadOnly() + "}";
    }
}
