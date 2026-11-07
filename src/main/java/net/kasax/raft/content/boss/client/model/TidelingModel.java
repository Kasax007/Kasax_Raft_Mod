package net.kasax.raft.content.boss.client.model;

import net.kasax.raft.Raft;
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
 * Tideling: a stocky fish with an underbite full of fangs, a spiny dorsal ridge, small pectoral fins and a forked tail.
 * It wags its tail while it swims, paddles its fins, snaps its jaw when it bites and flops on its side when stranded.
 * Texture 64x64 (+ glowing eyes and side spots).
 */
public class TidelingModel extends EntityModel<TidelingRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("tideling"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart spine;

    public TidelingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tailFin = this.tail.getChild("tail_fin");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.spine = this.body.getChild("spine");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(18, 18).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, -5.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-4.5F, 0.0F, -6.5F, 9.0F, 3.0F, 6.0F)
                .texOffs(44, 30).addBox(2.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(44, 30).mirror().addBox(-3.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F).mirror(false),
                PartPose.offset(0.0F, 1.0F, 0.5F));
        body.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-0.5F, -4.0F, 0.0F, 1.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, -4.0F, -4.0F));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create()
                .texOffs(30, 30).addBox(0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(4.0F, 1.0F, -2.0F, 0.0F, -0.3F, 0.5F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create()
                .texOffs(30, 30).mirror().addBox(-4.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-4.0F, 1.0F, -2.0F, 0.0F, 0.3F, -0.5F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(46, 18).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        tail.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-0.5F, -5.0F, 0.0F, 1.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TidelingRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = state.isInWater ? 0.45F : 0.9F;
        float amp = state.isInWater ? 0.35F + Math.min(0.4F, state.walkAnimationSpeed * 2.0F) : 0.6F;
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.tail.yRot = Mth.sin(t * speed) * amp;
        this.tailFin.yRot = Mth.sin(t * speed - 0.9F) * amp * 0.8F;
        this.head.yRot = -Mth.sin(t * speed) * amp * 0.12F;
        float fin = Mth.sin(t * 0.5F) * 0.3F;
        this.leftFin.zRot += fin;
        this.rightFin.zRot -= fin;
        this.spine.xRot = Mth.sin(t * 0.2F) * 0.05F;
        this.jaw.xRot = 0.08F + state.bite * 0.7F + (state.onLand ? 0.25F + Mth.sin(t * 0.7F) * 0.15F : 0.0F);
        if (state.flopping) {
            // on its side, slapping the ground
            this.body.zRot = Mth.HALF_PI * 0.95F;
            this.body.y += 2.5F;
            this.tail.yRot = Mth.sin(t * 1.4F) * 0.7F;
        }
    }
}
