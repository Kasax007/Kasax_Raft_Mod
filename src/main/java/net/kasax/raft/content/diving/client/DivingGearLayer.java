package net.kasax.raft.content.diving.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.DivingContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

/**
 * Adds the 3D parts of the diving gear to humanoid models (players, armor stands, zombies...): helmet portholes and
 * valve, the brass collar, the air cylinders on the back and the flipper blades.
 */
public class DivingGearLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final Identifier HELMET_TEXTURE = Raft.id("textures/entity/diving/diving_helmet_gear.png");
    private static final Identifier TANK_TEXTURE = Raft.id("textures/entity/diving/oxygen_tank_gear.png");
    private static final Identifier TITANIUM_TANK_TEXTURE = Raft.id("textures/entity/diving/titanium_oxygen_tank_gear.png");
    private static final Identifier FLIPPER_TEXTURE = Raft.id("textures/entity/diving/flippers_gear.png");

    private final Model.Simple helmet;
    private final Model.Simple lamp;
    private final Model.Simple collar;
    private final Model.Simple tank;
    private final Model.Simple flipper;

    public DivingGearLayer(RenderLayerParent<S, M> parent, EntityModelSet models) {
        super(parent);
        this.helmet = new Model.Simple(models.bakeLayer(DivingGearModels.HELMET), RenderTypes::armorCutoutNoCull);
        this.lamp = new Model.Simple(models.bakeLayer(DivingGearModels.HELMET_LAMP), RenderTypes::armorCutoutNoCull);
        this.collar = new Model.Simple(models.bakeLayer(DivingGearModels.COLLAR), RenderTypes::armorCutoutNoCull);
        this.tank = new Model.Simple(models.bakeLayer(DivingGearModels.TANK), RenderTypes::armorCutoutNoCull);
        this.flipper = new Model.Simple(models.bakeLayer(DivingGearModels.FLIPPER), RenderTypes::armorCutoutNoCull);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        M model = this.getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        if (state.headEquipment.is(DivingContent.DIVING_HELMET)) {
            draw(helmet, HELMET_TEXTURE, model, model.head, poseStack, collector, light, overlay, state);
            draw(collar, HELMET_TEXTURE, model, model.body, poseStack, collector, light, overlay, state);
            if (state.headEquipment.has(DivingContent.DIVING_LAMP)) {
                draw(lamp, HELMET_TEXTURE, model, model.head, poseStack, collector, LightCoordsUtil.FULL_BRIGHT, overlay, state);
            }
        }
        ItemStack chest = state.chestEquipment;
        if (chest.is(DivingContent.OXYGEN_TANK) || chest.is(DivingContent.TITANIUM_OXYGEN_TANK)) {
            draw(tank, chest.is(DivingContent.TITANIUM_OXYGEN_TANK) ? TITANIUM_TANK_TEXTURE : TANK_TEXTURE, model, model.body,
                    poseStack, collector, light, overlay, state);
        }
        if (state.feetEquipment.is(DivingContent.FLIPPERS)) {
            draw(flipper, FLIPPER_TEXTURE, model, model.rightLeg, poseStack, collector, light, overlay, state);
            draw(flipper, FLIPPER_TEXTURE, model, model.leftLeg, poseStack, collector, light, overlay, state);
        }
    }

    private void draw(Model.Simple part, Identifier texture, M model, ModelPart anchor, PoseStack poseStack,
                      SubmitNodeCollector collector, int light, int overlay, S state) {
        if (!anchor.visible) {
            return;
        }
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        anchor.translateAndRotate(poseStack);
        collector.submitModel(part, Unit.INSTANCE, poseStack, part.renderType(texture), light, overlay, state.outlineColor);
        poseStack.popPose();
    }
}
