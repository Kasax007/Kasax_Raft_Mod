package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Two-block grass that grows on sand (sea oats). */
public class BeachDoublePlantBlock extends DoublePlantBlock {
    public BeachDoublePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return BeachPlantBlock.isIslandSoil(state);
    }
}
