package net.kasax.raft;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.CableBlockEntity;
import net.kasax.raft.block.entity.*;
import net.kasax.raft.item.ModItemGroups;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.recipe.ModRecipes;
import net.kasax.raft.screen.ModScreenHandlers;
import net.kasax.raft.sound.ModSounds;
import net.kasax.raft.util.*;
import net.kasax.raft.world.gen.ModWorldGeneration;
import net.kasax.raft.world.gen.RandomizedBlockStateProvider;
import net.kasax.raft.world.tree.StarFoliagePlacer;
import net.kyrptonaught.customportalapi.api.CustomPortalBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.foliage.FoliagePlacerType;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.reborn.energy.api.EnergyStorage;

public class Raft implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "raft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final FoliagePlacerType<StarFoliagePlacer> STAR_FOLIAGE_PLACER =
			Registry.register(Registries.FOLIAGE_PLACER_TYPE, Identifier.of(Raft.MOD_ID, "star_foliage_placer"),
					new FoliagePlacerType<>(StarFoliagePlacer.CODEC));

	public static final BlockStateProviderType<RandomizedBlockStateProvider> RANDOMIZED_BLOCK_STATE_PROVIDER =
			Registry.register(Registries.BLOCK_STATE_PROVIDER_TYPE,
					Identifier.of("your_mod_id", "randomized_block_state_provider"),
					new BlockStateProviderType<>(RandomizedBlockStateProvider.CODEC));

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModItemGroups.registerItemGroups();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		StrippableBlockRegistry.register(ModBlocks.DRIFTWOOD_LOG, ModBlocks.STRIPPED_DRIFTWOOD_LOG);
		StrippableBlockRegistry.register(ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_WOOD);

		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_DRIFTWOOD_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_PLANKS, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_LEAVES, 30, 60);

		StrippableBlockRegistry.register(ModBlocks.PALM_LOG, ModBlocks.STRIPPED_PALM_LOG);
		StrippableBlockRegistry.register(ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_WOOD);

		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_PALM_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_PALM_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_PLANKS, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_COCONUT_LEAVES, 30, 60);

		ModLootTableModifiers.modifyLootTables();
		ModWorldGeneration.generateModWorldGen();
		ModSounds.registerSounds();

		ModRecipes.registerRecipes();

		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();

		// Register the codec for the energy sync payload
		PayloadTypeRegistry.playS2C().register(EnergySyncPayload.ID, EnergySyncPayload.CODEC);

		EnergyStorage.SIDED.registerForBlockEntity(MakeshiftSolarPanelBlockEntity::getEnergyProvider, ModBlockEntities.MAKESHIT_SOLAR_PANEL_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(MakeshiftBatteryBlockEntity::getEnergyProvider, ModBlockEntities.MAKESHIT_BATTERY_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(QuarryBlockEntity::getEnergyProvider, ModBlockEntities.QUARRY_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(ChunkDestroyerBlockEntity::getEnergyProvider, ModBlockEntities.CHUNK_DESTROYER_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(CableBlockEntity::getSideEnergyStorage, ModBlockEntities.CABLE_BLOCK_ENTITY);

		CustomPortalBuilder.beginPortal().frameBlock(ModBlocks.DRIFTWOOD_PORTAL_FRAME)
				.lightWithItem(ModItems.ENERGY_STAFF)
						.destDimID(Identifier.of(Raft.MOD_ID, "raftdim"))
								.tintColor(0x2C3B8A)
				.setPortalSearchYRange(200, 220)
				.setReturnPortalSearchYRange(200, 220)
				.onlyLightInOverworld()
				//.registerInPortalAmbienceSound(ModSounds.Portal_Teleport_Sound)
				.registerPortal();

		FurnaceBlocks.init();
		FurnaceEntities.init();

		ModBlocks.registerCables();

		LOGGER.info("Raft mod initialized!");
	}
}