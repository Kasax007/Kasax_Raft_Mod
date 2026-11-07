package net.kasax.raft.content.shore.client.render;

import net.kasax.raft.compat.render.LivingEntityRenderState;
import net.kasax.raft.compat.render.AnimState;

/** Render state shared by the beach crab and the ember crab. */
public class CrabRenderState extends LivingEntityRenderState {
    public int variant;
    public float burrow;
    public float sideways;
    public boolean threatening;
    public boolean ember;
    public float seed;
    public final AnimState pinch = new AnimState();
    public final AnimState snap = new AnimState();
}
