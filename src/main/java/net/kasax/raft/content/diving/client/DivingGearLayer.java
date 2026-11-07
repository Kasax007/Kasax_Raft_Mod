package net.kasax.raft.content.diving.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.compat.render.Model;
import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the diving gear on players and humanoid mobs: helmet (with lamp), collar, oxygen tank and flippers, each fixed
 * to the body part it sits on.
 */
public class DivingGearLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation HELMET_TEXTURE = Raft.id("textures/entity/diving/diving_helmet_gear.png");
    private static final ResourceLocation TANK_TEXTURE = Raft.id("textures/entity/diving/oxygen_tank_gear.png");
    private static final ResourceLocation TITANIUM_TANK_TEXTURE = Raft.id("textures/entity/diving/titanium_oxygen_tank_gear.png");
    private static final ResourceLocation FLIPPER_TEXTURE = Raft.id("textures/entity/diving/flippers_gear.png");
    private static final int FULL_BRIGHT = 15728880;

    private final Model.Simple helmet;
    private final Model.Simple lamp;
    private final Model.Simple collar;
    private final Model.Simple tank;
    private final Model.Simple flipper;

    public DivingGearLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        this.helmet = new Model.Simple(models.bakeLayer(DivingGearModels.HELMET), RenderType::armorCutoutNoCull);
        this.lamp = new Model.Simple(models.bakeLayer(DivingGearModels.HELMET_LAMP), RenderType::armorCutoutNoCull);
        this.collar = new Model.Simple(models.bakeLayer(DivingGearModels.COLLAR), RenderType::armorCutoutNoCull);
        this.tank = new Model.Simple(models.bakeLayer(DivingGearModels.TANK), RenderType::armorCutoutNoCull);
        this.flipper = new Model.Simple(models.bakeLayer(DivingGearModels.FLIPPER), RenderType::armorCutoutNoCull);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        M model = this.getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(DivingContent.DIVING_HELMET)) {
            draw(helmet, HELMET_TEXTURE, model.head, poseStack, buffers, light, overlay);
            draw(collar, HELMET_TEXTURE, model.body, poseStack, buffers, light, overlay);
            if (head.has(DivingContent.DIVING_LAMP)) {
                draw(lamp, HELMET_TEXTURE, model.head, poseStack, buffers, FULL_BRIGHT, overlay);
            }
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(DivingContent.OXYGEN_TANK) || chest.is(DivingContent.TITANIUM_OXYGEN_TANK)) {
            draw(tank, chest.is(DivingContent.TITANIUM_OXYGEN_TANK) ? TITANIUM_TANK_TEXTURE : TANK_TEXTURE, model.body,
                    poseStack, buffers, light, overlay);
        }
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(DivingContent.FLIPPERS)) {
            draw(flipper, FLIPPER_TEXTURE, model.rightLeg, poseStack, buffers, light, overlay);
            draw(flipper, FLIPPER_TEXTURE, model.leftLeg, poseStack, buffers, light, overlay);
        }
    }

    private void draw(Model.Simple part, ResourceLocation texture, ModelPart anchor, PoseStack poseStack, MultiBufferSource buffers,
                      int light, int overlay) {
        if (!anchor.visible) {
            return;
        }
        poseStack.pushPose();
        anchor.translateAndRotate(poseStack);
        part.resetPose();
        part.renderToBuffer(poseStack, buffers.getBuffer(part.renderType(texture)), light, overlay, -1);
        poseStack.popPose();
    }
}
