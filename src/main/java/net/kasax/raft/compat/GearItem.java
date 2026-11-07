package net.kasax.raft.compat;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.21.1 port helper: wearable gear that is not drawn by the vanilla armor layer (the newer game gives such items an
 * equippable component). The gear settings come with the item properties ({@link #gear(Item.Properties, Gear)}).
 */
public class GearItem extends Item implements Equipable {
    private static final Map<Item.Properties, Gear> PENDING = new IdentityHashMap<>();

    /** Slot, enchantability, repair material and equip sound of a piece of gear. */
    public record Gear(EquipmentSlot slot, int enchantability, TagKey<Item> repair, Holder<SoundEvent> equipSound) {
    }

    private final Gear gear;

    public static Item.Properties gear(Item.Properties properties, Gear gear) {
        PENDING.put(properties, gear);
        return properties;
    }

    public GearItem(Properties properties) {
        super(properties);
        this.gear = PENDING.remove(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return this.gear.slot();
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return this.gear.equipSound();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public int getEnchantmentValue() {
        return this.gear.enchantability();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(this.gear.repair());
    }
}
