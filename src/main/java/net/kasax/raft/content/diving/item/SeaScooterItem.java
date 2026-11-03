package net.kasax.raft.content.diving.item;

import net.kasax.raft.content.diving.DivingSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.function.Consumer;

/**
 * Hold use under water and the scooter pulls you along where you look. It runs on a 3 000 FE battery (Team Reborn
 * Energy item component), charged in the air compressor or any charger that speaks the energy API.
 */
public class SeaScooterItem extends Item implements SimpleEnergyItem {
    public static final long CAPACITY = 3_000;
    public static final long MAX_INPUT = 100;
    /** FE per tick while running: a full battery lasts 75 seconds. */
    public static final long DRAIN = 2;
    private static final double ACCELERATION = 0.085;
    private static final double MAX_SPEED = 0.62;

    public SeaScooterItem(Properties properties) {
        super(properties);
    }

    public static boolean canRun(LivingEntity user, ItemStack stack) {
        return user.isInWater() && SimpleEnergyItem.getStoredEnergyUnchecked(stack) >= DRAIN;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isInWater()) {
            return InteractionResult.PASS;
        }
        if (getStoredEnergy(stack) < DRAIN) {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.5F, 0.7F);
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!canRun(user, stack)) {
            user.releaseUsingItem();
            return;
        }
        // movement of players is decided by their client; other users (none yet) move on the server
        if (level.isClientSide() == (user instanceof Player)) {
            Vec3 look = user.getLookAngle();
            Vec3 motion = user.getDeltaMovement().add(look.scale(ACCELERATION));
            if (motion.length() > MAX_SPEED) {
                motion = motion.normalize().scale(MAX_SPEED);
            }
            user.setDeltaMovement(motion);
            user.resetFallDistance();
        }
        if (level instanceof ServerLevel serverLevel) {
            tryUseEnergy(stack, DRAIN);
            Vec3 look = user.getLookAngle();
            serverLevel.sendParticles(ParticleTypes.BUBBLE, user.getX() - look.x * 0.8, user.getY() + 0.9 - look.y * 0.8,
                    user.getZ() - look.z * 0.8, 3, 0.15, 0.15, 0.15, 0.05);
            int used = getUseDuration(stack, user) - ticksRemaining;
            if (used % 12 == 0) {
                serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(), DivingSounds.SEA_SCOOTER_RUN, SoundSource.PLAYERS,
                        0.6F, 1.0F + user.getRandom().nextFloat() * 0.1F);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72_000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    // ------------------------------------------------------------------------------------------- energy
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return MAX_INPUT;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    public ItemStack charged() {
        ItemStack stack = new ItemStack(this);
        setStoredEnergy(stack, CAPACITY);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * getStoredEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) getStoredEnergy(stack) / CAPACITY;
        return ARGB.colorFromFloat(1.0F, 1.0F - 0.6F * f, 0.55F + 0.4F * f, 0.15F) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.sea_scooter.energy", getStoredEnergy(stack), CAPACITY)
                .withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.translatable("tooltip.raft.sea_scooter").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
