package com.jacky8399.balancedvillagertrades.fields;

import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

public interface Field<TOwner, TField> {

    TField get(TOwner owner);

    void set(TOwner owner, TField value);

    boolean isReadOnly();

    Class<TField> getFieldClass();

    @NotNull
    default BiPredicate<TradeWrapper, TField> parsePredicate(@NotNull String input) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    default BiFunction<TradeWrapper, TField, TField> parseTransformer(@Nullable String input) {
        throw new UnsupportedOperationException();
    }


    default <TInner> FieldProxy<TOwner, TField, TInner> andThen(Field<TField, TInner> field) {
        if (field == null)
            return null;
        return new FieldProxy<>(this, field, null);
    }

    static <TOwner, TField> Field<TOwner, TField> field(Class<TField> clazz, Function<TOwner, TField> getter, @Nullable BiConsumer<TOwner, TField> setter) {
        return new SimpleField<>(clazz, getter, setter);
    }

    static <TOwner, TField> SimpleField<TOwner, TField> readOnlyField(Class<TField> clazz, Function<TOwner, TField> getter) {
        return new SimpleField<>(clazz, getter, null);
    }
}
