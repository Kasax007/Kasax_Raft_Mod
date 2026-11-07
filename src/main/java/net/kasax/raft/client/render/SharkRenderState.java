package net.kasax.raft.client.render;

import net.kasax.raft.compat.render.LivingEntityRenderState;

/** Render state of the shark and the hammerhead: how far the jaw is open for the bite animation (0..1). */
public class SharkRenderState extends LivingEntityRenderState {
    public float bite;
}
