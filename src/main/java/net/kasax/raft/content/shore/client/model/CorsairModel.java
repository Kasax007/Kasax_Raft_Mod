package net.kasax.raft.content.shore.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.content.shore.entity.DrownedCorsairEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Drowned corsair: the vanilla humanoid (drowned) body. With a blade in hand it holds the weapon high and slashes down
 * like a buccaneer instead of stretching its arms out like a zombie; it swims like a drowned. The same class renders
 * the coat layer and the tricorn hat layer ({@link #HAT_LAYER}), so all layers move together.
 */
public class CorsairModel extends DrownedModel<DrownedCorsairEntity> {
    public static final ModelLayerLocation HAT_LAYER = new ModelLayerLocation(Raft.id("drowned_corsair"), "hat");

    public CorsairModel(ModelPart root) {
        super(root);
    }

    /** Tricorn hat with turned-up brim on three sides and a feather; the other humanoid parts are empty. Texture 64x32. */
    public static LayerDefinition createHatLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("tricorn_crown",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -11.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        head.addOrReplaceChild("tricorn_back",
                CubeListBuilder.create().texOffs(0, 10).addBox(-6.0F, -3.5F, -0.5F, 12.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 4.6F, -0.3F, 0.0F, 0.0F));
        head.addOrReplaceChild("tricorn_left",
                CubeListBuilder.create().texOffs(0, 15).addBox(-7.0F, -3.5F, -0.5F, 14.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(3.1F, -8.0F, -1.2F, 0.3F, -1.1F, 0.0F));
        head.addOrReplaceChild("tricorn_right",
                CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-7.0F, -3.5F, -0.5F, 14.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-3.1F, -8.0F, -1.2F, 0.3F, 1.1F, 0.0F));
        head.addOrReplaceChild("feather",
                CubeListBuilder.create().texOffs(30, 0).addBox(-0.5F, -6.0F, 0.0F, 1.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(4.2F, -9.5F, 1.5F, -0.55F, 0.0F, 0.35F));
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static boolean armed(DrownedCorsairEntity corsair) {
        return !corsair.getMainHandItem().isEmpty();
    }

    @Override
    public void setupAnim(DrownedCorsairEntity corsair, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        super.setupAnim(corsair, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!corsair.isAggressive() || !armed(corsair)) {
            return;
        }
        // a buccaneer's slash instead of the zombie's stretched arms
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightArm.setPos(-5.0F, 2.0F, 0.0F);
        this.leftArm.setPos(5.0F, 2.0F, 0.0F);
        AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, corsair, this.attackTime, ageInTicks);
        float swimAmount = this.swimAmount;
        if (swimAmount > 0.0F) {
            // swimming: the free arm strokes, the blade stays up
            ModelPart free = corsair.getMainArm() == HumanoidArm.RIGHT ? this.leftArm : this.rightArm;
            float side = free == this.leftArm ? 1.0F : -1.0F;
            free.xRot = Mth.rotLerp(swimAmount, free.xRot * Mth.RAD_TO_DEG, -144.0F) * Mth.DEG_TO_RAD
                    - side * swimAmount * 0.35F * Mth.sin(0.1F * ageInTicks);
            free.zRot = side * 0.15F * swimAmount;
        }
    }
}
