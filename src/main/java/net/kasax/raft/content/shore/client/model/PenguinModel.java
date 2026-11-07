package net.kasax.raft.content.shore.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.client.render.PenguinRenderState;
import net.kasax.raft.compat.render.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Penguin: round upright body, head with beak, two flippers, webbed feet and a short tail. It waddles from side to
 * side, holds its flippers out for balance, lies flat on its belly to slide and flaps the flippers to swim.
 * Texture 64x64 (adult and fluffy chick share the layout).
 */
public class PenguinModel extends EntityModel<PenguinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("penguin"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart tail;

    public PenguinModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftFlipper = this.body.getChild("left_flipper");
        this.rightFlipper = this.body.getChild("right_flipper");
        this.leftFoot = this.body.getChild("left_foot");
        this.rightFoot = this.body.getChild("right_foot");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -9.0F, -3.0F, 7.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 15).addBox(-3.0F, -5.0F, -2.5F, 6.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -9.0F, -0.5F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(22, 15).addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -2.5F, 0.1F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_flipper",
                CubeListBuilder.create().texOffs(26, 0).addBox(0.0F, 0.0F, -1.5F, 1.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(3.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        body.addOrReplaceChild("right_flipper",
                CubeListBuilder.create().texOffs(26, 0).mirror().addBox(-1.0F, 0.0F, -1.5F, 1.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(-3.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        body.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(1.8F, 0.0F, -1.0F, 0.0F, -0.2F, 0.0F));
        body.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(34, 0).mirror().addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(-1.8F, 0.0F, -1.0F, 0.0F, 0.2F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(34, 5).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, 0.5F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PenguinRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float lie = Math.max(state.slide, state.swim);
        float stand = 1.0F - lie;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.8F) * stand;

        // waddle: rock from foot to foot, flippers out for balance
        float rock = Mth.sin(walk * 0.9F);
        this.body.zRot = rock * 0.22F * speed;
        this.body.y -= Mth.abs(Mth.cos(walk * 0.9F)) * 0.6F * speed;
        this.leftFoot.xRot = Mth.sin(walk * 0.9F) * 0.6F * speed;
        this.rightFoot.xRot = -Mth.sin(walk * 0.9F) * 0.6F * speed;
        this.leftFoot.y -= Math.max(0.0F, -rock) * 1.0F * speed;
        this.rightFoot.y -= Math.max(0.0F, rock) * 1.0F * speed;
        float balance = 0.35F * speed + Mth.sin(age * 0.07F + state.seed) * 0.05F;
        this.leftFlipper.zRot -= balance + Math.max(0.0F, rock) * 0.3F * speed;
        this.rightFlipper.zRot += balance + Math.max(0.0F, -rock) * 0.3F * speed;
        this.tail.yRot = rock * 0.3F * speed;

        // head looks around; idle head tilt
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(age * 0.04F + state.seed) * 0.08F * stand;

        // belly slide / swim: lies flat, head forward, flippers back, feet trailing
        if (lie > 0.0F) {
            this.body.xRot = lie * Mth.HALF_PI + state.swimPitch * state.swim;
            this.body.y -= lie * 1.0F;
            this.head.xRot += -lie * Mth.HALF_PI * 0.85F;
            this.head.yRot *= 1.0F - lie * 0.7F;
            this.leftFoot.xRot = Mth.lerp(lie, this.leftFoot.xRot, 0.9F);
            this.rightFoot.xRot = Mth.lerp(lie, this.rightFoot.xRot, 0.9F);
            float paddle = state.swim * Mth.sin(age * 0.6F) * 0.9F;
            float slideFlip = state.slide * (1.0F - state.swim);
            this.leftFlipper.zRot = Mth.lerp(lie, this.leftFlipper.zRot, -0.25F - paddle - slideFlip * 0.6F);
            this.rightFlipper.zRot = Mth.lerp(lie, this.rightFlipper.zRot, 0.25F + paddle + slideFlip * 0.6F);
            this.leftFlipper.xRot = lie * 0.2F;
            this.rightFlipper.xRot = lie * 0.2F;
            float kick = state.swim * Mth.sin(age * 0.6F + 1.5F) * 0.4F;
            this.leftFoot.xRot += kick;
            this.rightFoot.xRot -= kick;
            this.tail.xRot = Mth.lerp(lie, this.tail.xRot, 0.0F);
        }

        // the chick has a big head
        if (state.isBaby) {
            this.head.xScale = 1.35F;
            this.head.yScale = 1.35F;
            this.head.zScale = 1.35F;
        }
    }
}
