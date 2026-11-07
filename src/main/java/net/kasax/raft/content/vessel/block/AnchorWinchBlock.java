package net.kasax.raft.content.vessel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The anchor winch of a built raft, set on the deck edge with the anchor hanging over the side ({@code facing} points
 * outboard). Use it to drop or raise the anchor: while the anchor is down a sailing raft stops and holds its position
 * (it stays a raft); raise it to sail on. On land it is just a winch with its anchor up or down.
 */
public class AnchorWinchBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty DROPPED = BooleanProperty.create("dropped");
    private static final Map<Direction, VoxelShape> SHAPES = net.kasax.raft.compat.VShapes.rotateHorizontal(Shapes.or(
            Block.box(3.0, 0.0, 4.0, 13.0, 2.0, 12.0),
            Block.box(3.0, 2.0, 5.0, 13.0, 11.0, 11.0)));

    public AnchorWinchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DROPPED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(AnchorWinchBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DROPPED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean drop = !state.getValue(DROPPED);
            level.setBlock(pos, state.setValue(DROPPED, drop), Block.UPDATE_ALL);
            level.playSound(null, pos, drop ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, drop ? 0.7F : 1.0F);
            if (drop) {
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 0.9F);
            }
            player.displayClientMessage(Component.translatable(drop ? "message.raft.anchor.dropped" : "message.raft.anchor.raised"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
