package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.CorsairModel;
import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Drowned corsair: drowned body with its own skin, a ragged pirate coat (outer layer) and a tricorn hat with a
 * feather (own model layer). The hat is hidden when the corsair wears something on its head.
 */
public class CorsairRenderer extends AbstractZombieRenderer<DrownedCorsairEntity, CorsairModel> {
    private static final ResourceLocation TEXTURE = Raft.id("textures/entity/drowned_corsair/drowned_corsair.png");
    private static final ResourceLocation COAT = Raft.id("textures/entity/drowned_corsair/drowned_corsair_coat.png");
    private static final ResourceLocation HAT = Raft.id("textures/entity/drowned_corsair/drowned_corsair_hat.png");

    public CorsairRenderer(EntityRendererProvider.Context context) {
        super(context, new CorsairModel(context.bakeLayer(ModelLayers.DROWNED)),
                new CorsairModel(context.bakeLayer(ModelLayers.DROWNED_INNER_ARMOR)),
                new CorsairModel(context.bakeLayer(ModelLayers.DROWNED_OUTER_ARMOR)));
        this.addLayer(new CoatLayer(this, context.getModelSet()));
        this.addLayer(new HatLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(DrownedCorsairEntity corsair, PoseStack poseStack, float bob, float bodyRot, float partialTick,
                                  float scale) {
        super.setupRotations(corsair, poseStack, bob, bodyRot, partialTick, scale);
        float swimAmount = corsair.getSwimAmount(partialTick);
        if (swimAmount > 0.0F) {
            float rotationX = Mth.lerp(swimAmount, 0.0F, -10.0F - corsair.getXRot());
            poseStack.rotateAround(Axis.XP.rotationDegrees(rotationX), 0.0F, corsair.getBbHeight() / 2.0F / scale, 0.0F);
        }
    }

    /** Ragged coat, sash and boots on a slightly bigger copy of the body. */
    private static class CoatLayer extends RenderLayer<DrownedCorsairEntity, CorsairModel> {
        private final CorsairModel model;

        CoatLayer(RenderLayerParent<DrownedCorsairEntity, CorsairModel> parent, EntityModelSet models) {
            super(parent);
            this.model = new CorsairModel(models.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, DrownedCorsairEntity corsair, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            coloredCutoutModelCopyLayerRender(this.getParentModel(), this.model, COAT, poseStack, buffers, light, corsair, limbSwing,
                    limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
        }
    }

    /** The tricorn. */
    private static class HatLayer extends RenderLayer<DrownedCorsairEntity, CorsairModel> {
        private final CorsairModel model;

        HatLayer(RenderLayerParent<DrownedCorsairEntity, CorsairModel> parent, EntityModelSet models) {
            super(parent);
            this.model = new CorsairModel(models.bakeLayer(CorsairModel.HAT_LAYER));
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, DrownedCorsairEntity corsair, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (corsair.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                coloredCutoutModelCopyLayerRender(this.getParentModel(), this.model, HAT, poseStack, buffers, light, corsair, limbSwing,
                        limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
            }
        }
    }
}
