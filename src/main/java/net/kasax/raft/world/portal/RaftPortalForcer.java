package net.kasax.raft.world.portal;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.custom.RaftPortalBlock;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.Optional;

/**
 * Finds the closest raft portal in a level or builds a new one: a driftwood frame with the
 * portal inside. In the raft dimension the frame floats high above the sea (Y 200 to 220)
 * on a small driftwood platform.
 */
public final class RaftPortalForcer {
    public static final int RAFT_MIN_Y = 200;
    public static final int RAFT_MAX_Y = 220;
    private static final int SEARCH_RADIUS = 128;

    private RaftPortalForcer() {}

    public static Optional<BlockPos> findClosestPortalPosition(ServerLevel level, BlockPos approximateExitPos, WorldBorder worldBorder) {
        PoiManager poiManager = level.getPoiManager();
        poiManager.ensureLoadedAndValid(level, approximateExitPos, SEARCH_RADIUS);
        return poiManager.getInSquare(type -> type.is(ModPoiTypes.RAFT_PORTAL_KEY), approximateExitPos, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(worldBorder::isWithinBounds)
                .filter(pos -> level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(approximateExitPos)).thenComparingInt(Vec3i::getY));
    }

    /**
     * Builds a 4x5 frame with a 2x3 portal. {@code origin} is the horizontal position (the Y
     * is chosen here). Returns the interior rectangle; its min corner is the lowest block with the
     * smallest coordinate along the axis.
     */
    public static Optional<BlockUtil.FoundRectangle> createPortal(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction side = direction.getClockWise();
        boolean raftDim = level.dimension() == ModDimensions.RAFTDIM_LEVEL_KEY;

        int minY = level.getMinY() + 2;
        int maxY = Math.min(level.getMaxY(), level.getMinY() + level.getLogicalHeight() - 1) - 6;
        int baseY;
        if (raftDim) {
            baseY = Mth.clamp(Mth.clamp(origin.getY(), RAFT_MIN_Y + 5, RAFT_MAX_Y - 5), minY, maxY);
        } else {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
            baseY = Mth.clamp(surface, minY, maxY);
        }
        if (maxY < minY) {
            return Optional.empty();
        }

        BlockPos base = level.getWorldBorder().clampToBounds(new BlockPos(origin.getX(), baseY, origin.getZ()));
        base = new BlockPos(base.getX(), baseY, base.getZ());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState frame = ModBlocks.DRIFTWOOD_PORTAL_FRAME.defaultBlockState();
        BlockState planks = ModBlocks.DRIFTWOOD_PLANKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Platform and clearance: 6 wide (along the portal) x 4 deep, five blocks of headroom.
        for (int w = -2; w <= 3; w++) {
            for (int d = -2; d <= 1; d++) {
                for (int h = -1; h <= 4; h++) {
                    pos.setWithOffset(base, w * direction.getStepX() + d * side.getStepX(), h, w * direction.getStepZ() + d * side.getStepZ());
                    if (h == -1) {
                        BlockState existing = level.getBlockState(pos);
                        if (raftDim || existing.canBeReplaced() || !existing.getFluidState().isEmpty()) {
                            level.setBlock(pos, planks, 18);
                        }
                    } else {
                        level.setBlock(pos, air, 18);
                    }
                }
            }
        }

        // Frame ring around the 2x3 interior.
        for (int w = -1; w <= 2; w++) {
            for (int h = -1; h <= 3; h++) {
                if (w == -1 || w == 2 || h == -1 || h == 3) {
                    pos.setWithOffset(base, w * direction.getStepX(), h, w * direction.getStepZ());
                    level.setBlock(pos, frame, 18);
                }
            }
        }

        RaftPortalShape.findEmptyPortalShape(level, base, axis).ifPresent(shape -> shape.createPortalBlocks(level));
        return Optional.of(new BlockUtil.FoundRectangle(base.immutable(), 2, 3));
    }

    /** Which axis a portal block at the position uses (X when it is not a portal). */
    public static Direction.Axis axisOf(BlockState state) {
        return state.getOptionalValue(RaftPortalBlock.AXIS).orElse(Direction.Axis.X);
    }
}
