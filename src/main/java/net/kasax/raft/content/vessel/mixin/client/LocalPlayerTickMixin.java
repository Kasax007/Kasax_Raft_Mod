package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.VesselCarry;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See {@link VesselCarry}: a raft carry earlier in this tick is drawn smoothly; and a player just put onto a raft waits
 * for it (see VesselClient.beforePlayerTick).
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void raft$smoothRaftCarry(CallbackInfo ci) {
        VesselCarry.beforeLocalPlayerTick((LocalPlayer) (Object) this);
        net.kasax.raft.content.vessel.client.VesselClient.beforePlayerTick((LocalPlayer) (Object) this);
    }
}
