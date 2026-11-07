package net.kasax.raft.content.npc.client;

import net.kasax.raft.content.npc.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The player-shaped body (vanilla player model layers with jacket, sleeves and trouser overlays) for the people of the
 * sea; slim models have three-pixel arms like the Alex skin. Sitting uses the riding pose.
 */
public class NpcModel extends PlayerModel<NpcEntity> {
    public NpcModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void prepareMobModel(NpcEntity npc, float limbSwing, float limbSwingAmount, float partialTick) {
        HumanoidModel.ArmPose main = armPose(npc, npc.getMainArm());
        HumanoidModel.ArmPose off = armPose(npc, npc.getMainArm().getOpposite());
        this.rightArmPose = npc.getMainArm() == HumanoidArm.RIGHT ? main : off;
        this.leftArmPose = npc.getMainArm() == HumanoidArm.RIGHT ? off : main;
        super.prepareMobModel(npc, limbSwing, limbSwingAmount, partialTick);
    }

    private static HumanoidModel.ArmPose armPose(NpcEntity npc, HumanoidArm arm) {
        ItemStack held = npc.getMainArm() == arm ? npc.getMainHandItem() : npc.getOffhandItem();
        if (held.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (npc.isUsingItem() && held.is(Items.SPYGLASS)) {
            HumanoidArm usedArm = npc.getUsedItemHand() == InteractionHand.MAIN_HAND ? npc.getMainArm() : npc.getMainArm().getOpposite();
            if (usedArm == arm) return HumanoidModel.ArmPose.SPYGLASS;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public void setupAnim(NpcEntity npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (npc.isSitting()) {
            this.riding = true;
        }
        super.setupAnim(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
