package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 1.21.1 port helper: in the newer game a configured feature is one object (its settings and its placing code).
 * 1.21.1 splits it into a feature type and a configuration; here the object is the configuration and
 * {@link CompatFeature} is the type that asks it to place itself. Its JSON settings sit under "config".
 */
public interface WorldFeature extends FeatureConfiguration {
    MapCodec<? extends WorldFeature> codec();

    boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin);
}
