package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: an item to draw (the newer game resolves the model first; 1.21.1 draws the stack directly). */
public class ItemStackRenderState {
    public ItemStack stack = ItemStack.EMPTY;
    public ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    public @Nullable LivingEntity owner;
    public @Nullable Level level;
    public int seed;

    public void clear() {
        this.stack = ItemStack.EMPTY;
        this.owner = null;
        this.level = null;
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        if (this.stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (this.owner != null) {
            mc.getItemRenderer().renderStatic(this.owner, this.stack, this.displayContext, false, poseStack, collector.buffers(),
                    this.owner.level(), light, overlay, this.seed);
        } else {
            mc.getItemRenderer().renderStatic(this.stack, this.displayContext, light, overlay, poseStack, collector.buffers(),
                    this.level, this.seed);
        }
    }
}
