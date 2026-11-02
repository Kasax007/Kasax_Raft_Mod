package net.kasax.raft.content.marine.client.model;

import net.kasax.raft.content.marine.entity.JellyfishEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Jellyfish render state: variant, bell contraction (0..1) and the drift of the body in model space. */
public class JellyfishRenderState extends LivingEntityRenderState {
    public JellyfishEntity.Variant variant = JellyfishEntity.Variant.MOON;
    public float contraction;
    public float dragX;
    public float dragZ;
    public float rise;
    public int phase;
}
