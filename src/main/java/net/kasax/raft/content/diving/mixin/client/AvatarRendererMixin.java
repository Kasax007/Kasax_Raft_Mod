package net.kasax.raft.content.diving.mixin.client;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players hold the harpoon gun raised and aimed like a loaded crossbow (vanilla only does this for the crossbow). */
@Mixin(PlayerRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void raft$harpoonGunPose(AbstractClientPlayer player, InteractionHand hand,
                                            CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (!player.swinging && player.getItemInHand(hand).is(DivingContent.HARPOON_GUN)) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
