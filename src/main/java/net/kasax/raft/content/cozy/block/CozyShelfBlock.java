package net.kasax.raft.content.cozy.block;

import com.mojang.serialization.MapCodec;
import net.kasax.raft.compat.VShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 1.21.1 port: the shelf of the newer game (palm, teak and weathered shelves), which 1.21.1 does not have. Three items
 * stand on the front; use with an item puts it on the slot you look at (swapping with what stands there), use with an
 * empty hand takes it. A powered shelf joins up to two powered neighbours facing the same way (side chain) and use
 * swaps the whole row with the end of the hotbar (3, 6 or 9 slots), as the newer game's shelf does. The comparator
 * reads which slots are filled (bits 1, 2, 4). Same block state properties and models as the newer game.
 */
public class CozyShelfBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CozyShelfBlock> CODEC = simpleCodec(CozyShelfBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART = EnumProperty.create("side_chain", SideChainPart.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = VShapes.rotateHorizontal(Shapes.or(
            Block.box(0.0, 12.0, 11.0, 16.0, 16.0, 13.0), Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 13.0)));
    private static final int MAX_CHAIN = 3;

    public CozyShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CozyShelfBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SIDE_CHAIN_PART, WATERLOGGED);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) updateNeighborsAfterPoweringDown(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean signal = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != signal) {
            BlockState newState = state.setValue(POWERED, signal);
            if (!signal) newState = newState.setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED);
            level.setBlockAndUpdate(pos, newState);
            playSound(level, pos, signal ? SoundEvents.WOODEN_BUTTON_CLICK_ON : SoundEvents.WOODEN_BUTTON_CLICK_OFF);
            level.gameEvent(signal ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(newState));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
                .setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ------------------------------------------------------------------------------------------------ use
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CozyShelfBlockEntity shelf) || hand == InteractionHand.OFF_HAND) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        OptionalInt slot = hitSlot(hit, state.getValue(FACING));
        if (slot.isEmpty()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        Inventory inventory = player.getInventory();
        if (level.isClientSide) {
            // an empty hand takes an item: the client cannot see it is there for sure, the server decides
            return inventory.getSelected().isEmpty() && shelf.getItem(slot.getAsInt()).isEmpty()
                    ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.CONSUME;
        }
        if (!state.getValue(POWERED)) {
            boolean removed = swapSingleItem(stack, player, shelf, slot.getAsInt(), inventory);
            if (removed) {
                playSound(level, pos, stack.isEmpty() ? SoundEvents.CHISELED_BOOKSHELF_PICKUP : SoundEvents.CHISELED_BOOKSHELF_INSERT);
            } else {
                if (stack.isEmpty()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
                playSound(level, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (!swapHotbar(level, pos, inventory)) return ItemInteractionResult.CONSUME;
        playSound(level, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED);
        return ItemInteractionResult.SUCCESS;
    }

    private static boolean swapSingleItem(ItemStack stack, Player player, CozyShelfBlockEntity shelf, int slot, Inventory inventory) {
        ItemStack removed = shelf.swapItemNoUpdate(slot, stack);
        ItemStack inHand = player.hasInfiniteMaterials() && removed.isEmpty() ? stack.copy() : removed;
        inventory.setItem(inventory.selected, inHand);
        inventory.setChanged();
        shelf.setChanged(GameEvent.ITEM_INTERACT_FINISH);
        return !removed.isEmpty();
    }

    private boolean swapHotbar(Level level, BlockPos pos, Inventory inventory) {
        List<BlockPos> chain = connectedTo(level, pos);
        if (chain.isEmpty()) return false;
        boolean any = false;
        for (int part = 0; part < chain.size(); part++) {
            if (!(level.getBlockEntity(chain.get(part)) instanceof CozyShelfBlockEntity shelf)) continue;
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
                int inventorySlot = 9 - (chain.size() - part) * shelf.getContainerSize() + slot;
                if (inventorySlot >= 0 && inventorySlot <= inventory.getContainerSize()) {
                    ItemStack placed = inventory.removeItemNoUpdate(inventorySlot);
                    ItemStack taken = shelf.swapItemNoUpdate(slot, placed);
                    if (!placed.isEmpty() || !taken.isEmpty()) {
                        inventory.setItem(inventorySlot, taken);
                        any = true;
                    }
                }
            }
            inventory.setChanged();
            shelf.setChanged(GameEvent.ENTITY_INTERACT);
        }
        return any;
    }

    /** The slot (0 left .. 2 right, seen from the front) the player aims at, empty when not looking at the front. */
    static OptionalInt hitSlot(BlockHitResult hit, Direction facing) {
        Direction side = hit.getDirection();
        if (side != facing) return OptionalInt.empty();
        BlockPos in = hit.getBlockPos().relative(side);
        Vec3 rel = hit.getLocation().subtract(in.getX(), in.getY(), in.getZ());
        double x = switch (side) {
            case NORTH -> 1.0 - rel.x;
            case SOUTH -> rel.x;
            case WEST -> rel.z;
            case EAST -> 1.0 - rel.z;
            default -> -1;
        };
        if (x < 0) return OptionalInt.empty();
        return OptionalInt.of(Mth.clamp(Mth.floor(x * 16.0F / (16.0F / 3)), 0, 2));
    }

    private static void playSound(LevelAccessor level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------------ side chain
    private static boolean connectable(BlockState state) {
        return state.getBlock() instanceof CozyShelfBlock && state.getValue(POWERED);
    }

    /** The shelves of the powered row this one belongs to, left to right; empty when it is not powered. */
    List<BlockPos> connectedTo(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!connectable(state)) return List.of();
        Direction facing = state.getValue(FACING);
        LinkedList<BlockPos> out = new LinkedList<>();
        out.add(pos);
        for (int steps = 1; steps < MAX_CHAIN; steps++) {
            BlockPos p = pos.relative(facing.getClockWise(), steps);
            SideChainPart part = partOf(level, p, facing);
            if (part != null && part.isConnectionTowards(SideChainPart.LEFT)) out.addFirst(p);
            if (part == null || part.isChainEnd()) break;
        }
        for (int steps = 1; steps < MAX_CHAIN; steps++) {
            BlockPos p = pos.relative(facing.getCounterClockWise(), steps);
            SideChainPart part = partOf(level, p, facing);
            if (part != null && part.isConnectionTowards(SideChainPart.RIGHT)) out.addLast(p);
            if (part == null || part.isChainEnd()) break;
        }
        return new ArrayList<>(out);
    }

    private static @Nullable SideChainPart partOf(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return connectable(s) && s.getValue(FACING) == facing ? s.getValue(SIDE_CHAIN_PART) : null;
    }

    private static void setPart(LevelAccessor level, BlockPos pos, SideChainPart part) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CozyShelfBlock && state.getValue(SIDE_CHAIN_PART) != part) {
            level.setBlock(pos, state.setValue(SIDE_CHAIN_PART, part), Block.UPDATE_ALL);
        }
    }

    private void updateNeighborsAfterPoweringDown(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos left = pos.relative(facing.getClockWise());
        BlockPos right = pos.relative(facing.getCounterClockWise());
        SideChainPart l = partOf(level, left, facing);
        if (l != null) setPart(level, left, l.whenDisconnectedFromTheRight());
        SideChainPart r = partOf(level, right, facing);
        if (r != null) setPart(level, right, r.whenDisconnectedFromTheLeft());
    }

    private void updateSelfAndNeighborsOnPoweringUp(LevelAccessor level, BlockPos pos, BlockState state, BlockState oldState) {
        if (!connectable(state)) return;
        boolean byNeighbour = state.getValue(SIDE_CHAIN_PART).isConnected()
                || connectable(oldState) && oldState.getValue(SIDE_CHAIN_PART).isConnected();
        if (byNeighbour) return;
        Direction facing = state.getValue(FACING);
        BlockPos left = pos.relative(facing.getClockWise());
        BlockPos right = pos.relative(facing.getCounterClockWise());
        SideChainPart l = partOf(level, left, facing);
        SideChainPart r = partOf(level, right, facing);
        int leftChain = l != null ? connectedTo(level, left).size() : 0;
        int rightChain = r != null ? connectedTo(level, right).size() : 0;
        SideChainPart self = SideChainPart.UNCONNECTED;
        int length = 1;
        if (leftChain > 0 && length + leftChain <= MAX_CHAIN) {
            self = self.whenConnectedToTheLeft();
            setPart(level, left, l.whenConnectedToTheRight());
            length += leftChain;
        }
        if (rightChain > 0 && length + rightChain <= MAX_CHAIN) {
            self = self.whenConnectedToTheRight();
            setPart(level, right, r.whenConnectedToTheLeft());
        }
        setPart(level, pos, self);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (state.getValue(POWERED)) {
            updateSelfAndNeighborsOnPoweringUp(level, pos, state, oldState);
        } else {
            updateNeighborsAfterPoweringDown(level, pos, state);
        }
    }

    // ------------------------------------------------------------------------------------------------ water, comparator
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CozyShelfBlockEntity shelf)) return 0;
        int bits = 0;
        for (int i = 0; i < 3; i++) if (!shelf.getItem(i).isEmpty()) bits |= 1 << i;
        return bits;
    }

    /** Where a shelf sits in a powered row (the newer game's side chain part; same names in the block states). */
    public enum SideChainPart implements StringRepresentable {
        UNCONNECTED("unconnected"), RIGHT("right"), CENTER("center"), LEFT("left");

        private final String name;

        SideChainPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        boolean isConnected() {
            return this != UNCONNECTED;
        }

        boolean isConnectionTowards(SideChainPart end) {
            return this == CENTER || this == end;
        }

        boolean isChainEnd() {
            return this != CENTER;
        }

        SideChainPart whenConnectedToTheRight() {
            return this == UNCONNECTED || this == LEFT ? LEFT : CENTER;
        }

        SideChainPart whenConnectedToTheLeft() {
            return this == UNCONNECTED || this == RIGHT ? RIGHT : CENTER;
        }

        SideChainPart whenDisconnectedFromTheRight() {
            return this == UNCONNECTED || this == LEFT ? UNCONNECTED : RIGHT;
        }

        SideChainPart whenDisconnectedFromTheLeft() {
            return this == UNCONNECTED || this == RIGHT ? UNCONNECTED : LEFT;
        }
    }
}
