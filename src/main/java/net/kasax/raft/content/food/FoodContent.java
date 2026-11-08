package net.kasax.raft.content.food;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.cozy.block.CozyBlock;
import net.kasax.raft.content.food.block.CookingPotBlock;
import net.kasax.raft.content.food.block.CookingPotBlockEntity;
import net.kasax.raft.content.food.block.FishTrapBlock;
import net.kasax.raft.content.food.block.FishTrapBlockEntity;
import net.kasax.raft.content.food.block.IslandCropBlock;
import net.kasax.raft.content.food.block.SeaGrapesBlock;
import net.kasax.raft.content.food.block.WildPlantBlock;
import net.kasax.raft.item.custom.TooltipBlockItem;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.core.Holder;
import net.kasax.raft.compat.ItemProps;
import net.kasax.raft.item.custom.DrinkItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Blocks and items of the "food" module (Beach Craft 3.3): island crops and their wild plants, sea grapes, dishes,
 * drinks and treats, the cooking pot and the fish trap. Hunger and saturation are set against vanilla (bread 5/6,
 * baked potato 5/6, cooked fish 5-6, rabbit stew 10/12).
 */
public final class FoodContent {
    private static final BcTabs.Tab TAB = BcTabs.Tab.KITCHEN;

    private FoodContent() {
    }

