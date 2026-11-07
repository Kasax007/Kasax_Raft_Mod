package net.kasax.raft.content.encounters.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.encounters.entity.ScavengerEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
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
import net.minecraft.world.item.Items;

/**
 * Scavenger: a full-size humanoid (texture 64x64, the zombie net) with a canvas backpack and a bedroll on the back.
 * With a blade it raises the weapon and slashes down; empty-handed it puts its fists up like a brawler; with a bow
 * it draws like a skeleton.
 */
public class ScavengerModel extends HumanoidModel<ScavengerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("scavenger"), "main");

    public ScavengerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition body = mesh.getRoot().getChild("body");
        // the pack hangs 0.1 px off the back (no shared face with the body)
        body.addOrReplaceChild("pack", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, 1.0F, 2.1F, 6.0F, 7.0F, 3.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("bedroll", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, -1.1F, 2.6F, 8.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(ScavengerEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        if (entity.isAggressive() && entity.getMainHandItem().is(Items.BOW)) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            } else {
                this.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(ScavengerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!entity.isAggressive() || entity.getMainHandItem().is(Items.BOW)) {
            return;
        }
        if (!entity.getMainHandItem().isEmpty()) {
            AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, entity, this.attackTime, ageInTicks);
        } else {
            // fists up, the punching arm jabs forward
            float jab = Mth.sin(this.attackTime * (float) Math.PI);
            this.rightArm.xRot = -1.1F - jab * 0.6F;
            this.leftArm.xRot = -1.1F;
            this.rightArm.yRot = -0.35F;
            this.leftArm.yRot = 0.35F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);
        }
    }
}
