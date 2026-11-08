package net.kasax.raft.mixin.client;

import net.kasax.raft.client.render.ModelZFix;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Beach Craft's own model layers are baked without z-fighting faces (see ModelZFix). */
@Mixin(EntityModelSet.class)
public class EntityModelSetZFixMixin {
    @Inject(method = "bakeLayer", at = @At("RETURN"))
    private void raft$zfix(ModelLayerLocation id, CallbackInfoReturnable<ModelPart> cir) {
        if ("raft".equals(id.model().getNamespace())) {
            ModelZFix.fix(cir.getReturnValue());
        }
    }
}
