package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * 1.21.1 port helper: an entity renderer in the newer game's shape (create and extract a render state, then submit
 * it). 1.21.1 calls {@code render(...)} with the entity; this builds the state and calls {@link #submit}.
 */
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    static final CameraRenderState CAMERA = new CameraRenderState();
    private T renderingEntity;
    private float renderingYaw;
    private float renderingPartial;
    private MultiBufferSource renderingBuffers;
    private S currentState;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    public final S createRenderState(T entity, float partialTicks) {
        S state = this.createRenderState();
        this.extractRenderState(entity, state, partialTicks);
        return state;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        fillEntityState(this.entityRenderDispatcher, entity, state, partialTicks);
    }

    static void fillEntityState(net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher, Entity entity, EntityRenderState state,
                                float partialTicks) {
        state.entityType = entity.getType();
        state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.isInvisible = entity.isInvisible();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.partialTick = partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        state.isDiscrete = entity.isDiscrete();
        state.displayFireAnimation = entity.displayFireAnimation();
        state.distanceToCameraSq = dispatcher.distanceToSqr(entity);
        state.nameTag = entity.getDisplayName();
    }

    /** Draws the state; the default draws the name tag and leash like 1.21.1 does. */
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (this.renderingEntity != null) {
            super.render(this.renderingEntity, this.renderingYaw, this.renderingPartial, poseStack, collector.buffers(), state.lightCoords);
        }
    }

    @Override
    public void render(T e, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        S state = this.createRenderState();
        this.extractRenderState(e, state, partialTick);
        state.lightCoords = light;
        this.currentState = state;
        this.renderingEntity = e;
        this.renderingYaw = yaw;
        this.renderingPartial = partialTick;
        this.renderingBuffers = buffers;
        try {
            this.submit(state, poseStack, new SubmitNodeCollector(buffers), CAMERA);
        } finally {
            this.renderingEntity = null;
            this.renderingBuffers = null;
        }
    }

    protected S currentState() {
        return this.currentState;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MissingTextureAtlasSprite.getLocation();
    }

    protected AABB getBoundingBoxForCulling(T entity, float partialTicks) {
        return entity.getBoundingBoxForCulling();
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        if (!entity.shouldRender(x, y, z)) return false;
        if (entity.noCulling) return true;
        AABB box = this.getBoundingBoxForCulling(entity, 1.0F).inflate(0.5);
        if (box.hasNaN() || box.getSize() == 0.0) {
            box = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
        }
        return frustum.isVisible(box);
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius;
    }

    @Override
    protected float getShadowRadius(T entity) {
        S state = this.currentState;
        if (state == null) state = this.createRenderState(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        return this.getShadowRadius(state);
    }
}
