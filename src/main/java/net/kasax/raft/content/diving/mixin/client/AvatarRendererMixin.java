package net.kasax.raft.content.diving.mixin.client;

import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players hold the harpoon gun raised and aimed like a loaded crossbow (vanilla only does this for the crossbow). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void raft$harpoonGunPose(Avatar avatar, ItemStack itemInHand, InteractionHand hand,
                                            CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (!avatar.isSwinging() && itemInHand.is(DivingContent.HARPOON_GUN)) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
