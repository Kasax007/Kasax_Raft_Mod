package net.kasax.raft.content.shore.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Render state of the sea otter. */
public class SeaOtterRenderState extends LivingEntityRenderState {
    public float back;
    public float swim;
    public float swimPitch;
    public boolean carrying;
    public float seed;
    public final AnimationState crack = new AnimationState();
}
