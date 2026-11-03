package net.kasax.raft.content.boss;

import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.boss.item.TidebreakerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;

/** Items of the boss module. Everything goes into the STORY tab (the leviathan egg only exists for creative/testing). */
public final class BossItems {
    public static final TagKey<Item> TIDEBREAKER_REPAIR = TagKey.create(Registries.ITEM, Raft.id("tidebreaker_repair_materials"));
    /** Netherite-level spear: 2031 uses, 4 + 4 + 1 = 9 attack damage (see {@link TidebreakerItem}). */
    public static final ToolMaterial TIDEBREAKER_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15,
            TIDEBREAKER_REPAIR);

    public static Item LEVIATHAN_SCALE;
    public static Item TIDEBREAKER;
    public static Item LEVIATHAN_SPAWN_EGG;
    public static Item TIDELING_SPAWN_EGG;

    private BossItems() {
    }

    static void register() {
        BcTabs.Tab tab = BcTabs.Tab.STORY;
        LEVIATHAN_SCALE = BcReg.item("leviathan_scale", Item::new, new Item.Properties().rarity(Rarity.RARE).fireResistant(), tab);
        TIDEBREAKER = BcReg.item("tidebreaker", TidebreakerItem::new, new Item.Properties()
                .sword(TIDEBREAKER_MATERIAL, 4.0F, -2.6F).rarity(Rarity.EPIC).fireResistant(), tab);
        BossBlocks.registerItems();
        LEVIATHAN_SPAWN_EGG = BcReg.item("leviathan_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(BossEntities.LEVIATHAN).rarity(Rarity.EPIC), tab);
        TIDELING_SPAWN_EGG = BcReg.item("tideling_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(BossEntities.TIDELING), tab);
    }
}
