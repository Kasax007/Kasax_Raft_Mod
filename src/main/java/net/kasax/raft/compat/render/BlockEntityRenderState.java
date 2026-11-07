package net.kasax.raft.compat.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1.21.1 port helper: render data of a block entity for one frame. */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState;
    public int lightCoords;
    public float partialTick;
}
