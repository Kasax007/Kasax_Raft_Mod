package net.kasax.raft.content.gear;

import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.gear.entity.BoomerangEntity;
import net.kasax.raft.content.gear.item.BoomerangItem;
import net.kasax.raft.content.gear.item.CaptainsSpyglassItem;
import net.kasax.raft.content.gear.item.WreckCompassItem;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * Items of the "gear" module (Beach Craft 3.3), balanced against vanilla: the shark tooth spear is an iron spear
 * with more durability, the machete a fast iron blade that clears plants, the shell armour sits between chainmail
 * and iron (13 armour) with Resistance in water as a set, the explorer set is leather-strong (7) with speed and luck.
 */
public final class GearContent {
    private static final BcTabs.Tab TAB = BcTabs.Tab.GEAR;

    private GearContent() {
    }

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Raft.id(name));
    }

    public static final TagKey<Block> MACHETE_MINEABLE = TagKey.create(Registries.BLOCK, Raft.id("mineable/machete"));
    public static final ToolMaterial SHARK_TOOTH = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 320, 6.0F, 2.0F, 14,
            itemTag("repairs_shark_tooth"));
    public static final ToolMaterial MACHETE_STEEL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 280, 9.0F, 2.0F, 14,
            itemTag("repairs_machete"));

    private static ArmorMaterial material(String name, int durability, int[] defense, int enchantability,
                                          net.minecraft.core.Holder<SoundEvent> sound, float toughness) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Raft.id(name));
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, defense[0], ArmorType.CHESTPLATE, defense[1],
                ArmorType.LEGGINGS, defense[2], ArmorType.BOOTS, defense[3], ArmorType.BODY, defense[1]), enchantability,
                sound, toughness, 0.0F, itemTag("repairs_" + name + "_armor"), asset);
    }

    public static final ArmorMaterial SHELL = material("shell", 18, new int[]{2, 5, 4, 2}, 12,
            SoundEvents.ARMOR_EQUIP_TURTLE, 0.5F);
    public static final ArmorMaterial EXPLORER = material("explorer", 9, new int[]{1, 3, 2, 1}, 15,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F);

    // ------------------------------------------------------------------------------------------------ sounds, entities
    public static final SoundEvent BOOMERANG_THROW = BcReg.sound("boomerang.throw");
    public static final EntityType<BoomerangEntity> BOOMERANG_ENTITY = BcReg.entity("boomerang",
            EntityType.Builder.<BoomerangEntity>of(BoomerangEntity::new, MobCategory.MISC).sized(0.5F, 0.25F)
                    .clientTrackingRange(6).updateInterval(1));

    // ------------------------------------------------------------------------------------------------ weapons
    public static final Item SHARK_TOOTH_SPEAR = BcReg.item("shark_tooth_spear", p -> new TooltipItem(p, "tooltip.raft.shark_tooth_spear"),
            new Item.Properties().spear(SHARK_TOOTH, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F), TAB);
    public static final Item BOOMERANG = BcReg.item("boomerang", BoomerangItem::new,
            new Item.Properties().durability(200).repairable(itemTag("repairs_boomerang")).enchantable(10), TAB);
    public static final Item MACHETE = BcReg.item("machete", p -> new TooltipItem(p, "tooltip.raft.machete"),
            new Item.Properties().tool(MACHETE_STEEL, MACHETE_MINEABLE, 2.5F, -2.0F, 0.0F), TAB);

    // ------------------------------------------------------------------------------------------------ armour
    public static final Item SHELL_HELMET = armour("shell_helmet", SHELL, ArmorType.HELMET, "tooltip.raft.shell_set");
    public static final Item SHELL_CHESTPLATE = armour("shell_chestplate", SHELL, ArmorType.CHESTPLATE, "tooltip.raft.shell_set");
    public static final Item SHELL_LEGGINGS = armour("shell_leggings", SHELL, ArmorType.LEGGINGS, "tooltip.raft.shell_set");
    public static final Item SHELL_BOOTS = armour("shell_boots", SHELL, ArmorType.BOOTS, "tooltip.raft.shell_set");
    public static final Item EXPLORER_HELMET = armour("explorer_helmet", EXPLORER, ArmorType.HELMET, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_CHESTPLATE = armour("explorer_chestplate", EXPLORER, ArmorType.CHESTPLATE, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_LEGGINGS = armour("explorer_leggings", EXPLORER, ArmorType.LEGGINGS, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_BOOTS = armour("explorer_boots", EXPLORER, ArmorType.BOOTS, "tooltip.raft.explorer_set");

    // ------------------------------------------------------------------------------------------------ tools
    public static final Item WRECK_COMPASS = BcReg.item("wreck_compass", WreckCompassItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), TAB);
    public static final Item CAPTAINS_SPYGLASS = BcReg.item("captains_spyglass", CaptainsSpyglassItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), TAB);
    public static final Item FISH_BAIT = BcReg.item("fish_bait", p -> new TooltipItem(p, "tooltip.raft.fish_bait",
            "tooltip.raft.fish_bait.1"), new Item.Properties(), TAB);
    public static final Item GOLDEN_LURE = BcReg.item("golden_lure", p -> new TooltipItem(p, "tooltip.raft.golden_lure"),
            new Item.Properties().rarity(Rarity.UNCOMMON), TAB);

    private static Item armour(String name, ArmorMaterial material, ArmorType type, String tooltip) {
        return BcReg.item(name, p -> new TooltipItem(p, tooltip), new Item.Properties().humanoidArmor(material, type), TAB);
    }

    public static void init() {
        BcTabs.icon(TAB, () -> new ItemStack(BOOMERANG));
    }
}
