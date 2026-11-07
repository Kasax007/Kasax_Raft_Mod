package net.kasax.raft.block.custom;

import net.kasax.raft.world.portal.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.kasax.raft.compat.VShapes;

/**
 * A two block tall scarecrow. Natural hostile mob spawns within 24 blocks and crop trampling within
 * 8 blocks are prevented (see {@link #isNear}), it is tracked with a point of interest.
 */
public class ScarecrowBlock extends DoublePlantBlock {
    public static final int SPAWN_RADIUS = 24;
    public static final int TRAMPLE_RADIUS = 8;

    private static final VoxelShape SHAPE = VShapes.column(8.0, 0.0, 16.0);

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ScarecrowBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Vanilla places the upper half from the default state (facing north); the head must face the same way as the body. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        BlockPos above = pos.above();
        level.setBlockAndUpdate(above, copyWaterloggedFrom(level, above,
                defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, state.getValue(FACING))));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    /** True if a scarecrow stands within {@code radius} blocks of the position. */
    public static boolean isNear(ServerLevel level, BlockPos pos, int radius) {
        PoiManager poiManager = level.getPoiManager();
        return poiManager.getInRange(type -> type.is(ModPoiTypes.SCARECROW_KEY), pos, radius, PoiManager.Occupancy.ANY)
                .findAny().isPresent();
    }

    /** The lower halves are the point of interest, the upper half would count twice. */
    public static boolean isLower(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }
}
