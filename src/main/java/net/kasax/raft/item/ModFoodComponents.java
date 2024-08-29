package net.kasax.raft.item;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;

public class ModFoodComponents {
    public static final FoodComponent COCONUT = new FoodComponent.Builder().hunger(6).saturationModifier(0.25f).build();
    public static final FoodComponent PINA_COLADA = new FoodComponent.Builder().hunger(9).saturationModifier(1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3000), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 50), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20), 1f)
            .alwaysEdible()
            .build();
}
