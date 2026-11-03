package net.kasax.raft.content.boss.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.boss.client.model.LeviathanModel;
import net.kasax.raft.content.boss.client.model.LeviathanRenderState;
import net.kasax.raft.content.boss.entity.LeviathanBody;
import net.kasax.raft.content.boss.entity.LeviathanEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders the Leviathan: the head at the entity, the six body segments and the tail along the body chain
 * ({@link LeviathanBody}), with a slow side-to-side undulation in water. Eyes, bioluminescent spots and the shackle
 * runes glow through an emissive layer that pulses faster in the later phases. When it dies, teal rays break out of
 * it and the body dissolves into light.
 */
public class LeviathanRenderer extends EntityRenderer<LeviathanEntity, LeviathanRenderState> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/leviathan/leviathan.png");
    private static final Identifier GLOW_TEXTURE = Raft.id("textures/entity/leviathan/leviathan_glow.png");
    private static final Identifier DISSOLVE_MASK = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
    private static final RenderType DYING = RenderTypes.entityCutoutDissolve(TEXTURE, DISSOLVE_MASK);
    private static final RenderType GLOW = RenderTypes.eyes(GLOW_TEXTURE);
    private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);

    private final LeviathanModel model;

    public LeviathanRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LeviathanModel(context.bakeLayer(LeviathanModel.LAYER));
        this.shadowRadius = 1.6F;
    }

    @Override
    public LeviathanRenderState createRenderState() {
        return new LeviathanRenderState();
    }

    @Override
    protected AABB getBoundingBoxForCulling(LeviathanEntity entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(15.0, 10.0, 15.0);
    }

    @Override
    public void extractRenderState(LeviathanEntity entity, LeviathanRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.jaw = entity.getJaw(partialTicks);
        state.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTicks : 0.0F;
        state.hurt = entity.hurtTime > 0 && entity.deathTime <= 0;
        state.inWater = entity.isInWater();
        state.phase = entity.getPhase();
        state.state = entity.getState();
        state.stateTicks = entity.clientStateTicks + partialTicks;
        state.speed = (float) Math.min(1.0, entity.getDeltaMovement().length() * 2.0);
        state.glow = 1.0F;

        Vec3 pivot = entity.getPosition(partialTicks).add(0.0, LeviathanEntity.PIVOT, 0.0);
        float headYaw = entity.getYRot(partialTicks);
        float headPitch = entity.getXRot(partialTicks);
        state.px[0] = 0.0F;
        state.py[0] = 0.0F;
        state.pz[0] = 0.0F;
        state.yaw[0] = headYaw;
        state.pitch[0] = headPitch;
        state.bend[0] = 0.0F;

        Vec3[] joints = new Vec3[LeviathanBody.JOINTS];
        joints[0] = entity.neckPos(partialTicks);
        Vec3 back = entity.forward(partialTicks).scale(-1.0);
        for (int i = 1; i < LeviathanBody.JOINTS; i++) {
            joints[i] = entity.body.isInitialised() ? entity.body.joint(i, partialTicks)
                    : joints[i - 1].add(back.scale(LeviathanBody.LENGTHS[i - 1]));
        }
        // a slow travelling wave along the body (only visual, stronger towards the tail)
        float t = state.ageInTicks;
        float amp = state.inWater ? 0.16F : 0.04F;
        if (state.deathTime > 0.0F) {
            amp *= Math.max(0.0F, 1.0F - state.deathTime / 40.0F);
        }
        for (int i = 2; i < LeviathanBody.JOINTS; i++) {
            Vec3 d = joints[i - 1].subtract(joints[i]);
            Vec3 side = new Vec3(-d.z, 0.0, d.x);
            if (side.lengthSqr() > 1.0E-6) {
                side = side.normalize();
                float w = Mth.sin(t * (0.13F + state.speed * 0.15F) - i * 0.85F) * amp * (i / (float) LeviathanBody.JOINTS) * 2.0F;
                joints[i] = joints[i].add(side.scale(w));
            }
        }
        float prevYaw = headYaw;
        for (int s = 0; s < LeviathanBody.JOINTS - 1; s++) {
            Vec3 front = joints[s];
            Vec3 dir = front.subtract(joints[s + 1]);
            float yaw = prevYaw;
            float pitch = 0.0F;
            double h = dir.horizontalDistance();
            if (dir.lengthSqr() > 1.0E-6) {
                if (h > 1.0E-3) {
                    yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
                }
                pitch = (float) -(Mth.atan2(dir.y, h) * Mth.RAD_TO_DEG);
            }
            int part = s + 1;
            Vec3 rel = front.subtract(pivot);
            state.px[part] = (float) rel.x;
            state.py[part] = (float) rel.y;
            state.pz[part] = (float) rel.z;
            state.yaw[part] = yaw;
            state.pitch[part] = pitch;
            state.bend[part] = Mth.wrapDegrees(yaw - prevYaw);
            prevYaw = yaw;
        }
    }

    @Override
    public void submit(LeviathanRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, (float) LeviathanEntity.PIVOT, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float death = state.deathTime;
        if (death > 50.0F) {
            float fade = Mth.clamp(1.0F - (death - 50.0F) / 38.0F, 0.0F, 1.0F);
            collector.submitModel(this.model, state, poseStack, DYING, state.lightCoords, OverlayTexture.NO_OVERLAY, ARGB.white(fade), null,
                    state.outlineColor);
        } else {
            int overlay = OverlayTexture.pack(0.0F, state.hurt);
            collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, overlay, state.outlineColor);
        }
        float speed = state.phase >= 3 ? 0.28F : state.phase == 2 ? 0.17F : 0.09F;
        float pulse = 0.78F + 0.22F * Mth.sin(state.ageInTicks * speed);
        if (state.phase >= 2) {
            pulse = Math.min(1.0F, pulse + 0.1F);
        }
        if (death > 0.0F) {
            pulse *= Mth.clamp(1.0F - (death - 60.0F) / 25.0F, 0.0F, 1.0F);
        }
        if (pulse > 0.01F) {
            collector.submitModel(this.model, state, poseStack, GLOW, state.lightCoords, OverlayTexture.NO_OVERLAY,
                    ARGB.colorFromFloat(1.0F, pulse, pulse, pulse), null, state.outlineColor);
        }
        poseStack.popPose();
        if (death > 0.0F && death < 80.0F) {
            poseStack.pushPose();
            poseStack.translate(0.0F, (float) LeviathanEntity.PIVOT, 0.0F);
            submitRays(poseStack, Math.min(1.0F, death / 72.0F), collector);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    /** Light rays bursting out of the dying Leviathan (teal version of the dragon's death rays). */
    private static void submitRays(PoseStack poseStack, float progress, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, RenderTypes.dragonRays(), (pose, buffer) -> {
            float overDrive = Math.min(progress > 0.8F ? (progress - 0.8F) / 0.2F : 0.0F, 1.0F);
            int inner = ARGB.colorFromFloat(1.0F - overDrive * 0.6F, 0.75F, 1.0F, 0.97F);
            int outer = ARGB.color(0, 0x2FE0D0);
            RandomSource random = RandomSource.createThreadLocalInstance(1912L);
            Vector3f origin = new Vector3f();
            Vector3f left = new Vector3f();
            Vector3f right = new Vector3f();
            Vector3f bottom = new Vector3f();
            Quaternionf rot = new Quaternionf();
            int count = Mth.floor((progress + progress * progress) / 2.0F * 50.0F);
            for (int i = 0; i < count; i++) {
                rot.rotationXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI)
                        .rotateXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI,
                                random.nextFloat() * Mth.TWO_PI + progress * Mth.HALF_PI);
                pose.rotate(rot);
                float length = random.nextFloat() * 9.0F + 3.0F + overDrive * 6.0F;
                float width = random.nextFloat() * 1.2F + 0.6F + overDrive * 1.2F;
                left.set(-HALF_SQRT_3 * width, length, -0.5F * width);
                right.set(HALF_SQRT_3 * width, length, -0.5F * width);
                bottom.set(0.0F, length, width);
                buffer.addVertex(pose, origin).setColor(inner);
                buffer.addVertex(pose, left).setColor(outer);
                buffer.addVertex(pose, right).setColor(outer);
                buffer.addVertex(pose, origin).setColor(inner);
                buffer.addVertex(pose, right).setColor(outer);
                buffer.addVertex(pose, bottom).setColor(outer);
                buffer.addVertex(pose, origin).setColor(inner);
                buffer.addVertex(pose, bottom).setColor(outer);
                buffer.addVertex(pose, left).setColor(outer);
            }
        });
    }
}
