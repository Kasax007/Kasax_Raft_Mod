package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselColliders;
import net.kasax.raft.content.vessel.VesselEntity;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * A player standing on a sailing raft has no world blocks under their feet (the raft is an entity), so the server
 * would kick them for "floating too long". On or right next to a raft they count as standing on something.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void raft$standingOnRaft(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VesselColliders.isAboard(entity)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * A player put onto a raft's deck by the server (teleport, login, dimension change, respawn) is told where on the
     * raft, so that their client does not let them fall through it before its copy of the raft is there.
     */
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void raft$arrivedOnRaft(double x, double y, double z, float yRot, float xRot, Set<RelativeMovement> relatives, CallbackInfo ci) {
        VesselEntity.playerArrived(this.player, null);
    }

}
