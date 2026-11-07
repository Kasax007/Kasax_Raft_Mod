package net.kasax.raft.content.shore.client.render;

import net.kasax.raft.compat.render.LivingEntityRenderState;
import net.kasax.raft.compat.render.AnimState;

/** Render state of the sea otter. */
public class SeaOtterRenderState extends LivingEntityRenderState {
    public float back;
    public float swim;
    public float swimPitch;
    public boolean carrying;
    public float seed;
    public final AnimState crack = new AnimState();
}
