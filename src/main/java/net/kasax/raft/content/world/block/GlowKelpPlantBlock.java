package net.kasax.raft.content.world.block;

import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpPlantBlock;

/** Stalk part of glow kelp (light 8). Breaking it drops glow kelp. */
public class GlowKelpPlantBlock extends KelpPlantBlock {
    public GlowKelpPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) WorldBlocks.GLOW_KELP;
    }
}
