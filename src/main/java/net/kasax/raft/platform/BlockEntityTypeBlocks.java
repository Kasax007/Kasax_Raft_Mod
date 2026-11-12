package net.kasax.raft.platform;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

/** Fabric's injected {@code BlockEntityType.addValidBlock}: handed to NeoForge in {@link BlockEntityTypeAddBlocksEvent}. */
public final class BlockEntityTypeBlocks {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private BlockEntityTypeBlocks() {
    }

    public static void addValidBlock(BlockEntityType<?> type, Block block) {
        ENTRIES.add(new Entry(type, block));
    }

    static void onAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        for (Entry e : ENTRIES) event.modify(e.type, e.block);
    }

    private record Entry(BlockEntityType<?> type, Block block) {
    }
}
