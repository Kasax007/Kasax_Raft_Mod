package net.kasax.raft.content.voyage.client;

import net.kasax.raft.compat.render.EntityRenderState;

/** Everything the Expedition Raft model needs for one frame. */
public class ExpeditionRaftRenderState extends EntityRenderState {
    public float yRot;
    public float hurtTime;
    public int hurtDir;
    public float damageTime;
    public boolean isUnderWater;
    public boolean inWater;
    public int upgrades;
    public float anchorDrop;
    public float sailFill;
    public float propeller;
    public float turn;
    public boolean engineRunning;
    public boolean rowing;
    public float rowingTimeLeft;
    public float rowingTimeRight;
}
