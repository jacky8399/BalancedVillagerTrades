import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.FieldProxy;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.utils.TradeWrapper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TradeTest {

    static ServerMock server;
    static BalancedVillagerTrades plugin;

    @BeforeAll
    public static void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BalancedVillagerTrades.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    @SuppressWarnings("unchecked")
    private static void setField(Field<TradeWrapper, ?> field, TradeWrapper tradeWrapper, Object value) {
        ((Field<TradeWrapper, ? super Object>) field).set(tradeWrapper, value);
    }

    @Test
    public void testIngredientSplitting() {
        MerchantRecipe merchantRecipe = new MerchantRecipe(
                new ItemStack(Material.EMERALD, 1), 0, 16, true, 2,
                0.05f, 0, 0, false
        );
        merchantRecipe.setIngredients(List.of(new ItemStack(Material.STICK, 32)));

        TradeWrapper tradeWrapper = new TradeWrapper(null, merchantRecipe, 0, false);

        Map<String, Object> fields = Map.of(
                "ingredient-0.amount", 32,
                "ingredient-0.type", NamespacedKey.minecraft("stick"),
                "ingredient-1.type", NamespacedKey.minecraft("air")
        );
        fields.forEach((field, expected) ->
                assertEquals(expected, Fields.findField(null, field, true).get(tradeWrapper)));

        FieldProxy<TradeWrapper, ?, ?> amountField = Fields.findField(null, "ingredient-0.amount", true);
        setField(amountField, tradeWrapper, 30);
        assertEquals(30, merchantRecipe.getIngredients().get(0).getAmount());

        setField(amountField, tradeWrapper, 120);
        assertEquals(64, merchantRecipe.getIngredients().get(0).getAmount());
        assertEquals(56, merchantRecipe.getIngredients().get(1).getAmount());

        if (true) // https://github.com/MockBukkit/MockBukkit/issues/1111
            return;
        ItemStack stack = new ItemStack(Material.STICK, 65);
        ItemMeta meta = stack.getItemMeta();
        meta.setMaxStackSize(90);
        stack.setItemMeta(meta);
        assertEquals(90, stack.getMaxStackSize());
        merchantRecipe.setIngredients(List.of(stack));

        assertEquals(65, amountField.get(tradeWrapper));
        setField(amountField, tradeWrapper, 67);
        assertEquals(67, amountField.get(tradeWrapper));
        assertEquals(67, merchantRecipe.getIngredients().getFirst().getAmount());

        setField(amountField, tradeWrapper, 99);
        assertEquals(90, merchantRecipe.getIngredients().get(0).getAmount());
        assertEquals(9, merchantRecipe.getIngredients().get(1).getAmount());
    }
}
