package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselColliders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Entities collide with the blocks of sailing rafts like with world blocks: the raft's block boxes join the entity
 * collision shapes (used for movement, step-up, sneaking at edges and "is there room" checks).
 */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
    @Inject(method = "getEntityCollisions", at = @At("RETURN"), cancellable = true)
    private void raft$vesselCollisions(@Nullable Entity source, AABB testArea, CallbackInfoReturnable<List<VoxelShape>> cir) {
        if ((Object) this instanceof Level level) {
            List<VoxelShape> extra = VesselColliders.collisions(level, source, testArea);
            if (extra != null) {
                List<VoxelShape> base = cir.getReturnValue();
                List<VoxelShape> all = new ArrayList<>(base.size() + extra.size());
                all.addAll(base);
                all.addAll(extra);
                cir.setReturnValue(all);
            }
        }
    }
}
