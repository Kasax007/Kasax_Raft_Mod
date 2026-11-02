package net.kasax.raft.content.marine.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Moray render state: lunge (0..1, wide jaw) and whether it sits in its hole (calm sway). */
public class MorayEelRenderState extends LivingEntityRenderState {
    public float lunge;
    public boolean hiding;
    public int phase;
}
