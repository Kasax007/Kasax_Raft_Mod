package net.kasax.raft.platform;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric's PointOfInterestHelper: NeoForge's registry callback fills the block state to POI map itself. */
public final class PointOfInterestHelper {
    private PointOfInterestHelper() {
    }

    public static PoiType register(ResourceLocation id, int ticketCount, int searchDistance, Block... blocks) {
        Set<BlockState> states = new HashSet<>();
        for (Block block : blocks) {
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        return register(id, ticketCount, searchDistance, states);
    }

    public static PoiType register(ResourceLocation id, int ticketCount, int searchDistance, Iterable<BlockState> states) {
        PoiType type = new PoiType(ImmutableSet.copyOf(states), ticketCount, searchDistance);
        return Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, id, type);
    }
}
