package net.kasax.raft.content.building.block;

import net.kasax.raft.content.building.BuildingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A small wind turbine of the Meridian Expedition. The rotor sits in front of the block ({@code facing}); it needs a
 * free 3x3 area of air there and a height above y 90 to make 20 FE per tick, which it pushes into neighbouring energy
 * storages (cables, batteries, machines). The blades are drawn by a block entity renderer and spin while it runs.
 */
public class WindTurbineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /** Pole, yaw bearing, nacelle and tail vane, front (rotor side) to the north. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(5, 0, 5, 11, 1, 11),
            Block.box(6.5, 1, 6.5, 9.5, 5, 9.5),
            Block.box(5, 5, 2, 11, 11, 14),
            Block.box(7.5, 6, 14, 8.5, 13, 16));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);

    public WindTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindTurbineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, BuildingBlocks.WIND_TURBINE_BE, (lvl, pos, st, be) -> be.clientTick());
        }
        return createTickerHelper(type, BuildingBlocks.WIND_TURBINE_BE, (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WindTurbineBlockEntity turbine) {
            turbine.refreshWind(level, pos, state);
            String status = switch (turbine.getWind()) {
                case WindTurbineBlockEntity.WIND_RUNNING -> "message.raft.wind_turbine.running";
                case WindTurbineBlockEntity.WIND_TOO_LOW -> "message.raft.wind_turbine.too_low";
                default -> "message.raft.wind_turbine.blocked";
            };
            player.sendOverlayMessage(Component.translatable("message.raft.wind_turbine.energy",
                    turbine.getEnergy(), turbine.getMaxEnergy(), Component.translatable(status)));
        }
        return InteractionResult.SUCCESS;
    }
}
