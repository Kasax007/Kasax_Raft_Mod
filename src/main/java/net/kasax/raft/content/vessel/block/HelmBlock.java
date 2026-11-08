package net.kasax.raft.content.vessel.block;

import net.kasax.raft.content.vessel.VesselAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The helm of a built raft: a ship's wheel on a pedestal with a stool behind it. {@code facing} is the bow (the
 * direction the helmsman looks). Using it turns the raft it stands on into a sailing vessel and seats the player.
 */
public class HelmBlock extends HorizontalDirectionalBlock {
    /**
     * Which model the block shows: always {@code full} in the world. A sailing raft draws the helm as {@code base} plus
     * {@code wheel}, and turns the wheel while the helmsman steers.
     */
    public enum Part implements net.minecraft.util.StringRepresentable {
        FULL("full"), BASE("base"), WHEEL("wheel");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Part> PART =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("part", Part.class);
    /** Shape for facing=north: stool at the south (stern) side, pedestal and wheel at the north side. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(5.0, 0.0, 3.0, 11.0, 2.0, 8.0),
            Block.box(6.5, 2.0, 4.0, 9.5, 12.0, 7.0),
            Block.box(0.0, 4.0, 7.5, 16.0, 16.0, 8.5),
            Block.box(4.0, 0.0, 10.0, 12.0, 7.5, 15.5));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);

    public HelmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.FULL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            VesselAssembler.setSail(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
