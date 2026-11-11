package net.kasax.raft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * Fabric's fuel registry. On NeoForge burn times come from the data map {@code neoforge:furnace_fuels}; Beach Craft
 * ships it in {@code data/neoforge/data_maps/item/furnace_fuels.json}. The values registered here are kept so the
 * game tests can check that file against the code.
 */
public final class FuelRegistry {
    public static final FuelRegistry INSTANCE = new FuelRegistry();
    private final Map<Item, Integer> fuels = new LinkedHashMap<>();

    private FuelRegistry() {
    }

    public void add(ItemLike item, Integer cookTime) {
        fuels.put(item.asItem(), cookTime);
    }

    public Map<Item, Integer> registered() {
        return fuels;
    }
}
