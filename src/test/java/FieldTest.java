import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.fields.MapField;
import com.jacky8399.balancedvillagertrades.fields.NamespacedKeyField;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldTest {
    Map<String, Class<?>> fields = Map.of(
            "ingredient-0", ItemStack.class,
            "ingredient-1.type", NamespacedKey.class,
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
     * @param field Field to parse predicate with
     * @param test Object to test against
     * @param result The expected result
     * @param inputs Predicate parse inputs
     */
    record PredicateOutcome<T>(Field<?, T> field, T test, boolean result, String... inputs) {}

    Map<NamespacedKey, Object> testMap = Map.of(new NamespacedKey("minecraft","tnt"), new Object());
    List<PredicateOutcome<?>> predicateOutcomes = List.of(
            new PredicateOutcome<>(Field.readOnlyField(Boolean.class, null), true, true, "true"),
            new PredicateOutcome<>(Field.readOnlyField(Integer.class, null), 13, true,
                    "> 12", "= 13", "13", "!= 5", "<= 13", "between 5 and 13", "in 5-13", "in 5..13"),
            new PredicateOutcome<>(Field.readOnlyField(Integer.class, null), 13, false,
                    "< 12", "!= 13", "<> 13", "between 5 and 6", "in 0-0"),
            new PredicateOutcome<>(Field.readOnlyField(String.class, null), "minecraft:tnt", true,
                    "minecraft:tnt", "MiNeCrAfT:TnT",
                    "= minecraft:tnt", "= \"minecraft:tnt\"i",
                    "contains tnt", "contains TNT", "contains \"TNT\"i",
                    "matches ^minecraft:.+", "matches \"minecraft:tnt\"", "matches \"^MINECRAFT\"i"),
            new PredicateOutcome<>(Field.readOnlyField(String.class, null), "minecraft:tnt", false,
                    "= \"MINECRAFT:TNT\"", "contains \"TNT\"", "matches \"^MINECRAFT\""),
            new PredicateOutcome<>(new MapField<>(null, null, NamespacedKey::fromString, Object.class),
                    testMap, true, "contains tnt", "contains minecraft:tnt"),
            new PredicateOutcome<>(new MapField<>(null, null, NamespacedKey::fromString, Object.class),
                    testMap, false, "contains sand", "contains invalid:item"),
            new PredicateOutcome<>(new NamespacedKeyField<>(null, null),
                    new NamespacedKey("minecraft", "value"), true,
                    "= value", "= VALUE", "= minecraft:value", "value", "VALUE", "minecraft:value", "contains value"),
            new PredicateOutcome<>(new NamespacedKeyField<>(null, null),
                    new NamespacedKey("minecraft", "value"), false,
                    "contains something_else", "matches ^example:value$")
            // can't test other fields as they depend on Bukkit
    );
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testPredicate() {
        for (var outcome : predicateOutcomes) {
            var field = outcome.field();
            for (var input : outcome.inputs()) {
                var predicate = (BiPredicate) field.parsePredicate(input);
                assertEquals(outcome.result, predicate.test(null, outcome.test),
                        () -> "Clazz: %s, testing \"%s\" against %s"
                                .formatted(field.getFieldClass().getSimpleName(), input, outcome.test.toString())
                );
            }
        }
    }

    record TransformerOutcome<T>(Field<?, T> field, T input, Map<String, T> transformations) {}
    List<TransformerOutcome<?>> transformerOutcomes = List.of(
            new TransformerOutcome<>(Field.readOnlyField(Integer.class, null), 5, Map.of(
                    ">= 6", 6, "= 10", 10, "11", 11,
                    "+= 15", 20, "/= 3", 1, "%= 3", 2,
                    "between 6 and 7", 6, "in 0..3", 3
            ))
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testTransformer() {
        for (var outcome : transformerOutcomes) {
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
}
