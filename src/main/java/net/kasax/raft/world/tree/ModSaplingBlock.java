package net.kasax.raft.world.tree;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.util.math.BlockPos;

import javax.swing.text.html.BlockView;
import java.util.function.Supplier;

public class ModSaplingBlock extends SaplingBlock {
    private final Supplier<Block> ground;

    public ModSaplingBlock(SaplingGenerator generator, Settings settings, Supplier<Block> ground) {
        super(generator, settings);
        this.ground = ground;
    }
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(ground.get());
    }
}
