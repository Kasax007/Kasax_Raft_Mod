package net.kasax.raft.content.world.block;

import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.kasax.raft.compat.VShapes;

/**
 * Pineapple crop, planted from a pineapple crown on farmland or on the raft's crop plot (both are in
 * {@code #minecraft:supports_crops}). Ages 0..7; ripe at 7 it drops a pineapple and a new crown.
 */
public class PineappleCropBlock extends CropBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        int[] heights = {4, 5, 7, 8, 10, 11, 13, 14};
        int[] widths = {6, 8, 10, 10, 12, 12, 14, 14};
        for (int i = 0; i < 8; i++) {
            SHAPES[i] = VShapes.column(widths[i], 0.0, heights[i]);
        }
    }

    public PineappleCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return WorldBlocks.PINEAPPLE_CROWN;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }
}
