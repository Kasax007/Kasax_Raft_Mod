package net.kasax.raft.content.decor;

import net.kasax.raft.platform.FlammableBlockRegistry;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.decor.block.BrazierBlock;
import net.kasax.raft.content.decor.block.BuoyBlock;
import net.kasax.raft.content.decor.block.CoinPileBlock;
import net.kasax.raft.content.decor.block.DecorBlock;
import net.kasax.raft.content.decor.block.DecorBlock.Config;
import net.kasax.raft.content.decor.block.DecorChestBlock;
import net.kasax.raft.content.decor.block.DecorChestBlockEntity;
import net.kasax.raft.content.decor.block.DecorLanternBlock;
import net.kasax.raft.content.decor.block.HammockBlock;
import net.kasax.raft.content.decor.block.NeonTubeBlock;
import net.kasax.raft.content.decor.block.RumBottlesBlock;
import net.kasax.raft.content.decor.block.ShipBellBlock;
import net.kasax.raft.content.decor.block.ShipCannonBlock;
import net.kasax.raft.content.decor.block.TallDecorBlock;
import net.kasax.raft.content.decor.block.TikiTorchBlock;
import net.kasax.raft.content.decor.block.WindChimeBlock;
import net.kasax.raft.content.decor.item.BuoyItem;
import net.kasax.raft.item.custom.TooltipDoubleBlockItem;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * All blocks and items of the "decor" module (spec section 6), registered in the order of the DECOR creative tab:
 * lights, sea and pirate things, then furniture.
 */
public final class DecorBlocks {
    private static final BcTabs.Tab TAB = BcTabs.Tab.DECOR;
    private static final List<Block> FLAMMABLE = new ArrayList<>();
    private static final String SIT = "tooltip.raft.sit";

    private DecorBlocks() {
    }

