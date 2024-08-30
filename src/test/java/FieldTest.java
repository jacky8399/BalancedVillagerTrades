import be.seeseemelk.mockbukkit.MockBukkit;
import com.google.common.collect.ImmutableMap;
import com.jacky8399.balancedvillagertrades.BalancedVillagerTrades;
import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.fields.LuaProxy;
import com.jacky8399.balancedvillagertrades.fields.NamespacedKeyField;
import com.jacky8399.balancedvillagertrades.fields.item.EnchantmentsField;
import com.jacky8399.balancedvillagertrades.fields.item.ItemLoreField;
import com.jacky8399.balancedvillagertrades.fields.item.ItemStackField;
import com.jacky8399.balancedvillagertrades.utils.lua.ScriptRunner;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.DangerousMocks;
import utils.FieldUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class FieldTest {
    @BeforeAll
    public static void registerFakeEnchants() {
//        BalancedVillagerTrades.LOGGER = Logger.getLogger("BVT");
    }
    static BalancedVillagerTrades plugin;

    @BeforeAll
    public static void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(BalancedVillagerTrades.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    Map<String, Class<?>> fields = Map.of(
            "ingredient-0", ItemStackField.ItemStackWrapper.class,
            "ingredient-1.type", NamespacedKey.class,
            "result.enchantments.MENDING", Integer.class,
            "result.enchantments.minecraft:mending", Integer.class,
            "result.enchantments.size", Integer.class,
            "villager.world.environment", String.class
    );

    @Test
    public void testFields() {
        fields.forEach((fieldName, clazz) -> {
            Field<?, ?> field = Fields.findField(null, fieldName, true);
            assertEquals(clazz, field.getFieldClass());
        });
    }

    /**
     * @param field    Field to parse predicate with
     * @param test     Object to test against
     * @param expected The expected result
     * @param inputs   Predicate parse inputs
     */
    record PredicateTest<T>(Field<?, T> field, T test, boolean expected, String... inputs) {
    }

    List<PredicateTest<?>> predicateTests = List.of(
            new PredicateTest<>(Field.readOnlyField(Boolean.class, null), true, true, "true"),
            new PredicateTest<>(Field.readOnlyField(Integer.class, null), 13, true,
                    "> 12", "= 13", "13", "!= 5", "<= 13", "between 5 and 13", "in 5-13", "in 5..13"),
            new PredicateTest<>(Field.readOnlyField(Integer.class, null), 13, false,
                    "< 12", "!= 13", "<> 13", "between 5 and 6", "in 0-0"),
            new PredicateTest<>(Field.readOnlyField(String.class, null), "minecraft:tnt", true,
                    "minecraft:tnt", "MiNeCrAfT:TnT",
                    "= minecraft:tnt", "= \"minecraft:tnt\"i",
                    "contains tnt", "contains TNT", "contains \"TNT\"i",
                    "matches ^minecraft:.+", "matches \"minecraft:tnt\"", "matches \"^MINECRAFT\"i"),
            new PredicateTest<>(Field.readOnlyField(String.class, null), "minecraft:tnt", false,
                    "= \"MINECRAFT:TNT\"", "contains \"TNT\"", "matches \"^MINECRAFT\""),
            new PredicateTest<>(new NamespacedKeyField<>(null, null),
                    new NamespacedKey("minecraft", "value"), true,
                    "= value", "= VALUE", "= minecraft:value", "value", "VALUE", "minecraft:value", "contains value"),
            new PredicateTest<>(new NamespacedKeyField<>(null, null),
                    new NamespacedKey("minecraft", "value"), false,
                    "contains something_else", "matches ^example:value$")
            // can't test other fields as they depend on Bukkit
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testPredicate() {
        for (var outcome : predicateTests) {
            var field = outcome.field();
            for (var input : outcome.inputs()) {
                var predicate = (BiPredicate) field.parsePredicate(input);
                assertEquals(outcome.expected, predicate.test(null, outcome.test),
                        () -> "Clazz: %s, testing \"%s\" against %s"
                                .formatted(field.getFieldClass().getSimpleName(), input, outcome.test.toString())
                );
            }
        }
    }

    record TransformerTest<T>(Field<?, T> field, T input, Map<String, T> transformations) {
    }

    List<TransformerTest<?>> transformerTests = List.of(
            new TransformerTest<>(Field.readOnlyField(Integer.class, null), 5, Map.of(
                    ">= 6", 6, "= 10", 10, "11", 11,
                    "+= 15", 20, "/= 3", 1, "%= 3", 2,
                    "between 6 and 7", 6, "in 0..3", 3
            ))
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testTransformer() {
        for (var outcome : transformerTests) {
            var field = outcome.field();
            var input = outcome.input();
            outcome.transformations().forEach((transformer, expected) -> {
                var transformerFunc = (BiFunction) field.parseTransformer(transformer);
                var output = transformerFunc.apply(null, input);
                assertEquals(expected, output,
                        () -> "Clazz: %s, transforming %s using %s"
                                .formatted(field.getFieldClass().getSimpleName(), input.toString(), transformer));
            });
        }
    }

    record EnchantmentPredicateTest(Map<Enchantment, Integer> enchantments, boolean expected, String... inputs) {
    }

    List<EnchantmentPredicateTest> enchantmentPredicateTests = List.of(
            // contains and conflicts
            new EnchantmentPredicateTest(Map.of(Enchantment.SILK_TOUCH, 1), true,
                    "contains silk_touch", "contains minecraft:silk_touch",
                    "conflicts with silk_touch", "conflicts with SILK_TOUCH",
                    "conflicts with fortune", "conflicts with minecraft:Fortune"),
            new EnchantmentPredicateTest(Map.of(Enchantment.SILK_TOUCH, 1), false,
                    "contains vanishing_curse", "contains MINEcraft:fortune",
                    "conflicts with mending", "conflicts with MENDING"),
            // categories
            new EnchantmentPredicateTest(Map.of(Enchantment.SILK_TOUCH, 1, Enchantment.FORTUNE, 1),
                    true, "all is non-treasure", "all is nontreasure", "all are not curses",
                    "all is n't curse", "all isn't curse", "all is nt curse", "all isnt curse",
                    "none is treasure", "none is curse", "some is not treasure", "any is not curse"),
            new EnchantmentPredicateTest(Map.of(Enchantment.MENDING, 1, Enchantment.VANISHING_CURSE, 1),
                    false, "all isn't treasure", "all is curse", "all is not treasure", "all is not curse",
                    "none is treasure", "none are curses")
    );

    @Test
    public void testEnchantmentField() {
        var enchantmentField = new EnchantmentsField(null);
        for (var test : enchantmentPredicateTests) {
            // create respective ItemMeta instances

            ItemMeta meta = DangerousMocks.mockEnchants(test.enchantments);
            var wrapper = new ItemStackField.ItemStackWrapper(null, meta);
            EnchantmentStorageMeta storageMeta = DangerousMocks.mockStoredEnchants(test.enchantments);
            var storageWrapper = new ItemStackField.ItemStackWrapper(null, storageMeta);

            for (String input : test.inputs()) {
                var predicate = enchantmentField.parsePredicate(input);

                assertEquals(test.expected(), predicate.test(null, meta), () ->
                        "Test \"" + input + "\" failed for enchantments: " + test.enchantments);
                assertEquals(test.expected(), predicate.test(null, meta), () ->
                        "Test \"" + input + "\" failed for stored enchantments: " + test.enchantments);
            }
        }

        // test Lua interop
        // maintain order with ImmutableMap
        var enchants = ImmutableMap.of(
                Enchantment.FORTUNE, 3,
                Enchantment.SILK_TOUCH, 1
        );
        ItemMeta meta = DangerousMocks.mockEnchants(enchants);
        var wrapper = new ItemStackField.ItemStackWrapper(null, meta);
        var stackField = ScriptRunner.wrapField(wrapper, new ItemStackField<>(Function.identity(), null));
        var script = """
                str = ""
                for k, v in item.enchantments.entries() do
                    str = str .. k .. "=" .. v .. "\\n"
                end
                return str
                """;
        assertEquals("minecraft:fortune=3\nminecraft:silk_touch=1\n", LuaTest.run(script,
                globals -> globals.set("item", stackField)
        ).checkjstring());
        // ensure that enchantments appear first in legacy next()
        var legacyScript = """
                k = next(item.enchantments)
                return k .. "=" .. item.enchantments[k]
                """;
        assertEquals("minecraft:fortune=3", LuaTest.run(legacyScript,
                globals -> globals.set("item", stackField)
        ).checkjstring());
    }

    @Test
    public void testLuaProxy() {
        assertInstanceOf(LuaProxy.class, Fields.findField(null, "result.enchantments", true).child);
    }

    @Test
    public void testLore() {
        List<String> lore = new ArrayList<>(List.of("aaa", "bbb", "ccc"));
        ItemMeta meta = DangerousMocks.mockLore(lore);
        var wrapper = new ItemStackField.ItemStackWrapper(null, meta);

        ItemLoreField field = new ItemLoreField();
        assertEquals("aaa\nbbb\nccc", field.getFieldWrapped("text").get(wrapper));
        for (int i = 1; i <= lore.size(); i++)
            assertEquals(lore.get(i - 1), field.getFieldWrapped(Integer.toString(i)).get(wrapper));

        FieldUtils.set(wrapper, field, "4", "ddd");
        FieldUtils.set(wrapper, field, "new-line", "ddd");
        assertIterableEquals(List.of("aaa", "bbb", "ccc", "ddd", "ddd"), lore);

        FieldUtils.set(wrapper, field, "7", "eee");
        assertIterableEquals(List.of("aaa", "bbb", "ccc", "ddd", "ddd", "", "eee"), lore);

        FieldUtils.set(wrapper, field, "7", "");
        assertIterableEquals(List.of("aaa", "bbb", "ccc", "ddd", "ddd"), lore);

    }

}
