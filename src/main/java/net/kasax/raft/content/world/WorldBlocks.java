package net.kasax.raft.content.world;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.world.block.BarnaclesBlock;
import net.kasax.raft.content.world.block.BeachDoublePlantBlock;
import net.kasax.raft.content.world.block.BeachFlowerBlock;
import net.kasax.raft.content.world.block.BeachPlantBlock;
import net.kasax.raft.content.world.block.BeachTallFlowerBlock;
import net.kasax.raft.content.world.block.BubbleVentBlock;
import net.kasax.raft.content.world.block.GiantClamBlock;
import net.kasax.raft.content.world.block.GlowKelpBlock;
import net.kasax.raft.content.world.block.GlowKelpPlantBlock;
import net.kasax.raft.content.world.block.PineappleBushBlock;
import net.kasax.raft.content.world.block.PineappleCropBlock;
import net.kasax.raft.content.world.block.ScatterBlock;
import net.kasax.raft.content.world.block.SeaUrchinBlock;
import net.kasax.raft.content.world.block.StarfishBlock;
import net.kasax.raft.content.world.block.UnderwaterPlantBlock;
import net.kasax.raft.item.custom.TooltipBlockItem;
import net.kasax.raft.item.custom.TooltipDoubleBlockItem;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Natural blocks and items of the raft dimension (docs/design/beachcraft3.md section 4.3), creative tab NATURE.
 * Textures, models, loot tables, recipes and tags come from tools/content/world.
 */
public final class WorldBlocks {
    private static final BcTabs.Tab TAB = BcTabs.Tab.NATURE;

    // ------------------------------------------------------------------------------------------- ground
    public static Block SHELL_SAND;
    public static Block BLACK_SAND;
    public static Block VOLCANIC_ASH;
    public static Block PUMICE;
    public static Block BUBBLE_VENT;
    // ------------------------------------------------------------------------------------------- island plants
    public static Block BEACH_GRASS;
    public static Block SEA_OATS;
    public static Block HIBISCUS;
    public static Block BIRD_OF_PARADISE;
    public static Block TROPICAL_FERN;
    public static Block PINEAPPLE_BUSH;
    public static Block PINEAPPLE_CROP;
    public static Block POTTED_HIBISCUS;
    public static Block POTTED_TROPICAL_FERN;
    public static Block POTTED_PALM_SAPLING;
    // ------------------------------------------------------------------------------------------- sea life
    public static Block GLOW_KELP;
    public static Block GLOW_KELP_PLANT;
    public static Block SEA_ANEMONE_RED;
    public static Block SEA_ANEMONE_PURPLE;
    public static Block SEA_ANEMONE_GLOW;
    public static Block GIANT_CLAM;
    public static Block STARFISH_ORANGE;
    public static Block STARFISH_PURPLE;
    public static Block STARFISH_RED;
    public static Block SEA_URCHIN;
    public static Block BARNACLES;
    public static Block TUBE_WORMS;
    public static Block SEASHELLS;
    public static Block SEA_GLASS_SCATTER;
    // ------------------------------------------------------------------------------------------- items
    public static Item PINEAPPLE;
    public static Item PINEAPPLE_CROWN;
    public static Item PEARL;
    public static Item SEASHELL;
    public static Item SEA_GLASS_SHARD;
    public static Item URCHIN_SPINE;
    public static Item GLOW_KELP_ITEM;

    private WorldBlocks() {
    }

    private static String tip(String id) {
        return "tooltip.raft." + id;
    }

    private static BlockBehaviour.Properties plant(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak().sound(SoundType.GRASS)
                .ignitedByLava().pushReaction(PushReaction.POPPED);
    }

    private static BlockBehaviour.Properties seaLife(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak().sound(sound)
                .pushReaction(PushReaction.POPPED);
    }

