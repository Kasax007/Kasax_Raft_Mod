package net.kasax.raft.content.diving.client;

import net.kasax.raft.Raft;
import net.kasax.raft.compat.render.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.kasax.raft.compat.render.RenderTypes;
import net.minecraft.util.Unit;

/**
 * The harpoon bolt: a steel shaft with a barbed head, a brass collar and red/white fins. Built along the Y axis with
 * the point at -Y (like the vanilla trident model), 1 block long.
 */
public class HarpoonModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("harpoon"), "main");

    public HarpoonModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shaft = root.addOrReplaceChild("shaft",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 17.0F, 1.0F), PartPose.ZERO);
        // head: socket, blade, point
        shaft.addOrReplaceChild("socket",
                CubeListBuilder.create().texOffs(4, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("blade",
                CubeListBuilder.create().texOffs(12, 0).addBox(-0.5F, -6.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("point",
                CubeListBuilder.create().texOffs(18, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        // two pairs of barbs, swept back
        shaft.addOrReplaceChild("barb_left",
                CubeListBuilder.create().texOffs(4, 4).addBox(-2.0F, -4.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("barb_right",
                CubeListBuilder.create().texOffs(4, 4).mirror().addBox(1.0F, -4.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("barb_front",
                CubeListBuilder.create().texOffs(8, 4).addBox(-0.5F, -4.0F, -2.0F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("barb_back",
                CubeListBuilder.create().texOffs(8, 4).mirror().addBox(-0.5F, -4.0F, 1.0F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        // brass collar and line eye near the tail
        shaft.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(12, 5).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("eye",
                CubeListBuilder.create().texOffs(20, 5).addBox(0.5F, 8.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
        // fins: two crossed planes
        shaft.addOrReplaceChild("fin_x",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, 11.0F, 0.0F, 5.0F, 4.0F, 0.0F), PartPose.ZERO);
        shaft.addOrReplaceChild("fin_z",
                CubeListBuilder.create().texOffs(10, 18).addBox(0.0F, 11.0F, -2.5F, 0.0F, 4.0F, 5.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }
}
