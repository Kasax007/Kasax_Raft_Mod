package net.kasax.raft.content.cozy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.compat.client.ItemModelProperties;
import net.kasax.raft.content.cozy.block.CozyShelfBlock;
import net.kasax.raft.content.cozy.block.CozyShelfBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.21.1 port: draws the three items standing on a shelf like the newer game's shelf renderer (quarter size, centred
 * on the board; flat items face out, block items are turned to show their front). Items with a separate GUI model
 * (Captain's Spyglass, Shark Tooth Spear) show it, as the newer game's "on shelf" display does.
 */
public class ShelfRenderer implements BlockEntityRenderer<CozyShelfBlockEntity> {
    private static final Map<BakedModel, float[]> HEIGHTS = new WeakHashMap<>();

    public ShelfRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CozyShelfBlockEntity shelf, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = shelf.getBlockState().getValue(CozyShelfBlock.FACING);
        float yRot = -facing.toYRot();
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        int seed = (int) shelf.getBlockPos().asLong();
        for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
            ItemStack stack = shelf.getItem(slot);
            if (stack.isEmpty()) continue;
            BakedModel model;
            ItemModelProperties.GUI.set(true);
            try {
                model = renderer.getModel(stack, shelf.getLevel(), null, seed + slot);
            } finally {
                ItemModelProperties.GUI.set(false);
            }
            float[] h = heights(model);
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(yRot));
            pose.translate((slot - 1) * 0.3125F, shelf.alignItemsToBottom() ? -0.25F : 0.0F, -0.25F);
            pose.scale(0.25F, 0.25F, 0.25F);
            float offsetY = -h[0];
            if (!shelf.alignItemsToBottom()) offsetY -= (h[1] - h[0]) / 2.0F;
            pose.translate(0.0F, offsetY, 0.0F);
            if (model.isGui3d()) pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            renderer.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, overlay, model);
            pose.popPose();
        }
    }

    /** Lowest and highest point of the model (block units, centred like the item renderer draws it). */
    private static float[] heights(BakedModel model) {
        return HEIGHTS.computeIfAbsent(model, m -> {
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            RandomSource random = RandomSource.create(42L);
            for (Direction d : new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                for (BakedQuad quad : m.getQuads(null, d, random)) {
                    int[] v = quad.getVertices();
                    int stride = v.length / 4;
                    for (int i = 0; i < 4; i++) {
                        float y = Float.intBitsToFloat(v[i * stride + 1]);
                        min = Math.min(min, y);
                        max = Math.max(max, y);
                    }
                }
            }
            if (min > max) return new float[]{-0.5F, 0.5F};
            return new float[]{min - 0.5F, max - 0.5F};
        });
    }
}
