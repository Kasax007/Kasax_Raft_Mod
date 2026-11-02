package net.kasax.raft.content.shore.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.render.SeagullRenderState;
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
 * Seagull: body, head with an opening beak, tail, two-part wings and legs. On the ground the wings lie folded along
 * the body with the black tips crossed over the tail; in the air they spread, flap with a lagging wing tip, glide in a
 * shallow V and bank into turns. Texture 64x64.
 */
public class SeagullModel extends EntityModel<SeagullRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("seagull"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart lowerBeak;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart leftTip;
    private final ModelPart rightWing;
    private final ModelPart rightTip;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public SeagullModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.lowerBeak = this.head.getChild("lower_beak");
        this.tail = this.body.getChild("tail");
        this.leftWing = this.body.getChild("left_wing");
        this.leftTip = this.leftWing.getChild("left_wing_tip");
        this.rightWing = this.body.getChild("right_wing");
        this.rightTip = this.rightWing.getChild("right_wing_tip");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -3.5F, -3.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, -1.5F, -3.5F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -3.0F));
        head.addOrReplaceChild("lower_beak",
                CubeListBuilder.create().texOffs(40, 4).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, -1.5F, -3.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -0.5F, 0.0F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, -0.15F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -0.5F, 0.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(2.5F, 1.5F, -2.5F));
        leftWing.addOrReplaceChild("left_wing_tip",
                CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, -0.5F, 0.0F, 6.0F, 1.0F, 3.0F),
                PartPose.offset(5.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-5.0F, -0.5F, 0.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(-2.5F, 1.5F, -2.5F));
        rightWing.addOrReplaceChild("right_wing_tip",
                CubeListBuilder.create().texOffs(0, 21).mirror().addBox(-6.0F, -0.5F, 0.0F, 6.0F, 1.0F, 3.0F),
                PartPose.offset(-5.0F, 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(26, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(30, 7).addBox(-1.0F, 2.0F, -2.0F, 2.0F, 1.0F, 2.0F);
        body.addOrReplaceChild("left_leg", leg, PartPose.offset(1.2F, 2.0F, 0.5F));
        body.addOrReplaceChild("right_leg", CubeListBuilder.create().mirror()
                .texOffs(26, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(30, 7).addBox(-1.0F, 2.0F, -2.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(-1.2F, 2.0F, 0.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static float seconds(AnimationState anim, float ageInTicks) {
        return anim.isStarted() ? anim.getTimeInMillis(ageInTicks) / 1000.0F : -1.0F;
    }

    @Override
    public void setupAnim(SeagullRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float fly = state.flyAmount;
        float ground = 1.0F - fly;
        float walk = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 2.0F) * ground;

        // body: chest up while standing, pitched with the flight path and banked into turns in the air
        this.body.xRot = -0.18F * ground + state.flightPitch * fly;
        this.body.zRot = state.roll * Mth.DEG_TO_RAD * fly;
        if (state.floating) {
            this.body.y += 2.2F + Mth.sin(age * 0.12F + state.seed) * 0.35F;
            this.body.xRot = -0.05F;
        }

        // head: looks around, kept level in flight, bobs while walking
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * (1.0F - fly * 0.6F);
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F - this.body.xRot * 0.8F;
        this.head.z += Mth.sin(walk * 1.3F) * 0.6F * speed;

        // wings: folded <-> spread
        float flap = Mth.sin(state.flapPhase) * (0.25F + 0.75F * state.flapPower);
        float tipLag = Mth.sin(state.flapPhase - 0.7F) * (0.2F + 0.5F * state.flapPower);
        float glideV = (1.0F - state.flapPower) * -0.18F;
        float spreadZ = (flap * 0.95F + glideV) * fly;
        this.leftWing.xRot = Mth.HALF_PI * ground;
        this.leftWing.yRot = -Mth.HALF_PI * ground;
        this.leftWing.zRot = -spreadZ;
        this.rightWing.xRot = Mth.HALF_PI * ground;
        this.rightWing.yRot = Mth.HALF_PI * ground;
        this.rightWing.zRot = spreadZ;
        this.leftWing.y = Mth.lerp(fly, 1.5F, -1.0F);
        this.rightWing.y = this.leftWing.y;
        this.leftWing.x = Mth.lerp(fly, 2.9F, 2.0F);
        this.rightWing.x = -this.leftWing.x;
        this.leftTip.zRot = -tipLag * fly + 0.12F * fly;
        this.rightTip.zRot = tipLag * fly - 0.12F * fly;
        // resting wings twitch now and then
        float shuffle = Math.max(0.0F, Mth.sin(age * 0.05F + state.seed) - 0.92F) * 2.0F * ground;
        this.leftWing.zRot -= shuffle;
        this.rightWing.zRot += shuffle;

        // tail: fans in flight, wags on the ground
        this.tail.xRot += -0.1F * fly + Mth.sin(age * 0.1F) * 0.04F;
        this.tail.yRot = Mth.sin(walk * 1.3F) * 0.15F * speed;

        // legs: walk on the ground, tucked back in the air, paddle when floating
        float legSwing = Mth.cos(walk * 1.3F) * 0.9F * speed;
        this.leftLeg.xRot = legSwing + 1.25F * fly - this.body.xRot * ground;
        this.rightLeg.xRot = -legSwing + 1.25F * fly - this.body.xRot * ground;
        if (state.floating) {
            this.leftLeg.xRot = 0.6F + Mth.sin(age * 0.3F) * 0.5F;
            this.rightLeg.xRot = 0.6F - Mth.sin(age * 0.3F) * 0.5F;
        }

        // peck at a crop (or food)
        float peck = seconds(state.peck, age);
        if (peck >= 0.0F && peck < 0.5F) {
            float p = Mth.sin(peck / 0.5F * Mth.PI);
            this.body.xRot += p * 0.45F;
            this.head.xRot += p * 1.0F;
        }
        // call: head thrown back, beak wide open
        float call = seconds(state.call, age);
        float beak = 0.0F;
        if (call >= 0.0F && call < 0.9F) {
            float c = Mth.sin(call / 0.9F * Mth.PI);
            this.head.xRot -= c * 0.7F;
            beak = c * 0.7F * (0.6F + 0.4F * Mth.abs(Mth.sin(call * 18.0F)));
        }
        if (state.holdsItem) {
            beak = Math.max(beak, 0.25F);
        }
        this.lowerBeak.xRot = beak;
    }

    /** Moves the pose stack to the tip of the beak (for the carried food). */
    public void translateToBeak(PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, -1.2F / 16.0F, -4.5F / 16.0F);
    }
}
