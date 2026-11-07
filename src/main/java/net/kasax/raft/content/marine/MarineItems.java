package net.kasax.raft.content.marine;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

/** Items of the marine module: creature drops, eel food and the spawn eggs (creative tab CREATURES). */
public final class MarineItems {
    public static final FoodProperties EEL_FILLET_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build();
    public static final FoodProperties COOKED_EEL_FILLET_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build();

    public static Item HAMMERHEAD_SHARK_SPAWN_EGG;
    public static Item WHALE_SPAWN_EGG;
    public static Item MANTA_RAY_SPAWN_EGG;
    public static Item JELLYFISH_SPAWN_EGG;
    public static Item ANGLERFISH_SPAWN_EGG;
    public static Item MORAY_EEL_SPAWN_EGG;

    public static Item JELLY_BLOB;
    public static Item GLOW_JELLY;
    public static Item ANGLER_LURE;
    public static Item EEL_FILLET;
    public static Item COOKED_EEL_FILLET;

    private MarineItems() {
    }

    static void register() {
        BcTabs.Tab tab = BcTabs.Tab.CREATURES;
        // the 2.0 shark egg leads the creature tab
        BcTabs.add(tab, ModItems.SHARK_SPAWN_EGG);
        HAMMERHEAD_SHARK_SPAWN_EGG = BcReg.item("hammerhead_shark_spawn_egg", p -> new SpawnEggItem(MarineEntities.HAMMERHEAD_SHARK, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        WHALE_SPAWN_EGG = BcReg.item("whale_spawn_egg", p -> new SpawnEggItem(MarineEntities.WHALE, 0xFFFFFF, 0xFFFFFF, p), new Item.Properties(), tab);
        MANTA_RAY_SPAWN_EGG = BcReg.item("manta_ray_spawn_egg", p -> new SpawnEggItem(MarineEntities.MANTA_RAY, 0xFFFFFF, 0xFFFFFF, p), new Item.Properties(), tab);
        JELLYFISH_SPAWN_EGG = BcReg.item("jellyfish_spawn_egg", p -> new SpawnEggItem(MarineEntities.JELLYFISH, 0xFFFFFF, 0xFFFFFF, p), new Item.Properties(), tab);
        ANGLERFISH_SPAWN_EGG = BcReg.item("anglerfish_spawn_egg", p -> new SpawnEggItem(MarineEntities.ANGLERFISH, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        MORAY_EEL_SPAWN_EGG = BcReg.item("moray_eel_spawn_egg", p -> new SpawnEggItem(MarineEntities.MORAY_EEL, 0xFFFFFF, 0xFFFFFF, p), new Item.Properties(), tab);

        JELLY_BLOB = BcReg.item("jelly_blob", p -> new TooltipItem(p, "tooltip.raft.jelly_blob"), new Item.Properties(), tab);
        GLOW_JELLY = BcReg.item("glow_jelly", p -> new TooltipItem(p, "tooltip.raft.glow_jelly"),
                new Item.Properties().rarity(Rarity.UNCOMMON), tab);
        ANGLER_LURE = BcReg.item("angler_lure", p -> new TooltipItem(p, "tooltip.raft.angler_lure"),
                new Item.Properties().rarity(Rarity.UNCOMMON), tab);
        EEL_FILLET = BcReg.item("eel_fillet", Item::new, new Item.Properties().food(EEL_FILLET_FOOD), tab);
        COOKED_EEL_FILLET = BcReg.item("cooked_eel_fillet", Item::new, new Item.Properties().food(COOKED_EEL_FILLET_FOOD), tab);

        BcTabs.icon(tab, () -> new ItemStack(WHALE_SPAWN_EGG));
    }
}
