package net.kasax.raft.content.shore.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Render state shared by the beach crab and the ember crab. */
public class CrabRenderState extends LivingEntityRenderState {
    public int variant;
    public float burrow;
    public float sideways;
    public boolean threatening;
    public boolean ember;
    public float seed;
    public final AnimationState pinch = new AnimationState();
    public final AnimationState snap = new AnimationState();
}
