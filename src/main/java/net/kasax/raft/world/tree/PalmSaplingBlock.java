package net.kasax.raft.world.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public class PalmSaplingBlock extends SaplingBlock {
    public PalmSaplingBlock(TreeGrower grower, Properties properties) {
        super(grower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
        return floor.is(Blocks.SAND);
    }
}
