package net.kasax.raft.content.shore;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

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
        CRAB_SPAWN_EGG = BcReg.item("crab_spawn_egg", p -> new SpawnEggItem(ShoreEntities.CRAB, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        EMBER_CRAB_SPAWN_EGG = BcReg.item("ember_crab_spawn_egg", p -> new SpawnEggItem(ShoreEntities.EMBER_CRAB, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        SEAGULL_SPAWN_EGG = BcReg.item("seagull_spawn_egg", p -> new SpawnEggItem(ShoreEntities.SEAGULL, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        PENGUIN_SPAWN_EGG = BcReg.item("penguin_spawn_egg", p -> new SpawnEggItem(ShoreEntities.PENGUIN, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        SEA_OTTER_SPAWN_EGG = BcReg.item("sea_otter_spawn_egg", p -> new SpawnEggItem(ShoreEntities.SEA_OTTER, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);
        DROWNED_CORSAIR_SPAWN_EGG = BcReg.item("drowned_corsair_spawn_egg", p -> new SpawnEggItem(ShoreEntities.DROWNED_CORSAIR, 0xFFFFFF, 0xFFFFFF, p),
                new Item.Properties(), tab);

        CRAB_CLAW = BcReg.item("crab_claw", Item::new, new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()), tab);
        COOKED_CRAB_CLAW = BcReg.item("cooked_crab_claw", Item::new, new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()), tab);
        EMBER_SHELL = BcReg.item("ember_shell", p -> new TooltipItem(p, "tooltip.raft.ember_shell"),
                new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON), tab);
        // Iron blade with a teak grip: 6 attack damage like an iron sword, but quicker (1.8 attacks per second).
        CUTLASS = BcReg.item("cutlass", p -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, p) {
                    @Override
                    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                                                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
                        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.raft.cutlass").withStyle(net.minecraft.ChatFormatting.AQUA));
                    }
                },
                new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.IRON, 3, -2.2F))
                        .rarity(Rarity.UNCOMMON), tab);
    }
}
