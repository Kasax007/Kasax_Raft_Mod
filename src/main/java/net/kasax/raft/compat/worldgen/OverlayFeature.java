package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/** 1.21.1 port of the newer game's {@code minecraft:overlay}: places every listed feature at the same origin, in order. */
public record OverlayFeature(List<Holder<PlacedFeature>> features) implements WorldFeature {
    public static final MapCodec<OverlayFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PlacedFeature.CODEC.listOf().fieldOf("features").forGetter(OverlayFeature::features)
    ).apply(i, OverlayFeature::new));

    @Override
    public MapCodec<OverlayFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        boolean any = false;
        for (Holder<PlacedFeature> f : this.features) {
            any |= f.value().place(level, generator, random, origin);
        }
        return any;
    }
}
