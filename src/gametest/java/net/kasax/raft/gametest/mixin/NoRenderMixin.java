package net.kasax.raft.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.kasax.raft.gametest.BcScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client draws a frame on every tick. While a clip is recorded (or during "wait"), only the screenshots are needed:
 * skip drawing the per-tick frames (the screenshot renders its own frame), which halves the time per recorded frame
 * on the software renderer. The frame is still extracted (camera, picking, chunk sections), only not drawn.
 */
@Mixin(Minecraft.class)
public class NoRenderMixin {
    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V"))
    private void bctest$skipFrame(GameRenderer renderer, Operation<Void> original) {
        if (!BcScript.skipFrames) original.call(renderer);
    }
}
