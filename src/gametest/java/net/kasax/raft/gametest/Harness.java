package net.kasax.raft.gametest;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.kasax.raft.platform.client.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * The 1.21.1 test runner: Fabric's client game test API does not exist for this version, so this client entrypoint
 * starts {@link BcScript} on its own thread once the title screen is up, feeds it client ticks and closes the game
 * when the script is done.
 */
@Mod(value = "raft_gametest", dist = Dist.CLIENT)
public class Harness {
    private static boolean started;

    public Harness() {
        onInitializeClient();
    }

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientGameTestContext.onClientTick();
            // the title screen, or the accessibility screen 1.21.1 shows on a first start
            if (!started && mc.gui.overlay() == null && (mc.gui.screen() instanceof TitleScreen
                    || mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) {
                started = true;
                start(mc);
            }
        });
    }

    private static void start(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.onboardAccessibility = false;
        BcScript.LOG.info("[bctest] harness starts the script");
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
