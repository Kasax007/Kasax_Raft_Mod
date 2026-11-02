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
 * The anglerfish: a big rounded head-body with an underbite jaw full of fangs, a two-part fishing rod on the forehead
 * with a glowing bulb (drawn full-bright by the renderer's glow layer), small fins and a short tail. It hovers with
 * a gentle bob, the lure swaying; the jaw gapes on a bite. Texture 64x64.
 */
public class AnglerfishModel extends EntityModel<AnglerfishRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("anglerfish"), "main");

    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart lure;
    private final ModelPart lureTip;
    private final ModelPart bulb;
    private final ModelPart tail;
    private final ModelPart tailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public AnglerfishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.lure = this.body.getChild("lure");
        this.lureTip = this.lure.getChild("lure_tip");
        this.bulb = this.lureTip.getChild("bulb");
        this.tail = this.body.getChild("tail");
        this.tailFin = this.tail.getChild("tail_fin");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -4.0F, -5.0F, 9.0F, 8.0F, 10.0F)
                .texOffs(54, 18).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(4, 27).addBox(-3.5F, 3.5F, -5.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(4, 27).addBox(2.5F, 3.5F, -5.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(8, 27).addBox(-1.5F, 3.5F, -5.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(12, 27).addBox(0.5F, 3.5F, -5.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        body.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-4.5F, 0.0F, -5.5F, 9.0F, 3.0F, 6.0F)
                .texOffs(4, 27).addBox(-4.0F, -2.0F, -5.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(4, 27).addBox(3.0F, -2.0F, -5.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(4, 27).addBox(-2.0F, -2.5F, -5.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(4, 27).addBox(1.0F, -2.5F, -5.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 2.5F, -2.0F));
        PartDefinition lure = body.addOrReplaceChild("lure", CubeListBuilder.create()
                .texOffs(30, 18).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, -3.0F, 0.6F, 0.0F, 0.0F));
        PartDefinition lure_tip = lure.addOrReplaceChild("lure_tip", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.9F, 0.0F, 0.0F));
        lure_tip.addOrReplaceChild("bulb", CubeListBuilder.create()
                .texOffs(34, 18).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        tail.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(38, 0).addBox(-0.5F, -4.0F, 0.0F, 1.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create()
                .texOffs(46, 18).addBox(0.0F, -1.5F, 0.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(4.5F, 2.0F, 0.0F, 0.0F, -0.5F, 0.0F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create()
                .texOffs(46, 18).mirror().addBox(-1.0F, -1.5F, 0.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-4.5F, 2.0F, 0.0F, 0.0F, 0.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnglerfishRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float move = Mth.clamp(state.walkAnimationSpeed * 3.0F, 0.0F, 1.0F);
        float p = t * (0.12F + 0.3F * move);
        float amp = 0.14F + 0.3F * move;
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.body.y += Mth.sin(t * 0.07F) * 0.5F;
        this.tail.yRot = Mth.sin(p) * amp;
        this.tailFin.yRot = Mth.sin(p - 0.9F) * amp * 1.3F;
        this.leftFin.yRot += Mth.sin(t * 0.28F) * 0.35F;
        this.rightFin.yRot -= Mth.sin(t * 0.28F) * 0.35F;
        this.jaw.xRot = 0.12F + Mth.sin(t * 0.045F) * 0.06F + state.bite * 0.75F;
        // the lure dangles and sways
        this.lure.xRot += Mth.sin(t * 0.06F) * 0.12F;
        this.lure.zRot = Mth.sin(t * 0.045F) * 0.08F;
        this.lureTip.xRot += Mth.sin(t * 0.06F - 0.7F) * 0.2F;
        this.bulb.xRot = -this.lure.xRot - this.lureTip.xRot + 1.5F;
        if (!state.isInWater) {
            this.body.zRot = 1.3F;
        }
    }
}
