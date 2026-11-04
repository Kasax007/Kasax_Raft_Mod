package net.kasax.raft.content.npc.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Render state of a person of the sea: the skin and whether they sit (on the ground, legs stretched out). */
public class NpcRenderState extends HumanoidRenderState {
    public Identifier texture;
    public boolean sitting;
}
