package net.kasax.raft.platform.mixin;

import net.kasax.raft.platform.AttachmentRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entities saved by the Fabric build: their Beach Craft attachments are taken over (see AttachmentRegistry). */
@Mixin(Entity.class)
abstract class FabricAttachmentsMixin {
    @Inject(method = "load", at = @At("TAIL"))
    private void raft$fabricAttachments(ValueInput input, CallbackInfo ci) {
        AttachmentRegistry.migrateFabricData((Entity) (Object) this, input);
    }
}
