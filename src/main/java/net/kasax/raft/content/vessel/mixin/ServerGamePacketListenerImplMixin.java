package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselColliders;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player standing on a sailing raft has no world blocks under their feet (the raft is an entity), so the server
 * would kick them for "floating too long". On or right next to a raft they count as standing on something.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void raft$standingOnRaft(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VesselColliders.isAboard(entity)) {
            cir.setReturnValue(false);
        }
    }
}
