package net.kasax.raft;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.kasax.raft.datagen.*;
import net.kasax.raft.world.biome.ModBiomes;
import net.kasax.raft.world.dimension.ModDimensions;
import net.kasax.raft.world.gen.ModConfiguredFeatures;
import net.kasax.raft.world.gen.ModPlacedFeatures;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class RaftDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider(ModItemTagProvider::new);
		pack.addProvider(ModLootTableProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModWorldGenerator::new);
		pack.addProvider(ModBiomeTagProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.FEATURE, ModConfiguredFeatures::bootstrap);
		registryBuilder.add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);
		registryBuilder.add(Registries.DIMENSION_TYPE, ModDimensions::bootstrapType);
		registryBuilder.add(Registries.BIOME, ModBiomes::bootstrap);
	}
}
