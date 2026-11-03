package net.kasax.raft.content.boss.client.model;

import net.kasax.raft.Raft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

/** Brine bolt: a bright core inside a wobbling translucent shell of brine. Texture 32x32. */
public class BrineBoltModel extends EntityModel<EntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("brine_bolt"), "main");

    private final ModelPart core;
    private final ModelPart shell;

    public BrineBoltModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucentEmissive);
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        this.core.xRot = t * 0.3F;
        this.core.yRot = t * 0.4F;
        this.shell.xRot = -t * 0.15F;
        this.shell.zRot = t * 0.2F;
        float s = 1.0F + Mth.sin(t * 0.8F) * 0.12F;
        this.shell.xScale = s;
        this.shell.yScale = 2.0F - s;
        this.shell.zScale = s;
    }
}
