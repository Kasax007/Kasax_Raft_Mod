package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** A conch shell to put on a shelf or table; right-click blows it like a horn (heard far across the water). */
public class ConchShellBlock extends CozyBlock {
    public ConchShellBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player.getCooldowns().isOnCooldown(asItem())) return InteractionResult.PASS;
            player.getCooldowns().addCooldown(asItem(), 40);
            level.playSound(null, pos, CozyContent.CONCH_HORN, SoundSource.RECORDS, 4.0F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
            level.gameEvent(player, GameEvent.INSTRUMENT_PLAY, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
