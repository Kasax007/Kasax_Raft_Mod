package net.kasax.raft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.kasax.raft.block.entity.renderer.ItemCatcherBlockEntityRenderer;
import net.kasax.raft.screen.ItemCollectorScreen;
import net.kasax.raft.screen.ModScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

public class RaftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRIFTWOOD_LEAVES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRIFTWOOD_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TITANIUM_GRATES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRIFTWOOD_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRIFTWOOD_TRAPDOOR, RenderLayer.getCutout());

        HandledScreens.register(ModScreenHandlers.ITEM_COLLECTOR_SCREEN_HANDLER, ItemCollectorScreen::new);

        BlockEntityRendererFactories.register(ModBlockEntities.ITEM_COLLECTOR_BLOCK_ENTITY, ItemCatcherBlockEntityRenderer::new);

    }
}
