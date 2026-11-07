package net.kasax.raft.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: the equipment slot a ticking stack is worn in (the newer game passes it to inventoryTick). */
public final class Equip {
    private Equip() {
    }

    public static @Nullable EquipmentSlot slotOf(Entity owner, ItemStack stack) {
        if (!(owner instanceof LivingEntity living)) return null;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (living.getItemBySlot(slot) == stack) return slot;
        }
        return null;
    }
}
