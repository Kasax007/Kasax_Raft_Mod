package net.fabricmc.fabric.api.client.gametest.v1.context;

import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.TitleScreen;

/** An open singleplayer test world (see FabricClientGameTest). */
public final class TestSingleplayerContext {
    private final ClientGameTestContext ctx;
    private final TestServerContext server;
    private final TestWorldSave save;

    public TestSingleplayerContext(ClientGameTestContext ctx, TestServerContext server, TestWorldSave save) {
        this.ctx = ctx;
        this.server = server;
        this.save = save;
    }

    public TestServerContext getServer() {
        return this.server;
    }

    public TestWorldSave getWorldSave() {
        return this.save;
    }

    /** Leaves the world (it is saved) and waits until the server has stopped. */
    public void close() {
        this.ctx.runOnClient(mc -> {
            if (mc.level != null) {
                mc.level.disconnect(net.minecraft.network.chat.Component.empty());
            }
            mc.disconnect(new TitleScreen(), false);
        });
        this.ctx.waitFor(mc -> mc.getSingleplayerServer() == null && mc.level == null, 6000);
    }
}
