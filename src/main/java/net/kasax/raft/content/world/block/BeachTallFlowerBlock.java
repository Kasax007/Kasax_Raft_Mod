package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Two-block flower that also grows on beach sand (bird of paradise). Bone meal drops a copy like vanilla tall flowers. */
public class BeachTallFlowerBlock extends TallFlowerBlock {
    public BeachTallFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return BeachPlantBlock.isIslandSoil(state);
    }
}
