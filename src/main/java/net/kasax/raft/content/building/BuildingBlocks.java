package net.kasax.raft.content.building;

import net.kasax.raft.platform.BlockSetTypeBuilder;
import net.kasax.raft.platform.FlammableBlockRegistry;
import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.building.block.AxisShapedBlock;
import net.kasax.raft.content.building.block.CatwalkBlock;
import net.kasax.raft.content.building.block.ControlPanelBlock;
import net.kasax.raft.content.building.block.HorizontalAxisBlock;
import net.kasax.raft.content.building.block.PipeValveBlock;
import net.kasax.raft.content.building.block.ServerRackBlock;
import net.kasax.raft.content.building.block.ShapedFacingBlock;
import net.kasax.raft.content.building.block.WindTurbineBlock;
import net.kasax.raft.content.building.block.WindTurbineBlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.kasax.raft.platform.energy.EnergyStorage;

import java.util.ArrayList;
import java.util.List;
import net.kasax.raft.compat.ItemProps;

/**
 * All blocks of the "building" module (spec section 5): wood, sea glass, coral stone, tidestone, drowned-city
 * concrete and asphalt, ship steel, and the Expedition tech blocks. Everything goes into the BUILDING tab in this order.
 */
public final class BuildingBlocks {
    private static final BcTabs.Tab TAB = BcTabs.Tab.BUILDING;
    private static final List<Block> WOODEN = new ArrayList<>();

    /** Steel doors and hatches of the Expedition: iron sounds, but they open by hand. */
    public static final BlockSetType STEEL = BlockSetTypeBuilder.copyOf(BlockSetType.IRON)
            .openableByHand(true).openableByWindCharge(true).register(Raft.id("steel"));

