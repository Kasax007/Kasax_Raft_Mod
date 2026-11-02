package net.kasax.raft.world.portal;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Set;

/** Point of interest used to find raft portals quickly, like the nether portal POI. */
public class ModPoiTypes {
    public static final ResourceKey<PoiType> RAFT_PORTAL_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Raft.id("raft_portal"));
    public static PoiType RAFT_PORTAL;

    public static void register() {
        RAFT_PORTAL = PoiTypes.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, RAFT_PORTAL_KEY,
                Set.copyOf(ModBlocks.RAFT_PORTAL.getStateDefinition().getPossibleStates()), 0, 1);
    }
}
