package net.kasax.raft.content.shore;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;

/** Items of the shore module: crab food, the ember shell, the corsair cutlass and the spawn eggs. */
public final class ShoreItems {
    public static Item CRAB_CLAW;
    public static Item COOKED_CRAB_CLAW;
    public static Item EMBER_SHELL;
    public static Item CUTLASS;

    public static Item CRAB_SPAWN_EGG;
    public static Item EMBER_CRAB_SPAWN_EGG;
    public static Item SEAGULL_SPAWN_EGG;
    public static Item PENGUIN_SPAWN_EGG;
    public static Item SEA_OTTER_SPAWN_EGG;
    public static Item DROWNED_CORSAIR_SPAWN_EGG;

    private ShoreItems() {
    }

    public static void register() {
        BcTabs.Tab tab = BcTabs.Tab.CREATURES;
        CRAB_SPAWN_EGG = BcReg.item("crab_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.CRAB), tab);
        EMBER_CRAB_SPAWN_EGG = BcReg.item("ember_crab_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.EMBER_CRAB), tab);
        SEAGULL_SPAWN_EGG = BcReg.item("seagull_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.SEAGULL), tab);
        PENGUIN_SPAWN_EGG = BcReg.item("penguin_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.PENGUIN), tab);
        SEA_OTTER_SPAWN_EGG = BcReg.item("sea_otter_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.SEA_OTTER), tab);
        DROWNED_CORSAIR_SPAWN_EGG = BcReg.item("drowned_corsair_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(ShoreEntities.DROWNED_CORSAIR), tab);

        CRAB_CLAW = BcReg.item("crab_claw", Item::new, new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()), tab);
        COOKED_CRAB_CLAW = BcReg.item("cooked_crab_claw", Item::new, new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()), tab);
        EMBER_SHELL = BcReg.item("ember_shell", p -> new TooltipItem(p, "tooltip.raft.ember_shell"),
                new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON), tab);
        // Iron blade with a teak grip: 6 attack damage like an iron sword, but quicker (1.8 attacks per second).
        CUTLASS = BcReg.item("cutlass", p -> new TooltipItem(p, "tooltip.raft.cutlass"),
                new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.2F).rarity(Rarity.UNCOMMON), tab);
    }
}
