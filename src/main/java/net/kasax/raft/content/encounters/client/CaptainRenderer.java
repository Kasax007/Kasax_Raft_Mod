package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.render.CorsairRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Corsair Captain: the corsair's body in a navy captain's coat and a tricorn with a red plume. */
public class CaptainRenderer extends CorsairRenderer {
    public CaptainRenderer(EntityRendererProvider.Context context) {
        super(context, Raft.id("textures/entity/corsair_captain/corsair_captain.png"),
                Raft.id("textures/entity/corsair_captain/corsair_captain_coat.png"),
                Raft.id("textures/entity/corsair_captain/corsair_captain_hat.png"));
    }
}
