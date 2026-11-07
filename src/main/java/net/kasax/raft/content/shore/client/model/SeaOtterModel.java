package net.kasax.raft.content.shore.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.render.SeaOtterRenderState;
import net.kasax.raft.compat.render.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.kasax.raft.compat.render.AnimState;

/**
 * Sea otter: long body, round head with snout and ears, small front paws, webbed hind feet and a thick tail. Floating
 * on its back it holds the paws on its chest, sticks the hind feet out of the water and bobs on the waves; cracking a
 * shell it bangs a stone on the shell lying on its belly. Under water it swims belly-down with an undulating body.
 * Texture 64x64.
 */
public class SeaOtterModel extends EntityModel<SeaOtterRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("sea_otter"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;
    private final ModelPart stone;
    private final ModelPart shell;

    public SeaOtterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
        this.tail = this.body.getChild("tail");
        this.stone = this.rightArm.getChild("stone");
        this.shell = this.body.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 19.5F, 0.0F));
        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 4.0F, 4.0F)
                        .texOffs(18, 15).addBox(-1.5F, -1.0F, -6.0F, 3.0F, 2.0F, 2.0F)
                        .texOffs(28, 15).addBox(-2.5F, -3.5F, -1.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(28, 15).mirror().addBox(1.5F, -3.5F, -1.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -5.0F));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 1.5F, -3.5F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 1.5F, -3.5F));
        rightArm.addOrReplaceChild("stone",
                CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.8F, 3.2F, 0.0F));
        body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(2.0F, 1.5F, 3.5F));
        body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(-2.0F, 1.5F, 3.5F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 6).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 5.0F, -0.1F, 0.0F, 0.0F));
        body.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(52, 4).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 2.5F, -1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static float seconds(AnimState anim, float ageInTicks) {
        return anim.isStarted() ? anim.getTimeInMillis(ageInTicks) / 1000.0F : -1.0F;
    }

    @Override
    public void setupAnim(SeaOtterRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float back = state.back;
        float swim = state.swim * (1.0F - back);
        float land = state.isInWater ? 0.0F : 1.0F;
        float walk = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.8F);

        // head looks around (upright again while on the back)
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // walking on land: a bounding quadruped gait
        float gait = Mth.cos(walk * 0.9F) * 1.0F * speed * land;
        this.leftArm.xRot = gait;
        this.rightArm.xRot = -gait;
        this.leftLeg.xRot = -gait;
        this.rightLeg.xRot = gait;
        this.body.xRot = Mth.sin(walk * 0.9F) * 0.06F * speed * land;

        // swimming belly-down: the whole body undulates, hind feet push, arms tucked
        if (swim > 0.0F) {
            float wave = Mth.sin(age * 0.45F);
            this.body.xRot += state.swimPitch * swim + wave * 0.06F * swim;
            this.tail.xRot += Mth.sin(age * 0.45F - 1.2F) * 0.45F * swim;
            this.head.xRot -= wave * 0.08F * swim;
            this.leftLeg.xRot = Mth.lerp(swim, this.leftLeg.xRot, 1.3F + Mth.sin(age * 0.45F) * 0.5F);
            this.rightLeg.xRot = Mth.lerp(swim, this.rightLeg.xRot, 1.3F + Mth.sin(age * 0.45F) * 0.5F);
            this.leftArm.xRot = Mth.lerp(swim, this.leftArm.xRot, 1.2F);
            this.rightArm.xRot = Mth.lerp(swim, this.rightArm.xRot, 1.2F);
        }

        // floating on the back
        if (back > 0.0F) {
            float bob = Mth.sin(age * 0.1F + state.seed);
            this.body.zRot = Mth.PI * back;
            this.body.xRot += bob * 0.04F * back;
            this.body.y += bob * 0.4F * back;
            // keep the head upright and looking along the body
            this.head.zRot = -Mth.PI * back;
            this.head.xRot = Mth.lerp(back, this.head.xRot, 0.35F + bob * 0.05F);
            this.head.yRot *= 1.0F - back * 0.5F;
            this.head.y -= 1.5F * back;
            // paws folded on the chest, hind feet poking out of the water
            this.leftArm.xRot = Mth.lerp(back, this.leftArm.xRot, -1.3F);
            this.rightArm.xRot = Mth.lerp(back, this.rightArm.xRot, -1.3F);
            this.leftArm.zRot = 0.3F * back;
            this.rightArm.zRot = -0.3F * back;
            float paddle = Mth.sin(age * 0.15F + state.seed) * 0.2F;
            this.leftLeg.xRot = Mth.lerp(back, this.leftLeg.xRot, 0.5F + paddle);
            this.rightLeg.xRot = Mth.lerp(back, this.rightLeg.xRot, 0.5F - paddle);
            this.tail.xRot += 0.15F * back + Mth.sin(age * 0.08F) * 0.08F * back;
        }

        // cracking a shell with a stone on the belly
        float crack = seconds(state.crack, age);
        boolean cracking = crack >= 0.0F && crack < 3.0F;
        this.stone.visible = cracking;
        this.shell.visible = cracking || state.carrying;
        if (cracking) {
            float hit = Mth.abs(Mth.sin(crack * Mth.PI * 1.65F));
            this.rightArm.xRot = -1.9F + hit * 0.9F;
            this.leftArm.xRot = -1.9F + hit * 0.9F;
            this.rightArm.zRot = -0.15F;
            this.leftArm.zRot = 0.15F;
            this.head.xRot += 0.2F;
        }
    }
}
