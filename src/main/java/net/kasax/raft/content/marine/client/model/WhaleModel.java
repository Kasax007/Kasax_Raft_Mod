package net.kasax.raft.content.marine.client.model;

import net.kasax.raft.Raft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The whale (humpback-like): a long body with a knobbly head, a pleated lower jaw, long white-edged flippers and a
 * three-segment tail stock ending in broad flukes. It swims with slow up-and-down strokes travelling down the tail;
 * the flippers sway, the jaw opens while it sings and the head lifts when it spouts. Rendered at twice the model size
 * (texture 128x128).
 */
public class WhaleModel extends EntityModel<WhaleRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("whale"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart leftFluke;
    private final ModelPart rightFluke;

    public WhaleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftFlipper = this.body.getChild("left_flipper");
        this.rightFlipper = this.body.getChild("right_flipper");
        this.tail1 = this.body.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.leftFluke = this.tail3.getChild("left_fluke");
        this.rightFluke = this.tail3.getChild("right_fluke");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, -9.0F, -9.0F, 20.0F, 18.0F, 20.0F)
                .texOffs(88, 62).addBox(-1.0F, -11.0F, 6.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 38).addBox(-9.0F, -7.0F, -8.0F, 18.0F, 12.0F, 8.0F)
                .texOffs(100, 62).addBox(2.0F, -8.0F, -7.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(100, 62).addBox(-3.0F, -8.0F, -4.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(100, 62).addBox(-4.0F, -7.0F, -11.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, -1.0F, -9.0F));
        // the rostrum: the head narrows towards the tip of the snout
        head.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(52, 71).addBox(-7.0F, -6.0F, -12.0F, 14.0F, 11.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(60, 38).addBox(-9.5F, 0.0F, -7.0F, 19.0F, 6.0F, 8.0F)
                .texOffs(88, 71).addBox(-7.5F, 0.0F, -11.0F, 15.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 5.0F, -1.0F));
        body.addOrReplaceChild("left_flipper", CubeListBuilder.create()
                .texOffs(0, 79).addBox(0.0F, -1.0F, -3.0F, 20.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(9.0F, 6.0F, -5.0F, 0.0F, -0.4F, 0.6F));
        body.addOrReplaceChild("right_flipper", CubeListBuilder.create()
                .texOffs(0, 79).mirror().addBox(-20.0F, -1.0F, -3.0F, 20.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(-9.0F, 6.0F, -5.0F, 0.0F, 0.4F, -0.6F));
        PartDefinition tail1 = body.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(80, 0).addBox(-7.0F, -7.0F, 0.0F, 14.0F, 14.0F, 10.0F),
                PartPose.offset(0.0F, -1.0F, 11.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(0, 62).addBox(-4.0F, -4.5F, 0.0F, 8.0F, 9.0F, 8.0F),
                PartPose.offset(0.0F, 0.5F, 10.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                .texOffs(32, 62).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));
        tail3.addOrReplaceChild("left_fluke", CubeListBuilder.create()
                .texOffs(50, 62).addBox(0.0F, -1.0F, -2.0F, 12.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(1.0F, 0.0F, 3.5F, 0.0F, -0.3F, 0.0F));
        tail3.addOrReplaceChild("right_fluke", CubeListBuilder.create()
                .texOffs(50, 62).mirror().addBox(-12.0F, -1.0F, -2.0F, 12.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 3.5F, 0.0F, 0.3F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(WhaleRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float move = Mth.clamp(state.walkAnimationSpeed * 6.0F, 0.35F, 1.0F);
        float p = t * (0.07F + 0.03F * move);
        float a = 0.07F + 0.07F * move;
        if (!state.isInWater) {
            p = t * 0.05F;
            a = 0.05F;
        }
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD + Mth.sin(p + 1.4F) * a * 0.12F;
        this.head.xRot = -Mth.sin(p + 0.5F) * a * 0.15F - state.spout * 0.06F;
        this.tail1.xRot = Mth.sin(p) * a * 0.7F;
        this.tail2.xRot = Mth.sin(p - 0.7F) * a * 1.2F;
        this.tail3.xRot = Mth.sin(p - 1.4F) * a * 1.7F;
        float fluke = Mth.sin(p - 2.1F) * a * 2.4F;
        this.leftFluke.xRot = fluke;
        this.rightFluke.xRot = fluke;
        // long flippers: slow sway, a little out of phase
        float sway = Mth.sin(t * 0.045F) * 0.14F;
        this.leftFlipper.zRot += sway;
        this.rightFlipper.zRot -= sway;
        this.leftFlipper.xRot = Mth.sin(t * 0.045F + 1.0F) * 0.08F;
        this.rightFlipper.xRot = Mth.sin(t * 0.045F + 1.0F) * 0.08F;
        // the song: the jaw opens and trembles
        this.jaw.xRot = 0.02F + state.song * (0.16F + Mth.sin(t * 0.35F) * 0.04F);
    }
}
