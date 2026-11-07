package net.kasax.raft.gametest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The mouse callbacks, for real clicks of the test (1.21.1 harness). */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onPress")
    void bc$onPress(long window, int button, int action, int mods);

    @Invoker("onMove")
    void bc$onMove(long window, double x, double y);

    @Invoker("onScroll")
    void bc$onScroll(long window, double x, double y);
}
