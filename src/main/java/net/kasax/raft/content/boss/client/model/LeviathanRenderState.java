package net.kasax.raft.content.boss.client.model;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state of the Leviathan. Parts: 0 = head, 1..6 = body segments, 7 = tail. Positions are in blocks relative to
 * the head pivot (centre of the hitbox), angles in degrees (Minecraft yaw/pitch of each part).
 */
public class LeviathanRenderState extends EntityRenderState {
    public static final int PARTS = 8;
    public final float[] px = new float[PARTS];
    public final float[] py = new float[PARTS];
    public final float[] pz = new float[PARTS];
    public final float[] yaw = new float[PARTS];
    public final float[] pitch = new float[PARTS];
    /** Bend between a segment and the one in front of it (degrees, for fin flex). */
    public final float[] bend = new float[PARTS];
    public float jaw;
    public float deathTime;
    public boolean hurt;
    public boolean inWater;
    public int phase;
    public int state;
    public float stateTicks;
    public float speed;
    public float glow;
}
