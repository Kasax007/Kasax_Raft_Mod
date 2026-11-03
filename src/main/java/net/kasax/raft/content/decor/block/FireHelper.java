package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Shared lighting / dousing rules of the brazier and the tiki torch. */
final class FireHelper {
    private FireHelper() {
    }

    static boolean isIgniter(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    static boolean isDouser(ItemStack stack) {
        return stack.is(ItemTags.SHOVELS);
    }

    /** Plays the sound and uses up the igniter. */
    static void ignite(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.FIRE_CHARGE)) {
            level.playSound(player, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            stack.consume(1, player);
        } else {
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    static void douse(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, double smokeY) {
        level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + smokeY, pos.getZ() + 0.5,
                    6, 0.15, 0.1, 0.15, 0.01);
        }
        if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
