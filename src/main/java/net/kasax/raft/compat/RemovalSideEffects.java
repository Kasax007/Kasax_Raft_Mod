package net.kasax.raft.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.21.1 port helper: the newer game tells a block entity just before its block is removed (containers drop their
 * items there). {@code compat.mixin.BlockStateRemoveMixin} calls this for Beach Craft's block entities.
 */
public interface RemovalSideEffects {
    void preRemoveSideEffects(BlockPos pos, BlockState state);
}
