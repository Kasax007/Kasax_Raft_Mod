package net.kasax.raft.content.npc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Where the people of the sea walk and stand: on floors. Vanilla mobs may step from a table or a chair onto a railing and
 * walk along it (the top of a fence counts as floor once they are up there); a person on the veranda railing looks
 * wrong and ends up in the sea, so for them the top of a fence, railing or wall is no floor at all.
 */
final class NpcPaths {
    private NpcPaths() {
    }

    static GroundPathNavigation navigation(Mob mob, Level level) {
        return new GroundPathNavigation(mob, level) {
            @Override
            protected PathFinder createPathFinder(int maxVisitedNodes) {
                this.nodeEvaluator = new Evaluator();
                return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
            }
        };
    }

    /** Fences, railings, walls and fence gates: things to lean on, not to stand on. */
    static boolean fenceLike(BlockState state) {
        return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || state.is(BlockTags.FENCE_GATES)
                || state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof FenceGateBlock;
    }

    /** True while the entity stands on top of a fence, railing or wall (old saves, or pushed up there). */
    static boolean onFenceTop(Entity e) {
        if (!e.onGround()) {
            return false;
        }
        Level level = e.level();
        return fenceLike(level.getBlockState(e.getOnPos()))
                || fenceLike(level.getBlockState(BlockPos.containing(e.getX(), e.getY() - 0.51, e.getZ())));
    }

    /**
     * A cell a person can stand in to work: nothing solid at the feet (a rug or a closed trapdoor is fine, a chair is
     * not) or at the head, no water, and a real floor below: a full block, a top slab or upside-down stairs (no table
     * top, chair or railing).
     */
    static boolean standable(BlockGetter level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) {
            return false;
        }
        if (!head.getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }
        VoxelShape f = feet.getCollisionShape(level, pos);
        if (!f.isEmpty() && f.max(Direction.Axis.Y) > 0.2) {
            return false;
        }
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (floor.isCollisionShapeFullBlock(level, below)) {
            return true;
        }
        return (floor.getBlock() instanceof SlabBlock || floor.getBlock() instanceof StairBlock)
                && floor.isFaceSturdy(level, below, Direction.UP);
    }

    /**
     * The walk node evaluator of the people of the sea: the cell above a fence, railing or wall is blocked, and so are
     * closed trapdoors that vanilla paths treat as open (a mob walking into them gets stuck): one in the upper half of
     * the feet cell or the lower half of the head cell, and a bench of trapdoors on the floor to step onto.
     */
    static final class Evaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            PathType type = super.getPathType(context, x, y, z);
            if (type != PathType.BLOCKED && type != PathType.OPEN && type != PathType.FENCE
                    && fenceLike(context.getBlockState(new BlockPos(x, y - 1, z)))) {
                return PathType.BLOCKED;
            }
            return type;
        }

        @Override
        public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
            PathType type = super.getPathTypeOfMob(context, x, y, z, mob);
            if (mob.getPathfindingMalus(type) >= 0.0F && trapdoorInTheWay(context, x, y, z)) {
                return PathType.BLOCKED;
            }
            return type;
        }

        private static boolean trapdoorInTheWay(PathfindingContext context, int x, int y, int z) {
            if (closedTrapdoor(context.getBlockState(new BlockPos(x, y, z)), Half.TOP)
                    || closedTrapdoor(context.getBlockState(new BlockPos(x, y + 1, z)), Half.BOTTOM)) {
                return true;
            }
            BlockPos floor = new BlockPos(x, y - 2, z);
            return closedTrapdoor(context.getBlockState(new BlockPos(x, y - 1, z)), Half.TOP)
                    && !context.getBlockState(floor).getCollisionShape(context.level(), floor).isEmpty();
        }

        private static boolean closedTrapdoor(BlockState state, Half half) {
            return state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN)
                    && state.getValue(TrapDoorBlock.HALF) == half;
        }
    }
}
