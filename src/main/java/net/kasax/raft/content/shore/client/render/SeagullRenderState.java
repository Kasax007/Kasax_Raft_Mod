package net.kasax.raft.content.shore.client.render;

import net.kasax.raft.compat.render.HoldingEntityRenderState;
import net.kasax.raft.compat.render.AnimState;

/** Render state of the seagull: flight blend, flap cycle, banking and the food in its beak. */
public class SeagullRenderState extends HoldingEntityRenderState {
    public float flyAmount;
    public float flapPhase;
    public float flapPower;
    public float roll;
    public float flightPitch;
    public boolean floating;
    public boolean holdsItem;
    public float seed;
    public final AnimState peck = new AnimState();
    public final AnimState call = new AnimState();
}
