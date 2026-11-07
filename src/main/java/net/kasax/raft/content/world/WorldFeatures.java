package net.kasax.raft.content.world;

import com.mojang.serialization.MapCodec;
import net.kasax.raft.Raft;
import net.kasax.raft.content.world.feature.BubbleVentFeature;
import net.kasax.raft.content.world.feature.ClearOfStructuresFilter;
import net.kasax.raft.content.world.feature.CraterLavaFeature;
import net.kasax.raft.content.world.feature.GravitySupportFeature;
import net.kasax.raft.content.world.feature.IceFloeFeature;
import net.kasax.raft.content.world.feature.RubbleFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Feature types of the raft dimension (used by the feature JSON of tools/content/world/features.py). */
public final class WorldFeatures {
    private WorldFeatures() {
    }

    private static <C extends net.kasax.raft.compat.worldgen.WorldFeature> void register(String name, MapCodec<C> codec) {
        Registry.register(BuiltInRegistries.FEATURE, Raft.id(name), new net.kasax.raft.compat.worldgen.CompatFeature<>(codec));
    }

    public static void init() {
        register("crater_lava", CraterLavaFeature.CODEC);
        register("bubble_vents", BubbleVentFeature.CODEC);
        register("rubble", RubbleFeature.CODEC);
        register("ice_floe", IceFloeFeature.CODEC);
        register("gravity_support", GravitySupportFeature.CODEC);
        // features and placement of the newer game that 1.21.1 lacks (the resource converter maps them to raft:*)
        register("overlay", net.kasax.raft.compat.worldgen.OverlayFeature.CODEC);
        register("block_blob", net.kasax.raft.compat.worldgen.BlockBlobFeature.CODEC);
        register("fallen_tree", net.kasax.raft.compat.worldgen.FallenTreeFeature.CODEC);
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Raft.id("offset"), net.kasax.raft.compat.worldgen.OffsetPlacement.TYPE);
        // placement filter: keeps features (basalt columns, ash, lava, boulders...) out of structures
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Raft.id("clear_of_structures"), ClearOfStructuresFilter.TYPE);
    }
}
