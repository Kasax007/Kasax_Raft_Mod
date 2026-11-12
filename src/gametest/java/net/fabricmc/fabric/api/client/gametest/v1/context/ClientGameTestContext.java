package net.fabricmc.fabric.api.client.gametest.v1.context;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The test's handle on the client (see FabricClientGameTest): the test runs on its own thread and waits for client
 * ticks; everything that touches the game runs on the client thread ({@link #runOnClient}).
 */
public final class ClientGameTestContext {
    private static final Object TICK = new Object();
    private static long ticks;
    private static final AtomicInteger SHOTS = new AtomicInteger();

    private final TestInput input = new TestInput(this);

    /** Called by the harness at the end of every client tick. */
    public static void onClientTick() {
        synchronized (TICK) {
            ticks++;
            TICK.notifyAll();
        }
    }

    public void waitTick() {
        this.waitTicks(1);
    }

    public void waitTicks(int n) {
        synchronized (TICK) {
            long target = ticks + n;
            while (ticks < target) {
                try {
                    TICK.wait(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /** Waits until the predicate holds on the client; the ticks it took. AssertionError on timeout. */
    public int waitFor(Predicate<Minecraft> predicate, int timeout) {
        for (int t = 0; ; t++) {
            if (this.computeOnClient(predicate::test)) {
                return t;
            }
            if (t >= timeout) {
                throw new AssertionError("timed out after " + timeout + " ticks");
            }
            this.waitTick();
        }
    }

    public void runOnClient(Consumer<Minecraft> action) {
        Minecraft mc = Minecraft.getInstance();
        mc.submit(() -> action.accept(mc)).join();
    }

    public <T> T computeOnClient(Function<Minecraft, T> function) {
        Minecraft mc = Minecraft.getInstance();
        return mc.submit(() -> function.apply(mc)).join();
    }

    public void setScreen(Supplier<Screen> screen) {
        this.runOnClient(mc -> mc.gui.setScreen(screen.get()));
        this.waitTick();
    }

    /**
     * Waits until a world is open and its sections are drawn. A confirmation the game asks on the way (1.21.1 warns
     * that modded world settings are "experimental") is answered with Yes, as a player does.
     */
    public void waitForWorld() {
        for (int t = 0; t < 12000; t++) {
            boolean open = this.computeOnClient(mc -> {
                if (mc.gui.screen() != null) {
                    for (var child : mc.gui.screen().children()) {
                        if (child instanceof net.minecraft.client.gui.components.Button b
                                && (b.getMessage().equals(net.minecraft.network.chat.CommonComponents.GUI_YES)
                                || b.getMessage().equals(net.minecraft.network.chat.CommonComponents.GUI_PROCEED))) {
                            b.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                            return false;
                        }
                    }
                }
                return mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && mc.gui.screen() == null;
            });
            if (open) {
                this.waitFor(mc -> mc.levelRenderer.hasRenderedAllSections(), 2400);
                return;
            }
            this.waitTick();
        }
        throw new AssertionError("no world after 12000 ticks");
    }

    public TestInput getInput() {
        return this.input;
    }

    public TestWorldBuilder worldBuilder() {
        return new TestWorldBuilder(this);
    }

    /**
     * The last frame the client drew (main render target), as a PNG. The script makes sure a fresh frame was drawn
     * ("wait n render") before it asks.
     */
    public Path takeScreenshot(TestScreenshotOptions options) {
        this.waitTick();
        Path dir = options.dir() != null ? options.dir() : Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
        String name = (options.counterPrefix() ? String.format("%04d_", SHOTS.incrementAndGet()) : "") + options.name() + ".png";
        Path file = dir.resolve(name);
        // 26.3 reads the frame back from the GPU asynchronously: wait for the callback before going on
        java.util.concurrent.CompletableFuture<Void> done = new java.util.concurrent.CompletableFuture<>();
        this.runOnClient(mc -> {
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                done.completeExceptionally(e);
                return;
            }
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                try (image) {
                    if (options.width() > 0 && (options.width() != image.getWidth() || options.height() != image.getHeight())) {
                        try (NativeImage scaled = new NativeImage(options.width(), options.height(), false)) {
                            image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), scaled);
                            scaled.writeToFile(file);
                        }
                    } else {
                        image.writeToFile(file);
                    }
                    done.complete(null);
                } catch (Exception e) {
                    done.completeExceptionally(e);
                }
            });
        });
        for (int t = 0; t < 200 && !done.isDone(); t++) {
            this.waitTick();
        }
        done.join();
        return file;
    }
}
