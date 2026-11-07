package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Fills the flat floors of volcano craters with a lava lake. The terrain makes the floor of the big volcanoes exactly
 * flat with its top block at {@code floor_y - 1}; this feature (one call per chunk, origin = chunk corner) turns every
 * such column whose four neighbours are not lower into lava, with magma underneath, so the lake cannot spill.
 * Craters that hold the Ashen Forge (story chapter 6) stay dry in and around the forge: no lava in a chunk that, or
 * whose neighbour, references the structure, so players can walk up to it.
 */
public record CraterLavaFeature(int floorY) implements net.kasax.raft.compat.worldgen.WorldFeature {
    public static final MapCodec<CraterLavaFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(-64, 320).optionalFieldOf("floor_y", 164).forGetter(CraterLavaFeature::floorY)
    ).apply(i, CraterLavaFeature::new));

    @Override
    public MapCodec<CraterLavaFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        if (nearForge(level, x0 >> 4, z0 >> 4)) {
            return false;
        }
        int top = floorY - 1;
        boolean any = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                if (surface(level, x, z) != top) {
                    continue;
                }
                boolean contained = true;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (surface(level, x + d.getStepX(), z + d.getStepZ()) < top) {
                        contained = false;
                        break;
                    }
                }
                if (!contained) {
                    continue;
                }
                pos.set(x, top, z);
                BlockState here = level.getBlockState(pos);
                if (here.isAir() || !here.getFluidState().isEmpty()) {
                    continue;
                }
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                pos.set(x, top - 1, z);
                level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                any = true;
            }
        }
        return any;
    }

    private static final ResourceLocation ASHEN_FORGE = ResourceLocation.fromNamespaceAndPath("raft", "ashen_forge");

    /** True when this chunk or one of its 8 neighbours belongs to an Ashen Forge (structure references). */
    private static boolean nearForge(WorldGenLevel level, int chunkX, int chunkZ) {
        Structure forge = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ASHEN_FORGE);
        if (forge == null) {
            return false;
        }
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (level.hasChunk(chunkX + ox, chunkZ + oz)
                        && !level.getChunk(chunkX + ox, chunkZ + oz).getReferencesForStructure(forge).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** y of the top solid block of the column. */
    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
    }
}
