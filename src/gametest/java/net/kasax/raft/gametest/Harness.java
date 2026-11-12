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
    private static int ticks;
    private static int menuTicks;

    public Harness() {
        onInitializeClient();
    }

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientGameTestContext.onClientTick();
            // the title screen, or the first-start screens before it (accessibility, ...): any menu without a world
            if (!started && ++ticks % 200 == 0) {
                BcScript.LOG.info("[bctest] harness waits: overlay {}, screen {}", mc.gui.overlay(), mc.gui.screen());
            }
            if (!started && mc.gui.overlay() == null && mc.level == null && mc.gui.screen() != null && ++menuTicks > 40) {
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
