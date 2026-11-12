package net.kasax.raft.gametest.neo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kasax.raft.Raft;
import net.kasax.raft.block.entity.MakeshiftSolarPanelBlockEntity;
import net.kasax.raft.content.guide.GuideContent;
import net.kasax.raft.content.npc.NpcContent;
import net.kasax.raft.platform.BlockTransformerHelper;
import net.kasax.raft.platform.FlammableBlockRegistry;
import net.kasax.raft.platform.energy.EnergyStorage;
import net.kasax.raft.platform.transfer.ItemStorage;
import net.kasax.raft.platform.transfer.ItemVariant;
import net.kasax.raft.platform.transfer.Storage;
import net.kasax.raft.platform.transfer.Transaction;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * NeoForge game tests of Beach Craft's loader layer (26.3 registry-based tests): energy and item capabilities on
 * NeoForge's transfer API, the cable network inside NeoForge transactions, flammability and stripping data maps, entity
 * attributes, the biome modifier, attachments, extended menus. The scripted in-game tests (tools/bctest) cover gameplay.
 */
public final class BcGameTests {
    private static final Map<String, TestCase> TESTS = new LinkedHashMap<>();

    private record TestCase(Consumer<GameTestHelper> function, int maxTicks) {
    }

    static {
        TESTS.put("battery_energy_capability", new TestCase(BcGameTests::batteryEnergyCapability, 100));
        TESTS.put("cable_moves_energy", new TestCase(BcGameTests::cableMovesEnergy, 200));
        TESTS.put("foreign_energy_transactions", new TestCase(BcGameTests::foreignEnergyTransactions, 100));
        TESTS.put("item_capabilities", new TestCase(BcGameTests::itemCapabilities, 100));
        TESTS.put("flammable_and_strippable", new TestCase(BcGameTests::flammableAndStrippable, 100));
        TESTS.put("every_entity_type", new TestCase(BcGameTests::everyEntityType, 100));
        TESTS.put("biome_modifier", new TestCase(BcGameTests::biomeModifier, 100));
        TESTS.put("attachments", new TestCase(BcGameTests::attachments, 100));
        TESTS.put("extended_menu_data", new TestCase(BcGameTests::extendedMenuData, 100));
    }

