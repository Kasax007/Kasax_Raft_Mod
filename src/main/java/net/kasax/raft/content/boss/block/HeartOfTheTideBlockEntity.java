package net.kasax.raft.content.boss.block;

import net.kasax.raft.content.boss.BossBlocks;
import net.kasax.raft.content.boss.BossSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The aura of the Heart of the Tide: every 2 s Conduit Power + Dolphin's Grace for players in water within 32 blocks. */
public class HeartOfTheTideBlockEntity extends BlockEntity {
    public static final int RANGE = 32;
    private static final int EFFECT_TICKS = 260;

    private long tickCount;
    private boolean hadPlayers;

    public HeartOfTheTideBlockEntity(BlockPos pos, BlockState state) {
        super(BossBlocks.HEART_OF_THE_TIDE_BE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeartOfTheTideBlockEntity be) {
        be.tickCount++;
        if (be.tickCount % 40 != 0) {
            return;
        }
        AABB box = new AABB(pos).inflate(RANGE);
        boolean any = false;
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player.isAlive() && pos.closerToCenterThan(player.position(), RANGE) && player.isInWaterOrRain()) {
                player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 0, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_TICKS, 0, true, true));
                any = true;
            }
        }
        if (any && !be.hadPlayers) {
            level.playSound(null, pos, BossSounds.HEART_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        be.hadPlayers = any;
        if (be.tickCount % 160 == 0) {
            level.playSound(null, pos, BossSounds.HEART_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, HeartOfTheTideBlockEntity be) {
        be.tickCount++;
    }
}
