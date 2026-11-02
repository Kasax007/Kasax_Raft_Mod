package net.kasax.raft.content.building.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * An open steel grating in slab form (bottom, top or double) that you can see through, like a copper grate. Neighbouring
 * gratings of the same height hide the faces between them so a walkway looks like one piece.
 */
public class CatwalkBlock extends SlabBlock {
    public CatwalkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        if (neighborState.is(this)) {
            SlabType mine = state.getValue(TYPE);
            SlabType theirs = neighborState.getValue(TYPE);
            if (direction.getAxis().isHorizontal()) {
                if (mine == theirs || theirs == SlabType.DOUBLE) return true;
            } else if (direction == Direction.UP) {
                if (mine != SlabType.BOTTOM && theirs != SlabType.TOP) return true;
            } else if (mine != SlabType.TOP && theirs != SlabType.BOTTOM) {
                return true;
            }
        }
        return super.skipRendering(state, neighborState, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return 1.0F;
    }
}
