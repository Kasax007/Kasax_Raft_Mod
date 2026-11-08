package net.kasax.raft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.kasax.raft.client.render.SharkModel;
import net.kasax.raft.client.render.SharkRenderer;
import net.kasax.raft.entity.ModEntities;
import net.kasax.raft.block.entity.renderer.SimpleGrillBlockEntityRenderer;
import net.kasax.raft.screen.WaterPurifierScreen;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.kasax.raft.block.entity.renderer.ItemCatcherBlockEntityRenderer;
import net.kasax.raft.screen.ItemCollectorScreen;
import net.kasax.raft.screen.MakeshiftBatteryScreen;
import net.kasax.raft.screen.ModScreenHandlers;
import net.kasax.raft.util.ModPackets;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class RaftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1.21.1: the render layers the newer game picks from the textures (tools/port1211/layers.py)
        net.kasax.raft.compat.client.RenderLayers.init();

        MenuScreens.register(ModScreenHandlers.ITEM_COLLECTOR_SCREEN_HANDLER, ItemCollectorScreen::new);
        MenuScreens.register(ModScreenHandlers.MAKESHIFT_BATTERY_SCREEN_HANDLER, MakeshiftBatteryScreen::new);

        MenuScreens.register(ModScreenHandlers.WATER_PURIFIER_MENU, WaterPurifierScreen::new);

        ModPackets.registerS2CPackets();

        BlockEntityRenderers.register(ModBlockEntities.ITEM_COLLECTOR_BLOCK_ENTITY, ItemCatcherBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SIMPLE_GRILL_BLOCK_ENTITY, SimpleGrillBlockEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(SharkModel.LAYER, SharkModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.SHARK, SharkRenderer::new);

        net.kasax.raft.compat.client.ItemModelProperties.init();
        // Beach Craft 3 content modules
        net.kasax.raft.content.ModContent.initClient();
    }
}