    // ------------------------------------------------------------------------------------------------- properties
    private static Properties props(MapColor color, SoundType sound, float strength) {
        return Properties.of().mapColor(color).sound(sound).strength(strength).noOcclusion()
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false);
    }

    private static Properties light(Properties p, int level) {
        return p.lightLevel(s -> level);
    }

    private static <T extends Block> T add(String name, Function<Properties, T> factory, Properties properties, String... tooltips) {
        return BcReg.blockWithItem(name, factory, properties, TAB, tooltips);
    }

    private static <T extends Block> T flammable(T block) {
        FLAMMABLE.add(block);
        return block;
    }

    private static DecorBlock decor(String name, Config config, Properties properties, String... tooltips) {
        return add(name, DecorBlock.factory(config), properties, tooltips);
    }

    // ------------------------------------------------------------------------------------------------- lights
    public static final Block STRING_LIGHTS_WARM = decor("string_lights_warm",
            Config.of("string_lights_warm").wall().water().noCollision(),
            light(props(MapColor.COLOR_YELLOW, SoundType.CHAIN, 0.2F).noCollision().pushReaction(PushReaction.POPPED), 12));
    public static final Block STRING_LIGHTS_COLORFUL = decor("string_lights_colorful",
            Config.of("string_lights_colorful").wall().water().noCollision(),
            light(props(MapColor.COLOR_LIGHT_BLUE, SoundType.CHAIN, 0.2F).noCollision().pushReaction(PushReaction.POPPED), 12));
    public static final Block BUNTING_RED_WHITE = flammable(decor("bunting_red_white",
            Config.of("bunting_red_white").wall().water().noCollision(),
            props(MapColor.COLOR_RED, SoundType.WOOL, 0.2F).noCollision().pushReaction(PushReaction.POPPED).ignitedByLava()));
    public static final Block BUNTING_RAINBOW = flammable(decor("bunting_rainbow",
            Config.of("bunting_rainbow").wall().water().noCollision(),
            props(MapColor.COLOR_YELLOW, SoundType.WOOL, 0.2F).noCollision().pushReaction(PushReaction.POPPED).ignitedByLava()));

    public static final Block PAPER_LANTERN_WHITE = paperLantern("white", MapColor.SNOW);
    public static final Block PAPER_LANTERN_RED = paperLantern("red", MapColor.COLOR_RED);
    public static final Block PAPER_LANTERN_ORANGE = paperLantern("orange", MapColor.COLOR_ORANGE);
    public static final Block PAPER_LANTERN_YELLOW = paperLantern("yellow", MapColor.COLOR_YELLOW);
    public static final Block PAPER_LANTERN_LIME = paperLantern("lime", MapColor.COLOR_LIGHT_GREEN);
    public static final Block PAPER_LANTERN_CYAN = paperLantern("cyan", MapColor.COLOR_CYAN);
    public static final Block PAPER_LANTERN_BLUE = paperLantern("blue", MapColor.COLOR_BLUE);
    public static final Block PAPER_LANTERN_PURPLE = paperLantern("purple", MapColor.COLOR_PURPLE);
    public static final Block PAPER_LANTERN_PINK = paperLantern("pink", MapColor.COLOR_PINK);

    public static final Block SHIP_LANTERN = add("ship_lantern", p -> new DecorLanternBlock(p, "ship_lantern"),
            light(props(MapColor.GOLD, SoundType.LANTERN, 3.0F).pushReaction(PushReaction.POPPED), 15));
    public static final Block JELLY_LAMP = add("jelly_lamp", p -> new DecorLanternBlock(p, "jelly_lamp"),
            light(props(MapColor.COLOR_CYAN, SoundType.GLASS, 0.5F).pushReaction(PushReaction.POPPED), 15));
    public static final Block TIKI_TORCH = registerTikiTorch();
    public static final Block BRAZIER = add("brazier", DecorBlock.create(Config.of("brazier").water(), BrazierBlock::new),
            props(MapColor.METAL, SoundType.LANTERN, 3.0F).lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 15 : 0),
            "tooltip.raft.brazier");
    public static final Block DRIFTWOOD_LAMP = flammable(decor("driftwood_lamp", Config.of("driftwood_lamp").water(),
            light(props(MapColor.SAND, SoundType.WOOD, 1.0F).ignitedByLava(), 13)));
    public static final Block SEASHELL_LAMP = decor("seashell_lamp", Config.of("seashell_lamp").water(),
            light(props(MapColor.TERRACOTTA_PINK, SoundType.BONE_BLOCK, 1.0F), 12));
    public static final Block NEON_TUBE_CYAN = add("neon_tube_cyan", p -> new NeonTubeBlock(p, "neon_tube_cyan"),
            light(props(MapColor.COLOR_CYAN, SoundType.GLASS, 0.3F), 12));
    public static final Block NEON_TUBE_PINK = add("neon_tube_pink", p -> new NeonTubeBlock(p, "neon_tube_pink"),
            light(props(MapColor.COLOR_PINK, SoundType.GLASS, 0.3F), 12));
    public static final Block BUOY_RED = buoy("buoy_red", MapColor.COLOR_RED);
    public static final Block BUOY_GREEN = buoy("buoy_green", MapColor.COLOR_GREEN);
    public static final Block LIGHTHOUSE_LENS = decor("lighthouse_lens", Config.of("lighthouse_lens"),
            light(props(MapColor.GOLD, SoundType.GLASS, 1.5F), 15));

    // ------------------------------------------------------------------------------------------------- ship and pirates
    public static final Block SHIP_CANNON = add("ship_cannon",
            DecorBlock.create(Config.of("ship_cannon").facing().water(), ShipCannonBlock::new),
            props(MapColor.COLOR_BLACK, SoundType.METAL, 5.0F).pushReaction(PushReaction.IMMOVEABLE), "tooltip.raft.ship_cannon");
    public static final Item CANNONBALL = BcReg.item("cannonball", Item::new, new Item.Properties().stacksTo(16), TAB);
    public static final Block SHIP_ANCHOR = decor("ship_anchor", Config.of("ship_anchor").facing().water(),
            props(MapColor.COLOR_GRAY, SoundType.METAL, 5.0F));
    public static final Block TREASURE_CHEST = add("treasure_chest",
            DecorBlock.create(Config.of("treasure_chest").facing().water(),
                    (p, c) -> new DecorChestBlock(p, c, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)),
            props(MapColor.COLOR_BROWN, SoundType.WOOD, 2.5F).ignitedByLava().instrument(NoteBlockInstrument.BASS));
    public static final Item GOLD_COIN = BcReg.item("gold_coin", p -> new TooltipItem(p, "tooltip.raft.gold_coin"),
            new Item.Properties(), TAB);
    public static final Block GOLD_COIN_PILE = add("gold_coin_pile",
            DecorBlock.create(Config.of("gold_coin_pile_1").water(), CoinPileBlock::new),
            Properties.of().mapColor(MapColor.GOLD).sound(SoundType.CHAIN).strength(0.6F).pushReaction(PushReaction.POPPED));
    public static final Block JOLLY_ROGER = flammable(decor("jolly_roger", Config.of("jolly_roger").wall().water().noCollision(),
            props(MapColor.COLOR_BLACK, SoundType.WOOL, 0.5F).noCollision().pushReaction(PushReaction.POPPED).ignitedByLava()));
    public static final Block WANTED_POSTER = flammable(decor("wanted_poster", Config.of("wanted_poster").wall().noCollision(),
            props(MapColor.SAND, SoundType.WOOL, 0.2F).noCollision().pushReaction(PushReaction.POPPED).ignitedByLava()));
    public static final Block RUM_BOTTLES = add("rum_bottles",
            DecorBlock.create(Config.of("rum_bottles_1").facing().water(), RumBottlesBlock::new),
            props(MapColor.COLOR_GREEN, SoundType.GLASS, 0.3F).pushReaction(PushReaction.POPPED));
    public static final Block POWDER_KEG = flammable(decor("powder_keg", Config.of("powder_keg").water(),
            props(MapColor.COLOR_BROWN, SoundType.WOOD, 1.5F).ignitedByLava().instrument(NoteBlockInstrument.BASS)));
    public static final Block WOODEN_CRATE = flammable(add("wooden_crate",
            DecorBlock.create(Config.of("wooden_crate").facing(),
                    (p, c) -> new DecorChestBlock(p, c, SoundEvents.BARREL_OPEN, SoundEvents.BARREL_CLOSE)),
            Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F).ignitedByLava()
                    .instrument(NoteBlockInstrument.BASS)));
    public static final Block SHIP_BELL = add("ship_bell", DecorBlock.create(Config.of("ship_bell").wall().water(), ShipBellBlock::new),
            props(MapColor.GOLD, SoundType.METAL, 3.0F), "tooltip.raft.ship_bell");
    public static final Block CHART_TABLE = flammable(decor("chart_table", Config.of("chart_table").facing(),
            props(MapColor.COLOR_BROWN, SoundType.WOOD, 2.0F).ignitedByLava()));
    public static final Block SPYGLASS_STAND = decor("spyglass_stand", Config.of("spyglass_stand").facing().water(),
            props(MapColor.GOLD, SoundType.WOOD, 1.0F).pushReaction(PushReaction.POPPED));
    public static final Block FIGUREHEAD = flammable(decor("figurehead", Config.of("figurehead").wall().water(),
            props(MapColor.COLOR_BROWN, SoundType.WOOD, 1.5F).ignitedByLava()));
    public static final Block LIFE_RING = decor("life_ring", Config.of("life_ring").wall().water(),
            props(MapColor.COLOR_RED, SoundType.WOOL, 0.5F).pushReaction(PushReaction.POPPED));
    public static final Block ROPE_RAILING = flammable(add("rope_railing", FenceBlock::new,
            Properties.of().mapColor(MapColor.SAND).sound(SoundType.WOOD).strength(1.5F, 3.0F).ignitedByLava()
                    .instrument(NoteBlockInstrument.BASS).forceSolidOn()));
    public static final Block CARGO_NET = flammable(decor("cargo_net", Config.of("cargo_net").wall().water(),
            props(MapColor.SAND, SoundType.LADDER, 0.4F).pushReaction(PushReaction.POPPED).ignitedByLava()));

    // ------------------------------------------------------------------------------------------------- furniture
    public static final Block HAMMOCK = registerHammock();
    public static final Block DECK_CHAIR = flammable(decor("deck_chair", Config.of("deck_chair").facing().seat(0.3),
            props(MapColor.COLOR_BLUE, SoundType.WOOD, 1.0F).ignitedByLava(), SIT));
    public static final Block BEACH_UMBRELLA_RED = umbrella("red", MapColor.COLOR_RED);
    public static final Block BEACH_UMBRELLA_BLUE = umbrella("blue", MapColor.COLOR_BLUE);
    public static final Block BEACH_UMBRELLA_YELLOW = umbrella("yellow", MapColor.COLOR_YELLOW);
    public static final Block CUSHION_WHITE = cushion("white", MapColor.SNOW);
    public static final Block CUSHION_RED = cushion("red", MapColor.COLOR_RED);
    public static final Block CUSHION_BLUE = cushion("blue", MapColor.COLOR_BLUE);
    public static final Block CUSHION_YELLOW = cushion("yellow", MapColor.COLOR_YELLOW);
    public static final Block CUSHION_GREEN = cushion("green", MapColor.COLOR_GREEN);
    public static final Block WOVEN_RUG = flammable(add("woven_rug", CarpetBlock::new,
            Properties.of().mapColor(MapColor.SAND).sound(SoundType.WOOL).strength(0.1F).ignitedByLava()));
    public static final Block DRIFTWOOD_TABLE = table("driftwood_table", MapColor.COLOR_LIGHT_GRAY);
    public static final Block PALM_TABLE = table("palm_table", MapColor.SAND);
    public static final Block DRIFTWOOD_CHAIR = chair("driftwood_chair", MapColor.COLOR_LIGHT_GRAY);
    public static final Block PALM_CHAIR = chair("palm_chair", MapColor.SAND);
    public static final Block DRIFTWOOD_SHELF = flammable(decor("driftwood_shelf", Config.of("driftwood_shelf").wall(),
            props(MapColor.COLOR_LIGHT_GRAY, SoundType.WOOD, 1.5F).ignitedByLava()));
    public static final Block WIND_CHIME = add("wind_chime",
            DecorBlock.create(Config.of("wind_chime").ceiling().water().noCollision(), WindChimeBlock::new),
            props(MapColor.SAND, SoundType.BAMBOO_WOOD, 0.5F).noCollision().pushReaction(PushReaction.POPPED),
            "tooltip.raft.wind_chime");
    public static final Block AQUARIUM = decor("aquarium", Config.of("aquarium"),
            light(props(MapColor.WATER, SoundType.GLASS, 1.0F), 6));
    public static final Block HANGING_PLANTER = flammable(decor("hanging_planter", Config.of("hanging_planter").ceiling(),
            props(MapColor.PLANT, SoundType.AZALEA, 0.5F).pushReaction(PushReaction.POPPED).ignitedByLava()));

    /** Block entity types named like their blocks, so structure NBT ({@code "id": "raft:treasure_chest"}) loads. */
    public static final BlockEntityType<DecorChestBlockEntity> TREASURE_CHEST_ENTITY = BcReg.blockEntity("treasure_chest",
            (pos, state) -> new DecorChestBlockEntity(DecorBlocks.TREASURE_CHEST_ENTITY, pos, state), TREASURE_CHEST);
    public static final BlockEntityType<DecorChestBlockEntity> WOODEN_CRATE_ENTITY = BcReg.blockEntity("wooden_crate",
            (pos, state) -> new DecorChestBlockEntity(DecorBlocks.WOODEN_CRATE_ENTITY, pos, state), WOODEN_CRATE);

    // ------------------------------------------------------------------------------------------------- helpers
    private static Block paperLantern(String colour, MapColor mapColor) {
        String name = "paper_lantern_" + colour;
        return flammable(add(name, p -> new DecorLanternBlock(p, name),
                light(props(mapColor, SoundType.WOOL, 0.3F).pushReaction(PushReaction.POPPED).ignitedByLava(), 14)));
    }

    private static Block buoy(String name, MapColor color) {
        Block block = BcReg.block(name, DecorBlock.create(Config.of(name).water(), BuoyBlock::new),
                light(props(color, SoundType.METAL, 1.5F), 10));
        BcReg.blockItem(name, (b, p) -> new BuoyItem(b, p), block, new Item.Properties(), TAB);
        return block;
    }

    private static Block registerTikiTorch() {
        Block block = BcReg.block("tiki_torch", TikiTorchBlock::new,
                props(MapColor.SAND, SoundType.BAMBOO, 1.0F).lightLevel(TikiTorchBlock::light).pushReaction(PushReaction.POPPED)
                        .ignitedByLava());
        BcReg.blockItem("tiki_torch", (b, p) -> new TooltipDoubleBlockItem(b, p, "tooltip.raft.tiki_torch"), block,
                new Item.Properties(), TAB);
        return block;
    }

    private static Block umbrella(String colour, MapColor color) {
        String name = "beach_umbrella_" + colour;
        Block block = BcReg.block(name, p -> new TallDecorBlock(p, name, true),
                props(color, SoundType.WOOL, 1.0F).pushReaction(PushReaction.POPPED).ignitedByLava());
        BcReg.blockItem(name, (b, p) -> new TooltipDoubleBlockItem(b, p), block, new Item.Properties(), TAB);
        return flammable(block);
    }

    private static Block registerHammock() {
        Block block = BcReg.block("hammock", HammockBlock::new,
                props(MapColor.COLOR_ORANGE, SoundType.WOOL, 0.5F).pushReaction(PushReaction.POPPED).ignitedByLava());
        BcReg.blockItem("hammock", (b, p) -> new HammockItem(b, p), block, new Item.Properties().stacksTo(1), TAB);
        return flammable(block);
    }

    private static Block cushion(String colour, MapColor color) {
        String name = "cushion_" + colour;
        return flammable(decor(name, Config.of(name).seat(0.25),
                props(color, SoundType.WOOL, 0.4F).ignitedByLava(), SIT));
    }

    private static Block table(String name, MapColor color) {
        return flammable(decor(name, Config.of(name).water(),
                props(color, SoundType.WOOD, 2.0F).ignitedByLava().instrument(NoteBlockInstrument.BASS)));
    }

    private static Block chair(String name, MapColor color) {
        return flammable(decor(name, Config.of(name).facing().water().seat(0.55),
                props(color, SoundType.WOOD, 2.0F).ignitedByLava().instrument(NoteBlockInstrument.BASS), SIT));
    }

    /** The hammock item: a bed item with tooltip lines. */
    static final class HammockItem extends net.minecraft.world.item.BlockItem {
        HammockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<net.minecraft.network.chat.Component> builder,
                                    net.minecraft.world.item.TooltipFlag flag) {
            builder.accept(net.minecraft.network.chat.Component.translatable("tooltip.raft.hammock")
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
            builder.accept(net.minecraft.network.chat.Component.translatable("tooltip.raft.hammock.1")
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
            super.appendHoverText(stack, context, display, builder, flag);
        }
    }

    /** Called from {@link DecorModule#init()}: forces class loading (registration) and sets fire spread and tab icon. */
    public static void init() {
        FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
        for (Block block : FLAMMABLE) fire.add(block, 5, 20);
        BcTabs.icon(TAB, () -> new ItemStack(PAPER_LANTERN_RED));
    }
}
