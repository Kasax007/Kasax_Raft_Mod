package net.kasax.raft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

public class ModFoodComponents {
    public static final FoodProperties COCONUT = new FoodProperties.Builder().nutrition(6).saturationModifier(0.25f).build();

    /** A drink (see {@link net.kasax.raft.item.custom.DrinkItem}): Speed, a short Regeneration and a moment of Nausea. */
    public static final FoodProperties PINA_COLADA = new FoodProperties.Builder().nutrition(9).saturationModifier(1f)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3000), 1.0F)
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 50), 1.0F)
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 20), 1.0F)
            .usingConvertsTo(Items.BOWL)
            .build();

    public static final FoodProperties SHARK_MEAT = new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f)
            .effect(new MobEffectInstance(MobEffects.HUNGER, 300, 0), 0.3f)
            .build();
    public static final FoodProperties COOKED_SHARK_MEAT = new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build();

    /** Fresh water: a quick drink that fills the stomach a little. */
    public static final FoodProperties FRESH_WATER = new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.SATURATION, 2, 0), 1.0F)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .build();

    /** Coconut milk: Regeneration I for five seconds. */
    public static final FoodProperties COCONUT_MILK = new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .build();
}
