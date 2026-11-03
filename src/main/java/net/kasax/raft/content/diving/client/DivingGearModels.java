package net.kasax.raft.content.diving.client;

import net.kasax.raft.Raft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 3D parts of the diving gear that the flat armor layer cannot show. Coordinates are in the space of the humanoid part
 * they follow (head, body or a leg; 1 unit = 1/16 block; the armor layer itself is 1 unit bigger than the body).
 */
public final class DivingGearModels {
    private DivingGearModels() {
    }

    public static final ModelLayerLocation HELMET = layer("helmet");
    public static final ModelLayerLocation HELMET_LAMP = layer("helmet_lamp");
    public static final ModelLayerLocation COLLAR = layer("collar");
    public static final ModelLayerLocation TANK = layer("tank");
    public static final ModelLayerLocation FLIPPER = layer("flipper");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Raft.id("diving_gear"), name);
    }

    /** Brass porthole rims, top valve with hand wheel and the air inlet on the back of the helmet (head space). */
    public static LayerDefinition createHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("front_port", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -7.5F, -6.0F, 6.0F, 6.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("left_port", CubeListBuilder.create().texOffs(16, 0)
                .addBox(5.0F, -6.5F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("right_port", CubeListBuilder.create().texOffs(16, 0).mirror()
                .addBox(-6.0F, -6.5F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("valve", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-1.0F, -11.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("valve_wheel", CubeListBuilder.create().texOffs(28, 4)
                .addBox(-2.0F, -11.5F, -2.0F, 4.0F, 0.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("inlet", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-1.5F, -5.5F, 5.0F, 3.0F, 3.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** The lamp on the right side of a helmet with a lamp fitted, lens to the front (head space, drawn full bright). */
    public static LayerDefinition createHelmetLamp() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("lamp", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-6.5F, -8.5F, -3.5F, 2.0F, 2.0F, 3.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** The bolted brass breastplate (corselet) the helmet sits on (body space). */
    public static LayerDefinition createCollar() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("collar", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-5.5F, -0.5F, -3.5F, 11.0F, 2.0F, 7.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Twin air cylinders with caps, manifold, regulator and hose (body space, on the back). */
    public static LayerDefinition createTank() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? -3.7F : 0.7F;
            String n = side == 0 ? "left" : "right";
            root.addOrReplaceChild("cylinder_" + n, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(x, 0.5F, 3.0F, 3.0F, 10.0F, 3.0F), PartPose.ZERO);
            root.addOrReplaceChild("cap_top_" + n, CubeListBuilder.create().texOffs(12, 0)
                    .addBox(x + 0.5F, -0.5F, 3.5F, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
            root.addOrReplaceChild("cap_bottom_" + n, CubeListBuilder.create().texOffs(12, 0)
                    .addBox(x + 0.5F, 10.5F, 3.5F, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
        }
        root.addOrReplaceChild("manifold", CubeListBuilder.create().texOffs(12, 4)
                .addBox(-3.0F, -1.5F, 4.0F, 6.0F, 1.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("regulator", CubeListBuilder.create().texOffs(26, 0)
                .addBox(-1.0F, -3.0F, 3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("hose", CubeListBuilder.create().texOffs(34, 0)
                .addBox(-0.5F, -5.5F, 4.0F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("strap_buckle", CubeListBuilder.create().texOffs(38, 0)
                .addBox(-2.0F, 5.0F, 2.6F, 4.0F, 1.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** One flipper blade in front of the foot (leg space; the foot sole is at y = 12). */
    public static LayerDefinition createFlipper() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.5F, 11.0F, -9.5F, 5.0F, 1.0F, 7.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }
}
