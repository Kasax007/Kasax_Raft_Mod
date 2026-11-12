package net.fabricmc.fabric.api.client.gametest.v1;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Beach Craft's stand-in for Fabric's client game test API on Minecraft 1.21.1 (it came with a later Fabric API):
 * the same names and the part of it the bctest harness uses, run by {@code net.kasax.raft.gametest.Harness}.
 * Only in the test source set; never in the mod jar.
 */
public interface FabricClientGameTest {
    void runTest(ClientGameTestContext context);
}
