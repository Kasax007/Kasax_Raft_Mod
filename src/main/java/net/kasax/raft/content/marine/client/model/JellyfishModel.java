package net.kasax.raft.content.marine.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.compat.render.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.kasax.raft.compat.render.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The jellyfish (translucent): a domed bell with a flaring rim, a cross of frilly oral arms and eight two-segment
 * tentacles. The bell contracts on every pulse (narrower and taller) and slowly relaxes; the tentacles draw in on the
 * pulse, spread out while it relaxes, wave gently and trail behind the drift. Texture 64x64.
 */
public class JellyfishModel extends EntityModel<JellyfishRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("jellyfish"), "main");
    private static final int TENTACLES = 8;

    private final ModelPart bell;
    private final ModelPart rim;
    private final ModelPart oralArms;
    private final ModelPart[] tentacles = new ModelPart[TENTACLES];
    private final ModelPart[] tentacleEnds = new ModelPart[TENTACLES];
    private final float[] angles = new float[TENTACLES];

    public JellyfishModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.bell = root.getChild("bell");
        this.rim = this.bell.getChild("rim");
        this.oralArms = root.getChild("oral_arms");
        for (int i = 0; i < TENTACLES; i++) {
            this.tentacles[i] = root.getChild("tentacle" + i);
            this.tentacleEnds[i] = this.tentacles[i].getChild("tentacle" + i + "_end");
            this.angles[i] = (float) (i * Math.PI / 4.0 + Math.PI / 8.0);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bell = root.addOrReplaceChild("bell", CubeListBuilder.create()
                .texOffs(36, 14).addBox(-3.5F, -5.0F, -3.5F, 7.0F, 2.0F, 7.0F)
                .texOffs(0, 14).addBox(-4.5F, -3.0F, -4.5F, 9.0F, 3.0F, 9.0F)
                .texOffs(0, 26).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        bell.addOrReplaceChild("rim", CubeListBuilder.create()
                .texOffs(6, 0).addBox(-5.5F, 0.0F, -5.5F, 11.0F, 2.0F, 11.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("oral_arms", CubeListBuilder.create()
                .texOffs(50, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 11.0F, 0.0F)
                .texOffs(0, 0).addBox(0.0F, 0.0F, -1.5F, 0.0F, 11.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition tentacle0 = root.addOrReplaceChild("tentacle0", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(3.97F, 17.0F, 1.65F));
        tentacle0.addOrReplaceChild("tentacle0_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle1 = root.addOrReplaceChild("tentacle1", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(1.65F, 17.0F, 3.97F));
        tentacle1.addOrReplaceChild("tentacle1_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle2 = root.addOrReplaceChild("tentacle2", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-1.65F, 17.0F, 3.97F));
        tentacle2.addOrReplaceChild("tentacle2_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle3 = root.addOrReplaceChild("tentacle3", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-3.97F, 17.0F, 1.65F));
        tentacle3.addOrReplaceChild("tentacle3_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle4 = root.addOrReplaceChild("tentacle4", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-3.97F, 17.0F, -1.65F));
        tentacle4.addOrReplaceChild("tentacle4_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle5 = root.addOrReplaceChild("tentacle5", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-1.65F, 17.0F, -3.97F));
        tentacle5.addOrReplaceChild("tentacle5_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle6 = root.addOrReplaceChild("tentacle6", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(1.65F, 17.0F, -3.97F));
        tentacle6.addOrReplaceChild("tentacle6_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        PartDefinition tentacle7 = root.addOrReplaceChild("tentacle7", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(3.97F, 17.0F, -1.65F));
        tentacle7.addOrReplaceChild("tentacle7_end", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JellyfishRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks + state.phase;
        float c = state.contraction;
        float sx = 1.0F - 0.17F * c;
        this.bell.xScale = sx;
        this.bell.zScale = sx;
        this.bell.yScale = 1.0F + 0.12F * c;
        this.bell.y -= c * 0.5F;
        this.rim.xScale = 1.0F - 0.1F * c;
        this.rim.zScale = 1.0F - 0.1F * c;
        this.bell.yRot = t * 0.004F;
        this.oralArms.yRot = t * 0.01F;
        this.oralArms.xRot = Mth.sin(t * 0.05F) * 0.06F - state.dragZ * 0.4F;
        this.oralArms.zRot = Mth.sin(t * 0.043F) * 0.06F + state.dragX * 0.4F;
        // tentacles: drawn in on the pulse, spread when relaxed, trailing the drift
        float spread = 0.10F + 0.22F * (1.0F - c) - Math.max(0.0F, state.rise) * 0.15F;
        for (int i = 0; i < TENTACLES; i++) {
            float a = this.angles[i];
            float wave = Mth.sin(t * 0.08F + i * 1.7F) * 0.08F;
            float out = spread + wave;
            ModelPart tp = this.tentacles[i];
            tp.x *= 1.0F - 0.17F * c;
            tp.z *= 1.0F - 0.17F * c;
            tp.y -= c * 0.5F;
            tp.zRot = -out * Mth.cos(a) + state.dragX * 0.9F;
            tp.xRot = out * Mth.sin(a) - state.dragZ * 0.9F;
            ModelPart end = this.tentacleEnds[i];
            float wave2 = Mth.sin(t * 0.08F + i * 1.7F - 1.2F) * 0.14F;
            end.zRot = -(wave2 - c * 0.12F) * Mth.cos(a) + state.dragX * 0.5F;
            end.xRot = (wave2 - c * 0.12F) * Mth.sin(a) - state.dragZ * 0.5F;
        }
    }
}