    public static void init() {
        // ----------------------------------------------------------------------------------------- ground
        SHELL_SAND = BcReg.blockWithItem("shell_sand", p -> new ColoredFallingBlock(new ColorRGBA(0xE3D2B2), p),
                BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F)
                        .sound(SoundType.SAND), TAB);
        BLACK_SAND = BcReg.blockWithItem("black_sand", p -> new ColoredFallingBlock(new ColorRGBA(0x2F2C34), p),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.SNARE)
                        .strength(0.5F).sound(SoundType.SAND), TAB);
        VOLCANIC_ASH = BcReg.blockWithItem("volcanic_ash", p -> new ColoredFallingBlock(new ColorRGBA(0x6E6964), p),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.SNARE)
                        .strength(0.4F).sound(SoundType.SAND), TAB);
        PUMICE = BcReg.blockWithItem("pumice", Block::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM)
                        .requiresCorrectToolForDrops().strength(1.0F, 3.0F).sound(SoundType.TUFF), TAB);
        BUBBLE_VENT = BcReg.blockWithItem("bubble_vent", BubbleVentBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                        .requiresCorrectToolForDrops().strength(1.25F, 4.2F).sound(SoundType.BASALT).lightLevel(s -> 3),
                TAB, tip("bubble_vent"));

        // ----------------------------------------------------------------------------------------- island plants
        SEA_OATS = BcReg.block("sea_oats", BeachDoublePlantBlock::new,
                plant(MapColor.SAND).replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
        BEACH_GRASS = BcReg.blockWithItem("beach_grass", p -> new BeachPlantBlock(() -> SEA_OATS, p),
                plant(MapColor.SAND).replaceable().offsetType(BlockBehaviour.OffsetType.XYZ), TAB);
        BcReg.blockItem("sea_oats", DoubleHighBlockItem::new, SEA_OATS, new Item.Properties(), TAB);
        TROPICAL_FERN = BcReg.blockWithItem("tropical_fern", p -> new BeachPlantBlock(null, p),
                plant(MapColor.PLANT).replaceable().offsetType(BlockBehaviour.OffsetType.XYZ), TAB);
        HIBISCUS = BcReg.blockWithItem("hibiscus", p -> new BeachFlowerBlock(MobEffects.WATER_BREATHING, 6.0F, p),
                plant(MapColor.COLOR_RED).offsetType(BlockBehaviour.OffsetType.XZ), TAB);
        BIRD_OF_PARADISE = BcReg.block("bird_of_paradise", BeachTallFlowerBlock::new,
                plant(MapColor.COLOR_ORANGE).offsetType(BlockBehaviour.OffsetType.XZ));
        BcReg.blockItem("bird_of_paradise", DoubleHighBlockItem::new, BIRD_OF_PARADISE, new Item.Properties(), TAB);
        PINEAPPLE_BUSH = BcReg.block("pineapple_bush", PineappleBushBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollision().sound(SoundType.GRASS)
                        .ignitedByLava().pushReaction(PushReaction.POPPED));
        BcReg.blockItem("pineapple_bush", (b, p) -> new TooltipBlockItem(b, p, tip("pineapple_bush")), PINEAPPLE_BUSH,
                new Item.Properties(), TAB);
        PINEAPPLE_CROP = BcReg.block("pineapple_crop", PineappleCropBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak()
                        .sound(SoundType.CROP).pushReaction(PushReaction.POPPED));
        POTTED_HIBISCUS = BcReg.block("potted_hibiscus", p -> new FlowerPotBlock(HIBISCUS, p), Blocks.flowerPotProperties());
        POTTED_TROPICAL_FERN = BcReg.block("potted_tropical_fern", p -> new FlowerPotBlock(TROPICAL_FERN, p),
                Blocks.flowerPotProperties());
        POTTED_PALM_SAPLING = BcReg.block("potted_palm_sapling", p -> new FlowerPotBlock(ModBlocks.PALM_SAPLING, p),
                Blocks.flowerPotProperties());

        // ----------------------------------------------------------------------------------------- sea life
        GLOW_KELP = BcReg.block("glow_kelp", GlowKelpBlock::new,
                seaLife(MapColor.WATER, SoundType.WET_GRASS).randomTicks().lightLevel(s -> 8));
        GLOW_KELP_PLANT = BcReg.block("glow_kelp_plant", GlowKelpPlantBlock::new,
                seaLife(MapColor.WATER, SoundType.WET_GRASS).lightLevel(s -> 8));
        GLOW_KELP_ITEM = BcReg.blockItem("glow_kelp", GLOW_KELP, new Item.Properties()
                .compostable(ContextIntProviders.COMPOSTABLE_LOW), TAB);
        SEA_ANEMONE_RED = BcReg.blockWithItem("sea_anemone_red", UnderwaterPlantBlock::new,
                seaLife(MapColor.COLOR_RED, SoundType.WET_GRASS), TAB);
        SEA_ANEMONE_PURPLE = BcReg.blockWithItem("sea_anemone_purple", UnderwaterPlantBlock::new,
                seaLife(MapColor.COLOR_PURPLE, SoundType.WET_GRASS), TAB);
        SEA_ANEMONE_GLOW = BcReg.blockWithItem("sea_anemone_glow",
                p -> new UnderwaterPlantBlock(Block.column(12.0, 0.0, 10.0), ParticleTypes.GLOW, p),
                seaLife(MapColor.COLOR_CYAN, SoundType.WET_GRASS).lightLevel(s -> 10), TAB);
        TUBE_WORMS = BcReg.blockWithItem("tube_worms",
                p -> new UnderwaterPlantBlock(Block.column(12.0, 0.0, 13.0), null, p),
                seaLife(MapColor.WOOL, SoundType.CORAL_BLOCK).lightLevel(s -> 6), TAB);
        GIANT_CLAM = BcReg.blockWithItem("giant_clam", GiantClamBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops()
                        .strength(1.5F, 6.0F).sound(SoundType.BONE_BLOCK).noOcclusion().randomTicks(), TAB, tip("giant_clam"), tip("giant_clam.2"));
        STARFISH_ORANGE = BcReg.blockWithItem("starfish_orange", StarfishBlock::new,
                seaLife(MapColor.COLOR_ORANGE, SoundType.CORAL_BLOCK).noOcclusion(), TAB);
        STARFISH_PURPLE = BcReg.blockWithItem("starfish_purple", StarfishBlock::new,
                seaLife(MapColor.COLOR_PURPLE, SoundType.CORAL_BLOCK).noOcclusion(), TAB);
        STARFISH_RED = BcReg.blockWithItem("starfish_red", StarfishBlock::new,
                seaLife(MapColor.COLOR_RED, SoundType.CORAL_BLOCK).noOcclusion(), TAB);
        SEA_URCHIN = BcReg.blockWithItem("sea_urchin", SeaUrchinBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3F).sound(SoundType.CORAL_BLOCK)
                        .noOcclusion().pushReaction(PushReaction.POPPED), TAB, tip("sea_urchin"));
        BARNACLES = BcReg.blockWithItem("barnacles", BarnaclesBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).noCollision().strength(0.3F)
                        .sound(SoundType.CALCITE).pushReaction(PushReaction.POPPED), TAB);
        SEASHELLS = BcReg.block("seashells", ScatterBlock.Seashells::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.SAND).noCollision().instabreak().sound(SoundType.BONE_BLOCK)
                        .noOcclusion().pushReaction(PushReaction.POPPED));
        SEA_GLASS_SCATTER = BcReg.block("sea_glass_scatter", ScatterBlock.SeaGlass::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().instabreak()
                        .sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED));

        // ----------------------------------------------------------------------------------------- items
        PINEAPPLE = BcReg.item("pineapple", Item::new, new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())
                .compostable(ContextIntProviders.COMPOSTABLE_MEDIUM), TAB);
        PINEAPPLE_CROWN = BcReg.item("pineapple_crown",
                p -> new TooltipBlockItem(PINEAPPLE_CROP, p.useItemDescriptionPrefix(), tip("pineapple_crown")),
                new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_LOW), TAB);
        PEARL = BcReg.item("pearl", p -> new TooltipItem(p, tip("pearl")), new Item.Properties().rarity(Rarity.UNCOMMON), TAB);
        SEASHELL = BcReg.item("seashell", p -> new BlockItem(SEASHELLS, p.useItemDescriptionPrefix()),
                new Item.Properties(), TAB);
        SEA_GLASS_SHARD = BcReg.item("sea_glass_shard", p -> new BlockItem(SEA_GLASS_SCATTER, p.useItemDescriptionPrefix()),
                new Item.Properties(), TAB);
        URCHIN_SPINE = BcReg.item("urchin_spine", p -> new TooltipItem(p, tip("urchin_spine")), new Item.Properties(), TAB);

        BcTabs.icon(TAB, () -> new ItemStack(HIBISCUS));
    }
}
