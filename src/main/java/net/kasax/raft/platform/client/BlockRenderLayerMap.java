package net.kasax.raft.platform.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/** Fabric's BlockRenderLayerMap on NeoForge's block render type table (used by models without a render type). */
public final class BlockRenderLayerMap {
    public static final BlockRenderLayerMap INSTANCE = new BlockRenderLayerMap();

    private BlockRenderLayerMap() {
    }

    public void putBlock(Block block, RenderType renderLayer) {
        ItemBlockRenderTypes.setRenderLayer(block, renderLayer);
    }

    public void putBlocks(RenderType renderLayer, Block... blocks) {
        for (Block block : blocks) {
            putBlock(block, renderLayer);
        }
    }
}
