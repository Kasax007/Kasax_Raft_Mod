package net.kasax.raft.item;

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
}
