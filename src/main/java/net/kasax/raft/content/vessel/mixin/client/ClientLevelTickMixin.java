package net.kasax.raft.content.vessel.mixin.client;

import net.kasax.raft.content.vessel.VesselCarry;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** See {@link VesselCarry}: a new round of entity ticks starts. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTickMixin {
    @Inject(method = "tickEntities", at = @At("HEAD"))
    private void raft$startRaftCarry(CallbackInfo ci) {
        VesselCarry.startEntityTicks();
    }
}
