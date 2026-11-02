package net.kasax.raft.content.shore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.model.CorsairModel;
import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Drowned corsair: drowned body with its own skin, a ragged pirate coat (outer layer) and a tricorn hat with a
 * feather (own model layer). The hat is hidden when the corsair wears something on its head.
 */
public class CorsairRenderer extends AbstractZombieRenderer<DrownedCorsairEntity, ZombieRenderState, CorsairModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/drowned_corsair/drowned_corsair.png");
    private static final Identifier COAT = Raft.id("textures/entity/drowned_corsair/drowned_corsair_coat.png");
    private static final Identifier HAT = Raft.id("textures/entity/drowned_corsair/drowned_corsair_hat.png");

    public CorsairRenderer(EntityRendererProvider.Context context) {
        this(context, new CorsairModel(context.bakeLayer(ModelLayers.DROWNED)),
                ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), CorsairModel::new));
    }

    private CorsairRenderer(EntityRendererProvider.Context context, CorsairModel model, ArmorModelSet<CorsairModel> armor) {
        super(context, model, model, armor, armor);
        this.addLayer(new CoatLayer(this, context.getModelSet()));
        this.addLayer(new HatLayer(this, context.getModelSet()));
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(ZombieRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float swimAmount = state.swimAmount;
        if (swimAmount > 0.0F) {
            float rotationX = Mth.lerp(swimAmount, 0.0F, -10.0F - state.xRot);
            poseStack.rotateAround(Axis.XP.rotationDegrees(rotationX), 0.0F, state.boundingBoxHeight / 2.0F / entityScale, 0.0F);
        }
    }

    /** Ragged coat, sash and boots on a slightly bigger copy of the body. */
    private static class CoatLayer extends RenderLayer<ZombieRenderState, CorsairModel> {
        private final CorsairModel model;

        CoatLayer(RenderLayerParent<ZombieRenderState, CorsairModel> parent, EntityModelSet models) {
            super(parent);
            this.model = new CorsairModel(models.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ZombieRenderState state,
                           float yRot, float xRot) {
            coloredCutoutModelCopyLayerRender(this.model, COAT, poseStack, collector, lightCoords, state, -1, 1);
        }
    }

    /** The tricorn. */
    private static class HatLayer extends RenderLayer<ZombieRenderState, CorsairModel> {
        private final CorsairModel model;

        HatLayer(RenderLayerParent<ZombieRenderState, CorsairModel> parent, EntityModelSet models) {
            super(parent);
            this.model = new CorsairModel(models.bakeLayer(CorsairModel.HAT_LAYER));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ZombieRenderState state,
                           float yRot, float xRot) {
            if (state.headEquipment.isEmpty() && state.headItem.isEmpty() && state.wornHeadType == null) {
                coloredCutoutModelCopyLayerRender(this.model, HAT, poseStack, collector, lightCoords, state, -1, 2);
            }
        }
    }
}
