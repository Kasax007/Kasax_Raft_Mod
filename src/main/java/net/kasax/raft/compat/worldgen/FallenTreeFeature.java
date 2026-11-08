package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

/**
 * 1.21.1 port of the newer game's {@code minecraft:fallen_tree}: a stump of {@code trunk_provider} and, one block
 * away, a log of {@code log_length} lying on the ground in a random direction (only where there is room and ground).
 * The decorators of the newer format are accepted and ignored.
 */
public record FallenTreeFeature(BlockStateProvider trunkProvider, IntProvider logLength) implements WorldFeature {
    public static final MapCodec<FallenTreeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(FallenTreeFeature::trunkProvider),
            IntProvider.codec(0, 16).fieldOf("log_length").forGetter(FallenTreeFeature::logLength),
            Codec.PASSTHROUGH.listOf().optionalFieldOf("log_decorators", List.of()).forGetter(f -> List.of()),
            Codec.PASSTHROUGH.listOf().optionalFieldOf("stump_decorators", List.of()).forGetter(f -> List.of())
    ).apply(i, (t, l, a, b) -> new FallenTreeFeature(t, l)));

    @Override
    public MapCodec<FallenTreeFeature> codec() {
        return CODEC;
    }

    private static boolean free(WorldGenLevel level, BlockPos p) {
        return level.getBlockState(p).canBeReplaced() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!free(level, origin)) {
            return false;
        }
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = this.logLength.sample(random);
        BlockPos start = origin.relative(dir, 2);
        for (int i = 0; i < length; i++) {
            if (!free(level, start.relative(dir, i))) {
                return false;
            }
        }
        level.setBlock(origin, this.trunkProvider.getState(random, origin), 3);
        for (int i = 0; i < length; i++) {
            BlockPos p = start.relative(dir, i);
            BlockState s = this.trunkProvider.getState(random, p);
            if (s.hasProperty(RotatedPillarBlock.AXIS)) {
                s = s.setValue(RotatedPillarBlock.AXIS, dir.getAxis());
            }
            level.setBlock(p, s, 3);
        }
        return true;
    }
}
