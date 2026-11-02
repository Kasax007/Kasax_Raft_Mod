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
 * The moray eel: a head with a long gaping jaw and a chain of four body segments and a tail, each with a fin seam on
 * the back. The body swings in an S-shaped wave travelling to the tail (calm while it sits in its hole), the head
 * follows its gaze and the jaw opens and closes as it breathes, wide when it lunges. Texture 64x64.
 */
public class MorayEelModel extends EntityModel<MorayEelRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("moray_eel"), "main");

    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] segments = new ModelPart[5];

    public MorayEelModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.segments[0] = this.head.getChild("seg1");
        this.segments[1] = this.segments[0].getChild("seg2");
        this.segments[2] = this.segments[1].getChild("seg3");
        this.segments[3] = this.segments[2].getChild("seg4");
        this.segments[4] = this.segments[3].getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-2.0F, -2.5F, -5.0F, 4.0F, 4.0F, 5.0F)
                .texOffs(10, 20).addBox(-1.5F, -2.0F, -7.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, -10.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(48, 11).addBox(-1.5F, 0.0F, -5.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 1.5F, -1.0F));
        PartDefinition seg1 = head.addOrReplaceChild("seg1", CubeListBuilder.create()
                .texOffs(12, 0).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 5.0F)
                .texOffs(0, 20).addBox(0.0F, -3.5F, 0.0F, 0.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 5.0F)
                .texOffs(0, 20).addBox(0.0F, -3.0F, 0.0F, 0.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg3", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 5.0F)
                .texOffs(0, 20).addBox(0.0F, -3.0F, 0.0F, 0.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg4", CubeListBuilder.create()
                .texOffs(18, 11).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F)
                .texOffs(0, 20).addBox(0.0F, -2.5F, 0.0F, 0.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        seg4.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(34, 11).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 5.0F)
                .texOffs(0, 0).addBox(0.0F, -2.5F, 0.0F, 0.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MorayEelRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks + state.phase;
        float move = Mth.clamp(state.walkAnimationSpeed * 3.0F, 0.0F, 1.0F);
        float freq = state.hiding ? 0.07F : 0.2F + 0.2F * move;
        float amp = state.hiding ? 0.1F : 0.16F + 0.28F * move;
        float look = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        this.head.yRot = look * 0.7F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float[] gain = {0.35F, 0.6F, 0.8F, 0.95F, 1.1F};
        for (int i = 0; i < this.segments.length; i++) {
            this.segments[i].yRot = Mth.sin(t * freq - i * 0.95F) * amp * gain[i];
        }
        // keep the body in line while the head turns to look
        this.segments[0].yRot -= this.head.yRot;
        float breathe = Mth.sin(t * 0.09F) * 0.5F + 0.5F;
        this.jaw.xRot = 0.08F + breathe * 0.32F + state.lunge * 0.55F;
        this.head.xRot -= state.lunge * 0.15F;
        if (!state.isInWater) {
            this.head.zRot = 1.2F;
        }
    }
}
