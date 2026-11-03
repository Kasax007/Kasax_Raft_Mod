package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A floating channel buoy ({@code waterlogged}) with a blinking lamp. It floats in the top water block (placed with
 * {@link net.kasax.raft.content.decor.item.BuoyItem} by clicking the water surface) or stands on the ground.
 */
public class BuoyBlock extends DecorBlock {
    public BuoyBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
}
