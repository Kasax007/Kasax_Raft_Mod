package net.kasax.raft.platform;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's {@code PoiTypes.register} (Fabric widens it): registers the type; NeoForge's registry callback maps the
 * block states to it (vanilla's own mapping would map them twice).
 */
public final class PoiTypesHelper {
    private PoiTypesHelper() {
    }

    public static PoiType register(Registry<PoiType> registry, ResourceKey<PoiType> id, Set<BlockState> matchingStates, int maxTickets, int validRange) {
        return Registry.register(registry, id, new PoiType(matchingStates, maxTickets, validRange));
    }
}
