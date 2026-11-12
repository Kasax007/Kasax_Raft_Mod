package net.fabricmc.fabric.impl.client.gametest.world;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServers;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.file.Path;

/** Opens a saved world of the saves folder (see FabricClientGameTest). */
public final class TestWorldSaveImpl implements TestWorldSave {
    private final ClientGameTestContext ctx;
    private final Path dir;

    public TestWorldSaveImpl(ClientGameTestContext ctx, Path dir) {
        this.ctx = ctx;
        this.dir = dir;
    }

    @Override
    public Path getSaveDirectory() {
        return this.dir;
    }

    public TestSingleplayerContext open() {
        String id = this.dir.getFileName().toString();
        this.ctx.runOnClient(mc -> mc.createWorldOpenFlows().openWorld(id, () -> mc.gui.setScreen(new TitleScreen())));
        this.ctx.waitForWorld();
        TestServerContext server = TestServers.of(this.ctx, this.ctx.computeOnClient(mc -> mc.getSingleplayerServer()));
        return new TestSingleplayerContext(this.ctx, server, this);
    }
}
