package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselShipyard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The shipyard plots of sailing rafts (see VesselShipyard) and the world: block changes in a plot go to its raft (and
 * from there to the players who see it); entities, sounds and level events (break particles, door sounds) that start in
 * a plot happen where that spot is in the world, on the raft. Relocated sounds and events reach everyone, also the
 * player who caused them (their client does not play them itself, it has no block there).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelShipyardMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void raft$plotBlockChanged(BlockPos pos, BlockState old, BlockState current, int updateFlags, CallbackInfo ci) {
        VesselShipyard.blockChanged((ServerLevel) (Object) this, pos);
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void raft$entityFromPlot(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        VesselShipyard.relocate((ServerLevel) (Object) this, entity);
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void raft$soundFromPlot(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source,
                                    float volume, float pitch, long seed, CallbackInfo ci) {
        if (VesselShipyard.maybePlot(x, z)) {
            ServerLevel self = (ServerLevel) (Object) this;
            Vec3 at = VesselShipyard.toWorld(self, x, y, z);
            if (at != null) {
                ci.cancel();
                self.playSeededSound(null, at.x, at.y, at.z, sound, source, volume, pitch, seed);
            }
        }
    }

    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void raft$eventFromPlot(@Nullable Entity source, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (VesselShipyard.maybePlot(pos.getX(), pos.getZ())) {
            ServerLevel self = (ServerLevel) (Object) this;
            Vec3 at = VesselShipyard.toWorld(self, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (at != null) {
                ci.cancel();
                self.levelEvent(null, type, BlockPos.containing(at), data);
            }
        }
    }
}
