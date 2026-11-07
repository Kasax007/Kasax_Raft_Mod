package net.kasax.raft.content.npc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.NpcCharacter;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/** Renders a person of the sea with their own skin ({@code textures/entity/npc/<id>.png}, player skin layout). */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, NpcModel> {
    private final ResourceLocation texture;

    public NpcRenderer(EntityRendererProvider.Context context, NpcCharacter character) {
        super(context, new NpcModel(context.bakeLayer(character.slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), character.slim), 0.5F);
        this.texture = Raft.id("textures/entity/npc/" + character.id + ".png");
        ModelLayerLocation extra = NpcAccessories.of(character);
        if (extra != null) {
            this.addLayer(new ExtraLayer(this, new HumanoidModel<>(context.bakeLayer(extra)), NpcAccessories.texture(character)));
        }
    }

    /** Straw hat brim, hard hat visor, hair bun or flower (NpcAccessories), moving with the head. */
    private static class ExtraLayer extends RenderLayer<NpcEntity, NpcModel> {
        private final HumanoidModel<NpcEntity> model;
        private final ResourceLocation texture;

        ExtraLayer(RenderLayerParent<NpcEntity, NpcModel> parent, HumanoidModel<NpcEntity> model, ResourceLocation texture) {
            super(parent);
            this.model = model;
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, NpcEntity npc, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                coloredCutoutModelCopyLayerRender(this.getParentModel(), this.model, this.texture, poseStack, buffers, light, npc,
                        limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity npc) {
        return this.texture;
    }

    @Override
    public Vec3 getRenderOffset(NpcEntity npc, float partialTick) {
        // sitting on the ground: the legs stick out forward, so the body sits lower
        return npc.isSitting() ? new Vec3(0.0, -0.62, 0.0) : super.getRenderOffset(npc, partialTick);
    }

    @Override
    protected boolean shouldShowName(NpcEntity npc) {
        if (npc.hasCustomName() && npc.isCustomNameVisible()) {
            return super.shouldShowName(npc);
        }
        // the name shows when the player looks at the NPC from up close
        return this.entityRenderDispatcher.distanceToSqr(npc) < 100.0 && npc == this.entityRenderDispatcher.crosshairPickEntity
                && super.shouldShowName(npc);
    }
}
