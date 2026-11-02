package net.kasax.raft.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.RaftCable;
import net.kasax.raft.util.FurnaceBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ModItemGroups {
    public static final CreativeModeTab RAFT_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Raft.id("raft"),
            FabricCreativeModeTab.builder().title(Component.translatable("itemgroup.raft"))
                    .icon(() -> new ItemStack(ModItems.ANCIENT_ENERGY)).displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.ANCIENT_BROKEN_RING);
                        entries.accept(ModItems.ANCIENT_ENERGY);
                        entries.accept(ModItems.ANCIENT_CRYSTAL);
                        entries.accept(ModItems.PLASTIC);
                        entries.accept(ModItems.CIRCUIT_BOARD);
                        entries.accept(ModItems.TRASH_CUBE);
                        entries.accept(ModItems.UNREFINED_OCEAN_GARBAGE);

                        entries.accept(ModItems.ENERGY_STAFF);
                        entries.accept(ModItems.RAFT_TELEPORTER);
                        entries.accept(ModItems.METAL_DETECTOR);

                        entries.accept(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

                        entries.accept(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
                        entries.accept(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
                        entries.accept(ModBlocks.DRIFTWOOD_LEAVES);
                        entries.accept(ModBlocks.DRIFTWOOD_SAPLING);
                        entries.accept(ModBlocks.DRIFTWOOD_LOG);
                        entries.accept(ModBlocks.DRIFTWOOD_WOOD);
                        entries.accept(ModBlocks.DRIFTWOOD_PLANKS);

                        entries.accept(ModBlocks.DRIFTWOOD_STAIRS);
                        entries.accept(ModBlocks.DRIFTWOOD_SLAB);
                        entries.accept(ModBlocks.DRIFTWOOD_DOOR);
                        entries.accept(ModBlocks.DRIFTWOOD_TRAPDOOR);
                        entries.accept(ModBlocks.DRIFTWOOD_FENCE);
                        entries.accept(ModBlocks.DRIFTWOOD_FENCE_GATE);
                        entries.accept(ModBlocks.DRIFTWOOD_BUTTON);
                        entries.accept(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);

                        entries.accept(ModBlocks.STRIPPED_PALM_LOG);
                        entries.accept(ModBlocks.STRIPPED_PALM_WOOD);
                        entries.accept(ModBlocks.PALM_LEAVES);
                        entries.accept(ModBlocks.PALM_COCONUT_LEAVES);
                        entries.accept(ModItems.COCONUT);
                        entries.accept(ModItems.PINA_COLADA);
                        entries.accept(ModBlocks.PALM_SAPLING);
                        entries.accept(ModBlocks.PALM_LOG);
                        entries.accept(ModBlocks.PALM_WOOD);
                        entries.accept(ModBlocks.PALM_PLANKS);

                        entries.accept(ModBlocks.PALM_STAIRS);
                        entries.accept(ModBlocks.PALM_SLAB);
                        entries.accept(ModBlocks.PALM_DOOR);
                        entries.accept(ModBlocks.PALM_TRAPDOOR);
                        entries.accept(ModBlocks.PALM_FENCE);
                        entries.accept(ModBlocks.PALM_FENCE_GATE);
                        entries.accept(ModBlocks.PALM_BUTTON);
                        entries.accept(ModBlocks.PALM_PRESSURE_PLATE);

                        entries.accept(ModBlocks.TITANIUM_ORE);
                        entries.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE);
                        entries.accept(ModBlocks.RAW_TITANIUM_BLOCK);
                        entries.accept(ModBlocks.TITANIUM_BLOCK);
                        entries.accept(ModItems.RAW_TITANIUM);
                        entries.accept(ModItems.TITANIUM_INGOT);
                        entries.accept(ModItems.TITANIUM_NUGGET);

                        entries.accept(ModItems.TITANIUM_HELMET);
                        entries.accept(ModItems.TITANIUM_CHESTPLATE);
                        entries.accept(ModItems.TITANIUM_LEGGINGS);
                        entries.accept(ModItems.TITANIUM_BOOTS);

                        entries.accept(ModBlocks.TITANIUM_GRATES);
                        entries.accept(ModBlocks.MAKESHIFT_SOLAR_PANEL);
                        entries.accept(ModBlocks.MAKESHIFT_BATTERY);

                        for (RaftCable.Cables cable : RaftCable.Cables.values()) {
                            entries.accept(new ItemStack(cable));
                        }

                        entries.accept(ModBlocks.QUARRY);
                        entries.accept(ModBlocks.CHUNK_DESTROYER);

                        entries.accept(ModItems.SOME_GAME_MUSIC_MUSIC_DISC);
                        entries.accept(ModItems.LOFI_BEAT_MUSIC_DISC);
                        entries.accept(ModItems.CRYSTAL_CASTLES_MUSIC_DISC);

                        entries.accept(ModBlocks.ITEM_CATCHER);
                        entries.accept(ModItems.NET);

                        // decoration
                        entries.accept(ModBlocks.PALM_THATCH);
                        entries.accept(ModBlocks.PALM_THATCH_STAIRS);
                        entries.accept(ModBlocks.PALM_THATCH_SLAB);
                        entries.accept(ModBlocks.RAFT_FOUNDATION);
                        entries.accept(ModBlocks.REINFORCED_FOUNDATION);
                        entries.accept(ModBlocks.SCRAP_METAL_BLOCK);
                        entries.accept(ModBlocks.PLASTIC_BLOCK);
                        entries.accept(ModBlocks.ROPE_LADDER);
                        entries.accept(ModBlocks.ROPE_COIL);
                        entries.accept(ModBlocks.SAIL_CANVAS);
                        entries.accept(ModBlocks.SAIL_CANVAS_RED_STRIPED);
                        entries.accept(ModBlocks.SHIP_WHEEL);
                        entries.accept(ModBlocks.SHARK_JAW_TROPHY);
                        entries.accept(ModBlocks.RAFT_FLAG);
                        entries.accept(ModBlocks.COCONUT_LANTERN);

                        // survival on the raft
                        entries.accept(ModBlocks.CROP_PLOT);
                        entries.accept(ModBlocks.SIMPLE_GRILL);
                        entries.accept(ModBlocks.WATER_PURIFIER);
                        entries.accept(ModBlocks.SCARECROW);
                        entries.accept(ModItems.ROPE);
                        entries.accept(ModItems.SCRAP_METAL);
                        entries.accept(ModItems.FRESH_WATER);
                        entries.accept(ModItems.COCONUT_MILK);
                        entries.accept(ModItems.RECEIVER);
                        entries.accept(ModItems.PLASTIC_HOOK);
                        entries.accept(ModItems.PADDLE);

                        entries.accept(ModItems.SHARK_TOOTH);
                        entries.accept(ModItems.SHARK_MEAT);
                        entries.accept(ModItems.COOKED_SHARK_MEAT);
                        entries.accept(ModItems.SHARK_SPAWN_EGG);

                        // Add custom furnaces to the group
                        for (Block furnace : FurnaceBlocks.getFurnaces()) {
                            entries.accept(new ItemStack(furnace));
                        }
                    }).build());


    public static void registerItemGroups() {
        Raft.LOGGER.info("Registering Raft Item Group for " + Raft.MOD_ID);
    }
}
