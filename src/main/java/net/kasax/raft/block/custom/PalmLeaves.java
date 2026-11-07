package net.kasax.raft.block.custom;

import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import static net.kasax.raft.block.custom.PalmCoconutLeaves.HAS_COCONUT;

public class PalmLeaves extends LeavesBlock {

    public PalmLeaves(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DISTANCE, 7).setValue(PERSISTENT, false).setValue(WATERLOGGED, false)
                .setValue(HAS_COCONUT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_COCONUT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HAS_COCONUT) || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.decaying(state)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        // The leaves regrow a coconut after a while: 1 in 20 random ticks, about 20-25 minutes at the default tick speed.
        if (state.getValue(HAS_COCONUT) && random.nextInt(20) == 0) {
            level.setBlock(pos, ModBlocks.PALM_COCONUT_LEAVES.defaultBlockState()
                    .setValue(PERSISTENT, state.getValue(PERSISTENT))
                    .setValue(DISTANCE, state.getValue(DISTANCE))
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED)), Block.UPDATE_ALL);
        }
    }
}
