package net.kasax.raft.block.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.CableBlockEntity;
import net.kasax.raft.block.cable.RaftCable;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModBlockEntities {

    public static final BlockEntityType<ItemCollectorBlockEntity> ITEM_COLLECTOR_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Raft.MOD_ID, "item_collector_be"),
                    BlockEntityType.Builder.create(ItemCollectorBlockEntity::new,
                            ModBlocks.ITEM_CATCHER).build());

    public static final BlockEntityType<MakeshiftSolarPanelBlockEntity> MAKESHIT_SOLAR_PANEL_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Raft.MOD_ID, "makeshift_solar_panel_be"),
                    BlockEntityType.Builder.create(MakeshiftSolarPanelBlockEntity::new,
                            ModBlocks.MAKESHIFT_SOLAR_PANEL).build());

    public static final BlockEntityType<MakeshiftBatteryBlockEntity> MAKESHIT_BATTERY_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Raft.MOD_ID, "makeshift_battery_be"),
                    BlockEntityType.Builder.create(MakeshiftBatteryBlockEntity::new,
                            ModBlocks.MAKESHIFT_BATTERY).build());

    public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Raft.MOD_ID, "quarry_be"),
                    BlockEntityType.Builder.create(QuarryBlockEntity::new,
                            ModBlocks.QUARRY).build());

    public static final BlockEntityType<ChunkDestroyerBlockEntity> CHUNK_DESTROYER_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Raft.MOD_ID, "chunk_destroyer_be"),
                    BlockEntityType.Builder.create(ChunkDestroyerBlockEntity::new,
                            ModBlocks.CHUNK_DESTROYER).build());

    public static final BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Raft.MOD_ID, "cable_block_entity"),
                    BlockEntityType.Builder.create(CableBlockEntity::new,
                            Arrays.stream(RaftCable.Cables.values())
                                    .map(cable -> cable.block) // Get the blocks from the enum
                                    .toArray(Block[]::new)
                    ).build()
            );


    public static void registerBlockEntities() {
        Raft.LOGGER.info("Registering Block Entities for "+ Raft.MOD_ID);
    }
}
