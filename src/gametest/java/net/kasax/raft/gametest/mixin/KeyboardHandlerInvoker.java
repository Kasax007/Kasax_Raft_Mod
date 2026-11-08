package net.kasax.raft.gametest.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The character callback, so typed text reaches screens and other mods' text boxes like real typing (1.21.1 harness). */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerInvoker {
    @Invoker("charTyped")
    void bc$charTyped(long window, int codepoint, int mods);
}