    // ------------------------------------------------------------------------------------------------- wood
    public static final Block WEATHERED_PLANKS = wood("weathered_planks", planks(MapColor.COLOR_LIGHT_GRAY), ItemProps.COOKING_TIME_WOOD_BLOCKS);
    public static final Block WEATHERED_STAIRS = woodStairs("weathered_stairs", WEATHERED_PLANKS);
    public static final Block WEATHERED_SLAB = woodSlab("weathered_slab", WEATHERED_PLANKS);
    public static final Block WEATHERED_FENCE = woodFence("weathered_fence", WEATHERED_PLANKS);
    public static final Block WEATHERED_FENCE_GATE = woodFenceGate("weathered_fence_gate", WEATHERED_PLANKS, WoodType.OAK);
    public static final Block WEATHERED_DOOR = door("weathered_door", BlockSetType.OAK,
            planks(MapColor.COLOR_LIGHT_GRAY).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY),
            ItemProps.COOKING_TIME_WOOD_ITEMS_LARGE);
    public static final Block WEATHERED_TRAPDOOR = trapdoor("weathered_trapdoor", BlockSetType.OAK,
            planks(MapColor.COLOR_LIGHT_GRAY).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never),
            ItemProps.COOKING_TIME_WOOD_BLOCKS);

    public static final Block TEAK_PLANKS = wood("teak_planks", planks(MapColor.TERRACOTTA_BROWN), ItemProps.COOKING_TIME_WOOD_BLOCKS);
    public static final Block TEAK_STAIRS = woodStairs("teak_stairs", TEAK_PLANKS);
    public static final Block TEAK_SLAB = woodSlab("teak_slab", TEAK_PLANKS);
    public static final Block TEAK_FENCE = woodFence("teak_fence", TEAK_PLANKS);
    public static final Block TEAK_FENCE_GATE = woodFenceGate("teak_fence_gate", TEAK_PLANKS, WoodType.DARK_OAK);
    public static final Block TEAK_DOOR = door("teak_door", BlockSetType.DARK_OAK,
            planks(MapColor.TERRACOTTA_BROWN).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY),
            ItemProps.COOKING_TIME_WOOD_ITEMS_LARGE);
    public static final Block TEAK_TRAPDOOR = trapdoor("teak_trapdoor", BlockSetType.DARK_OAK,
            planks(MapColor.TERRACOTTA_BROWN).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never),
            ItemProps.COOKING_TIME_WOOD_BLOCKS);

    // ------------------------------------------------------------------------------------------------- sea glass
    public static final Block AQUA_SEA_GLASS = block("aqua_sea_glass", TransparentBlock::new, glass(MapColor.COLOR_CYAN));
    public static final Block GREEN_SEA_GLASS = block("green_sea_glass", TransparentBlock::new, glass(MapColor.COLOR_LIGHT_GREEN));
    public static final Block BLUE_SEA_GLASS = block("blue_sea_glass", TransparentBlock::new, glass(MapColor.COLOR_LIGHT_BLUE));
    public static final Block AQUA_SEA_GLASS_PANE = block("aqua_sea_glass_pane", IronBarsBlock::new, glass(MapColor.COLOR_CYAN));
    public static final Block GREEN_SEA_GLASS_PANE = block("green_sea_glass_pane", IronBarsBlock::new, glass(MapColor.COLOR_LIGHT_GREEN));
    public static final Block BLUE_SEA_GLASS_PANE = block("blue_sea_glass_pane", IronBarsBlock::new, glass(MapColor.COLOR_LIGHT_BLUE));

    // ------------------------------------------------------------------------------------------------- coral stone
    public static final Block CORAL_STONE = block("coral_stone", Block::new, stone(MapColor.TERRACOTTA_PINK));
    public static final Block CORAL_STONE_STAIRS = stairs("coral_stone_stairs", CORAL_STONE);
    public static final Block CORAL_STONE_SLAB = slab("coral_stone_slab", CORAL_STONE);
    public static final Block CORAL_STONE_WALL = wall("coral_stone_wall", CORAL_STONE);
    public static final Block POLISHED_CORAL_STONE = block("polished_coral_stone", Block::new, stone(MapColor.TERRACOTTA_PINK));
    public static final Block POLISHED_CORAL_STONE_STAIRS = stairs("polished_coral_stone_stairs", POLISHED_CORAL_STONE);
    public static final Block POLISHED_CORAL_STONE_SLAB = slab("polished_coral_stone_slab", POLISHED_CORAL_STONE);
    public static final Block CORAL_STONE_BRICKS = block("coral_stone_bricks", Block::new, stone(MapColor.TERRACOTTA_PINK));
    public static final Block CORAL_STONE_BRICK_STAIRS = stairs("coral_stone_brick_stairs", CORAL_STONE_BRICKS);
    public static final Block CORAL_STONE_BRICK_SLAB = slab("coral_stone_brick_slab", CORAL_STONE_BRICKS);
    public static final Block CORAL_STONE_BRICK_WALL = wall("coral_stone_brick_wall", CORAL_STONE_BRICKS);
    public static final Block MOSSY_CORAL_STONE_BRICKS = block("mossy_coral_stone_bricks", Block::new, stone(MapColor.TERRACOTTA_PINK));
    public static final Block MOSSY_CORAL_STONE_BRICK_STAIRS = stairs("mossy_coral_stone_brick_stairs", MOSSY_CORAL_STONE_BRICKS);
    public static final Block MOSSY_CORAL_STONE_BRICK_SLAB = slab("mossy_coral_stone_brick_slab", MOSSY_CORAL_STONE_BRICKS);
    public static final Block MOSSY_CORAL_STONE_BRICK_WALL = wall("mossy_coral_stone_brick_wall", MOSSY_CORAL_STONE_BRICKS);

    // ------------------------------------------------------------------------------------------------- tidestone
    public static final Block TIDESTONE = block("tidestone", Block::new, stone(MapColor.COLOR_CYAN));
    public static final Block TIDESTONE_STAIRS = stairs("tidestone_stairs", TIDESTONE);
    public static final Block TIDESTONE_SLAB = slab("tidestone_slab", TIDESTONE);
    public static final Block TIDESTONE_WALL = wall("tidestone_wall", TIDESTONE);
    public static final Block TIDESTONE_BRICKS = block("tidestone_bricks", Block::new, stone(MapColor.COLOR_CYAN));
    public static final Block TIDESTONE_BRICK_STAIRS = stairs("tidestone_brick_stairs", TIDESTONE_BRICKS);
    public static final Block TIDESTONE_BRICK_SLAB = slab("tidestone_brick_slab", TIDESTONE_BRICKS);
    public static final Block TIDESTONE_BRICK_WALL = wall("tidestone_brick_wall", TIDESTONE_BRICKS);
    public static final Block CRACKED_TIDESTONE_BRICKS = block("cracked_tidestone_bricks", Block::new, stone(MapColor.COLOR_CYAN));
    public static final Block MOSSY_TIDESTONE_BRICKS = block("mossy_tidestone_bricks", Block::new, stone(MapColor.COLOR_CYAN));
    public static final Block MOSSY_TIDESTONE_BRICK_STAIRS = stairs("mossy_tidestone_brick_stairs", MOSSY_TIDESTONE_BRICKS);
    public static final Block MOSSY_TIDESTONE_BRICK_SLAB = slab("mossy_tidestone_brick_slab", MOSSY_TIDESTONE_BRICKS);
    public static final Block MOSSY_TIDESTONE_BRICK_WALL = wall("mossy_tidestone_brick_wall", MOSSY_TIDESTONE_BRICKS);
    public static final Block CHISELED_TIDESTONE = block("chiseled_tidestone", Block::new, stone(MapColor.COLOR_CYAN));
    public static final Block TIDESTONE_PILLAR = block("tidestone_pillar", RotatedPillarBlock::new, stone(MapColor.COLOR_CYAN));
    public static final Block GLOWING_TIDESTONE = block("glowing_tidestone", Block::new, stone(MapColor.COLOR_CYAN).lightLevel(s -> 10));

    // ------------------------------------------------------------------------------------------------- drowned city
    public static final Block CRACKED_CONCRETE = block("cracked_concrete", Block::new, concrete(MapColor.COLOR_GRAY));
    public static final Block CRACKED_CONCRETE_STAIRS = stairs("cracked_concrete_stairs", CRACKED_CONCRETE);
    public static final Block CRACKED_CONCRETE_SLAB = slab("cracked_concrete_slab", CRACKED_CONCRETE);
    public static final Block CRACKED_CONCRETE_WALL = wall("cracked_concrete_wall", CRACKED_CONCRETE);
    public static final Block MOSSY_CONCRETE = block("mossy_concrete", Block::new, concrete(MapColor.COLOR_GRAY));
    public static final Block REBAR_CONCRETE = block("rebar_concrete", Block::new, concrete(MapColor.COLOR_GRAY));
    public static final Block ASPHALT = block("asphalt", Block::new, asphalt());
    public static final Block ASPHALT_LINE = block("asphalt_line", HorizontalAxisBlock::new, asphalt());
    public static final Block ASPHALT_SLAB = slab("asphalt_slab", ASPHALT);
    public static final Block ASPHALT_STAIRS = stairs("asphalt_stairs", ASPHALT);
    public static final Block CRACKED_GLASS = block("cracked_glass", TransparentBlock::new, glass(MapColor.NONE));
    public static final Block CRACKED_GLASS_PANE = block("cracked_glass_pane", IronBarsBlock::new, glass(MapColor.NONE));

    // ------------------------------------------------------------------------------------------------- ship steel
    public static final Block RIVETED_HULL = block("riveted_hull", Block::new, metal(MapColor.COLOR_BLUE, SoundType.METAL));
    public static final Block RUSTY_HULL = block("rusty_hull", Block::new, metal(MapColor.TERRACOTTA_ORANGE, SoundType.METAL));
    public static final Block CORRUGATED_METAL = block("corrugated_metal", Block::new, metal(MapColor.METAL, SoundType.COPPER).strength(3.0F, 6.0F));
    public static final Block CORRUGATED_METAL_STAIRS = stairs("corrugated_metal_stairs", CORRUGATED_METAL);
    public static final Block CORRUGATED_METAL_SLAB = slab("corrugated_metal_slab", CORRUGATED_METAL);
    public static final Block RUSTED_PLATE = block("rusted_plate", Block::new, metal(MapColor.TERRACOTTA_ORANGE, SoundType.METAL));
    public static final Block STEEL_BEAM = block("steel_beam", p -> new AxisShapedBlock(p,
            new double[]{1, 0, 0, 15, 16, 3}, new double[]{1, 0, 13, 15, 16, 16}, new double[]{6, 0, 3, 10, 16, 13}),
            metal(MapColor.METAL, SoundType.METAL).strength(5.0F, 6.0F));
    public static final Block CATWALK = block("catwalk", CatwalkBlock::new,
            metal(MapColor.METAL, SoundType.COPPER_GRATE).strength(3.5F, 6.0F).noOcclusion()
                    .isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never).isSuffocating(Blocks::never));
    public static final Block STEEL_RAILING = block("steel_railing", IronBarsBlock::new,
            metal(MapColor.METAL, SoundType.METAL).strength(4.0F, 6.0F).noOcclusion());
    public static final Block PORTHOLE = block("porthole", TransparentBlock::new,
            metal(MapColor.METAL, SoundType.METAL).strength(3.0F, 6.0F).noOcclusion()
                    .isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never).isSuffocating(Blocks::never));
    public static final Block BULKHEAD_DOOR = door("bulkhead_door", STEEL,
            metal(MapColor.METAL, SoundType.METAL).strength(5.0F).noOcclusion().pushReaction(PushReaction.DESTROY), null);
    public static final Block METAL_HATCH = trapdoor("metal_hatch", STEEL,
            metal(MapColor.METAL, SoundType.METAL).strength(5.0F).noOcclusion().isValidSpawn(Blocks::never), null);
    private static final double[][] PIPE = {{4, 0, 4, 12, 16, 12}, {3, 0, 3, 13, 2, 13}, {3, 14, 3, 13, 16, 13}};
    public static final Block RUSTY_PIPE = block("rusty_pipe", p -> new AxisShapedBlock(p, PIPE),
            metal(MapColor.TERRACOTTA_ORANGE, SoundType.METAL).strength(3.0F, 6.0F));
    public static final Block RUSTY_PIPE_VALVE = BcReg.blockWithItem("rusty_pipe_valve", p -> new PipeValveBlock(p,
                    PIPE[0], PIPE[1], PIPE[2], new double[]{3.5, 5, 3.5, 12.5, 11, 12.5}, new double[]{7, 7, 12.5, 9, 9, 14.5},
                    new double[]{4, 4, 14.5, 12, 12, 15.5}), metal(MapColor.TERRACOTTA_ORANGE, SoundType.METAL).strength(3.0F, 6.0F),
            TAB, "tooltip.raft.rusty_pipe_valve.1");
    public static final Block SHIPPING_CONTAINER_RED = block("shipping_container_red", HorizontalAxisBlock::new,
            metal(MapColor.COLOR_RED, SoundType.METAL).strength(4.0F, 6.0F));
    public static final Block SHIPPING_CONTAINER_BLUE = block("shipping_container_blue", HorizontalAxisBlock::new,
            metal(MapColor.COLOR_BLUE, SoundType.METAL).strength(4.0F, 6.0F));
    public static final Block SHIPPING_CONTAINER_GREEN = block("shipping_container_green", HorizontalAxisBlock::new,
            metal(MapColor.COLOR_GREEN, SoundType.METAL).strength(4.0F, 6.0F));

    // ------------------------------------------------------------------------------------------------- Expedition tech
    public static final Block CONTROL_PANEL = block("control_panel", p -> new ControlPanelBlock(p, Shapes.or(
                    box(0, 0, 2, 16, 8, 16), box(0, 8, 2, 16, 9.5, 7), box(0, 8, 7, 16, 11, 11), box(0, 8, 11, 16, 15, 16))),
            metal(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL).strength(3.5F, 6.0F).lightLevel(s -> 4));
    public static final Block SERVER_RACK = block("server_rack", p -> new ServerRackBlock(p, box(1, 0, 1, 15, 16, 15)),
            metal(MapColor.COLOR_GRAY, SoundType.METAL).strength(3.5F, 6.0F).lightLevel(s -> 3));
    public static final Block SATELLITE_DISH = block("satellite_dish", p -> new ShapedFacingBlock(p, Shapes.or(
                    box(3, 0, 3, 13, 2, 13), box(6.5, 2, 6.5, 9.5, 7, 9.5), box(5, 6, 7, 11, 9, 9.5),
                    box(1, 4.5, 3, 15, 15, 14), box(6.5, 12, 2.5, 9.5, 15.5, 5))),
            metal(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL).strength(3.0F, 6.0F).noOcclusion());
    public static final Block WIND_TURBINE = BcReg.blockWithItem("wind_turbine", WindTurbineBlock::new,
            metal(MapColor.SNOW, SoundType.METAL).strength(3.5F, 6.0F).noOcclusion(), TAB,
            "tooltip.raft.wind_turbine.1", "tooltip.raft.wind_turbine.2", "tooltip.raft.wind_turbine.3");

    public static final BlockEntityType<WindTurbineBlockEntity> WIND_TURBINE_BE =
            BcReg.blockEntity("wind_turbine", WindTurbineBlockEntity::new, WIND_TURBINE);

    private BuildingBlocks() {
    }

    /** Called from {@link BuildingModule#init()}: forces the registrations above and wires up APIs. */
    public static void init() {
        EnergyStorage.SIDED.registerForBlockEntity(WindTurbineBlockEntity::getEnergyStorage, WIND_TURBINE_BE);
        net.kasax.raft.util.StoredEnergy.register(WIND_TURBINE, WindTurbineBlockEntity.CAPACITY);
        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
        for (Block block : WOODEN) {
            flammable.add(block, 5, 20);
        }
        BcTabs.icon(TAB, () -> new ItemStack(CHISELED_TIDESTONE));
    }

    // ------------------------------------------------------------------------------------------------- properties
    private static Properties planks(MapColor color) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
                .sound(SoundType.WOOD).ignitedByLava();
    }

    private static Properties glass(MapColor color) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS)
                .noOcclusion().isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never)
                .isSuffocating(Blocks::never);
    }

    private static Properties stone(MapColor color) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F);
    }

    private static Properties concrete(MapColor color) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(1.8F, 6.0F);
    }

    private static Properties asphalt() {
        return Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.TUFF);
    }

    private static Properties metal(MapColor color, SoundType sound) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
                .strength(4.0F, 6.0F).sound(sound);
    }

    private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Block.box(x0, y0, z0, x1, y1, z1);
    }

    // ------------------------------------------------------------------------------------------------- helpers
    private static <T extends Block> T block(String name, java.util.function.Function<Properties, T> factory, Properties properties) {
        return BcReg.blockWithItem(name, factory, properties, TAB);
    }

    private static Block stairs(String name, Block base) {
        return block(name, p -> new StairBlock(base.defaultBlockState(), p), Properties.ofFullCopy(base));
    }

    private static Block slab(String name, Block base) {
        return block(name, SlabBlock::new, Properties.ofFullCopy(base));
    }

    private static Block wall(String name, Block base) {
        return block(name, WallBlock::new, Properties.ofFullCopy(base).forceSolidOn());
    }

    /** A wooden block whose item burns in furnaces; it is also registered as flammable in {@link #init()}. */
    private static <T extends Block> T woodBlock(String name, java.util.function.Function<Properties, T> factory,
                                                 Properties properties, Integer fuel) {
        T block = BcReg.block(name, factory, properties);
        BcReg.blockItem(name, block, ItemProps.cookingFuel(new Item.Properties(), fuel), TAB);
        WOODEN.add(block);
        return block;
    }

    private static Block wood(String name, Properties properties, Integer fuel) {
        return woodBlock(name, Block::new, properties, fuel);
    }

    private static Block woodStairs(String name, Block base) {
        return woodBlock(name, p -> new StairBlock(base.defaultBlockState(), p), Properties.ofFullCopy(base),
                ItemProps.COOKING_TIME_WOOD_BLOCKS);
    }

    private static Block woodSlab(String name, Block base) {
        return woodBlock(name, SlabBlock::new, Properties.ofFullCopy(base), ItemProps.COOKING_TIME_WOOD_SLABS);
    }

    private static Block woodFence(String name, Block base) {
        return woodBlock(name, FenceBlock::new, Properties.ofFullCopy(base).forceSolidOn(),
                ItemProps.COOKING_TIME_WOOD_BLOCKS);
    }

    private static Block woodFenceGate(String name, Block base, WoodType type) {
        return woodBlock(name, p -> new FenceGateBlock(type, p), Properties.ofFullCopy(base).forceSolidOn(),
                ItemProps.COOKING_TIME_WOOD_BLOCKS);
    }

    private static Block door(String name, BlockSetType type, Properties properties, Integer fuel) {
        Block block = BcReg.block(name, p -> new DoorBlock(type, p), properties);
        Item.Properties itemProperties = new Item.Properties();
        if (fuel != null) {
            itemProperties = ItemProps.cookingFuel(itemProperties, fuel);
            WOODEN.add(block);
        }
        BcReg.blockItem(name, DoubleHighBlockItem::new, block, itemProperties, TAB);
        return block;
    }

    private static Block trapdoor(String name, BlockSetType type, Properties properties, Integer fuel) {
        if (fuel != null) {
            return woodBlock(name, p -> new TrapDoorBlock(type, p), properties, fuel);
        }
        return block(name, p -> new TrapDoorBlock(type, p), properties);
    }
}
