package net.fabricmc.fabric.api.client.gametest.v1.context;

import net.minecraft.server.MinecraftServer;

/** Makes a server context for an opened world (TestWorldSaveImpl lives in another package). */
public final class TestServers {
    private TestServers() {
    }

    public static TestServerContext of(ClientGameTestContext ctx, MinecraftServer server) {
        return new TestServerContext(ctx, server);
    }
}