    private BcGameTests() {
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("raft_gametest", name);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, e -> e.register(Registries.TEST_FUNCTION, helper ->
                TESTS.forEach((name, test) -> helper.register(id(name), test.function()))));
        modBus.addListener(RegisterGameTestsEvent.class, e -> {
            var environment = e.registerEnvironment(id("default"));
            TESTS.forEach((name, test) -> e.registerTest(id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(name)), new TestData<>(environment, id("empty"), test.maxTicks(), 0, true))));
        });
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Raft.id(id));
    }

    private static void batteryEnergyCapability(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block("makeshift_battery"));
        EnergyHandler cap = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(cap != null, "battery has no NeoForge energy capability");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(cap.insert(5_000, tx) == 5_000, "insert in a transaction");
        }
        helper.assertTrue(cap.getAmountAsLong() == 0, "an aborted transaction changed the battery");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(cap.insert(25_000, tx) == 10_000, "insert is not limited to 10000 per call");
            helper.assertTrue(cap.extract(4_000, tx) == 4_000, "extract does not work");
            tx.commit();
        }
        helper.assertTrue(cap.getAmountAsLong() == 6_000 && cap.getCapacityAsLong() > 0, "battery holds " + cap.getAmountAsLong());
        EnergyStorage own = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(own != null && own.getAmount() == 6_000 && !(own instanceof EnergyStorage.ForeignEnergy), "lookup does not find the battery storage");
        helper.succeed();
    }

    private static void cableMovesEnergy(GameTestHelper helper) {
        BlockPos solar = new BlockPos(1, 1, 1), cable = new BlockPos(2, 1, 1), battery = new BlockPos(3, 1, 1);
        helper.setBlock(solar, block("makeshift_solar_panel"));
        helper.setBlock(cable, block("copper_cable"));
        helper.setBlock(battery, block("makeshift_battery"));
        MakeshiftSolarPanelBlockEntity panel = helper.getBlockEntity(solar, MakeshiftSolarPanelBlockEntity.class);
        panel.getEnergyProvider(null).amount = 20_000;
        helper.succeedWhen(() -> {
            EnergyHandler cap = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(battery), Direction.WEST);
            helper.assertTrue(cap != null && cap.getAmountAsLong() >= 1_000, "no energy arrived in the battery");
        });
    }

    private static void foreignEnergyTransactions(GameTestHelper helper) {
        SimpleEnergyHandler target = new SimpleEnergyHandler(10_000, 1_000, 1_000, 2_000);
        EnergyStorage.ForeignEnergy foreign = new EnergyStorage.ForeignEnergy(target);
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(foreign.insert(5_000, tx) == 1_000, "insert is not limited by the other mod's rate");
        }
        helper.assertTrue(target.getAmountAsLong() == 2_000, "an aborted transaction moved energy");
        try (Transaction tx = Transaction.openOuter()) {
            try (Transaction nested = tx.openNested()) {
                foreign.extract(500, nested);
            }
            helper.assertTrue(foreign.insert(700, tx) == 700, "insert after an aborted nested transaction failed");
            tx.commit();
        }
        helper.assertTrue(target.getAmountAsLong() == 2_700, "committed transfer is wrong: " + target.getAmountAsLong());
        helper.succeed();
    }

    private static void itemCapabilities(GameTestHelper helper) {
        BlockPos catcher = new BlockPos(1, 1, 1), chest = new BlockPos(3, 1, 1);
        helper.setBlock(catcher, block("item_catcher"));
        ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(catcher), Direction.DOWN);
        helper.assertTrue(handler != null && handler.size() >= 9, "item catcher has no item handler");
        helper.setBlock(chest, Blocks.CHEST);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(chest), Direction.DOWN);
        helper.assertTrue(storage != null, "chest not found by the item lookup");
        try (Transaction tx = Transaction.openOuter()) {
            storage.insert(ItemVariant.of(new ItemStack(Items.COBBLESTONE)), 70, tx);
        }
        var container = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest));
        helper.assertTrue(container.countItem(Items.COBBLESTONE) == 0, "an aborted insert reached the chest");
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(storage.insert(ItemVariant.of(new ItemStack(Items.COBBLESTONE)), 70, tx) == 70, "insert into chest");
            tx.commit();
        }
        helper.assertTrue(container.countItem(Items.COBBLESTONE) == 70, "chest holds " + container.countItem(Items.COBBLESTONE));
        helper.succeed();
    }

    private static void flammableAndStrippable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        int checked = 0;
        for (var e : FlammableBlockRegistry.getDefaultInstance().registered().entrySet()) {
            BlockState state = e.getKey().defaultBlockState();
            int ignite = state.getFireSpreadSpeed(helper.getLevel(), pos, Direction.UP), burn = state.getFlammability(helper.getLevel(), pos, Direction.UP);
            helper.assertTrue(ignite == e.getValue()[0] && burn == e.getValue()[1], e.getKey() + " burns " + ignite + "/" + burn + ", code says "
                    + e.getValue()[0] + "/" + e.getValue()[1]);
            checked++;
        }
        helper.assertTrue(checked > 20, "too few flammable blocks: " + checked);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Block log : BlockTransformerHelper.stripping().keySet()) {
            BlockState state = log.defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) state = state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            helper.setBlock(pos, state);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            BlockPos abs = helper.absolutePos(pos);
            player.getMainHandItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            BlockState after = helper.getLevel().getBlockState(abs);
            helper.assertTrue(after.getBlock() != log, "the axe does not strip " + log);
            if (after.hasProperty(RotatedPillarBlock.AXIS)) {
                helper.assertTrue(after.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "stripping lost the axis of " + log);
            }
        }
        helper.assertTrue(BlockTransformerHelper.stripping().size() >= 4, "too few strippable blocks");
        helper.succeed();
    }

    private static void everyEntityType(GameTestHelper helper) {
        int living = 0, total = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!Raft.MOD_ID.equals(id.getNamespace())) continue;
            Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(entity != null, "cannot create " + id);
            total++;
            if (entity instanceof LivingEntity livingEntity) {
                living++;
                helper.assertTrue(DefaultAttributes.hasSupplier(type), "no attributes for " + id);
                helper.assertTrue(livingEntity.getMaxHealth() > 0, "no health for " + id);
            }
            entity.discard();
        }
        helper.assertTrue(total >= 30 && living >= 20, "too few entity types: " + total + "/" + living);
        helper.succeed();
    }

    private static void biomeModifier(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Biome ocean = biomes.getOrThrow(ModBiomes.RAFT_OCEAN_BIOME).value();
        helper.assertTrue(count(ocean, "palm_placed") == 1 && count(ocean, "titanium_ore_placed") == 1, "raft ocean features are doubled or missing");
        helper.assertTrue(count(biomes.getOrThrow(Biomes.BEACH).value(), "driftwood_beach_placed") == 1, "driftwood is not added to beaches");
        // 26.3 keeps the spawns in the biome's environment attributes; they are added by the same modifier (WorldSpawns)
        helper.succeed();
    }

    private static int count(Biome biome, String feature) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Raft.id(feature));
        int n = 0;
        for (var step : biome.getGenerationSettings().features()) {
            for (Holder<PlacedFeature> holder : step) {
                if (holder.is(key)) n++;
            }
        }
        return n;
    }

    private static void attachments(GameTestHelper helper) {
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(NpcContent.SPOTS_DONE.get(chunk) == null, "chunk attachment is not absent at first");
        NpcContent.SPOTS_DONE.set(chunk, List.of("test"));
        helper.assertTrue(List.of("test").equals(NpcContent.SPOTS_DONE.getOrElse(chunk, List.of())), "chunk attachment lost");
        NpcContent.SPOTS_DONE.set(chunk, null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuideContent.ALMANAC_GIVEN.set(player, true);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        var tag = output.buildResult();
        helper.assertTrue(tag.getCompoundOrEmpty("neoforge:attachments").contains("raft:almanac_given"), "player attachment is not saved");
        // a player saved by the Fabric build keeps the flag
        tag.remove("neoforge:attachments");
        var fabric = new net.minecraft.nbt.CompoundTag();
        fabric.putBoolean("raft:almanac_given", true);
        tag.put("fabric:attachments", fabric);
        Player moved = helper.makeMockPlayer(GameType.SURVIVAL);
        moved.load(net.minecraft.world.level.storage.TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(Boolean.TRUE.equals(GuideContent.ALMANAC_GIVEN.get(moved)), "Fabric attachment not taken over");
        helper.succeed();
    }

    private static void extendedMenuData(GameTestHelper helper) {
        var type = (net.kasax.raft.platform.ExtendedMenuType<?, ?>) BuiltInRegistries.MENU.getValue(Raft.id("makeshift_battery"));
        helper.assertTrue(type != null, "the battery menu is not an extended menu type");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block("makeshift_battery"));
        var factory = (net.kasax.raft.platform.ExtendedMenuProvider<?>) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var player = helper.makeMockServerPlayerInLevel();
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        write(buf, type, factory, player);
        var menu = type.create(1, player.getInventory(), buf);
        helper.assertTrue(menu != null && menu.getType() == type, "the battery menu was not made from its data");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <D> void write(net.minecraft.network.RegistryFriendlyByteBuf buf, net.kasax.raft.platform.ExtendedMenuType<?, ?> type,
                                  net.kasax.raft.platform.ExtendedMenuProvider<?> factory, net.minecraft.server.level.ServerPlayer player) {
        var codec = (net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, D>) type.getPacketCodec();
        codec.encode(buf, ((net.kasax.raft.platform.ExtendedMenuProvider<D>) factory).getScreenOpeningData(player));
    }
}
