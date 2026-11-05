package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.VesselCarry;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** See {@link VesselCarry}: a raft carry earlier in this tick is drawn smoothly. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void raft$smoothRaftCarry(CallbackInfo ci) {
        VesselCarry.beforeLocalPlayerTick((LocalPlayer) (Object) this);
    }
}
