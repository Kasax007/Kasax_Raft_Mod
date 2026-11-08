package net.kasax.raft.content.cozy;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.cozy.block.BasketBlock;
import net.kasax.raft.content.cozy.block.BasketBlockEntity;
import net.kasax.raft.content.cozy.block.BenchBlock;
import net.kasax.raft.content.cozy.block.ConchShellBlock;
import net.kasax.raft.content.cozy.block.CozyBlock;
import net.kasax.raft.content.cozy.block.CozyShelfBlock;
import net.kasax.raft.content.cozy.block.CozyShelfBlockEntity;
import net.kasax.raft.content.cozy.block.CozyBlock.Config;
import net.kasax.raft.content.cozy.block.PlateBlock;
import net.kasax.raft.content.cozy.block.ParasolBlock;
import net.kasax.raft.content.cozy.block.PlateBlockEntity;
import net.kasax.raft.content.cozy.block.SteelDrumBlock;
import net.kasax.raft.content.cozy.effect.RestedEffect;
import net.kasax.raft.item.custom.TooltipDoubleBlockItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Blocks of the "cozy" module (Beach Craft 3.3), in the order of the Cozy creative tab: seats and tables, storage,
 * rugs, lights, plants, shells and music, wall decor, the parasol and building blocks.
 */
public final class CozyContent {
    private static final BcTabs.Tab TAB = BcTabs.Tab.COZY;
    private static final List<Block> FLAMMABLE = new ArrayList<>();
    private static final String SIT = "tooltip.raft.sit";
    private static final String COMFORT = "tooltip.raft.comfort";
    private static final String TOGGLE = "tooltip.raft.toggle_light";

    private CozyContent() {
    }

