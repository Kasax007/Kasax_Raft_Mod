package net.kasax.raft.content.world.block;

import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.kasax.raft.compat.VShapes;
import net.minecraft.world.ItemInteractionResult;

/**
 * Wild pineapple bush of the islands (like a berry bush): grows from age 0 to 3, at age 3 it carries a ripe
 * pineapple that is picked with an empty hand; the bush then starts again at age 1.
 */
public class PineappleBushBlock extends BushBlock implements BonemealableBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return simpleCodec(PineappleBushBlock::new);
    }

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SHAPE_YOUNG = VShapes.column(10.0, 0.0, 8.0);
    private static final VoxelShape SHAPE_GROWN = VShapes.column(14.0, 0.0, 13.0);

    public PineappleBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return BeachPlantBlock.isIslandSoil(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SHAPE_YOUNG : SHAPE_GROWN;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && random.nextInt(6) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
            BlockState grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, grown, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(grown));
        }
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean ripe = state.getValue(AGE) == MAX_AGE;
        return !ripe && stack.is(Items.BONE_MEAL) ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) != MAX_AGE) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (level instanceof ServerLevel serverLevel) {
            int count = serverLevel.getRandom().nextInt(5) == 0 ? 2 : 1;
            Block.popResource(serverLevel, pos, new ItemStack(WorldBlocks.PINEAPPLE, count));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
                    0.7F + serverLevel.getRandom().nextFloat() * 0.3F);
            BlockState picked = state.setValue(AGE, 1);
            serverLevel.setBlock(pos, picked, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }
}
