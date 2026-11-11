package net.kasax.raft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.kasax.raft.platform.PayloadTypeRegistry;
import net.kasax.raft.platform.FlammableBlockRegistry;
import net.kasax.raft.platform.StrippableBlockRegistry;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.cable.CableBlockEntity;
import net.kasax.raft.block.entity.*;
import net.kasax.raft.entity.ModEntities;
import net.kasax.raft.item.ModItemGroups;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.recipe.ModRecipes;
import net.kasax.raft.screen.ModScreenHandlers;
import net.kasax.raft.sound.ModSounds;
import net.kasax.raft.util.*;
import net.kasax.raft.world.OceanDebris;
import net.kasax.raft.world.gen.ModWorldGeneration;
import net.kasax.raft.world.gen.RandomizedBlockStateProvider;
import net.kasax.raft.world.portal.ModPoiTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.kasax.raft.platform.energy.EnergyStorage;

@Mod(Raft.MOD_ID)
public class Raft {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MOD_ID = "raft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Axes strip the log and keep its axis. */
	private static void strippable(net.minecraft.world.level.block.Block block, net.minecraft.world.level.block.Block stripped) {
		StrippableBlockRegistry.register(block, stripped);
	}

	/** NeoForge: the mod's init runs in the first registry event (net.kasax.raft.platform.NeoPlatform). */
	public Raft(IEventBus modBus, ModContainer container, Dist dist) {
		net.kasax.raft.platform.NeoPlatform.init(modBus, container, dist, this::onInitialize);
	}

	public void onInitialize() {
		// the item component that carries a machine's energy when it is broken (3.4.2)
		StoredEnergy.init();

		// Block state provider used by the palm tree (5% coconut leaves)
		Registry.register(BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, id("randomized_block_state_provider"),
				RandomizedBlockStateProvider.TYPE);

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();

		strippable(ModBlocks.DRIFTWOOD_LOG, ModBlocks.STRIPPED_DRIFTWOOD_LOG);
		strippable(ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_WOOD);

		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_DRIFTWOOD_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_DRIFTWOOD_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_PLANKS, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.DRIFTWOOD_LEAVES, 30, 60);

		strippable(ModBlocks.PALM_LOG, ModBlocks.STRIPPED_PALM_LOG);
		strippable(ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_WOOD);

		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_PALM_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.STRIPPED_PALM_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_PLANKS, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_COCONUT_LEAVES, 30, 60);

		for (var b : new net.minecraft.world.level.block.Block[] {ModBlocks.DRIFTWOOD_STAIRS, ModBlocks.DRIFTWOOD_SLAB, ModBlocks.DRIFTWOOD_FENCE,
				ModBlocks.DRIFTWOOD_FENCE_GATE, ModBlocks.PALM_STAIRS, ModBlocks.PALM_SLAB, ModBlocks.PALM_FENCE, ModBlocks.PALM_FENCE_GATE}) {
			FlammableBlockRegistry.getDefaultInstance().add(b, 5, 20);
		}
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_THATCH, 60, 80);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_THATCH_STAIRS, 60, 80);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.PALM_THATCH_SLAB, 60, 80);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.RAFT_FOUNDATION, 5, 20);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.ROPE_LADDER, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.ROPE_COIL, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.SAIL_CANVAS, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.SAIL_CANVAS_RED_STRIPED, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.RAFT_FLAG, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.SCARECROW, 30, 60);

		ModLootTableModifiers.modifyLootTables();
		ModWorldGeneration.generateModWorldGen();
		ModSounds.registerSounds();

		ModRecipes.registerRecipes();

		ModEntities.register();
		ScarecrowEvents.register();
		OceanDebris.register();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
		ModPoiTypes.register();

		// Register the codec for the energy sync payload
		PayloadTypeRegistry.playS2C().register(EnergySyncPayload.ID, EnergySyncPayload.CODEC);

		EnergyStorage.SIDED.registerForBlockEntity(MakeshiftSolarPanelBlockEntity::getEnergyProvider, ModBlockEntities.MAKESHIFT_SOLAR_PANEL_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(MakeshiftBatteryBlockEntity::getEnergyProvider, ModBlockEntities.MAKESHIFT_BATTERY_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(QuarryBlockEntity::getEnergyProvider, ModBlockEntities.QUARRY_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(ChunkDestroyerBlockEntity::getEnergyProvider, ModBlockEntities.CHUNK_DESTROYER_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity(CableBlockEntity::getSideEnergyStorage, ModBlockEntities.CABLE_BLOCK_ENTITY);
		// machines that keep their energy when broken (cables do not: they only pass it on)
		StoredEnergy.register(ModBlocks.MAKESHIFT_SOLAR_PANEL, MakeshiftSolarPanelBlockEntity.CAPACITY);
		StoredEnergy.register(ModBlocks.MAKESHIFT_BATTERY, MakeshiftBatteryBlockEntity.CAPACITY);
		StoredEnergy.register(ModBlocks.QUARRY, QuarryBlockEntity.CAPACITY);
		StoredEnergy.register(ModBlocks.CHUNK_DESTROYER, ChunkDestroyerBlockEntity.CAPACITY);

		FurnaceBlocks.init();
		FurnaceEntities.init();

		ModBlocks.registerCables();

		// Beach Craft 3 content modules (world, building, decor, creatures, voyage, story, boss)
		net.kasax.raft.content.ModContent.init();
		net.kasax.raft.content.BcTabs.register();

		LOGGER.info("Raft mod initialized!");
	}
}
