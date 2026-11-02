package net.kasax.raft.content.building.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A rusty pipe with a red hand wheel. Turning the wheel squeaks and lets out a puff of steam or bubbles. */
public class PipeValveBlock extends AxisShapedBlock {
    public PipeValveBlock(Properties properties, double[]... uprightBoxes) {
        super(properties, uprightBoxes);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        level.playSound(player, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 0.6F + level.getRandom().nextFloat() * 0.2F);
        if (level instanceof ServerLevel server) {
            boolean wet = state.getValue(WATERLOGGED);
            server.sendParticles(wet ? ParticleTypes.BUBBLE : ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.75,
                    pos.getZ() + 0.5, wet ? 12 : 4, 0.15, 0.1, 0.15, wet ? 0.05 : 0.01);
        }
        return InteractionResult.SUCCESS;
    }
}
