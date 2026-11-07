package net.kasax.raft.content.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * A loose heap of blocks on the ground or sea floor (concrete rubble of the drowned city, wreck debris): {@code pieces}
 * blocks around the origin, each on top of the floor and sometimes stacked. Blocks that can hold water are
 * waterlogged when they land in water, logs and pillars lie on their side, facing blocks get a random facing.
 * <p>
 * {@code optional_blocks} are blocks of other content modules (looked up by id when placing, so a block that does not
 * exist is simply left out): with {@code optional_chance} a piece is one of them instead of {@code state_provider}.
 */
public record RubbleFeature(BlockStateProvider stateProvider, IntProvider pieces, int spread, float stackChance,
                            List<ResourceLocation> optionalBlocks, float optionalChance) implements net.kasax.raft.compat.worldgen.WorldFeature {
    public static final MapCodec<RubbleFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(RubbleFeature::stateProvider),
            IntProvider.codec(1, 64).fieldOf("pieces").forGetter(RubbleFeature::pieces),
            Codec.intRange(0, 7).optionalFieldOf("spread", 2).forGetter(RubbleFeature::spread),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("stack_chance", 0.3F).forGetter(RubbleFeature::stackChance),
            ResourceLocation.CODEC.listOf().optionalFieldOf("optional_blocks", List.of()).forGetter(RubbleFeature::optionalBlocks),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("optional_chance", 0.0F).forGetter(RubbleFeature::optionalChance)
    ).apply(i, RubbleFeature::new));

    @Override
    public MapCodec<RubbleFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int n = pieces.sample(random);
        List<Block> optional = new ArrayList<>();
        for (ResourceLocation id : optionalBlocks) {
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(optional::add);
        }
        boolean any = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            int x = origin.getX() + random.nextIntBetweenInclusive(-spread, spread);
            int z = origin.getZ() + random.nextIntBetweenInclusive(-spread, spread);
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            pos.set(x, y, z);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                continue;
            }
            int height = random.nextFloat() < stackChance ? 2 : 1;
            for (int h = 0; h < height; h++) {
                BlockState old = level.getBlockState(pos);
                boolean water = old.getFluidState().is(Fluids.WATER) && old.getFluidState().isSource();
                if (!(old.isAir() || water || old.is(BlockTags.REPLACEABLE))) {
                    break;
                }
                BlockState state = !optional.isEmpty() && random.nextFloat() < optionalChance
                        ? optional.get(random.nextInt(optional.size())).defaultBlockState()
                        : stateProvider.getState(random, pos);
                if (state.hasProperty(BlockStateProperties.AXIS)) {
                    state = state.setValue(BlockStateProperties.AXIS, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
                }
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                }
                if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    state = state.setValue(BlockStateProperties.WATERLOGGED, water);
                }
                level.setBlock(pos, state, 2);
                any = true;
                pos.move(Direction.UP);
            }
        }
        return any;
    }
}
