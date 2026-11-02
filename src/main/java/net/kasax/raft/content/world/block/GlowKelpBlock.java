package net.kasax.raft.content.world.block;

import net.kasax.raft.content.world.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Top of a glow kelp stalk: grows like kelp (age 0..25) and glows (light 8). */
public class GlowKelpBlock extends KelpBlock {
    public GlowKelpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return WorldBlocks.GLOW_KELP_PLANT;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
    }
}
