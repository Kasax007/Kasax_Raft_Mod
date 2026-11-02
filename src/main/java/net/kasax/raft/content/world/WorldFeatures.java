package net.kasax.raft.content.world;

import com.mojang.serialization.MapCodec;
import net.kasax.raft.Raft;
import net.kasax.raft.content.world.feature.BubbleVentFeature;
import net.kasax.raft.content.world.feature.CraterLavaFeature;
import net.kasax.raft.content.world.feature.RubbleFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Feature types of the raft dimension (used by the feature JSON of tools/content/world/features.py). */
public final class WorldFeatures {
    private WorldFeatures() {
    }

    private static void register(String name, MapCodec<? extends Feature> codec) {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, Raft.id(name), codec);
    }

    public static void init() {
        register("crater_lava", CraterLavaFeature.CODEC);
        register("bubble_vents", BubbleVentFeature.CODEC);
        register("rubble", RubbleFeature.CODEC);
    }
}
