package net.kasax.raft.content.decor.block;

import net.kasax.raft.content.decor.DecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A storage block with 27 slots and the vanilla chest screen ({@code facing}, {@code open}, optional
 * {@code waterlogged}): the treasure chest (its lid opens) and the wooden crate (its lid slides aside). The contents
 * stay in {@link DecorChestBlockEntity} and drop when the block breaks.
 */
public class DecorChestBlock extends DecorBlock implements EntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    final SoundEvent openSound;
    final SoundEvent closeSound;
    private final Map<Direction, VoxelShape> openShapes;

    public DecorChestBlock(Properties properties, Config config, SoundEvent openSound, SoundEvent closeSound) {
        super(properties, config);
        this.openSound = openSound;
        this.closeSound = closeSound;
        this.openShapes = DecorBlock.shapes(config.shape() + "_open", true);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(OPEN, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? openShapes.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecorChestBlockEntity(state.is(DecorBlocks.WOODEN_CRATE) ? DecorBlocks.WOODEN_CRATE_ENTITY
                : DecorBlocks.TREASURE_CHEST_ENTITY, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof DecorChestBlockEntity chest) {
            player.openMenu(chest);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MenuProvider provider ? provider : null;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof DecorChestBlockEntity chest) {
            chest.recheckOpen();
        }
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int b0, int b1) {
        super.triggerEvent(state, level, pos, b0, b1);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(b0, b1);
    }
}
