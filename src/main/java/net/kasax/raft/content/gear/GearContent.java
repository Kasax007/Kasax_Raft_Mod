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
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.kasax.raft.compat.GearItem;
import net.kasax.raft.content.gear.item.SharkToothSpearItem;
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
    /** 1.21.1: a tool material (the newer game's ToolMaterial record) as a tier. */
    private static Tier tier(int uses, float speed, float damage, int enchantability, String repairTag) {
        return new Tier() {
            @Override public int getUses() { return uses; }
            @Override public float getSpeed() { return speed; }
            @Override public float getAttackDamageBonus() { return damage; }
            @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
            @Override public int getEnchantmentValue() { return enchantability; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(itemTag(repairTag)); }
        };
    }

    public static final Tier SHARK_TOOTH = tier(320, 6.0F, 2.0F, 14, "repairs_shark_tooth");
    public static final Tier MACHETE_STEEL = tier(280, 9.0F, 2.0F, 14, "repairs_machete");

    /** An armour material with its texture layers textures/models/armor/&lt;name&gt;_layer_1/2.png (registered). */
    private static Holder<ArmorMaterial> material(String name, int durability, int[] defense, int enchantability,
                                                  Holder<SoundEvent> sound, float toughness) {
        Map<Type, Integer> d = new java.util.EnumMap<>(Map.of(Type.HELMET, defense[0], Type.CHESTPLATE, defense[1],
                Type.LEGGINGS, defense[2], Type.BOOTS, defense[3], Type.BODY, defense[1]));
        TagKey<Item> repair = itemTag("repairs_" + name + "_armor");
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Raft.id(name), new ArmorMaterial(d, enchantability,
                sound, () -> Ingredient.of(repair), java.util.List.of(new ArmorMaterial.Layer(Raft.id(name))), toughness, 0.0F));
    }

    /** Durability factors of the armour sets (times 11/16/15/13 for helmet/chestplate/leggings/boots). */
    private static final Map<Holder<ArmorMaterial>, Integer> DURABILITY = new java.util.HashMap<>();
    public static final Holder<ArmorMaterial> SHELL = durable(material("shell", 18, new int[]{2, 5, 4, 2}, 12,
            SoundEvents.ARMOR_EQUIP_TURTLE, 0.5F), 18);
    public static final Holder<ArmorMaterial> EXPLORER = durable(material("explorer", 9, new int[]{1, 3, 2, 1}, 15,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F), 9);

    private static Holder<ArmorMaterial> durable(Holder<ArmorMaterial> material, int factor) {
        DURABILITY.put(material, factor);
        return material;
    }

    // ------------------------------------------------------------------------------------------------ sounds, entities
    public static final SoundEvent BOOMERANG_THROW = BcReg.sound("boomerang.throw");
    public static final EntityType<BoomerangEntity> BOOMERANG_ENTITY = BcReg.entity("boomerang",
            EntityType.Builder.<BoomerangEntity>of(BoomerangEntity::new, MobCategory.MISC).sized(0.5F, 0.25F)
                    .clientTrackingRange(6).updateInterval(1));

    // ------------------------------------------------------------------------------------------------ weapons
    // 1.21.1: the newer game's spear mechanics do not exist, the spear is its own weapon (see SharkToothSpearItem)
    public static final Item SHARK_TOOTH_SPEAR = BcReg.item("shark_tooth_spear", p -> new SharkToothSpearItem(SHARK_TOOTH, p),
            new Item.Properties(), TAB);
    public static final Item BOOMERANG = BcReg.item("boomerang", BoomerangItem::new,
            new Item.Properties().durability(200), TAB);
    public static final Item MACHETE = BcReg.item("machete", p -> new DiggerItem(MACHETE_STEEL, MACHETE_MINEABLE, p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    lines.add(net.minecraft.network.chat.Component.translatable("tooltip.raft.machete").withStyle(net.minecraft.ChatFormatting.AQUA));
                    super.appendHoverText(stack, context, lines, flag);
                }
            }, new Item.Properties().attributes(DiggerItem.createAttributes(MACHETE_STEEL, 2.5F, -2.0F)), TAB);

    // ------------------------------------------------------------------------------------------------ armour
    public static final Item SHELL_HELMET = armour("shell_helmet", SHELL, Type.HELMET, "tooltip.raft.shell_set");
    public static final Item SHELL_CHESTPLATE = armour("shell_chestplate", SHELL, Type.CHESTPLATE, "tooltip.raft.shell_set");
    public static final Item SHELL_LEGGINGS = armour("shell_leggings", SHELL, Type.LEGGINGS, "tooltip.raft.shell_set");
    public static final Item SHELL_BOOTS = armour("shell_boots", SHELL, Type.BOOTS, "tooltip.raft.shell_set");
    public static final Item EXPLORER_HELMET = armour("explorer_helmet", EXPLORER, Type.HELMET, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_CHESTPLATE = armour("explorer_chestplate", EXPLORER, Type.CHESTPLATE, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_LEGGINGS = armour("explorer_leggings", EXPLORER, Type.LEGGINGS, "tooltip.raft.explorer_set");
    public static final Item EXPLORER_BOOTS = armour("explorer_boots", EXPLORER, Type.BOOTS, "tooltip.raft.explorer_set");

    // ------------------------------------------------------------------------------------------------ tools
    public static final Item WRECK_COMPASS = BcReg.item("wreck_compass", WreckCompassItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), TAB);
    public static final Item CAPTAINS_SPYGLASS = BcReg.item("captains_spyglass", CaptainsSpyglassItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), TAB);
    public static final Item FISH_BAIT = BcReg.item("fish_bait", p -> new TooltipItem(p, "tooltip.raft.fish_bait",
            "tooltip.raft.fish_bait.1"), new Item.Properties(), TAB);
    public static final Item GOLDEN_LURE = BcReg.item("golden_lure", p -> new TooltipItem(p, "tooltip.raft.golden_lure"),
            new Item.Properties().rarity(Rarity.UNCOMMON), TAB);

    private static Item armour(String name, Holder<ArmorMaterial> material, Type type, String tooltip) {
        Item.Properties p = GearItem.gear(new Item.Properties().durability(type.getDurability(DURABILITY.get(material))),
                new GearItem.Gear(material, type));
        return BcReg.item(name, q -> new GearItem(q) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
                                        net.minecraft.world.item.TooltipFlag flag) {
                lines.add(net.minecraft.network.chat.Component.translatable(tooltip).withStyle(net.minecraft.ChatFormatting.AQUA));
                super.appendHoverText(stack, context, lines, flag);
            }
        }, p, TAB);
    }

    public static void init() {
        BcTabs.icon(TAB, () -> new ItemStack(BOOMERANG));
    }
}
