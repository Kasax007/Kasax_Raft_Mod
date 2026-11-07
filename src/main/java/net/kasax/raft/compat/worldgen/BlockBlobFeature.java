package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

/**
 * 1.21.1 port of the newer game's {@code minecraft:block_blob} (like the forest rock): a lump of {@code state} on
 * ground that {@code can_place_on} accepts; it sinks down until it finds such ground.
 */
public record BlockBlobFeature(BlockState state, BlockPredicate canPlaceOn) implements WorldFeature {
    public static final MapCodec<BlockBlobFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(BlockBlobFeature::state),
            BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(BlockBlobFeature::canPlaceOn)
    ).apply(i, BlockBlobFeature::new));

    @Override
    public MapCodec<BlockBlobFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos pos = origin;
        while (pos.getY() > level.getMinBuildHeight() + 3) {
            if (!level.isEmptyBlock(pos.below()) && this.canPlaceOn.test(level, pos.below())) {
                break;
            }
            pos = pos.below();
        }
        if (pos.getY() <= level.getMinBuildHeight() + 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            int xr = random.nextInt(2);
            int yr = random.nextInt(2);
            int zr = random.nextInt(2);
            float r = (xr + yr + zr) * 0.333F + 0.5F;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-xr, -yr, -zr), pos.offset(xr, yr, zr))) {
                if (p.distSqr(pos) <= r * r) {
                    level.setBlock(p, this.state, 3);
                }
            }
            pos = pos.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
        }
        return true;
    }
}
