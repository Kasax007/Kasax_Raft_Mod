package net.kasax.raft.content.food.block;

import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.ItemInteractionResult;

/** The cooking pot: put ingredients in, set it on a fire, ladle the dish out with bowls (see CookingPotBlockEntity). */
public class CookingPotBlock extends BaseEntityBlock {
    public enum Contents implements StringRepresentable {
        EMPTY("empty"), COOKING("cooking"), READY("ready");

        private final String name;

        Contents(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 9, 14);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CookingPotBlock::new);
    }

    public CookingPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENTS, Contents.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONTENTS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FoodContent.COOKING_POT_ENTITY, CookingPotBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        return pot.useItem(stack, player) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) return InteractionResult.PASS;
        if (!level.isClientSide) pot.useEmptyHand(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTENTS) == Contents.EMPTY || !CookingPotBlockEntity.heated(level, pos)) return;
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5, z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        if (state.getValue(CONTENTS) == Contents.COOKING) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.45, z, 0, 0.02, 0);
        }
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.6, z, 0, 0.03, 0);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
