package net.kasax.raft.content.world.feature;

import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Keeps sand, gravel and ash from falling when a chunk is loaded. The surface rules lay sand and gravel on the sea
 * floor before the caves are carved, and gravel blobs (ore features) are placed after it, so some of these blocks end
 * up right above a cave. The first water or block update next to them (fluids flowing when the chunk is loaded) then
 * turns them into falling-block entities. This feature runs last (step top_layer_modification, one call per chunk,
 * origin = chunk corner): every sand / gravel / shell sand / black sand / volcanic ash block of the chunk with a free
 * block below (air, water, lava, replaceable) becomes its solid counterpart (sandstone, stone, basalt, pumice), so
 * the layer above it rests on it. The tops of beaches and sea floors keep their look; only cave ceilings change, as
 * with vanilla's sandstone-on-ceilings rule. Sections without any of these blocks are skipped (palette check).
 */
public final class GravitySupportFeature implements Feature {
    public static final GravitySupportFeature INSTANCE = new GravitySupportFeature();
    public static final MapCodec<GravitySupportFeature> CODEC = MapCodec.unit(INSTANCE);
    private static Map<Block, BlockState> solid;

    private GravitySupportFeature() {
    }

    @Override
    public MapCodec<GravitySupportFeature> codec() {
        return CODEC;
    }

    private static Map<Block, BlockState> solid() {
        if (solid == null) {
            Map<Block, BlockState> m = new IdentityHashMap<>();
            m.put(Blocks.SAND, Blocks.SANDSTONE.defaultBlockState());
            m.put(Blocks.RED_SAND, Blocks.RED_SANDSTONE.defaultBlockState());
            m.put(Blocks.GRAVEL, Blocks.STONE.defaultBlockState());
            m.put(WorldBlocks.SHELL_SAND, Blocks.SANDSTONE.defaultBlockState());
            m.put(WorldBlocks.BLACK_SAND, Blocks.BASALT.defaultBlockState());
            m.put(WorldBlocks.VOLCANIC_ASH, WorldBlocks.PUMICE.defaultBlockState());
            solid = m;
        }
        return solid;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        Map<Block, BlockState> map = solid();
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getZ()));
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(s -> map.containsKey(s.getBlock()))) {
                continue;
            }
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;
                if (y <= minY) {
                    continue;
                }
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        if (!(state.getBlock() instanceof FallingBlock)) {
                            continue;
                        }
                        BlockState replacement = map.get(state.getBlock());
                        if (replacement == null) {
                            continue;
                        }
                        BlockState below = ly > 0 ? section.getBlockState(lx, ly - 1, lz)
                                : chunk.getBlockState(pos.set(x0 + lx, y - 1, z0 + lz));
                        if (FallingBlock.isFree(below)) {
                            level.setBlock(pos.set(x0 + lx, y, z0 + lz), replacement, 2);
                            any = true;
                        }
                    }
                }
            }
        }
        return any;
    }
}
