package net.kasax.raft.compat;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.21.1 port helper: wearable gear (the newer game builds it from item properties with an equipment asset). Here
 * it is an armor item; its material and slot come with the item properties ({@link #gear(Item.Properties, Gear)}),
 * so the item classes keep their one-argument constructors.
 */
public class GearItem extends ArmorItem {
    private static final Map<Item.Properties, Gear> PENDING = new IdentityHashMap<>();

    /** Armor material (with its texture layers) and armor type of a piece of gear. */
    public record Gear(Holder<ArmorMaterial> material, ArmorItem.Type type) {
    }

    public static Item.Properties gear(Item.Properties properties, Gear gear) {
        PENDING.put(properties, gear);
        return properties;
    }

    private static Gear pending(Item.Properties properties) {
        Gear gear = PENDING.get(properties);
        if (gear == null) throw new IllegalStateException("gear item built without GearItem.gear(...)");
        return gear;
    }

    public GearItem(Properties properties) {
        super(pending(properties).material(), pending(properties).type(), properties);
        PENDING.remove(properties);
    }
}
