package net.kasax.raft.content.shore.entity;

import net.minecraft.world.entity.AnimationState;

/** What the crab renderer needs from a crab (the beach crab and the ember crab share the model). */
public interface CrabAnimated {
    /** 0 = standing, 1 = dug into the sand with only the eyes showing (client, interpolated). */
    float getBurrowAmount(float partialTicks);

    /** 0 = facing forward, 1 = scuttling sideways (client, interpolated). */
    float getSidewaysAmount(float partialTicks);

    /** Claws raised: the crab is angry. */
    boolean isThreatening();

    /** Pinch attack animation, started by an entity event. */
    AnimationState pinchAnimation();

    /** Snapping claw animation (idle or angry), started by an entity event. */
    AnimationState snapAnimation();
}
