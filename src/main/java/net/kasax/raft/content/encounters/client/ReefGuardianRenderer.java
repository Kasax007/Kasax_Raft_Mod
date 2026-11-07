package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Guardian;

/** Reef guardian and Reef Warden: the guardian body (and beam) with the reef skin. */
public class ReefGuardianRenderer extends GuardianRenderer {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/reef_guardian/reef_guardian.png");

    public ReefGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Guardian entity) {
        return TEXTURE;
    }
}
