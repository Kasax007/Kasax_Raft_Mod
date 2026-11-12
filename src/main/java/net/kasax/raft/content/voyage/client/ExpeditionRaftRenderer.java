package net.kasax.raft.content.voyage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.voyage.ExpeditionRaft;
import net.kasax.raft.content.voyage.RaftUpgrade;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.AABB;

/** Renders the Expedition Raft: the model, the glowing lantern flames and the water mask over the deck. */
public class ExpeditionRaftRenderer extends EntityRenderer<ExpeditionRaft, ExpeditionRaftRenderState> {
    private static final Identifier TEXTURE = net.kasax.raft.Raft.id("textures/entity/expedition_raft.png");
    private static final Identifier GLOW = net.kasax.raft.Raft.id("textures/entity/expedition_raft_glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private final ExpeditionRaftModel model;
    private final Model.Simple waterMask;

    public ExpeditionRaftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.9F;
        this.model = new ExpeditionRaftModel(context.bakeLayer(ExpeditionRaftModel.LAYER));
        this.waterMask = new Model.Simple(context.bakeLayer(ExpeditionRaftModel.WATER_MASK), texture -> RenderTypes.waterMask());
    }

    @Override
    public void submit(ExpeditionRaftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.6F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 20.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (RaftUpgrade.LANTERN.in(state.upgrades)) {
            collector.order(1).submitModel(this.model, state, poseStack, RenderTypes.eyes(GLOW), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    state.outlineColor);
        }
        if (!state.isUnderWater) {
            collector.submitModel(this.waterMask, Unit.INSTANCE, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY,
                    state.outlineColor);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public ExpeditionRaftRenderState createRenderState() {
        return new ExpeditionRaftRenderState();
    }

    @Override
    public void extractRenderState(ExpeditionRaft entity, ExpeditionRaftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
        state.isUnderWater = entity.isUnderWater();
        state.inWater = entity.isInWater();
        state.upgrades = entity.getUpgrades();
        state.anchorDrop = entity.getAnchorDrop(partialTicks);
        state.sailFill = entity.getSailFill(partialTicks);
        state.propeller = entity.getPropellerAngle(partialTicks);
        state.turn = entity.getTurn(partialTicks);
        state.engineRunning = entity.isEngineRunning();
        state.rowing = (entity.getPaddleState(0) || entity.getPaddleState(1)) && !state.engineRunning;
        state.rowingTimeLeft = entity.getRowingTime(0, partialTicks);
        state.rowingTimeRight = entity.getRowingTime(1, partialTicks);
    }

    /** The mast, lanterns and the dropped anchor reach far beyond the low hitbox. */
    @Override
    protected AABB getBoundingBoxForCulling(ExpeditionRaft entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
        return new AABB(box.minX - 0.6, box.minY - 3.0, box.minZ - 0.6, box.maxX + 0.6, box.maxY + 3.6, box.maxZ + 0.6);
    }
}
