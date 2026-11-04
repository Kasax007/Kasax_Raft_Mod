package net.kasax.raft.content.npc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The player-shaped body (vanilla player model layers with jacket, sleeves and trouser overlays) for the people of the
 * sea; slim models have three-pixel arms like the Alex skin. Sitting uses the riding pose.
 */
public class NpcModel extends HumanoidModel<NpcRenderState> {
    private final boolean slim;

    public NpcModel(ModelPart root, boolean slim) {
        super(root);
        this.slim = slim;
    }

    @Override
    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart part = this.getArm(arm);
        if (this.slim) {
            float offset = 0.5F * (arm == HumanoidArm.RIGHT ? 1 : -1);
            part.x += offset;
            part.translateAndRotate(poseStack);
            part.x -= offset;
        } else {
            part.translateAndRotate(poseStack);
        }
    }
}
