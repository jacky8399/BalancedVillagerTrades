import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class LuaTest {

    public static LuaValue run(String script) {
        return ScriptUtils.runScriptInSandbox(script, ".", ScriptUtils.createSandbox());
    }

    public static LuaValue run(String script, Consumer<Globals> globalsConsumer) {
        return ScriptUtils.runScriptInSandbox(script, ".", ScriptUtils.createSandbox(globalsConsumer));
    }

    @Test
    public void maxInstructionTest() {
        Config.luaMaxInstructions = 1000;
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertThrows(LuaError.class, () -> run("""
                    var = 0
                    while true do
                        var = var + 1
                    end
                    """));
        });
        Config.luaMaxInstructions = 0;
    }

    @Test
    public void errorPropagationTest() {
        assertThrows(LuaError.class, () -> run("error('test')"));
    }

    @Test
    public void iteratorTest() {
        assertEquals("abc", run("""
                        str = ""
                        for item in iterate() do
                            str = str .. item
                        end
                        return str
                        """,
                globals -> globals.set("iterate", ScriptUtils.getIteratorFor(List.of("a", "b", "c"), LuaValue::valueOf))
        ).checkjstring());

        assertEquals("a1b2c3", run("""
                        str = ""
                        for k, v in iterate() do
                            str = str .. k .. v
                        end
                        return str
                        """,
                globals -> globals.set("iterate", ScriptUtils.getIteratorFor(
                        List.of(Map.entry("a", 1), Map.entry("b", 2), Map.entry("c", 3)),
                        entry -> LuaValue.varargsOf(LuaValue.valueOf(entry.getKey()), LuaValue.valueOf(entry.getValue()), LuaValue.NIL)
                ))
        ).checkjstring());
    }
}
