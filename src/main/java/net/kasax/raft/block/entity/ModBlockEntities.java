package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

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

    public static void registerBlockEntities() {
        Raft.LOGGER.info("Registering Block Entities for "+ Raft.MOD_ID);
    }
}
