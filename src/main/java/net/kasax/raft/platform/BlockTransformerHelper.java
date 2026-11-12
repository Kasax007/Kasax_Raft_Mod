package net.kasax.raft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Fabric's BlockTransformerHelper (axe stripping). On NeoForge for 26.3 extra axe transforms come from the data map
 * {@code neoforge:transformables}; Beach Craft ships it in {@code data/neoforge/data_maps/block/transformables.json}.
 * The registrations are kept so the game tests can check that file against the code.
 */
public final class BlockTransformerHelper {
    private static final Map<Block, BlockStateProvider> STRIPPING = new LinkedHashMap<>();

    private BlockTransformerHelper() {
    }

    public static void registerStripping(Block block, BlockStateProvider stripped) {
        STRIPPING.put(block, stripped);
    }

    public static Map<Block, BlockStateProvider> stripping() {
        return STRIPPING;
    }
}
