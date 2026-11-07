package net.kasax.raft.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * The 1.21.1 test runner: Fabric's client game test API does not exist for this version, so this client entrypoint
 * starts {@link BcScript} on its own thread once the title screen is up, feeds it client ticks and closes the game
 * when the script is done.
 */
public class Harness implements ClientModInitializer {
    private static boolean started;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientGameTestContext.onClientTick();
            if (!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                started = true;
                start(mc);
            }
        });
    }

    private static void start(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        Thread t = new Thread(() -> {
            try {
                new BcScript().runTest(new ClientGameTestContext());
            } catch (Throwable e) {
                BcScript.LOG.error("[bctest] Test failed", e);
            } finally {
                mc.execute(mc::stop);
            }
        }, "Test thread");
        t.setDaemon(true);
        t.start();
    }
}
