package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A shell wind chime hanging from a ceiling: chimes softly now and then (more often in the rain) and when touched. */
public class WindChimeBlock extends DecorBlock {
    public WindChimeBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int chance = level.isRainingAt(pos.above()) || level.isThundering() ? 40 : 120;
        if (!state.getValue(WATERLOGGED) && random.nextInt(chance) == 0) {
            chime(level, pos, random, 0.25F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            float[] notes = {1.0F, 1.335F, 1.782F};
            for (float pitch : notes) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F,
                        pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void chime(Level level, BlockPos pos, RandomSource random, float volume) {
        float[] notes = {1.0F, 1.19F, 1.335F, 1.498F, 1.782F};
        int n = 1 + random.nextInt(3);
        for (int i = 0; i < n; i++) {
            float pitch = notes[random.nextInt(notes.length)];
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, volume, pitch, false);
        }
    }
}
