package net.kasax.raft.content.diving.client;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.content.diving.DivingGear;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.kasax.raft.compat.render.LivingEntityRenderer;
import net.kasax.raft.compat.render.RenderLayerParent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client-only init of the "diving" module: harpoon renderer, gear layer, oxygen HUD, compressor screen. */
public final class DivingClient {
    /** How much farther one sees under water with the diving helmet. */
    public static final float HELMET_FOG_FACTOR = 2.2F;

    private DivingClient() {
    }

    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(HarpoonModel.LAYER, HarpoonModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(DivingGearModels.HELMET, DivingGearModels::createHelmet);
        EntityModelLayerRegistry.registerModelLayer(DivingGearModels.HELMET_LAMP, DivingGearModels::createHelmetLamp);
        EntityModelLayerRegistry.registerModelLayer(DivingGearModels.COLLAR, DivingGearModels::createCollar);
        EntityModelLayerRegistry.registerModelLayer(DivingGearModels.TANK, DivingGearModels::createTank);
        EntityModelLayerRegistry.registerModelLayer(DivingGearModels.FLIPPER, DivingGearModels::createFlipper);
        EntityRendererRegistry.register(DivingContent.HARPOON, HarpoonRenderer::new);
        MenuScreens.register(DivingContent.AIR_COMPRESSOR_MENU, AirCompressorScreen::new);
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, delta) -> OxygenHud.extract(new net.kasax.raft.compat.gui.GuiGraphicsExtractor(graphics), delta));
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer.getModel() instanceof HumanoidModel<?>) {
                helper.register(gearLayer(renderer, context));
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DivingGearLayer gearLayer(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer,
                                             net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        return new DivingGearLayer((net.minecraft.client.renderer.entity.RenderLayerParent) renderer, context.getModelSet());
    }

    /** True if the camera entity wears the diving helmet (for the water fog). */
    public static boolean cameraWearsHelmet(Camera camera) {
        Entity entity = camera.getEntity();
        return entity instanceof LivingEntity living && DivingGear.wearsHelmet(living);
    }
}
