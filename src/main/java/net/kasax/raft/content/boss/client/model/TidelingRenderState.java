package net.kasax.raft.content.boss.client.model;

import net.kasax.raft.compat.render.LivingEntityRenderState;

/** Tideling render state: bite (0..1 jaw snap) and flopping (stranded on land). */
public class TidelingRenderState extends LivingEntityRenderState {
    public float bite;
    public boolean flopping;
    public boolean onLand;
}
