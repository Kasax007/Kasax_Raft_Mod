package net.kasax.raft.content.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kasax.raft.content.gear.entity.BoomerangEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws the boomerang lying flat and spinning fast around its middle. */
public class BoomerangRenderer extends EntityRenderer<BoomerangEntity> {
    private final ItemRenderer items;

    public BoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(BoomerangEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.1F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 45.0F));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(1.6F, 1.6F, 1.6F);
        items.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BoomerangEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
