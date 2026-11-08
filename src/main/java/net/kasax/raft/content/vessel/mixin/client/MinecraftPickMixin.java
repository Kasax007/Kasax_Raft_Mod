package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.client.VesselPicker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crosshair also finds the blocks of sailing rafts (exact block shapes, in the raft's own frame). */
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class MinecraftPickMixin {
    /** 1.21.1: the crosshair target is picked in GameRenderer.pick(float). */
    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void raft$pickRafts(float partialTicks, CallbackInfo ci) {
        VesselPicker.pick(Minecraft.getInstance(), partialTicks);
    }
}
