package net.kasax.raft.client.render;

import net.kasax.raft.Raft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * The shark model, built in code: a streamlined body with a head, a tail with two fin lobes,
 * a dorsal fin and two pectoral fins. The 64x32 texture is laid out as
 * body (0,0) head (0,20) tail (22,20) tail fins (44,0) (44,10) dorsal (54,0) pectoral (44,17).
 */
public class SharkModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("shark"), "main");

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tailFinUpper;
    private final ModelPart tailFinLower;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public SharkModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.tailFinUpper = this.tail.getChild("tail_fin_upper");
        this.tailFinLower = this.tail.getChild("tail_fin_lower");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -7.0F, 8.0F, 6.0F, 14.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, -7.0F));

        PartDefinition tail = body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(22, 20).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        tail.addOrReplaceChild("tail_fin_upper",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -6.0F, -1.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, -0.45F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_fin_lower",
                CubeListBuilder.create().texOffs(44, 10).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.4F, 0.0F, 0.0F));

        body.addOrReplaceChild("dorsal_fin",
                CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, 0.35F, 0.0F, 0.0F));

        body.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(44, 17).addBox(0.0F, 0.0F, -1.5F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(3.5F, 2.0F, -3.0F, 0.0F, 0.0F, 0.5F));
        body.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(44, 17).mirror().addBox(-4.0F, 0.0F, -1.5F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-3.5F, 2.0F, -3.0F, 0.0F, 0.0F, -0.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 4.0F);
        float swing = Mth.sin(state.ageInTicks * 0.25F) * (0.18F + 0.25F * speed);
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.tail.yRot = swing;
        this.tailFinUpper.yRot = swing * 0.8F;
        this.tailFinLower.yRot = swing * 0.8F;
        this.leftFin.zRot = 0.5F + Mth.sin(state.ageInTicks * 0.15F) * 0.1F;
        this.rightFin.zRot = -0.5F - Mth.sin(state.ageInTicks * 0.15F) * 0.1F;
        if (!state.isInWater) {
            this.body.zRot = 1.2F;
        }
    }
}
