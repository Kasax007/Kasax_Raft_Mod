package net.kasax.raft.content.diving;

import net.kasax.raft.platform.ServerTickEvents;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Server-side logic of the diving gear, run once per tick for every player:
 * <ul>
 *   <li>breathing from the tank (called by the air mixin instead of losing air),</li>
 *   <li>refilling the worn tank at the surface and in bubble columns,</li>
 *   <li>a warning beep when the tank runs low,</li>
 *   <li>the helmet lamp (Night Vision while the head is under water),</li>
 *   <li>the full set bonus (Dolphin's Grace while in water),</li>
 *   <li>the flippers' penalty on land.</li>
 * </ul>
 */
public final class DivingEvents {
    private DivingEvents() {
    }

    /** Air ticks per tick refilled with the head above water or in a bubble column. */
    public static final int REFILL_PER_TICK = 40;
    /** The tank's component is written every few ticks (fewer equipment updates); the average drain is 1 per tick. */
    private static final int DRAIN_STEP = 5;
    private static final int LOW_AIR_TICKS = 300;
    /**
     * Night Vision of the helmet lamp: given for 13 s and topped up below 11 s, so it never reaches the last 10 s in
     * which Night Vision flickers. Taken away again when the head leaves the water.
     */
    private static final int LAMP_TICKS = 260;
    private static final int LAMP_REFRESH = 220;

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                tickPlayer(level, player);
            }
        });
    }

    /**
     * Called from {@code LivingEntity.decreaseAirSupply} (server side): true if the entity breathed from its tank this
     * tick, so it does not lose air.
     */
    public static boolean breatheFromTank(LivingEntity entity) {
        if (!DivingGear.wearsHelmet(entity)) {
            return false;
        }
        ItemStack tank = DivingGear.tank(entity);
        if (tank.isEmpty()) {
            return false;
        }
        int air = OxygenTankItem.getAir(tank);
        if (air <= 0) {
            return false;
        }
        if (entity.level().getGameTime() % DRAIN_STEP == 0) {
            OxygenTankItem.setAir(tank, Math.max(0, air - DRAIN_STEP));
        }
        return true;
    }

    private static void tickPlayer(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ItemStack tank = DivingGear.tank(player);
        if (!tank.isEmpty()) {
            tickTank(level, player, tank);
        }

        tickLamp(player);

        if (DivingGear.fullSet(player) && player.isInWater()) {
            MobEffectInstance current = player.getEffect(MobEffects.DOLPHINS_GRACE);
            if (current == null || (current.getDuration() < 20 && current.getDuration() != -1)) {
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
            }
        }

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean penalty = DivingGear.wearsFlippers(player) && !player.isInWater() && !player.getAbilities().flying
                    && !player.isPassenger();
            boolean has = speed.hasModifier(DivingGear.FLIPPERS_ON_LAND);
            if (penalty && !has) {
                speed.addTransientModifier(new AttributeModifier(DivingGear.FLIPPERS_ON_LAND, -0.25,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!penalty && has) {
                speed.removeModifier(DivingGear.FLIPPERS_ON_LAND);
            }
        }
    }

    private static void tickLamp(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean lampOn = DivingGear.wearsLampHelmet(player) && player.isEyeInFluid(FluidTags.WATER);
        if (lampOn) {
            if (current == null || (current.isAmbient() && current.getDuration() < LAMP_REFRESH)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, LAMP_TICKS, 0, true, false, true));
            }
        } else if (current != null && current.isAmbient() && current.getDuration() <= LAMP_TICKS) {
            // the lamp's own effect (ambient, short); potions are not ambient and stay
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void tickTank(ServerLevel level, ServerPlayer player, ItemStack tank) {
        boolean headInWater = player.isEyeInFluid(FluidTags.WATER);
        BlockPos eye = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        boolean inBubbles = level.getBlockState(eye).is(Blocks.BUBBLE_COLUMN)
                || level.getBlockState(player.blockPosition()).is(Blocks.BUBBLE_COLUMN);
        if ((!headInWater || inBubbles) && !OxygenTankItem.isFull(tank)) {
            OxygenTankItem.fill(tank, REFILL_PER_TICK);
            if (OxygenTankItem.isFull(tank)) {
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), DivingSounds.TANK_REFILL,
                        SoundSource.PLAYERS, 0.5F, 1.2F);
            }
            return;
        }
        if (headInWater && DivingGear.wearsHelmet(player)) {
            int air = OxygenTankItem.getAir(tank);
            if (air > 0 && air <= LOW_AIR_TICKS && level.getGameTime() % 40 == 0) {
                // the regulator beeps faster pitch as the tank empties
                float pitch = 1.6F - 0.6F * air / LOW_AIR_TICKS;
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), DivingSounds.TANK_LOW,
                        SoundSource.PLAYERS, 0.6F, pitch);
            }
        }
    }
}
