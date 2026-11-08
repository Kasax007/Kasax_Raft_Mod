package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.resources.Identifier;

/** Reef guardian and Reef Warden: the guardian body (and beam) with the reef skin. */
public class ReefGuardianRenderer extends GuardianRenderer {
    private static final Identifier TEXTURE = Raft.id("textures/entity/reef_guardian/reef_guardian.png");

    public ReefGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(GuardianRenderState state) {
        return TEXTURE;
    }
}
