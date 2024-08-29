package net.kasax.raft.block.custom;

import net.kasax.raft.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import static net.kasax.raft.block.custom.PalmCoconutLeaves.HAS_COCONUT;

public class PalmLeaves extends LeavesBlock {

    public PalmLeaves(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HAS_COCONUT, false).with(PERSISTENT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_COCONUT);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.get(HAS_COCONUT) || (state.get(DISTANCE) == 7 && !(Boolean) state.get(PERSISTENT));
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        // Check if the block should regrow into coconut leaves
        if (state.get(HAS_COCONUT) && random.nextInt(5000) == 1) { // 0.1% chance per tick
            world.setBlockState(pos, ModBlocks.PALM_COCONUT_LEAVES.getDefaultState().with(PERSISTENT, state.get(PERSISTENT)).with(WATERLOGGED, false), Block.NOTIFY_ALL);
        }

        if (this.shouldDecay(state)) {
            dropStacks(state, world, pos);
            world.removeBlock(pos, false);
        }
    }
}
