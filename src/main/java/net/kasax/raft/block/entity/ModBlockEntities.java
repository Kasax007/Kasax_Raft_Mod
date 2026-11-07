package net.kasax.raft.block.entity;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.CableBlockEntity;
import net.kasax.raft.block.cable.RaftCable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ModBlockEntities {

    public static final BlockEntityType<ItemCollectorBlockEntity> ITEM_COLLECTOR_BLOCK_ENTITY =
            register("item_collector_be", ItemCollectorBlockEntity::new, ModBlocks.ITEM_CATCHER);

    public static final BlockEntityType<MakeshiftSolarPanelBlockEntity> MAKESHIFT_SOLAR_PANEL_BLOCK_ENTITY =
            register("makeshift_solar_panel_be", MakeshiftSolarPanelBlockEntity::new, ModBlocks.MAKESHIFT_SOLAR_PANEL);

    public static final BlockEntityType<MakeshiftBatteryBlockEntity> MAKESHIFT_BATTERY_BLOCK_ENTITY =
            register("makeshift_battery_be", MakeshiftBatteryBlockEntity::new, ModBlocks.MAKESHIFT_BATTERY);

    public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY =
            register("quarry_be", QuarryBlockEntity::new, ModBlocks.QUARRY);

    public static final BlockEntityType<ChunkDestroyerBlockEntity> CHUNK_DESTROYER_BLOCK_ENTITY =
            register("chunk_destroyer_be", ChunkDestroyerBlockEntity::new, ModBlocks.CHUNK_DESTROYER);

    public static final BlockEntityType<SimpleGrillBlockEntity> SIMPLE_GRILL_BLOCK_ENTITY =
            register("simple_grill_be", SimpleGrillBlockEntity::new, ModBlocks.SIMPLE_GRILL);

    public static final BlockEntityType<WaterPurifierBlockEntity> WATER_PURIFIER_BLOCK_ENTITY =
            register("water_purifier_be", WaterPurifierBlockEntity::new, ModBlocks.WATER_PURIFIER);

    public static final BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Raft.id("cable_block_entity"),
                    BlockEntityType.Builder.of(CableBlockEntity::new,
                            Arrays.stream(RaftCable.Cables.values())
                                    .map(cable -> (Block) cable.block)
                                    .toArray(Block[]::new)).build(null));

    private static <T extends BlockEntity> BlockEntityType<T> register(String name,
                                                                       BlockEntityType.BlockEntitySupplier<T> factory,
                                                                       Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Raft.id(name),
                BlockEntityType.Builder.of(factory, blocks).build(null));
    }

    public static void registerBlockEntities() {
        Raft.LOGGER.info("Registering Block Entities for " + Raft.MOD_ID);
    }
}
