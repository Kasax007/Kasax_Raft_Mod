package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** A brass bell on a wall bracket: rings when used or hit by a projectile. */
public class ShipBellBlock extends DecorBlock {
    public ShipBellBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ring(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel) ring(level, hit.getBlockPos(), projectile.getOwner() instanceof Player p ? p : null);
    }

    private static void ring(Level level, BlockPos pos, Player player) {
        level.playSound(player, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.6F, 1.25F + level.getRandom().nextFloat() * 0.1F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
