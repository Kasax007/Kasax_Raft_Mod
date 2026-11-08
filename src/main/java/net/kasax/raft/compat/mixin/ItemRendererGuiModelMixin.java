package net.kasax.raft.compat.mixin;

import net.kasax.raft.compat.client.ItemModelProperties;
import net.kasax.raft.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The energy staff, the captain's spyglass and the shark tooth spear are drawn flat in GUIs, on the ground and in item
 * frames, with their own model in the hand (like the trident and spyglass).
 */
@Mixin(ItemRenderer.class)
public class ItemRendererGuiModelMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private BakedModel raft$flatInGui(BakedModel model, ItemStack stack, ItemDisplayContext context) {
        if (!ItemModelProperties.GUI_MODEL_ITEMS.get().contains(stack.getItem())
                || !(context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED)) {
            return model;
        }
        ItemModelProperties.GUI.set(true);
        try {
            BakedModel flat = model.getOverrides().resolve(model, stack, Minecraft.getInstance().level, null, 0);
            return flat != null ? flat : model;
        } finally {
            ItemModelProperties.GUI.set(false);
        }
    }
}
