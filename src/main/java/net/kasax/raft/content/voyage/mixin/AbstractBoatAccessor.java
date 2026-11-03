package net.kasax.raft.content.voyage.mixin;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the private steering state of vanilla boats (the Expedition Raft turns slower and never ejects riders). */
@Mixin(AbstractBoat.class)
public interface AbstractBoatAccessor {
    @Accessor("deltaRotation")
    float raft$getDeltaRotation();

    @Accessor("deltaRotation")
    void raft$setDeltaRotation(float value);

    @Accessor("outOfControlTicks")
    void raft$setOutOfControlTicks(float value);
}
