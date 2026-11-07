package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;

/**
 * 1.21.1 port helper: a living entity renderer in the newer game's shape. Each frame it extracts a render state,
 * hands it to the model (which poses itself from it) and to the layers, and lets 1.21.1's renderer do the drawing.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends net.minecraft.client.renderer.entity.LivingEntityRenderer implements RenderLayerParent<S, M> {
    protected final ItemModelResolver itemModelResolver = new ItemModelResolver();
    private S currentState;
    private T currentEntity;
    private float currentBob;
    private float currentPartial;

    protected LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    public abstract S createRenderState();

    public abstract ResourceLocation getTextureLocation(S state);

    @Override
    public M getModel() {
        return (M) super.getModel();
    }

    @Override
    public S currentRenderState() {
        return this.currentState;
    }

    protected final boolean addLayer(RenderLayer<S, M> layer) {
        return super.addLayer(layer);
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        EntityRenderer.fillEntityState(this.entityRenderDispatcher, entity, state, partialTicks);
        float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
        float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
            bodyRot = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
            float diff = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -85.0F, 85.0F);
            bodyRot = headRot - diff;
            if (diff * diff > 2500.0F) bodyRot += diff * 0.2F;
        }
        state.bodyRot = bodyRot;
        state.yRot = Mth.wrapDegrees(headRot - bodyRot);
        state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.isUpsideDown = isEntityUpsideDown(entity);
        if (state.isUpsideDown) {
            state.xRot *= -1.0F;
            state.yRot *= -1.0F;
        }
        if (!entity.isPassenger() && entity.isAlive()) {
            state.walkAnimationPos = entity.walkAnimation.position(partialTicks);
            state.walkAnimationSpeed = entity.walkAnimation.speed(partialTicks);
        } else {
            state.walkAnimationPos = 0.0F;
            state.walkAnimationSpeed = 0.0F;
        }
        state.scale = entity.getScale();
        state.ageScale = entity.getAgeScale();
        state.pose = entity.getPose();
        state.bedOrientation = entity.getBedOrientation();
        if (state.bedOrientation != null) state.eyeHeight = entity.getEyeHeight(Pose.STANDING);
        state.isFullyFrozen = entity.isFullyFrozen();
        state.isBaby = entity.isBaby();
        state.isInWater = entity.isInWater();
        state.isAutoSpinAttack = entity.isAutoSpinAttack();
        state.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
        state.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTicks : 0.0F;
        Minecraft minecraft = Minecraft.getInstance();
        state.isInvisibleToPlayer = state.isInvisible && entity.isInvisibleTo(minecraft.player);
    }

    private S stateFor(Entity entity) {
        if (this.currentState != null && this.currentEntity == entity) return this.currentState;
        S state = this.createRenderState();
        this.extractRenderState((T) entity, state, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        return state;
    }

    @Override
    public void render(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        S state = this.createRenderState();
        this.extractRenderState((T) entity, state, partialTick);
        state.lightCoords = light;
        this.currentState = state;
        this.currentEntity = (T) entity;
        this.currentPartial = partialTick;
        this.getModel().pendingState = state;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    protected void setupRotations(S state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(this.currentEntity, poseStack, this.currentBob, bodyRot, this.currentPartial, entityScale);
    }

    @Override
    protected final void setupRotations(LivingEntity entity, PoseStack poseStack, float bob, float bodyRot, float partialTick, float scale) {
        this.currentBob = bob;
        this.currentPartial = partialTick;
        this.setupRotations(this.stateFor(entity), poseStack, bodyRot, scale);
    }

    protected void scale(S state, PoseStack poseStack) {
    }

    @Override
    protected final void scale(LivingEntity entity, PoseStack poseStack, float partialTick) {
        this.scale(this.stateFor(entity), poseStack);
    }

    @Override
    public final ResourceLocation getTextureLocation(Entity entity) {
        return this.getTextureLocation(this.stateFor(entity));
    }

    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected final int getBlockLightLevel(Entity entity, BlockPos pos) {
        return this.getBlockLightLevel((T) entity, pos);
    }

    protected AABB getBoundingBoxForCulling(T entity, float partialTicks) {
        return entity.getBoundingBoxForCulling();
    }

    @Override
    public boolean shouldRender(Entity entity, Frustum frustum, double x, double y, double z) {
        if (!entity.shouldRender(x, y, z)) return false;
        if (entity.noCulling) return true;
        AABB box = this.getBoundingBoxForCulling((T) entity, 1.0F).inflate(0.5);
        if (box.hasNaN() || box.getSize() == 0.0) {
            box = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
        }
        return frustum.isVisible(box);
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return super.shouldShowName(entity);
    }

    @Override
    protected final boolean shouldShowName(LivingEntity entity) {
        return this.shouldShowName((T) entity, this.entityRenderDispatcher.distanceToSqr(entity));
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius * state.scale * state.ageScale;
    }

    @Override
    protected final float getShadowRadius(LivingEntity entity) {
        return this.getShadowRadius(this.stateFor(entity));
    }
}
