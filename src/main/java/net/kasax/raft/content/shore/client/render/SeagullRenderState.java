package net.kasax.raft.content.shore.client.render;

import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

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
    public final AnimationState peck = new AnimationState();
    public final AnimationState call = new AnimationState();
}
