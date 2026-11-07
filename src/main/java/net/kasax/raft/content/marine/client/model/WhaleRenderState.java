package net.kasax.raft.content.marine.client.model;

import net.kasax.raft.compat.render.LivingEntityRenderState;

/** Whale render state: spout (0..1, head lifts) and song (0..1, jaw opens a little). */
public class WhaleRenderState extends LivingEntityRenderState {
    public float spout;
    public float song;
}
