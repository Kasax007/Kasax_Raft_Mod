package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

/**
 * A flat, ragged ice floe drifting at the sea surface (frost floes): packed ice with a rim of blue ice, a snow-covered
 * middle (snow blocks with a few snow layers on top), a thin ice keel below the waterline and now and then a small
 * pressure ridge. Only replaces water, so it never cuts into icebergs or land.
 */
public record IceFloeFeature(IntProvider radius, float ridgeChance) implements net.kasax.raft.compat.worldgen.WorldFeature {
    public static final MapCodec<IceFloeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IntProvider.codec(2, 10).fieldOf("radius").forGetter(IceFloeFeature::radius),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("ridge_chance", 0.3F).forGetter(IceFloeFeature::ridgeChance)
    ).apply(i, IceFloeFeature::new));

    @Override
    public MapCodec<IceFloeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int top = generator.getSeaLevel() - 1;
        int x0 = origin.getX();
        int z0 = origin.getZ();
        // open water only: deep enough below the floe and nothing on the surface yet
        if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0, z0) > top - 3 || !isWater(level, new BlockPos(x0, top, z0))) {
            return false;
        }
        int r = radius.sample(random);
        float p1 = random.nextFloat() * Mth.TWO_PI;
        float p2 = random.nextFloat() * Mth.TWO_PI;
        float a1 = 0.18F + random.nextFloat() * 0.12F;
        float a2 = 0.08F + random.nextFloat() * 0.1F;
        boolean ridge = r >= 5 && random.nextFloat() < ridgeChance;
        float ridgeAngle = random.nextFloat() * Mth.PI;
        float rc = Mth.cos(ridgeAngle);
        float rs = Mth.sin(ridgeAngle);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        int ext = r + 3;
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                float d = Mth.sqrt(dx * dx + dz * dz);
                float angle = (float) Mth.atan2(dz, dx);
                float edge = r * (1.0F + a1 * Mth.sin(3.0F * angle + p1) + a2 * Mth.sin(5.0F * angle + p2));
                if (d > edge) {
                    continue;
                }
                float inner = edge - d;   // distance to the rim
                int x = x0 + dx;
                int z = z0 + dz;
                pos.set(x, top, z);
                if (!isWater(level, pos)) {
                    continue;
                }
                BlockState surface;
                if (inner < 1.0F) {
                    surface = random.nextInt(4) == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
                } else if (inner < 2.0F && random.nextInt(3) == 0) {
                    surface = Blocks.PACKED_ICE.defaultBlockState();
                } else {
                    surface = Blocks.SNOW_BLOCK.defaultBlockState();
                }
                level.setBlock(pos, surface, 2);
                any = true;
                // keel below the waterline
                int keel = inner < 1.5F ? 1 : inner < 3.5F ? 2 : 3;
                for (int k = 1; k <= keel; k++) {
                    pos.set(x, top - k, z);
                    if (!isWater(level, pos)) {
                        break;
                    }
                    level.setBlock(pos, k == keel && random.nextBoolean() ? Blocks.ICE.defaultBlockState()
                            : Blocks.PACKED_ICE.defaultBlockState(), 2);
                }
                // pressure ridge across the floe, then snow on top
                int y = top + 1;
                if (ridge) {
                    float across = Math.abs(dx * rs - dz * rc);
                    float along = Math.abs(dx * rc + dz * rs);
                    if (across < 1.2F && along < edge * 0.7F) {
                        int h = along < edge * 0.35F ? 2 : 1;
                        for (int k = 0; k < h; k++) {
                            pos.set(x, y, z);
                            level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 2);
                            y++;
                        }
                        pos.set(x, y, z);
                        level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 2);
                        continue;
                    }
                }
                if (surface.is(Blocks.SNOW_BLOCK) && inner > 2.0F && random.nextInt(3) != 0) {
                    int layers = inner > 4.0F ? 1 + random.nextInt(3) : 1;
                    pos.set(x, y, z);
                    if (level.isEmptyBlock(pos)) {
                        level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), 2);
                    }
                }
            }
        }
        return any;
    }

    private static boolean isWater(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource();
    }
}
