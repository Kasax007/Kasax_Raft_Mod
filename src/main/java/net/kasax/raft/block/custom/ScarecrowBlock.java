package net.kasax.raft.block.custom;

import net.kasax.raft.world.portal.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

/**
 * A two block tall scarecrow. Natural hostile mob spawns within 24 blocks and crop trampling within
 * 8 blocks are prevented (see {@link #isNear}), it is tracked with a point of interest.
 */
public class ScarecrowBlock extends DoublePlantBlock {
    public static final int SPAWN_RADIUS = 24;
    public static final int TRAMPLE_RADIUS = 8;

    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);

    public ScarecrowBlock(Properties properties) {
        super(properties);
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
