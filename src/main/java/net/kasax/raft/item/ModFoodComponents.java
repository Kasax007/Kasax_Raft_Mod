package net.kasax.raft.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

public class ModFoodComponents {
    public static final FoodProperties COCONUT = new FoodProperties.Builder().nutrition(6).saturationModifier(0.25f).build();

    public static final FoodProperties PINA_COLADA = new FoodProperties.Builder().nutrition(9).saturationModifier(1f)
            .alwaysEdible()
            .build();
    public static final Consumable PINA_COLADA_EFFECTS = Consumable.builder()
            .animation(ItemUseAnimation.DRINK)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.SPEED, 3000),
                    new MobEffectInstance(MobEffects.REGENERATION, 50),
                    new MobEffectInstance(MobEffects.NAUSEA, 20))))
            .build();

    public static final FoodProperties SHARK_MEAT = new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build();
    public static final Consumable SHARK_MEAT_EFFECTS = Consumable.builder()
            .consumeSeconds(1.6f)
            .animation(ItemUseAnimation.EAT)
            .sound(SoundEvents.GENERIC_EAT)
            .hasConsumeParticles(true)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.HUNGER, 300, 0)), 0.3f))
            .build();
    public static final FoodProperties COOKED_SHARK_MEAT = new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build();

    /** Fresh water: a quick drink that fills the stomach a little. */
    public static final Consumable FRESH_WATER = Consumable.builder()
            .consumeSeconds(1.6f)
            .animation(ItemUseAnimation.DRINK)
            .sound(SoundEvents.GENERIC_DRINK)
            .hasConsumeParticles(false)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.SATURATION, 5, 0))))
            .build();

    /** Coconut milk: Regeneration I for five seconds. */
    public static final Consumable COCONUT_MILK = Consumable.builder()
            .consumeSeconds(1.6f)
            .animation(ItemUseAnimation.DRINK)
            .sound(SoundEvents.GENERIC_DRINK)
            .hasConsumeParticles(false)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.REGENERATION, 100, 0))))
            .build();
}
