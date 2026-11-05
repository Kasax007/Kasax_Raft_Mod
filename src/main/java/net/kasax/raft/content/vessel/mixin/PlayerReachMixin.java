package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselShipyard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reach checks for blocks of sailing rafts (a chest, furnace or crafting table stays open while its raft sails; signs
 * and breaking use the same check): the block is in the raft's shipyard plot, so the distance is measured to where
 * that block is in the world, on the raft.
 */
@Mixin(Player.class)
public abstract class PlayerReachMixin {
    @Inject(method = "isWithinBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void raft$reachOnRaft(BlockPos pos, double buffer, CallbackInfoReturnable<Boolean> cir) {
        if (!VesselShipyard.maybePlot(pos.getX(), pos.getZ())) {
            return;
        }
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        Vec3 at = VesselShipyard.toWorld(self.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (at != null) {
            // a block around its centre, with a little slack for the turning raft (its box is axis aligned here)
            double range = self.blockInteractionRange() + buffer + 0.5;
            cir.setReturnValue(at.distanceToSqr(self.getEyePosition()) < range * range);
        }
    }
}
