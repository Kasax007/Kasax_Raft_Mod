package net.kasax.raft.content.cozy.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rested (Beach Craft 3.3): the reward for sitting or lying down in a cozy place. Heals half a heart every 4 seconds
 * and makes hunger drain 25 % slower (the exhaustion part is in PlayerMixin). Its length comes from the comfort of the
 * place (CozyComfort).
 */
public class RestedEffect extends MobEffect {
    public RestedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2C46D);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % 80 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob.getHealth() < mob.getMaxHealth()) mob.heal(1.0F);
        return true;
    }
}
