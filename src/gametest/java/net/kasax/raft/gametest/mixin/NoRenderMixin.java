package net.kasax.raft.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.kasax.raft.gametest.BcScript;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While {@link BcScript#skipFrames} is set (waiting for server ticks) or a frame came sooner than
 * {@link BcScript#throttleMs}, the client skips drawing its frame: the game runs at full speed under Xvfb.
 * (1.21.1: the frame is GameRenderer.render(DeltaTracker, boolean) in Minecraft.runTick; the chunk meshes are built
 * by the next frame that is drawn.)
 */
@Mixin(Minecraft.class)
public class NoRenderMixin {
    private static long bctest$last;

    @WrapOperation(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void bctest$skipFrame(GameRenderer renderer, DeltaTracker delta, boolean tick, Operation<Void> original) {
        boolean skip = BcScript.skipFrames;
        if (!skip && BcScript.throttleMs > 0) {
            long now = System.currentTimeMillis();
            if (now - bctest$last < BcScript.throttleMs) {
                skip = true;
            } else {
                bctest$last = now;
            }
        }
        if (!skip) {
            original.call(renderer, delta, tick);
        }
    }
}
