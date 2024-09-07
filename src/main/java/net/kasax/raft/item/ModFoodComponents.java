package net.kasax.raft.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents {
    public static final FoodComponent COCONUT = new FoodComponent.Builder().nutrition(6).saturationModifier(0.25f).build();
    public static final FoodComponent PINA_COLADA = new FoodComponent.Builder().nutrition(9).saturationModifier(1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3000), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 50), 1f)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20), 1f)
            .alwaysEdible()
            .build();
}
