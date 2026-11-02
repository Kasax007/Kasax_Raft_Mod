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
 * The manta ray: a flat body disc with rolled cephalic fins, three-part swept wings (each outer part lags behind
 * the inner one, so each flap travels out to the wing tip as a wave) and a thin whip tail. Texture 64x64.
 */
public class MantaRayModel extends EntityModel<MantaRayRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("manta_ray"), "main");

    private final ModelPart body;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart leftWing;
    private final ModelPart leftWingMid;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingMid;
    private final ModelPart rightWingTip;
    private final ModelPart tail;

    public MantaRayModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftHorn = this.body.getChild("left_horn");
        this.rightHorn = this.body.getChild("right_horn");
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingMid = this.leftWing.getChild("left_wing_mid");
        this.leftWingTip = this.leftWingMid.getChild("left_wing_tip");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingMid = this.rightWing.getChild("right_wing_mid");
        this.rightWingTip = this.rightWingMid.getChild("right_wing_tip");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -2.0F, -7.0F, 10.0F, 3.0F, 13.0F)
                .texOffs(26, 31).addBox(-3.0F, -1.0F, 6.0F, 6.0F, 2.0F, 2.0F)
                .texOffs(0, 39).addBox(1.0F, -0.5F, 7.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(0, 39).mirror().addBox(-3.0F, -0.5F, 7.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        body.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(46, 0).addBox(0.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(3.0F, 0.0F, -7.0F, 0.0F, 0.15F, 0.0F));
        body.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(46, 0).mirror().addBox(-2.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 0.0F, -7.0F, 0.0F, -0.15F, 0.0F));
        PartDefinition left_wing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(30, 16).addBox(0.0F, -1.0F, -5.0F, 6.0F, 2.0F, 11.0F),
                PartPose.offset(5.0F, 0.0F, -2.0F));
        PartDefinition left_wing_mid = left_wing.addOrReplaceChild("left_wing_mid", CubeListBuilder.create()
                .texOffs(0, 31).addBox(0.0F, -0.5F, -3.0F, 6.0F, 1.0F, 7.0F),
                PartPose.offsetAndRotation(6.0F, 0.0F, -1.0F, 0.0F, -0.12F, 0.0F));
        left_wing_mid.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                .texOffs(42, 31).addBox(0.0F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, -0.25F, 0.0F));
        PartDefinition right_wing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(30, 16).mirror().addBox(-6.0F, -1.0F, -5.0F, 6.0F, 2.0F, 11.0F),
                PartPose.offset(-5.0F, 0.0F, -2.0F));
        PartDefinition right_wing_mid = right_wing.addOrReplaceChild("right_wing_mid", CubeListBuilder.create()
                .texOffs(0, 31).mirror().addBox(-6.0F, -0.5F, -3.0F, 6.0F, 1.0F, 7.0F),
                PartPose.offsetAndRotation(-6.0F, 0.0F, -1.0F, 0.0F, 0.12F, 0.0F));
        right_wing_mid.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                .texOffs(42, 31).mirror().addBox(-5.0F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.25F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 14.0F),
                PartPose.offset(0.0F, -0.5F, 8.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MantaRayRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks + state.phase;
        float speed = state.isInWater ? 0.13F : 0.3F;
        float flap = Mth.sin(t * speed);
        float lag = Mth.sin(t * speed - 0.8F);
        float lag2 = Mth.sin(t * speed - 1.6F);
        float amp = state.isInWater ? 0.3F : 0.1F;
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.body.zRot = state.bank;
        // the body rises a little on every down-stroke
        this.body.y -= flap * 0.6F;
        // the flap travels from the shoulder out to the wing tip
        this.leftWing.zRot = flap * amp;
        this.leftWingMid.zRot = lag * amp * 1.2F;
        this.leftWingTip.zRot = lag2 * amp * 1.5F;
        this.rightWing.zRot = -flap * amp;
        this.rightWingMid.zRot = -lag * amp * 1.2F;
        this.rightWingTip.zRot = -lag2 * amp * 1.5F;
        float curl = Mth.sin(t * 0.07F) * 0.12F;
        this.leftHorn.zRot = 0.1F + curl;
        this.rightHorn.zRot = -0.1F - curl;
        this.tail.yRot = Mth.sin(t * 0.09F) * 0.22F;
        this.tail.xRot = Mth.sin(t * 0.13F + 1.0F) * 0.07F - flap * 0.05F;
    }
}
