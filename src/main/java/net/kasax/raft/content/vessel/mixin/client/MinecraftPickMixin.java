package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.client.VesselPicker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crosshair also finds the blocks of sailing rafts (exact block shapes, in the raft's own frame). */
@Mixin(Minecraft.class)
public abstract class MinecraftPickMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void raft$pickRafts(float partialTicks, CallbackInfo ci) {
        VesselPicker.pick((Minecraft) (Object) this, partialTicks);
    }
}
