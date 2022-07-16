import com.jacky8399.balancedvillagertrades.fields.Field;
import com.jacky8399.balancedvillagertrades.fields.Fields;
import com.jacky8399.balancedvillagertrades.fields.MapField;
import com.jacky8399.balancedvillagertrades.fields.NamespacedKeyField;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
                    "> 12", "= 13", "13", "!= 5", "<= 13"),
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

    @Test
    @Disabled
    public void testTransformer() {
        // TODO add test cases
    }
}
