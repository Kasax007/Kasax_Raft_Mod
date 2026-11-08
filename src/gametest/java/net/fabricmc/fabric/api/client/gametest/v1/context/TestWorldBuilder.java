package net.fabricmc.fabric.api.client.gametest.v1.context;

import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.kasax.raft.gametest.mixin.CreateWorldScreenInvoker;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.function.Consumer;

/** Creates a singleplayer test world through the game's own Create World screen (see FabricClientGameTest). */
public final class TestWorldBuilder {
    private final ClientGameTestContext ctx;
    private Consumer<WorldCreationUiState> settings = s -> {
    };

    TestWorldBuilder(ClientGameTestContext ctx) {
        this.ctx = ctx;
    }

    public TestWorldBuilder setUseConsistentSettings(boolean consistent) {
        return this;
    }

    public TestWorldBuilder adjustSettings(Consumer<WorldCreationUiState> settings) {
        this.settings = settings;
        return this;
    }

    public TestSingleplayerContext create() {
        this.ctx.runOnClient(mc -> CreateWorldScreen.openFresh(mc, mc.screen));
        this.ctx.waitFor(mc -> mc.screen instanceof CreateWorldScreen, 2400);
        this.ctx.runOnClient(mc -> {
            CreateWorldScreen screen = (CreateWorldScreen) mc.screen;
            screen.getUiState().setName("bctest");
            this.settings.accept(screen.getUiState());
            ((CreateWorldScreenInvoker) screen).bc$onCreate();
        });
        this.ctx.waitForWorld();
        Path dir = this.ctx.computeOnClient(mc -> mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize());
        TestServerContext server = new TestServerContext(this.ctx, this.ctx.computeOnClient(mc -> mc.getSingleplayerServer()));
        TestWorldSave save = () -> dir;
        return new TestSingleplayerContext(this.ctx, server, save);
    }
}
