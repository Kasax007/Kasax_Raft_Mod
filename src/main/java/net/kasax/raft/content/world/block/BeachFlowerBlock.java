package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Flower that also grows on beach sand (hibiscus). */
public class BeachFlowerBlock extends FlowerBlock {
    public BeachFlowerBlock(Holder<MobEffect> stewEffect, float seconds, Properties properties) {
        super(stewEffect, seconds, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return BeachPlantBlock.isIslandSoil(state);
    }
}
