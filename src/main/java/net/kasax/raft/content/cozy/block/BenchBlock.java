package net.kasax.raft.content.cozy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * A bench: benches of the same kind and facing that stand side by side join into one long bench; only the ends
 * have legs and armrests ({@code part}: single, left, center, right as seen by someone sitting on it).
 */
public class BenchBlock extends CozyBlock {
    public enum Part implements StringRepresentable {
        SINGLE("single"), LEFT("left"), CENTER("center"), RIGHT("right");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BenchBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(PART, Part.SINGLE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    protected String shapeName(BlockState state) {
        return config().shape + "_" + state.getValue(PART).getSerializedName();
    }

    private boolean joins(LevelReader level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(FACING) == facing;
    }

    private BlockState withPart(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        boolean left = joins(level, pos.relative(facing.getCounterClockWise()), facing);
        boolean right = joins(level, pos.relative(facing.getClockWise()), facing);
        Part part = left && right ? Part.CENTER : left ? Part.RIGHT : right ? Part.LEFT : Part.SINGLE;
        return state.setValue(PART, part);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withPart(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        BlockState s = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        return s.is(this) && directionToNeighbour.getAxis().isHorizontal() ? withPart(s, level, pos) : s;
    }
}
