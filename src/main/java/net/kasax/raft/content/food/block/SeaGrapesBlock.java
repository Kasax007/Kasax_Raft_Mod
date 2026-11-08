package net.kasax.raft.content.food.block;

import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Sea grapes: an underwater crop. Planted from sea grapes on sand, gravel or dirt under water; grows four stages by
 * itself; when ripe a right-click picks 2-3 bunches and it grows back from stage 1.
 */
public class SeaGrapesBlock extends BushBlock implements BonemealableBlock, LiquidBlockContainer {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return null;
    }

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (int i = 0; i < 4; i++) SHAPES[i] = net.kasax.raft.compat.VShapes.column(12.0, 0.0, 5 + i * 3);
    }

    public SeaGrapesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP) && !state.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return fluid.is(Fluids.WATER) && fluid.getAmount() == 8 ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && level.getFluidState(pos).is(Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        BlockState s = super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
        if (!s.isAir()) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return s.isAir() ? Blocks.WATER.defaultBlockState() : s;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public boolean canPlaceLiquid(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 3;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < 3 && random.nextInt(4) == 0) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(FoodContent.SEA_GRAPES, 2 + level.getRandom().nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
                    0.8F + level.getRandom().nextFloat() * 0.4F);
            level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(FoodContent.SEA_GRAPES);
    }
}
