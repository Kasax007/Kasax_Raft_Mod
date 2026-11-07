package net.kasax.raft.content.world.block;

import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Barnacles growing as a thin layer on any side of stone, wood or metal (like glow lichen), in or out of water.
 * Spreadable so the world generator's multiface growth can place clusters.
 */
public class BarnaclesBlock extends MultifaceBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends MultifaceBlock> codec() {
        return simpleCodec(BarnaclesBlock::new);
    }

    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    public BarnaclesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return spreader;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, net.minecraft.core.BlockPos pos121) {
        return state.getFluidState().isEmpty();
    }
}
