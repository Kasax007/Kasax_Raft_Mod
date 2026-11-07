package net.kasax.raft.compat;

import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.21.1 port helper: the newer game sets furnace fuel and compost chance on the item properties. 1.21.1 keeps them
 * in Fabric registries, so the values are remembered here per properties object and registered when the item is
 * built (see {@code compat.mixin.ItemMixin}).
 */
public final class ItemProps {
    // the newer game's standard values
    public static final int COOKING_TIME_WOOD_BLOCKS = 300;
    public static final int COOKING_TIME_WOOD_SLABS = 150;
    public static final int COOKING_TIME_WOOD_ITEMS_LARGE = 200;
    public static final int COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL = 100;
    public static final int COOKING_TIME_DRY_PLANTS = 100;
    public static final int COOKING_TIME_BOATS = 1200;
    public static final float COMPOSTABLE_LOW = 0.3F;
    public static final float COMPOSTABLE_MEDIUM = 0.5F;
    public static final float COMPOSTABLE_HIGH = 0.65F;

    private static final Map<Item.Properties, Integer> FUEL = new IdentityHashMap<>();
    private static final Map<Item.Properties, net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType>> RESIST_PENDING = new IdentityHashMap<>();
    /** Items whose dropped stacks ignore the damage types of a tag (the newer game's damage_resistant component). */
    public static final Map<Item, net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType>> DAMAGE_RESISTANT = new IdentityHashMap<>();
    private static final Map<Item.Properties, Float> COMPOST = new IdentityHashMap<>();

    private ItemProps() {
    }

    public static Item.Properties cookingFuel(Item.Properties properties, int ticks) {
        FUEL.put(properties, ticks);
        return properties;
    }

    public static Item.Properties compostable(Item.Properties properties, float chance) {
        COMPOST.put(properties, chance);
        return properties;
    }

    public static Item.Properties damageResistant(Item.Properties properties, net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> types) {
        RESIST_PENDING.put(properties, types);
        return properties;
    }

    /** Called when an item is built from these properties. */
    public static void apply(Item item, Item.Properties properties) {
        Integer fuel = FUEL.remove(properties);
        if (fuel != null) FuelRegistry.INSTANCE.add(item, fuel);
        Float chance = COMPOST.remove(properties);
        if (chance != null) CompostingChanceRegistry.INSTANCE.add(item, chance);
        var resist = RESIST_PENDING.remove(properties);
        if (resist != null) DAMAGE_RESISTANT.put(item, resist);
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && AbstractFurnaceBlockEntity.isFuel(stack);
    }

    public static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer t = AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
        return t == null ? 0 : t;
    }
}
