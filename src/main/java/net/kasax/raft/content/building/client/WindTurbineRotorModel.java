package net.kasax.raft.content.building.client;

import net.kasax.raft.Raft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The wind turbine rotor: spinner, hub and three white blades with red tips. Coordinates are block pixels around the
 * rotor axis (x right, y up, z = -1 is the blade plane just in front of the block). The state is the rotor angle in
 * degrees. Texture: {@code textures/entity/wind_turbine_rotor.png} (32x32).
 */
public class WindTurbineRotorModel extends Model<Float> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("wind_turbine_rotor"), "main");

    private final ModelPart rotor;

    public WindTurbineRotorModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.rotor = root.getChild("rotor");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rotor = root.addOrReplaceChild("rotor", CubeListBuilder.create()
                        .texOffs(0, 23).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F)
                        .texOffs(18, 23).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        for (int i = 0; i < 3; i++) {
            // each blade points outwards along +y, twisted a little around its own axis (pitch)
            rotor.addOrReplaceChild("blade_" + i, CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.0F, 2.0F, -1.5F, 4.0F, 8.0F, 1.0F)
                            .texOffs(10, 0).addBox(-1.5F, 10.0F, -1.5F, 3.0F, 13.0F, 1.0F),
                    PartPose.rotation(0.0F, 0.28F, i * Mth.TWO_PI / 3.0F));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Float angleDegrees) {
        super.setupAnim(angleDegrees);
        rotor.zRot = angleDegrees * Mth.DEG_TO_RAD;
    }
}
