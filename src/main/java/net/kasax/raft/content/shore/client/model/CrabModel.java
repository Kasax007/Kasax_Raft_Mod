package net.kasax.raft.content.shore.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.render.CrabRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;

/**
 * Crab: a wide carapace on six jointed legs, two eye stalks and two claws of different size with a moving lower
 * pincer. It scuttles sideways (the whole body turns 90 degrees while it walks), snaps its claws, raises them when
 * angry and sinks into the sand until only the eyes show. Texture 64x64.
 */
public class CrabModel extends EntityModel<CrabRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("crab"), "main");
    public static final ModelLayerLocation EMBER_LAYER = new ModelLayerLocation(Raft.id("ember_crab"), "main");

    private static final float[] LEG_Z = {-1.5F, 0.5F, 2.5F};
    private static final float[] LEG_FAN = {0.45F, 0.0F, -0.45F};
    /** Upper leg rises from the shell to a high knee, the long shin reaches out and down to the sand (crab stance). */
    private static final float LEG_RISE = 0.75F;
    private static final float LEG_KNEE = 1.88F;

    private final ModelPart body;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart rightArm;
    private final ModelPart rightClaw;
    private final ModelPart rightPincer;
    private final ModelPart leftArm;
    private final ModelPart leftClaw;
    private final ModelPart leftPincer;
    private final ModelPart[] leftUpper = new ModelPart[3];
    private final ModelPart[] leftLower = new ModelPart[3];
    private final ModelPart[] rightUpper = new ModelPart[3];
    private final ModelPart[] rightLower = new ModelPart[3];

    public CrabModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftEye = this.body.getChild("left_eye");
        this.rightEye = this.body.getChild("right_eye");
        this.rightArm = this.body.getChild("right_arm");
        this.rightClaw = this.rightArm.getChild("right_claw");
        this.rightPincer = this.rightClaw.getChild("right_pincer");
        this.leftArm = this.body.getChild("left_arm");
        this.leftClaw = this.leftArm.getChild("left_claw");
        this.leftPincer = this.leftClaw.getChild("left_pincer");
        for (int i = 0; i < 3; i++) {
            this.leftUpper[i] = this.body.getChild("left_leg" + i);
            this.leftLower[i] = this.leftUpper[i].getChild("left_shin" + i);
            this.rightUpper[i] = this.body.getChild("right_leg" + i);
            this.rightLower[i] = this.rightUpper[i].getChild("right_shin" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        return create(false);
    }

    public static LayerDefinition createEmberLayer() {
        return create(true);
    }

    private static LayerDefinition create(boolean ember) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder shell = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F)
                .texOffs(0, 12).addBox(-4.0F, -3.0F, -3.0F, 8.0F, 1.0F, 6.0F);
        if (ember) {
            // basalt spikes along the back
            shell.texOffs(48, 0).addBox(-3.0F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                    .texOffs(48, 0).addBox(2.0F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                    .texOffs(52, 0).addBox(-0.5F, -6.0F, 0.5F, 1.0F, 3.0F, 1.0F)
                    .texOffs(48, 4).addBox(-1.5F, -4.0F, -2.5F, 1.0F, 1.0F, 1.0F)
                    .texOffs(48, 4).addBox(0.5F, -4.0F, 2.0F, 1.0F, 1.0F, 1.0F);
        }
        PartDefinition body = root.addOrReplaceChild("body", shell, PartPose.offset(0.0F, 19.0F, 0.0F));

        body.addOrReplaceChild("left_eye", eyeStalk(false), PartPose.offset(1.5F, -2.5F, -3.0F));
        body.addOrReplaceChild("right_eye", eyeStalk(true), PartPose.offset(-1.5F, -2.5F, -3.0F));

        // big claw on the right
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(-3.5F, 0.5F, -3.5F, -0.25F, 0.45F, 0.0F));
        PartDefinition rightClaw = rightArm.addOrReplaceChild("right_claw",
                CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(14, 26).addBox(-1.0F, -1.5F, -7.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, -0.85F, 0.0F));
        rightClaw.addOrReplaceChild("right_pincer",
                CubeListBuilder.create().texOffs(14, 30).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 1.5F, -4.0F));

        // smaller claw on the left
        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(3.5F, 0.5F, -3.5F, -0.25F, -0.45F, 0.0F));
        PartDefinition leftClaw = leftArm.addOrReplaceChild("left_claw",
                CubeListBuilder.create()
                        .texOffs(24, 20).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F)
                        .texOffs(24, 25).addBox(-0.5F, -1.0F, -5.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, 0.85F, 0.0F));
        leftClaw.addOrReplaceChild("left_pincer",
                CubeListBuilder.create().texOffs(30, 25).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.0F, -3.0F));

        for (int i = 0; i < 3; i++) {
            PartDefinition left = body.addOrReplaceChild("left_leg" + i,
                    CubeListBuilder.create().texOffs(40, 12).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(4.5F, 0.5F, LEG_Z[i], 0.0F, LEG_FAN[i], -LEG_RISE));
            left.addOrReplaceChild("left_shin" + i,
                    CubeListBuilder.create().texOffs(40, 14).addBox(0.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, LEG_KNEE));
            PartDefinition right = body.addOrReplaceChild("right_leg" + i,
                    CubeListBuilder.create().texOffs(40, 12).mirror().addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-4.5F, 0.5F, LEG_Z[i], 0.0F, -LEG_FAN[i], LEG_RISE));
            right.addOrReplaceChild("right_shin" + i,
                    CubeListBuilder.create().texOffs(40, 14).mirror().addBox(-8.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -LEG_KNEE));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder eyeStalk(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(36, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(40, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 2.0F, 2.0F);
    }

    private static float seconds(AnimationState anim, float ageInTicks) {
        return anim.isStarted() ? anim.getTimeInMillis(ageInTicks) / 1000.0F : -1.0F;
    }

    @Override
    public void setupAnim(CrabRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.6F);

        // scuttle sideways: the right side leads while walking
        this.body.yRot = state.sideways * Mth.HALF_PI;
        this.body.y += Mth.sin(walk * 2.4F) * 0.35F * speed;
        this.body.zRot = Mth.sin(walk * 1.2F) * 0.04F * speed;

        for (int i = 0; i < 3; i++) {
            float phase = walk * 1.2F + i * 2.1F;
            float lift = Mth.sin(phase) * 0.45F * speed;
            float swing = Mth.cos(phase) * 0.3F * speed;
            this.leftUpper[i].zRot -= Math.max(0.0F, lift);
            this.leftUpper[i].yRot += swing;
            this.leftLower[i].zRot += Math.max(0.0F, lift) * 0.4F;
            float rphase = phase + Mth.PI;
            float rlift = Mth.sin(rphase) * 0.45F * speed;
            float rswing = Mth.cos(rphase) * 0.3F * speed;
            this.rightUpper[i].zRot += Math.max(0.0F, rlift);
            this.rightUpper[i].yRot -= rswing;
            this.rightLower[i].zRot -= Math.max(0.0F, rlift) * 0.4F;
        }

        // breathing and idle claw sway
        float breathe = Mth.sin(age * 0.08F + state.seed) * 0.05F;
        this.rightArm.xRot += breathe;
        this.leftArm.xRot -= breathe;
        this.rightClaw.zRot = Mth.sin(age * 0.05F + state.seed) * 0.06F;
        this.leftClaw.zRot = -Mth.sin(age * 0.06F + state.seed * 1.3F) * 0.06F;

        // eye stalks twitch and follow the head yaw a little
        float look = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD * 0.4F;
        this.leftEye.yRot = look + Mth.sin(age * 0.07F + state.seed) * 0.12F;
        this.rightEye.yRot = look + Mth.sin(age * 0.09F + state.seed * 2.0F) * 0.12F;
        this.leftEye.zRot = 0.12F + Mth.sin(age * 0.11F + state.seed) * 0.06F;
        this.rightEye.zRot = -0.12F - Mth.sin(age * 0.13F + state.seed) * 0.06F;

        float rightOpen = 0.0F;
        float leftOpen = 0.0F;
        if (state.threatening) {
            float raise = 0.85F;
            this.rightArm.xRot -= raise;
            this.leftArm.xRot -= raise;
            this.rightArm.yRot += 0.2F;
            this.leftArm.yRot -= 0.2F;
            this.rightClaw.xRot -= 0.5F;
            this.leftClaw.xRot -= 0.5F;
            rightOpen = 0.35F + Mth.sin(age * 0.7F) * 0.3F;
            leftOpen = 0.35F + Mth.sin(age * 0.7F + 1.3F) * 0.3F;
        }

        float snap = seconds(state.snap, age);
        if (snap >= 0.0F && snap < 0.45F) {
            float s = Mth.sin(snap / 0.45F * Mth.PI);
            rightOpen += s * 0.7F;
            leftOpen += Mth.sin(Math.max(0.0F, snap - 0.1F) / 0.35F * Mth.PI) * 0.6F * (snap > 0.1F ? 1.0F : 0.0F);
        }

        float pinch = seconds(state.pinch, age);
        if (pinch >= 0.0F && pinch < 0.5F) {
            float p = pinch / 0.5F;
            float reach = Mth.sin(p * Mth.PI);
            // the big claw lunges forward from the raised threat pose (or up from rest) and snaps shut
            float lunge = state.threatening ? 0.75F : -0.6F;
            this.rightArm.xRot += reach * lunge;
            this.rightArm.yRot -= reach * 0.35F;
            this.rightClaw.yRot += reach * 0.5F;
            this.rightClaw.xRot += state.threatening ? reach * 0.4F : 0.0F;
            this.leftArm.xRot += reach * lunge * 0.4F;
            this.body.xRot = reach * 0.12F;
            rightOpen += p < 0.55F ? 0.8F * (p / 0.55F) : 0.8F * (1.0F - (p - 0.55F) / 0.45F) - 0.1F;
        }
        this.rightPincer.xRot = Math.max(0.0F, rightOpen);
        this.leftPincer.xRot = Math.max(0.0F, leftOpen);

        // dug into the sand: only the eye stalks stay above the ground and keep watch
        if (state.burrow > 0.0F) {
            float b = state.burrow;
            this.body.y += b * 8.5F;
            this.body.yRot *= 1.0F - b;
            this.leftEye.xRot -= b * 0.15F;
            this.rightEye.xRot -= b * 0.15F;
            this.leftEye.yRot += b * Mth.sin(age * 0.05F) * 0.5F;
            this.rightEye.yRot += b * Mth.sin(age * 0.05F + 0.4F) * 0.5F;
            for (int i = 0; i < 3; i++) {
                this.leftUpper[i].zRot += b * 0.5F;
                this.rightUpper[i].zRot -= b * 0.5F;
            }
        }
    }
}
