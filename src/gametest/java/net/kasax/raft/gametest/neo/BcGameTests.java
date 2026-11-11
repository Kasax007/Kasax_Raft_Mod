package net.kasax.raft.gametest.neo;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** NeoForge game tests of Beach Craft (machines, energy, raft, entities, world). */
@GameTestHolder("raft_gametest")
@PrefixGameTestTemplate(false)
public class BcGameTests {
    @GameTest(template = "empty")
    public static void boots(GameTestHelper helper) {
        helper.succeed();
    }
}