    // ------------------------------------------------------------------------------------------------ sounds, effect
    public static final SoundEvent STEEL_DRUM_NOTE = BcReg.sound("block.steel_drum.note");
    public static final SoundEvent CONCH_HORN = BcReg.sound("block.conch_shell.blow");
    public static final SoundEvent RESTED_CHIME = BcReg.sound("cozy.rested");
    public static final Holder<MobEffect> RESTED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Raft.id("rested"),
            new RestedEffect());

    // ------------------------------------------------------------------------------------------------ helpers
    private static Properties props(MapColor color, SoundType sound, float strength) {
        return Properties.of().mapColor(color).sound(sound).strength(strength).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false);
    }

    private static Properties wood(MapColor color, float strength) {
        return props(color, SoundType.WOOD, strength).ignitedByLava().instrument(NoteBlockInstrument.BASS);
    }

    private static <T extends Block> T add(String name, Function<Properties, T> factory, Properties properties, String... tooltips) {
        return BcReg.blockWithItem(name, factory, properties, TAB, tooltips);
    }

    private static <T extends Block> T flammable(T block) {
        FLAMMABLE.add(block);
        return block;
    }

    private static CozyBlock cozy(String name, Config config, Properties properties, String... tooltips) {
        return add(name, CozyBlock.factory(config), properties, tooltips);
    }

    private static Block tall(String name, Config config, Properties properties, String... tooltips) {
        return tall(name, CozyBlock.factory(config), properties, tooltips);
    }

    private static Block tall(String name, Function<Properties, ? extends CozyBlock> factory, Properties properties, String... tooltips) {
        CozyBlock block = BcReg.block(name, factory, properties);
        BcReg.blockItem(name, (b, p) -> new TooltipDoubleBlockItem(b, p, tooltips), block, new Item.Properties(), TAB);
        return block;
    }

    // ------------------------------------------------------------------------------------------------ seats & tables
    public static final Block PALM_BENCH = flammable(add("palm_bench",
            CozyBlock.create(Config.of("palm_bench").facing().water().seat(0.5), BenchBlock::new),
            wood(MapColor.SAND, 2.0F), SIT, "tooltip.raft.bench"));
    public static final Block TEAK_BENCH = flammable(add("teak_bench",
            CozyBlock.create(Config.of("teak_bench").facing().water().seat(0.5), BenchBlock::new),
            wood(MapColor.COLOR_BROWN, 2.0F), SIT, "tooltip.raft.bench"));
    public static final Block BAR_STOOL = flammable(cozy("bar_stool", Config.of("bar_stool").water().seat(0.87),
            props(MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD, 1.5F).ignitedByLava(), SIT));
    public static final Block SUN_LOUNGER = flammable(cozy("sun_lounger", Config.of("sun_lounger").facing().seat(0.34),
            wood(MapColor.COLOR_LIGHT_BLUE, 1.5F), SIT));
    public static final Block RATTAN_ARMCHAIR = flammable(cozy("rattan_armchair",
            Config.of("rattan_armchair").facing().seat(0.43),
            props(MapColor.SAND, SoundType.BAMBOO_WOOD, 1.5F).ignitedByLava(), SIT));
    public static final Block TEAK_TABLE = flammable(cozy("teak_table", Config.of("teak_table").water(),
            wood(MapColor.COLOR_BROWN, 2.0F)));
    public static final Block BAMBOO_TABLE = flammable(cozy("bamboo_table", Config.of("bamboo_table").water(),
            props(MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD, 2.0F).ignitedByLava()));
    public static final Block SERVING_PLATE = add("serving_plate",
            CozyBlock.create(Config.of("serving_plate").water(), PlateBlock::new),
            props(MapColor.SNOW, SoundType.DECORATED_POT, 0.3F).pushReaction(PushReaction.DESTROY),
            "tooltip.raft.plate", "tooltip.raft.plate.1");

    // ------------------------------------------------------------------------------------------------ storage
    public static final Block PALM_SHELF = shelf("palm_shelf", MapColor.SAND);
    public static final Block TEAK_SHELF = shelf("teak_shelf", MapColor.COLOR_BROWN);
    public static final Block WEATHERED_SHELF = shelf("weathered_shelf", MapColor.COLOR_LIGHT_GRAY);
    public static final Block WICKER_BASKET = flammable(add("wicker_basket",
            CozyBlock.create(Config.of("wicker_basket").facing().openable(), BasketBlock::new),
            props(MapColor.SAND, SoundType.BAMBOO_WOOD, 1.0F).ignitedByLava(), "tooltip.raft.wicker_basket"));

    // ------------------------------------------------------------------------------------------------ rugs
    public static final Block BEACH_TOWEL_RED = rug("beach_towel_red", MapColor.COLOR_RED);
    public static final Block BEACH_TOWEL_BLUE = rug("beach_towel_blue", MapColor.COLOR_BLUE);
    public static final Block BEACH_TOWEL_YELLOW = rug("beach_towel_yellow", MapColor.COLOR_YELLOW);
    public static final Block ROPE_RUG = rug("rope_rug", MapColor.SAND);

    // ------------------------------------------------------------------------------------------------ lights
    public static final Block HURRICANE_LANTERN = cozy("hurricane_lantern",
            Config.of("hurricane_lantern").hangable().water().light(14),
            props(MapColor.GOLD, SoundType.LANTERN, 2.0F).lightLevel(CozyBlock.lightFn(14)).pushReaction(PushReaction.DESTROY),
            TOGGLE);
    public static final Block SHELL_CANDLE = cozy("shell_candle",
            Config.of("shell_candle").light(10).flame(8, 10.5, 8),
            props(MapColor.TERRACOTTA_WHITE, SoundType.CANDLE, 0.3F).lightLevel(CozyBlock.lightFn(10))
                    .pushReaction(PushReaction.DESTROY), TOGGLE);
    public static final Block SEA_GLASS_LAMP = cozy("sea_glass_lamp", Config.of("sea_glass_lamp").water().light(14),
            props(MapColor.COLOR_CYAN, SoundType.GLASS, 1.0F).lightLevel(CozyBlock.lightFn(14)), TOGGLE);
    public static final Block DRIFTWOOD_SCONCE = flammable(cozy("driftwood_sconce",
            Config.of("driftwood_sconce").wall().light(12).flame(8, 12.5, 10),
            props(MapColor.COLOR_LIGHT_GRAY, SoundType.WOOD, 1.0F).lightLevel(CozyBlock.lightFn(12)).ignitedByLava()
                    .noCollission().pushReaction(PushReaction.DESTROY), TOGGLE));

    // ------------------------------------------------------------------------------------------------ plants
    public static final Block POTTED_MONSTERA = cozy("potted_monstera", Config.of("potted_monstera"),
            props(MapColor.TERRACOTTA_ORANGE, SoundType.DECORATED_POT, 0.5F), COMFORT);
    public static final Block COCONUT_PLANTER = cozy("coconut_planter", Config.of("coconut_planter"),
            props(MapColor.COLOR_BROWN, SoundType.WOOD, 0.5F), COMFORT);
    public static final Block FLOWER_BOX = flammable(cozy("flower_box", Config.of("flower_box").facing(),
            wood(MapColor.COLOR_LIGHT_GRAY, 1.0F), COMFORT));
    public static final Block CORAL_VASE = cozy("coral_vase", Config.of("coral_vase").water(),
            props(MapColor.COLOR_CYAN, SoundType.GLASS, 0.5F), COMFORT);

    // ------------------------------------------------------------------------------------------------ shells, music
    public static final Block CONCH_SHELL = add("conch_shell",
            CozyBlock.create(Config.of("conch_shell").facing().water(), ConchShellBlock::new),
            props(MapColor.TERRACOTTA_PINK, SoundType.BONE_BLOCK, 0.5F), "tooltip.raft.conch_shell");
    public static final Block STEEL_DRUM = add("steel_drum",
            CozyBlock.create(Config.of("steel_drum").water(), SteelDrumBlock::new),
            props(MapColor.COLOR_BLUE, SoundType.METAL, 2.0F), "tooltip.raft.steel_drum");
    public static final Block SHELL_GARLAND = cozy("shell_garland", Config.of("shell_garland").wall().water().noCollision(),
            props(MapColor.TERRACOTTA_WHITE, SoundType.BONE_BLOCK, 0.2F).noCollission().pushReaction(PushReaction.DESTROY), COMFORT);
    public static final Block FISHING_NET = flammable(cozy("fishing_net", Config.of("fishing_net").wall().water().noCollision(),
            props(MapColor.SAND, SoundType.WOOL, 0.2F).noCollission().ignitedByLava().pushReaction(PushReaction.DESTROY), COMFORT));
    public static final Block DOCK_BOLLARD = cozy("dock_bollard", Config.of("dock_bollard").water(),
            props(MapColor.COLOR_BLACK, SoundType.METAL, 3.0F));

    // ------------------------------------------------------------------------------------------------ wall decor
    public static final Block SURFBOARD_RED = tall("surfboard_red", Config.of("surfboard").wall().tall().noCollision(),
            props(MapColor.COLOR_RED, SoundType.WOOD, 0.8F).noCollission(), COMFORT);
    public static final Block SURFBOARD_BLUE = tall("surfboard_blue", Config.of("surfboard").wall().tall().noCollision(),
            props(MapColor.COLOR_BLUE, SoundType.WOOD, 0.8F).noCollission(), COMFORT);
    public static final Block SURFBOARD_YELLOW = tall("surfboard_yellow", Config.of("surfboard").wall().tall().noCollision(),
            props(MapColor.COLOR_YELLOW, SoundType.WOOD, 0.8F).noCollission(), COMFORT);
    public static final Block FISH_TROPHY = flammable(cozy("fish_trophy", Config.of("fish_trophy").wall(),
            wood(MapColor.COLOR_BROWN, 1.0F), COMFORT));
    public static final Block BAMBOO_BLINDS = flammable(cozy("bamboo_blinds", Config.of("bamboo_blinds").facing().openable(),
            props(MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD, 0.8F).ignitedByLava(), "tooltip.raft.bamboo_blinds"));
    public static final Block THATCH_PARASOL = flammable(tall("thatch_parasol",
            CozyBlock.create(Config.of("thatch_parasol").tall(), ParasolBlock::new),
            props(MapColor.COLOR_YELLOW, SoundType.GRASS, 1.0F).ignitedByLava().pushReaction(PushReaction.DESTROY),
            "tooltip.raft.parasol"));

    // ------------------------------------------------------------------------------------------------ building
    public static final Block SHELL_TILES = add("shell_tiles", Block::new,
            Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).sound(SoundType.CALCITE).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops());
    public static final Block SHELL_TILE_STAIRS = add("shell_tile_stairs", p -> new StairBlock(SHELL_TILES.defaultBlockState(), p),
            Properties.ofFullCopy(SHELL_TILES));
    public static final Block SHELL_TILE_SLAB = add("shell_tile_slab", SlabBlock::new, Properties.ofFullCopy(SHELL_TILES));
    public static final Block SHELL_TILE_WALL = add("shell_tile_wall", WallBlock::new, Properties.ofFullCopy(SHELL_TILES).forceSolidOn());
    public static final Block PUMICE_BRICKS = add("pumice_bricks", Block::new,
            Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.TUFF_BRICKS).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops());
    public static final Block PUMICE_BRICK_STAIRS = add("pumice_brick_stairs", p -> new StairBlock(PUMICE_BRICKS.defaultBlockState(), p),
            Properties.ofFullCopy(PUMICE_BRICKS));
    public static final Block PUMICE_BRICK_SLAB = add("pumice_brick_slab", SlabBlock::new, Properties.ofFullCopy(PUMICE_BRICKS));
    public static final Block PUMICE_BRICK_WALL = add("pumice_brick_wall", WallBlock::new, Properties.ofFullCopy(PUMICE_BRICKS).forceSolidOn());
    public static final Block DRIFTWOOD_LATTICE = flammable(add("driftwood_lattice", IronBarsBlock::new,
            Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.WOOD).strength(1.0F).noOcclusion().ignitedByLava()));

    // ------------------------------------------------------------------------------------------------ block entities
    public static final BlockEntityType<PlateBlockEntity> PLATE_ENTITY = BcReg.blockEntity("serving_plate",
            PlateBlockEntity::new, SERVING_PLATE);
    public static final BlockEntityType<CozyShelfBlockEntity> SHELF_ENTITY = BcReg.blockEntity("shelf",
            CozyShelfBlockEntity::new, PALM_SHELF, TEAK_SHELF, WEATHERED_SHELF);
    public static final BlockEntityType<BasketBlockEntity> BASKET_ENTITY = BcReg.blockEntity("wicker_basket",
            BasketBlockEntity::new, WICKER_BASKET);

    private static Block shelf(String name, MapColor color) {
        // 1.21.1 port: Beach Craft's own shelf (the newer game's shelf block does not exist), same properties
        return flammable(add(name, CozyShelfBlock::new, Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
                .sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava().strength(2.0F, 3.0F), "tooltip.raft.shelf"));
    }

    private static Block rug(String name, MapColor color) {
        Properties p = Properties.of().mapColor(color).sound(SoundType.WOOL).strength(0.1F).ignitedByLava();
        // the round rope rug shows the floor at its corners: it must not hide the face of the block under it
        if (name.equals("rope_rug")) p = p.noOcclusion();
        return flammable(add(name, CarpetBlock::new, p, COMFORT));
    }

    /** Called from {@link CozyModule#init()}: forces class loading (registration), fire spread and the tab icon. */
    public static void init() {
        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        for (Block block : FLAMMABLE) fire.add(block, 5, 20);
        BcTabs.icon(TAB, () -> new ItemStack(HURRICANE_LANTERN));
    }
}
