package net.kasax.raft.item.custom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/** A drink: drunk with the drinking animation and sound (1.21.1 eats every food otherwise). */
public class DrinkItem extends TooltipItem {
    public DrinkItem(Properties properties, String... tooltips) {
        super(properties, tooltips);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
}
