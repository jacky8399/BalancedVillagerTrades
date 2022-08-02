import com.jacky8399.balancedvillagertrades.Config;
import com.jacky8399.balancedvillagertrades.utils.ScriptUtils;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaError;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class LuaTest {
    @Test
    public void runMaliciousScript() {
        Config.luaMaxInstructions = 1000;
        var sandbox = ScriptUtils.createSandbox();
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertThrows(LuaError.class, () -> ScriptUtils.runScriptInSandbox("""
                    var = 0
                    while true do
                        var = var + 1
                    end
                    """, ".", sandbox));
        });
        Config.luaMaxInstructions = 0;
    }

    @Test
    public void errorPropagationTest() {
        assertThrows(LuaError.class, () -> ScriptUtils.runScriptInSandbox("error('test')",
                ".", ScriptUtils.createSandbox()));
    }
}
