package com.jacky8399.balancedvillagertrades.fields;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

// TODO
public class EnchantmentsField implements ContainerField<ItemStack, ItemMeta> {
    @Override
    public @Nullable Field<ItemMeta, ?> getField(String fieldName) {
        return null;
    }

    @Override
    public ItemMeta get(ItemStack stack) {
        return stack.getItemMeta();
    }

    @Override
    public void set(ItemStack stack, ItemMeta value) {
        stack.setItemMeta(value);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public Class<ItemMeta> getFieldClass() {
        return ItemMeta.class;
    }

}
