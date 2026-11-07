package net.kasax.raft.content.voyage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

import java.util.function.Function;

/** Draws a Beach Craft boat or raft with one model and its own texture (like the vanilla boat renderer). */
public class ModBoatRenderer extends EntityRenderer<Boat> {
    private final ResourceLocation texture;
    private final ListModel<Boat> model;

    public ModBoatRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                           Function<net.minecraft.client.model.geom.ModelPart, ListModel<Boat>> model) {
        super(context);
        this.shadowRadius = 0.8F;
        this.model = model.apply(context.bakeLayer(layer));
        ResourceLocation id = layer.getModel();
        this.texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/entity/" + id.getPath() + ".png");
    }

    @Override
    public void render(Boat boat, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurt = boat.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, boat.getDamage() - partialTick);
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * damage / 10.0F * boat.getHurtDir()));
        }
        float bubble = boat.getBubbleAngle(partialTick);
        if (!Mth.equal(bubble, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(bubble * Mth.DEG_TO_RAD, 1.0F, 0.0F, 1.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        this.model.setupAnim(boat, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer buffer = buffers.getBuffer(this.model.renderType(this.texture));
        this.model.renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        if (!boat.isUnderWater() && this.model instanceof WaterPatchModel patch) {
            patch.waterPatch().render(poseStack, buffers.getBuffer(RenderType.waterMask()), light, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        super.render(boat, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return this.texture;
    }
}
