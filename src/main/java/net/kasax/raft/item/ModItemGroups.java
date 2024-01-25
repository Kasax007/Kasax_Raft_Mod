package net.kasax.raft.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup RAFT_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(Raft.MOD_ID, "raft"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.raft"))
                    .icon(() -> new ItemStack(ModItems.ANCIENT_ENERGY)).entries((displayContext, entries) -> {
                        entries.add(ModItems.ANCIENT_BROKEN_RING);
                        entries.add(ModItems.ANCIENT_ENERGY);
                        entries.add(ModItems.ANCIENT_CRYSTAL);

                        entries.add(ModItems.ENERGY_STAFF);

                        entries.add(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

                        entries.add(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
                        entries.add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
                        entries.add(ModBlocks.DRIFTWOOD_LEAVES);
                        entries.add(ModBlocks.DRIFTWOOD_SAPLING);
                        entries.add(ModBlocks.DRIFTWOOD_LOG);
                        entries.add(ModBlocks.DRIFTWOOD_WOOD);
                        entries.add(ModBlocks.DRIFTWOOD_PLANKS);

                        entries.add(ModBlocks.DRIFTWOOD_STAIRS);
                        entries.add(ModBlocks.DRIFTWOOD_SLAB);
                        entries.add(ModBlocks.DRIFTWOOD_DOOR);
                        entries.add(ModBlocks.DRIFTWOOD_TRAPDOOR);
                        entries.add(ModBlocks.DRIFTWOOD_FENCE);
                        entries.add(ModBlocks.DRIFTWOOD_FENCE_GATE);
                        entries.add(ModBlocks.DRIFTWOOD_BUTTON);
                        entries.add(ModBlocks.DRIFTWOOD_PRESSURE_PLATE);

                        entries.add(ModBlocks.TITANIUM_ORE);
                        entries.add(ModBlocks.DEEPSLATE_TITANIUM_ORE);
                        entries.add(ModBlocks.RAW_TITANIUM_BLOCK);
                        entries.add(ModBlocks.TITANIUM_BLOCK);
                        entries.add(ModItems.RAW_TITANIUM);
                        entries.add(ModItems.TITANIUM_INGOT);
                        entries.add(ModItems.TITANIUM_NUGGET);
                        entries.add(ModBlocks.TITANIUM_GRATES);

                        entries.add(ModItems.SOME_GAME_MUSIC_MUSIC_DISC);
                        entries.add(ModItems.LOFI_BEAT_MUSIC_DISC);

                        entries.add(ModBlocks.ITEM_CATCHER);
                        entries.add(ModItems.NET);

                    }).build());


    public static void registerItemGroups() {
        Raft.LOGGER.info("Registering Raft Item Group for " + Raft.MOD_ID);
    }
}
