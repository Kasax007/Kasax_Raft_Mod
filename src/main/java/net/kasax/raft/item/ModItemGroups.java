package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.util.FurnaceBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * The items of the original Raft mod for the creative tab "Raft & Survival" ({@link BcTabs.Tab#RAFT}). Their order on
 * screen, and the wood sets and food that moved to the Building, Nature, Kitchen and Creatures tabs, are set in
 * {@link net.kasax.raft.content.BcTabOrder}.
 */
public class ModItemGroups {
    private static void add(ItemLike item) {
        BcTabs.add(BcTabs.Tab.RAFT, item);
    }

    public static void registerItemGroups() {
        Raft.LOGGER.info("Registering Raft Item Group for " + Raft.MOD_ID);
        BcTabs.icon(BcTabs.Tab.RAFT, () -> new ItemStack(ModItems.ANCIENT_ENERGY));

        add(ModItems.ANCIENT_BROKEN_RING);
        add(ModItems.ANCIENT_ENERGY);
        add(ModItems.ANCIENT_CRYSTAL);
        add(ModItems.PLASTIC);
        add(ModItems.CIRCUIT_BOARD);
        add(ModItems.TRASH_CUBE);
        add(ModItems.UNREFINED_OCEAN_GARBAGE);

        add(ModItems.ENERGY_STAFF);
        add(ModItems.RAFT_TELEPORTER);
        add(ModItems.METAL_DETECTOR);

        add(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

        add(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
        add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
        add(ModBlocks.DRIFTWOOD_LEAVES);
        add(ModBlocks.DRIFTWOOD_SAPLING);
        add(ModBlocks.DRIFTWOOD_LOG);
        add(ModBlocks.DRIFTWOOD_WOOD);
        add(ModBlocks.DRIFTWOOD_PLANKS);

        add(ModBlocks.DRIFTWOOD_STAIRS);
        add(ModBlocks.DRIFTWOOD_SLAB);
        add(ModBlocks.DRIFTWOOD_DOOR);
        add(ModBlocks.DRIFTWOOD_TRAPDOOR);
        add(ModBlocks.DRIFTWOOD_FENCE);
        add(ModBlocks.DRIFTWOOD_FENCE_GATE);
        add(ModBlocks.DRIFTWOOD_BUTTON);
        add(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);

        add(ModBlocks.STRIPPED_PALM_LOG);
        add(ModBlocks.STRIPPED_PALM_WOOD);
        add(ModBlocks.PALM_LEAVES);
        add(ModBlocks.PALM_COCONUT_LEAVES);
        add(ModItems.COCONUT);
        add(ModItems.PINA_COLADA);
        add(ModBlocks.PALM_SAPLING);
        add(ModBlocks.PALM_LOG);
        add(ModBlocks.PALM_WOOD);
        add(ModBlocks.PALM_PLANKS);

        add(ModBlocks.PALM_STAIRS);
        add(ModBlocks.PALM_SLAB);
        add(ModBlocks.PALM_DOOR);
        add(ModBlocks.PALM_TRAPDOOR);
        add(ModBlocks.PALM_FENCE);
        add(ModBlocks.PALM_FENCE_GATE);
        add(ModBlocks.PALM_BUTTON);
        add(ModBlocks.PALM_PRESSURE_PLATE);

        add(ModBlocks.TITANIUM_ORE);
        add(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        add(ModBlocks.RAW_TITANIUM_BLOCK);
        add(ModBlocks.TITANIUM_BLOCK);
        add(ModItems.RAW_TITANIUM);
        add(ModItems.TITANIUM_INGOT);
        add(ModItems.TITANIUM_NUGGET);

        add(ModItems.TITANIUM_HELMET);
        add(ModItems.TITANIUM_CHESTPLATE);
        add(ModItems.TITANIUM_LEGGINGS);
        add(ModItems.TITANIUM_BOOTS);

        add(ModBlocks.TITANIUM_GRATES);
        add(ModBlocks.MAKESHIFT_SOLAR_PANEL);
        add(ModBlocks.MAKESHIFT_BATTERY);

        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
            add(cable);
        }

        add(ModBlocks.QUARRY);
        add(ModBlocks.CHUNK_DESTROYER);

        add(ModItems.SOME_GAME_MUSIC_MUSIC_DISC);
        add(ModItems.LOFI_BEAT_MUSIC_DISC);
        add(ModItems.CRYSTAL_CASTLES_MUSIC_DISC);

        add(ModBlocks.ITEM_CATCHER);
        add(ModItems.NET);

        // decoration
        add(ModBlocks.PALM_THATCH);
        add(ModBlocks.PALM_THATCH_STAIRS);
        add(ModBlocks.PALM_THATCH_SLAB);
        add(ModBlocks.RAFT_FOUNDATION);
        add(ModBlocks.REINFORCED_FOUNDATION);
        add(ModBlocks.SCRAP_METAL_BLOCK);
        add(ModBlocks.PLASTIC_BLOCK);
        add(ModBlocks.ROPE_LADDER);
        add(ModBlocks.ROPE_COIL);
        add(ModBlocks.SAIL_CANVAS);
        add(ModBlocks.SAIL_CANVAS_RED_STRIPED);
        add(ModBlocks.SHIP_WHEEL);
        add(ModBlocks.SHARK_JAW_TROPHY);
        add(ModBlocks.RAFT_FLAG);
        add(ModBlocks.COCONUT_LANTERN);

        // survival on the raft
        add(ModBlocks.CROP_PLOT);
        add(ModBlocks.SIMPLE_GRILL);
        add(ModBlocks.WATER_PURIFIER);
        add(ModBlocks.SCARECROW);
        add(ModItems.ROPE);
        add(ModItems.SCRAP_METAL);
        add(ModItems.FRESH_WATER);
        add(ModItems.COCONUT_MILK);
        add(ModItems.RECEIVER);
        add(ModItems.PLASTIC_HOOK);
        add(ModItems.PADDLE);

        add(ModItems.SHARK_TOOTH);
        add(ModItems.SHARK_MEAT);
        add(ModItems.COOKED_SHARK_MEAT);
        add(ModItems.SHARK_SPAWN_EGG);

        // Add custom furnaces to the group
        for (Block furnace : FurnaceBlocks.getFurnaces()) {
            add(furnace);
        }
    }
}
