package net.kasax.raft.world.portal;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Set;
import java.util.stream.Collectors;

/** Point of interest used to find raft portals quickly, like the nether portal POI. */
public class ModPoiTypes {
    public static final ResourceKey<PoiType> RAFT_PORTAL_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Raft.id("raft_portal"));
    public static PoiType RAFT_PORTAL;

    public static final ResourceKey<PoiType> SCARECROW_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Raft.id("scarecrow"));
    public static PoiType SCARECROW;

    public static void register() {
        RAFT_PORTAL = net.kasax.raft.platform.PointOfInterestHelper.register(RAFT_PORTAL_KEY.location(), 0, 1,
                Set.copyOf(ModBlocks.RAFT_PORTAL.getStateDefinition().getPossibleStates()));

        // Only the lower half of a scarecrow counts.
        Set<BlockState> scarecrowStates = ModBlocks.SCARECROW.getStateDefinition().getPossibleStates().stream()
                .filter(state -> state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER)
                .collect(Collectors.toSet());
        SCARECROW = net.kasax.raft.platform.PointOfInterestHelper.register(SCARECROW_KEY.location(), 0, 1, scarecrowStates);
    }
}
