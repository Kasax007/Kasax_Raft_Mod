package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.kasax.raft.compat.VShapes;

/**
 * Plant that only lives under water (sea anemones, tube worms): placed into water only, always waterlogged, sits on
 * any sturdy floor, and pops off when its water is taken away. Optional glow particles for the glowing kinds.
 */
public class UnderwaterPlantBlock extends BushBlock implements SimpleWaterloggedBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return simpleCodec(UnderwaterPlantBlock::new);
    }

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final VoxelShape shape;
    private final @Nullable ParticleOptions particle;

    public UnderwaterPlantBlock(VoxelShape shape, @Nullable ParticleOptions particle, Properties properties) {
        super(properties);
        this.shape = shape;
        this.particle = particle;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true));
    }

    public UnderwaterPlantBlock(Properties properties) {
        this(VShapes.column(12.0, 0.0, 10.0), null, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP) && !state.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(WATERLOGGED, true);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.getValue(WATERLOGGED) || !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, BlockPos pos121) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (particle != null && random.nextInt(8) == 0) {
            level.addParticle(particle, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.015, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
}
