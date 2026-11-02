package net.kasax.raft.content.marine.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Manta ray render state: banking from the turn rate (radians, + = left wing down). */
public class MantaRayRenderState extends LivingEntityRenderState {
    public float bank;
    public int phase;
}
