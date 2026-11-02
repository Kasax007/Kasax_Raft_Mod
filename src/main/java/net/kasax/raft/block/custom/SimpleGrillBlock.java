package net.kasax.raft.block.custom;

import net.kasax.raft.block.entity.ModBlockEntities;
import net.kasax.raft.block.entity.SimpleGrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A small iron grill. Put planks, logs or coal in as fuel and raw food on top. */
public class SimpleGrillBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 10.0);

    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes = SimpleGrillBlockEntity.createRecipeCheck();

    public SimpleGrillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleGrillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(type, ModBlockEntities.SIMPLE_GRILL_BLOCK_ENTITY,
                    (lvl, pos, st, be) -> SimpleGrillBlockEntity.serverTick(serverLevel, pos, st, be, recipes));
        }
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SimpleGrillBlockEntity grill) {
            if (SimpleGrillBlockEntity.fuelValue(stack) > 0) {
                if (!level.isClientSide()) {
                    if (grill.addFuel(stack, !player.getAbilities().instabuild)) {
                        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 1.0f);
                        return InteractionResult.SUCCESS_SERVER;
                    }
                    return InteractionResult.CONSUME;
                }
                return InteractionResult.SUCCESS;
            }
            if (level instanceof ServerLevel serverLevel) {
                if (grill.placeFood(serverLevel, player, stack, recipes)) {
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
                    return InteractionResult.SUCCESS_SERVER;
                }
                if (!stack.isEmpty()) {
                    // full grill or not cookable: do nothing here (do not take food back), let the item do its own thing
                    return serverLevel.recipeAccess().propertySet(net.minecraft.world.item.crafting.RecipePropertySet.CAMPFIRE_INPUT).test(stack)
                            ? InteractionResult.CONSUME : InteractionResult.PASS;
                }
            } else if (level.recipeAccess().propertySet(net.minecraft.world.item.crafting.RecipePropertySet.CAMPFIRE_INPUT).test(stack)) {
                return InteractionResult.SUCCESS;
            } else if (!stack.isEmpty()) {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // take the last item back
        if (level.getBlockEntity(pos) instanceof SimpleGrillBlockEntity grill) {
            if (!level.isClientSide()) {
                ItemStack taken = grill.takeLast(level);
                if (!taken.isEmpty()) {
                    if (!player.getInventory().add(taken)) {
                        net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), taken);
                    }
                    return InteractionResult.SUCCESS_SERVER;
                }
                return InteractionResult.PASS;
            }
            return grill.getItems().stream().anyMatch(s -> !s.isEmpty()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            if (random.nextInt(10) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
            }
            if (random.nextInt(5) == 0) {
                for (int i = 0; i < random.nextInt(1) + 1; i++) {
                    level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                            random.nextFloat() / 2.0f, 5.0E-5, random.nextFloat() / 2.0f);
                }
            }
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.9,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.03, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }
}
