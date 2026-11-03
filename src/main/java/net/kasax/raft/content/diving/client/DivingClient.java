package net.kasax.raft.content.diving.client;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.content.diving.DivingGear;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client-only init of the "diving" module: harpoon renderer, gear layer, oxygen HUD, compressor screen. */
public final class DivingClient {
    /** How much farther one sees under water with the diving helmet. */
    public static final float HELMET_FOG_FACTOR = 2.2F;

    private DivingClient() {
    }

    public static void init() {
        ModelLayerRegistry.registerModelLayer(HarpoonModel.LAYER, HarpoonModel::createLayer);
        ModelLayerRegistry.registerModelLayer(DivingGearModels.HELMET, DivingGearModels::createHelmet);
        ModelLayerRegistry.registerModelLayer(DivingGearModels.COLLAR, DivingGearModels::createCollar);
        ModelLayerRegistry.registerModelLayer(DivingGearModels.TANK, DivingGearModels::createTank);
        ModelLayerRegistry.registerModelLayer(DivingGearModels.FLIPPER, DivingGearModels::createFlipper);
        EntityRendererRegistry.register(DivingContent.HARPOON, HarpoonRenderer::new);
        MenuScreens.register(DivingContent.AIR_COMPRESSOR_MENU, AirCompressorScreen::new);
        HudElementRegistry.attachElementAfter(VanillaHudElements.AIR_BAR, OxygenHud.ID, OxygenHud::extract);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer.getModel() instanceof HumanoidModel<?>) {
                helper.register(gearLayer(renderer, context));
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DivingGearLayer gearLayer(LivingEntityRenderer<?, ?, ?> renderer,
                                             net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        return new DivingGearLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
                (RenderLayerParent) renderer, context.getModelSet());
    }

    /** True if the camera entity wears the diving helmet (for the water fog). */
    public static boolean cameraWearsHelmet(Camera camera) {
        Entity entity = camera.entity();
        return entity instanceof LivingEntity living && DivingGear.wearsHelmet(living);
    }
}
