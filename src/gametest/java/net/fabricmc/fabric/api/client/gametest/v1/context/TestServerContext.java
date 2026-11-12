package net.fabricmc.fabric.api.client.gametest.v1.context;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** The integrated server of a test world (see FabricClientGameTest). */
public final class TestServerContext {
    private final ClientGameTestContext ctx;
    private final MinecraftServer server;

    TestServerContext(ClientGameTestContext ctx, MinecraftServer server) {
        this.ctx = ctx;
        this.server = server;
    }

    /** Runs a command as the server (all permissions). */
    public void runCommand(String command) {
        this.runOnServer(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), command));
    }

    public void runOnServer(Consumer<MinecraftServer> action) {
        this.server.submit(() -> action.accept(this.server)).join();
    }

    public <T> T computeOnServer(Function<MinecraftServer, T> function) {
        return this.server.submit(() -> function.apply(this.server)).join();
    }

    /** Waits (client ticks) until the predicate holds on the server; the ticks it took. AssertionError on timeout. */
    public int waitFor(Predicate<MinecraftServer> predicate, int timeout) {
        for (int t = 0; ; t++) {
            if (this.computeOnServer(predicate::test)) {
                return t;
            }
            if (t >= timeout) {
                throw new AssertionError("timed out after " + timeout + " ticks");
            }
            this.ctx.waitTick();
        }
    }
}
