package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselColliders;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A sneaking player on a sailing raft: their own client keeps them from walking off the deck (it sees the raft's
 * blocks). The server does not test players against rafts, so it would think they stand at an edge of nothing, cut
 * their moves short and pull them back. On the server, players aboard a raft skip the sneak edge check.
 */
@Mixin(Player.class)
public abstract class PlayerEdgeMixin {
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void raft$noServerEdgeOnRafts(Vec3 delta, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() && VesselColliders.isAboard(self)) {
            cir.setReturnValue(delta);
        }
    }
}
