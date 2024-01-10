package net.kasax.raft.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.kasax.raft.Raft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ANCIENT_ENERGY = registerItem("ancient_energy", new Item(new FabricItemSettings()));
    public static final Item ANCIENT_BROKEN_RING= registerItem("ancient_broken_ring", new Item(new FabricItemSettings()));

    //Item layout
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Raft.MOD_ID, name), item);
    }
    public static void registerModItems() {
        Raft.LOGGER.info("Registering Mod Items for " + Raft.MOD_ID);
    }
}
