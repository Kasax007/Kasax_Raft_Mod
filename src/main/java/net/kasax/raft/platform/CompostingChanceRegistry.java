package net.kasax.raft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * Fabric's composting chance registry. On NeoForge the composter reads only the data map
 * {@code neoforge:compostables}; Beach Craft ships it in {@code data/neoforge/data_maps/item/compostables.json}. The
 * values registered here are kept so the game tests can check that file against the code.
 */
public final class CompostingChanceRegistry {
    public static final CompostingChanceRegistry INSTANCE = new CompostingChanceRegistry();
    private final Map<Item, Float> chances = new LinkedHashMap<>();

    private CompostingChanceRegistry() {
    }

    public void add(ItemLike item, Float chance) {
        chances.put(item.asItem(), chance);
    }

    public Map<Item, Float> registered() {
        return chances;
    }
}
