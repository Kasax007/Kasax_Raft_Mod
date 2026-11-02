package net.kasax.raft;

import net.fabricmc.api.ClientModInitializer;
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
        // Block render layers are picked automatically from the textures in 26.x (no BlockRenderLayerMap).

        MenuScreens.register(ModScreenHandlers.ITEM_COLLECTOR_SCREEN_HANDLER, ItemCollectorScreen::new);
        MenuScreens.register(ModScreenHandlers.MAKESHIFT_BATTERY_SCREEN_HANDLER, MakeshiftBatteryScreen::new);

        ModPackets.registerS2CPackets();

        BlockEntityRenderers.register(ModBlockEntities.ITEM_COLLECTOR_BLOCK_ENTITY, ItemCatcherBlockEntityRenderer::new);
    }
}
