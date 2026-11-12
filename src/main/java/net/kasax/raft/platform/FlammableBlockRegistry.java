package net.kasax.raft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;

/**
 * Fabric's flammable block registry. On NeoForge for 26.3 the fire block reads the data map {@code neoforge:flammable};
 * Beach Craft ships it in {@code data/neoforge/data_maps/block/flammable.json}. The values registered here are kept
 * so the game tests can check that file against the code.
 */
public final class FlammableBlockRegistry {
    private static final FlammableBlockRegistry DEFAULT = new FlammableBlockRegistry();
    private final Map<Block, int[]> entries = new LinkedHashMap<>();

    private FlammableBlockRegistry() {
    }

    public static FlammableBlockRegistry getDefaultInstance() {
        return DEFAULT;
    }

    /** Fabric's order: burn (vanilla's ignite odds), then spread (vanilla's burn odds). */
    public void add(Block block, int burn, int spread) {
        entries.put(block, new int[] {burn, spread});
    }

    public Map<Block, int[]> registered() {
        return entries;
    }
}