    // ------------------------------------------------------------------------------------------------ helpers
    private static Properties crop() {
        return Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    private static Properties wild() {
        return Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                .offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY);
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    /**
     * 1.21.1: food properties carry what the newer game keeps in the consumable component (eating time, effect,
     * the bowl or bottle left over).
     */
    private static FoodProperties food(int nutrition, float saturation, float seconds, Holder<MobEffect> effect, int ticks,
                                       net.minecraft.world.level.ItemLike remainder) {
        FoodProperties.Builder b = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
        if (effect != null) b.effect(new MobEffectInstance(effect, ticks, 0), 1.0F);
        if (remainder != null) b.usingConvertsTo(remainder);
        FoodProperties f = b.build();
        return new FoodProperties(f.nutrition(), f.saturation(), f.canAlwaysEat(), seconds, f.usingConvertsTo(), f.effects());
    }

    private static Item.Properties plain(float compost) {
        return ItemProps.compostable(new Item.Properties(), compost);
    }

    /** A block item named like an item (the newer game's useItemDescriptionPrefix): "Sweet Potato", not the crop. */
    private static Item seed(String name, Block block, Item.Properties properties, String tooltip) {
        return BcReg.item(name, p -> new TooltipBlockItem(block, p, tooltip) {
            @Override
            public String getDescriptionId() {
                return getOrCreateDescriptionId();
            }
        }, properties, TAB);
    }

    private static Item food(String name, Item.Properties properties, String... tooltips) {
        return BcReg.item(name, p -> tooltips.length == 0 ? new Item(p) : new TooltipItem(p, tooltips), properties, TAB);
    }

    private static Item dish(String name, int nutrition, float saturation, Holder<MobEffect> effect, int ticks, String... tooltips) {
        return food(name, new Item.Properties().stacksTo(16).food(food(nutrition, saturation, 1.6F, effect, ticks, Items.BOWL)),
                tooltips);
    }

    // ------------------------------------------------------------------------------------------------ crops
    public static final Block SWEET_POTATOES = BcReg.block("sweet_potatoes",
            p -> new IslandCropBlock(p, () -> FoodContent.SWEET_POTATO, 9), crop());
    public static final Block CHILI_CROP = BcReg.block("chili_crop",
            p -> new IslandCropBlock(p, () -> FoodContent.CHILI_SEEDS, 13), crop());
    public static final Block BANANA_CROP = BcReg.block("banana_crop",
            p -> new IslandCropBlock(p, () -> FoodContent.BANANA_SHOOT, 15), crop());
    public static final Block RICE_CROP = BcReg.block("rice_crop",
            p -> new IslandCropBlock(p, () -> FoodContent.RICE, 13), crop());
    public static final Block SEA_GRAPES_CROP = BcReg.block("sea_grapes_crop", SeaGrapesBlock::new,
            Properties.of().mapColor(MapColor.WATER).noCollission().randomTicks().instabreak().sound(SoundType.WET_GRASS)
                    .pushReaction(PushReaction.DESTROY));

    // ------------------------------------------------------------------------------------------------ seeds and produce
    public static final Item SWEET_POTATO = seed("sweet_potato", SWEET_POTATOES,
            plain(ItemProps.COMPOSTABLE_MEDIUM).food(food(1, 0.3F)), "tooltip.raft.plant_farmland");
    public static final Item BAKED_SWEET_POTATO = food("baked_sweet_potato", plain(ItemProps.COMPOSTABLE_MEDIUM_HIGH).food(food(6, 0.6F)));
    public static final Item CHILI_SEEDS = seed("chili_seeds", CHILI_CROP, plain(ItemProps.COMPOSTABLE_LOW),
            "tooltip.raft.plant_farmland");
    public static final Item CHILI_PEPPER = food("chili_pepper", plain(ItemProps.COMPOSTABLE_LOW_MEDIUM)
            .food(food(2, 0.2F, 1.0F, MobEffects.FIRE_RESISTANCE, 200, null)), "tooltip.raft.chili_pepper");
    public static final Item BANANA_SHOOT = seed("banana_shoot", BANANA_CROP, plain(ItemProps.COMPOSTABLE_LOW_MEDIUM),
            "tooltip.raft.plant_farmland");
    public static final Item BANANA = food("banana", plain(ItemProps.COMPOSTABLE_MEDIUM).food(food(4, 0.3F)));
    public static final Item RICE = seed("rice", RICE_CROP, plain(ItemProps.COMPOSTABLE_LOW), "tooltip.raft.plant_farmland");
    public static final Item COOKED_RICE = food("cooked_rice", new Item.Properties().food(food(4, 0.5F)));
    public static final Item SEA_GRAPES = seed("sea_grapes", SEA_GRAPES_CROP,
            plain(ItemProps.COMPOSTABLE_LOW_MEDIUM).food(food(2, 0.4F, 0.8F, null, 0, null)), "tooltip.raft.sea_grapes");

    // ------------------------------------------------------------------------------------------------ dishes, drinks, treats
    public static final Item SUSHI_ROLL = food("sushi_roll", new Item.Properties().food(food(3, 0.6F, 0.8F, null, 0, null)));
    public static final Item BANANA_BREAD = food("banana_bread", plain(ItemProps.COMPOSTABLE_MEDIUM_HIGH).food(food(7, 0.6F)));
    public static final Item FRUIT_SALAD = dish("fruit_salad", 8, 0.5F, MobEffects.REGENERATION, 160);
    public static final Item CHILI_CRAB = dish("chili_crab", 9, 0.8F, MobEffects.FIRE_RESISTANCE, 1800);
    public static final Item ISLAND_CURRY = dish("island_curry", 10, 0.9F, MobEffects.REGENERATION, 200);
    public static final Item SEAFOOD_CHOWDER = dish("seafood_chowder", 10, 0.8F, MobEffects.WATER_BREATHING, 1200);
    public static final Item SWEET_POTATO_SOUP = dish("sweet_potato_soup", 8, 0.7F, null, 0);
    public static final Item COCONUT_ICE_CREAM = dish("coconut_ice_cream", 6, 0.5F, null, 0);
    public static final Item TROPICAL_SMOOTHIE = BcReg.item("tropical_smoothie", DrinkItem::new, new Item.Properties().stacksTo(16)
            .food(food(6, 0.5F, 1.6F, MobEffects.DIG_SPEED, 1200, Items.GLASS_BOTTLE)), TAB);

    // ------------------------------------------------------------------------------------------------ kitchen
    public static final Block COOKING_POT = BcReg.blockWithItem("cooking_pot", CookingPotBlock::new,
            Properties.of().mapColor(MapColor.METAL).sound(SoundType.LANTERN).strength(2.0F).noOcclusion()
                    .requiresCorrectToolForDrops(), TAB, "tooltip.raft.cooking_pot", "tooltip.raft.cooking_pot.1");
    public static final Block FISH_TRAP = BcReg.blockWithItem("fish_trap",
            CozyBlock.create(CozyBlock.Config.of("fish_trap").facing().water(), FishTrapBlock::new),
            Properties.of().mapColor(MapColor.SAND).sound(SoundType.BAMBOO_WOOD).strength(1.0F).noOcclusion()
                    .ignitedByLava(), TAB, "tooltip.raft.fish_trap", "tooltip.raft.fish_trap.1");
    public static final BlockEntityType<CookingPotBlockEntity> COOKING_POT_ENTITY = BcReg.blockEntity("cooking_pot",
            CookingPotBlockEntity::new, COOKING_POT);
    public static final BlockEntityType<FishTrapBlockEntity> FISH_TRAP_ENTITY = BcReg.blockEntity("fish_trap",
            FishTrapBlockEntity::new, FISH_TRAP);

    // ------------------------------------------------------------------------------------------------ wild plants
    public static final Block WILD_SWEET_POTATO = wildPlant("wild_sweet_potato");
    public static final Block WILD_CHILI = wildPlant("wild_chili");
    public static final Block WILD_BANANA = wildPlant("wild_banana");
    public static final Block WILD_RICE = wildPlant("wild_rice");

    private static Block wildPlant(String name) {
        return BcReg.blockWithItem(name, WildPlantBlock::new, wild(), TAB, "tooltip.raft.wild_plant");
    }

    public static void init() {
        BcTabs.icon(TAB, () -> new ItemStack(ISLAND_CURRY));
    }
}
