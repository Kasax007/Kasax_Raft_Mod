package net.kasax.raft.content.vessel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A sail for a built raft: a panel of canvas in the middle of the block, {@code facing} = the bow. Sails set side by
 * side and on top of each other join into one sheet; the edges of the sheet get the yard (top), a rope hem (bottom and
 * free sides), and next to a {@link MastBlock} the canvas reaches over to the mast. Every set sail block on a raft adds
 * wind power. Use a sail (not with a block in hand) to furl or set the whole sheet: furled, the canvas is rolled up
 * under the yard and gives no power.
 */
public class SailBlock extends HorizontalDirectionalBlock {
    public enum Side implements StringRepresentable {
        NONE("none"), SAIL("sail"), MAST("mast");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /** Is there sail above / below (then no yard / hem on that edge)? */
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    /** What is beside the sail on its left / right, seen from the stern looking to the bow. */
    public static final EnumProperty<Side> LEFT = EnumProperty.create("left", Side.class);
    public static final EnumProperty<Side> RIGHT = EnumProperty.create("right", Side.class);
    public static final BooleanProperty FURLED = BooleanProperty.create("furled");
    /** Most sail blocks that furl or set together. */
    private static final int MAX_SHEET = 256;

    /** Shape for facing=north: the canvas panel. */
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0));
    private static final Map<Direction, VoxelShape> FURLED_SHAPES = Shapes.rotateHorizontal(Block.box(0.0, 12.0, 7.0, 16.0, 16.0, 9.0));
    private static final Map<Direction, VoxelShape> BRAIL_SHAPES = Shapes.rotateHorizontal(Block.box(3.0, 0.0, 7.0, 13.0, 16.0, 9.0));

    public SailBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false)
                .setValue(LEFT, Side.NONE).setValue(RIGHT, Side.NONE).setValue(FURLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UP, DOWN, LEFT, RIGHT, FURLED);
    }

    /** Left of the bow direction (facing north: west). */
    private static Direction left(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        boolean furled = false;
        // a sail set next to another sail takes over its direction and whether it is furled: one sheet
        for (Direction d : new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState n = level.getBlockState(pos.relative(d));
            // a sail beside it (in its sheet's plane) or above/below
            if (n.getBlock() instanceof SailBlock && (d.getAxis().isVertical() || d.getAxis() != n.getValue(FACING).getAxis())) {
                facing = n.getValue(FACING);
                furled = n.getValue(FURLED);
                break;
            }
        }
        return connect(defaultBlockState().setValue(FACING, facing).setValue(FURLED, furled), level, pos);
    }

    /** All connections of a sail at pos from its neighbours. */
    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(UP, sameSheet(state, level.getBlockState(pos.above())))
                .setValue(DOWN, sameSheet(state, level.getBlockState(pos.below())))
                .setValue(LEFT, side(state, level.getBlockState(pos.relative(left(facing)))))
                .setValue(RIGHT, side(state, level.getBlockState(pos.relative(left(facing).getOpposite()))));
    }

    private static boolean sameSheet(BlockState self, BlockState other) {
        return other.getBlock() instanceof SailBlock && other.getValue(FACING).getAxis() == self.getValue(FACING).getAxis();
    }

    private static Side side(BlockState self, BlockState other) {
        if (sameSheet(self, other)) {
            return Side.SAIL;
        }
        return other.getBlock() instanceof MastBlock ? Side.MAST : Side.NONE;
    }

    /** Placed by any means (also /setblock, /fill, a structure or a raft setting sail): join the neighbouring sheet. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState connected = connect(state, level, pos);
        if (connected != state) {
            level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        if (direction == Direction.UP) {
            return state.setValue(UP, sameSheet(state, neighbourState));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(DOWN, sameSheet(state, neighbourState));
        }
        if (direction == left(facing)) {
            return state.setValue(LEFT, side(state, neighbourState));
        }
        if (direction == left(facing).getOpposite()) {
            return state.setValue(RIGHT, side(state, neighbourState));
        }
        return state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        // with a block in hand the player builds (more sail, a mast); anything else furls or sets the sail
        return stack.getItem() instanceof BlockItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean furl = !state.getValue(FURLED);
            for (BlockPos p : sheet(level, pos, state)) {
                BlockState s = level.getBlockState(p);
                level.setBlock(p, s.setValue(FURLED, furl), Block.UPDATE_CLIENTS);
            }
            level.playSound(null, pos, furl ? SoundEvents.WOOL_BREAK : SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, furl ? 0.8F : 1.1F);
            player.sendOverlayMessage(Component.translatable(furl ? "message.raft.sail.furled" : "message.raft.sail.set"));
        }
        return InteractionResult.SUCCESS;
    }

    /** The connected sail blocks of one sheet (same axis), at most {@link #MAX_SHEET}. */
    public static Set<BlockPos> sheet(BlockGetter level, BlockPos start, BlockState startState) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        seen.add(start.immutable());
        todo.add(start.immutable());
        Direction.Axis axis = startState.getValue(FACING).getAxis();
        Direction side = startState.getValue(FACING).getClockWise();
        while (!todo.isEmpty() && seen.size() < MAX_SHEET) {
            BlockPos p = todo.poll();
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, side, side.getOpposite()}) {
                BlockPos q = p.relative(d);
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof MastBlock && d.getAxis().isHorizontal()) {
                    // one sail on both sides of a mast: furl and set them together
                    q = q.relative(d);
                    s = level.getBlockState(q);
                }
                if (!seen.contains(q) && s.getBlock() instanceof SailBlock && s.getValue(FACING).getAxis() == axis) {
                    seen.add(q);
                    todo.add(q);
                }
            }
        }
        return seen;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState s = state.rotate(mirror.getRotation(state.getValue(FACING)));
        return mirror == Mirror.NONE ? s : s.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        if (!state.getValue(FURLED)) {
            return SHAPES.get(f);
        }
        return state.getValue(UP) ? BRAIL_SHAPES.get(f) : FURLED_SHAPES.get(f);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        if (!state.getValue(FURLED)) {
            return SHAPES.get(f);
        }
        return state.getValue(UP) ? Shapes.empty() : FURLED_SHAPES.get(f);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
