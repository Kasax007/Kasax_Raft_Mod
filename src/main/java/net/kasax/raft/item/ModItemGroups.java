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

                        entries.add(ModBlocks.DRIFTWOOD_PORTAL_FRAME);

                        entries.add(ModBlocks.STRIPPED_DRIFTWOOD_LOG);
                        entries.add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD);
                        entries.add(ModBlocks.DRIFTWOOD_LEAVES);
                        entries.add(ModBlocks.DRIFTWOOD_LOG);
                        entries.add(ModBlocks.DRIFTWOOD_WOOD);
                        entries.add(ModBlocks.DRIFTWOOD_PLANKS);

                    }).build());


    public static void registerItemGroups() {
        Raft.LOGGER.info("Registering Raft Item Group for " + Raft.MOD_ID);
    }
}
