package net.kasax.raft.content.shore.client.model;

import net.kasax.raft.Raft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;

/**
 * Drowned corsair: the vanilla humanoid (drowned) body. With a blade in hand it holds the weapon high and slashes down
 * like a buccaneer instead of stretching its arms out like a zombie; it swims like a drowned. The same class renders
 * the coat layer and the tricorn hat layer ({@link #HAT_LAYER}), so all layers move together.
 */
public class CorsairModel extends HumanoidModel<ZombieRenderState> {
    public static final ModelLayerLocation HAT_LAYER = new ModelLayerLocation(Raft.id("drowned_corsair"), "hat");

    public CorsairModel(ModelPart root) {
        super(root);
    }

    /** Tricorn hat with turned-up brim on three sides and a feather; the other humanoid parts are empty. Texture 64x32. */
    public static LayerDefinition createHatLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("tricorn_crown",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -11.0F, -3.5F, 7.0F, 3.0F, 7.0F, new net.minecraft.client.model.geom.builders.CubeDeformation(0.25F)),
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

    private static boolean armed(ZombieRenderState state) {
        return !state.getMainHandItemStack().isEmpty();
    }

    @Override
    protected void setupAttackAnimation(ZombieRenderState state) {
        if (state.isAggressive) {
            if (armed(state)) {
                AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, state.mainArm, state.swingAnimation, state.ageInTicks);
            } else {
                super.setupAttackAnimation(state);
                AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, state);
            }
        } else {
            super.setupAttackAnimation(state);
        }
    }

    @Override
    protected void setupSwimAnimation(ZombieRenderState state, float animationPos, float swimAmount) {
        boolean keepBlade = state.isAggressive && armed(state);
        if (!keepBlade || state.mainArm != net.minecraft.world.entity.HumanoidArm.RIGHT) {
            this.rightArm.xRot = Mth.rotLerpRad(swimAmount, this.rightArm.xRot, (float) (-Math.PI * 4.0 / 5.0))
                    + swimAmount * 0.35F * Mth.sin(0.1F * state.ageInTicks);
            this.rightArm.zRot = Mth.rotLerpRad(swimAmount, this.rightArm.zRot, -0.15F);
        }
        if (!keepBlade || state.mainArm != net.minecraft.world.entity.HumanoidArm.LEFT) {
            this.leftArm.xRot = Mth.rotLerpRad(swimAmount, this.leftArm.xRot, (float) (-Math.PI * 4.0 / 5.0))
                    - swimAmount * 0.35F * Mth.sin(0.1F * state.ageInTicks);
            this.leftArm.zRot = Mth.rotLerpRad(swimAmount, this.leftArm.zRot, 0.15F);
        }
        this.leftLeg.xRot = this.leftLeg.xRot - swimAmount * 0.55F * Mth.sin(0.1F * state.ageInTicks);
        this.rightLeg.xRot = this.rightLeg.xRot + swimAmount * 0.55F * Mth.sin(0.1F * state.ageInTicks);
        this.head.xRot = 0.0F;
    }
}
