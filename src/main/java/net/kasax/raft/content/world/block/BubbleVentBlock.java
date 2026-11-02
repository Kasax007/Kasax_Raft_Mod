package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Volcanic vent in the sea floor. It is in {@code #minecraft:enables_bubble_column_push_up}, so the water above it
 * becomes an upward bubble column like over soul sand (divers can catch their breath in it), and it fizzes with
 * bubbles.
 */
public class BubbleVentBlock extends Block {
    public BubbleVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BubbleColumnBlock.updateColumn(Blocks.BUBBLE_COLUMN, level, pos.above(), state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (!level.getFluidState(above).is(FluidTags.WATER)) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
            }
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.02,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS, 0.4F + random.nextFloat() * 0.2F, 0.8F + random.nextFloat() * 0.2F, false);
        }
    }
}
