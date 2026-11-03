package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lantern that stands on a block or hangs from one ({@code hanging}, {@code waterlogged}) with the shapes of its
 * own models: paper lanterns, the ship lantern and the jelly lamp.
 */
public class DecorLanternBlock extends LanternBlock {
    private final VoxelShape standing;
    private final VoxelShape hanging;

    /** @param shape name of the standing shape; the hanging one is {@code <shape>_hanging} */
    public DecorLanternBlock(Properties properties, String shape) {
        super(properties);
        this.standing = DecorBlock.shape(shape);
        this.hanging = DecorBlock.shape(shape + "_hanging");
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? hanging : standing;
    }
}
