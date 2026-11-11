package net.kasax.raft.gametest.neo;

import java.util.List;
import java.util.Map;
import net.kasax.raft.Raft;
import net.kasax.raft.block.entity.MakeshiftSolarPanelBlockEntity;
import net.kasax.raft.content.guide.GuideContent;
import net.kasax.raft.content.npc.NpcContent;
import net.kasax.raft.platform.CompostingChanceRegistry;
import net.kasax.raft.platform.FuelRegistry;
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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * NeoForge game tests of Beach Craft's loader layer: energy and item capabilities, the cable network with the
 * transaction shim, data maps, flammability and stripping, attribute registration of every entity, the biome
 * modifier (features and spawns), attachments, extended menus. The scripted in-game tests (tools/bctest) cover the
 * gameplay itself.
 */
@GameTestHolder("raft_gametest")
@PrefixGameTestTemplate(false)
public class BcGameTests {
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(Raft.id(id));
    }

    @GameTest(template = "empty")
    public static void battery_energy_capability(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block("makeshift_battery"));
        IEnergyStorage cap = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(cap != null, "battery has no NeoForge energy capability");
        helper.assertTrue(cap.receiveEnergy(5_000, true) == 5_000 && cap.getEnergyStored() == 0, "simulated receive changed the battery");
        helper.assertTrue(cap.receiveEnergy(25_000, false) == 10_000, "receive is not limited to 10000 per call");
        helper.assertTrue(cap.extractEnergy(4_000, false) == 4_000 && cap.getEnergyStored() == 6_000, "extract does not work");
        helper.assertTrue(cap.canReceive() && cap.canExtract() && cap.getMaxEnergyStored() > 0, "battery flags are wrong");
        // the loader layer hands Beach Craft's own storage back, not a wrapper of the capability
        EnergyStorage own = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(own != null && own.getAmount() == 6_000 && !(own instanceof EnergyStorage.ForeignEnergy), "lookup does not find the battery storage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cable_moves_energy(GameTestHelper helper) {
        BlockPos solar = new BlockPos(1, 1, 1), cable = new BlockPos(2, 1, 1), battery = new BlockPos(3, 1, 1);
        helper.setBlock(solar, block("makeshift_solar_panel"));
        helper.setBlock(cable, block("copper_cable"));
        helper.setBlock(battery, block("makeshift_battery"));
        MakeshiftSolarPanelBlockEntity panel = (MakeshiftSolarPanelBlockEntity) helper.getBlockEntity(solar);
        panel.getEnergyProvider(null).amount = 20_000;
        helper.succeedWhen(() -> {
            IEnergyStorage cap = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(battery), Direction.WEST);
            helper.assertTrue(cap != null && cap.getEnergyStored() >= 1_000, "no energy arrived in the battery");
        });
    }

    @GameTest(template = "empty")
    public static void foreign_energy_transactions(GameTestHelper helper) {
        net.neoforged.neoforge.energy.EnergyStorage target = new net.neoforged.neoforge.energy.EnergyStorage(10_000, 1_000, 1_000, 2_000);
        EnergyStorage.ForeignEnergy foreign = new EnergyStorage.ForeignEnergy(target);
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(foreign.insert(5_000, tx) == 1_000, "insert is not limited by the other mod's rate");
        }
        helper.assertTrue(target.getEnergyStored() == 2_000, "an aborted transaction moved energy");
        try (Transaction tx = Transaction.openOuter()) {
            try (Transaction nested = tx.openNested()) {
                foreign.extract(500, nested);
            }
            helper.assertTrue(foreign.insert(700, tx) == 700, "insert after an aborted nested transaction failed");
            tx.commit();
        }
        helper.assertTrue(target.getEnergyStored() == 2_700, "committed transfer is wrong: " + target.getEnergyStored());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void item_capabilities(GameTestHelper helper) {
        BlockPos catcher = new BlockPos(1, 1, 1), chest = new BlockPos(3, 1, 1);
        helper.setBlock(catcher, block("item_catcher"));
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(catcher), Direction.DOWN);
        helper.assertTrue(handler != null && handler.getSlots() >= 9, "item catcher has no item handler");
        helper.setBlock(chest, Blocks.CHEST);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(chest), Direction.DOWN);
        helper.assertTrue(storage != null, "chest not found by the item lookup");
        try (Transaction tx = Transaction.openOuter()) {
            storage.insert(ItemVariant.of(new ItemStack(Items.COBBLESTONE)), 70, tx);
        }
        helper.assertContainerEmpty(chest);
        try (Transaction tx = Transaction.openOuter()) {
            helper.assertTrue(storage.insert(ItemVariant.of(new ItemStack(Items.COBBLESTONE)), 70, tx) == 70, "insert into chest");
            tx.commit();
        }
        int count = ((net.minecraft.world.Container) helper.getBlockEntity(chest)).countItem(Items.COBBLESTONE);
        helper.assertTrue(count == 70, "chest holds " + count + " cobblestone, not 70");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void data_maps_match_code(GameTestHelper helper) {
        int checked = 0;
        for (Map.Entry<Item, Float> e : CompostingChanceRegistry.INSTANCE.registered().entrySet()) {
            float value = ComposterBlock.getValue(new ItemStack(e.getKey()));
            helper.assertTrue(Math.abs(value - e.getValue()) < 1e-6, "compost chance of " + e.getKey() + " is " + value + ", code says " + e.getValue());
            checked++;
        }
        for (Map.Entry<Item, Integer> e : FuelRegistry.INSTANCE.registered().entrySet()) {
            int value = new ItemStack(e.getKey()).getBurnTime(null);
            helper.assertTrue(value == e.getValue(), "burn time of " + e.getKey() + " is " + value + ", code says " + e.getValue());
            checked++;
        }
        helper.assertTrue(checked > 20, "too few data map entries: " + checked);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flammable_and_strippable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState planks = block("driftwood_planks").defaultBlockState();
        helper.assertTrue(planks.getFlammability(helper.getLevel(), pos, Direction.UP) == 20
                && planks.getFireSpreadSpeed(helper.getLevel(), pos, Direction.UP) == 5, "driftwood planks do not burn like Fabric's 5/20");
        helper.setBlock(pos, block("driftwood_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        BlockPos abs = helper.absolutePos(pos);
        UseOnContext ctx = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        BlockState stripped = helper.getLevel().getBlockState(abs).getToolModifiedState(ctx, ItemAbilities.AXE_STRIP, true);
        helper.assertTrue(stripped != null && stripped.is(block("stripped_driftwood_log")) && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "axe does not strip the driftwood log: " + stripped);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void every_entity_type(GameTestHelper helper) {
        int living = 0, total = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!Raft.MOD_ID.equals(id.getNamespace())) continue;
            Entity entity = type.create(helper.getLevel());
            helper.assertTrue(entity != null, "cannot create " + id);
            total++;
            if (entity instanceof LivingEntity) {
                living++;
                helper.assertTrue(DefaultAttributes.hasSupplier(type), "no attributes for " + id);
                helper.assertTrue(((LivingEntity) entity).getMaxHealth() > 0, "no health for " + id);
            }
            entity.discard();
        }
        helper.assertTrue(total >= 30 && living >= 20, "too few entity types: " + total + "/" + living);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void biome_modifier(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Biome ocean = biomes.getOrThrow(ModBiomes.RAFT_OCEAN_BIOME);
        helper.assertTrue(count(ocean, "palm_placed") == 1 && count(ocean, "titanium_ore_placed") == 1, "raft ocean features are doubled or missing");
        helper.assertTrue(count(biomes.getOrThrow(Biomes.BEACH), "driftwood_beach_placed") == 1, "driftwood is not added to beaches");
        boolean spawns = false;
        for (var entry : biomes.entrySet()) {
            if (!Raft.MOD_ID.equals(entry.getKey().location().getNamespace())) continue;
            for (MobCategory category : MobCategory.values()) {
                for (var data : entry.getValue().getMobSettings().getMobs(category).unwrap()) {
                    if (Raft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getNamespace())) spawns = true;
                }
            }
        }
        helper.assertTrue(spawns, "no Beach Craft creature spawns in the Beach Craft biomes");
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

    @GameTest(template = "empty")
    public static void attachments(GameTestHelper helper) {
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(NpcContent.SPOTS_DONE.get(chunk) == null, "chunk attachment is not absent at first");
        NpcContent.SPOTS_DONE.set(chunk, List.of("test"));
        helper.assertTrue(List.of("test").equals(NpcContent.SPOTS_DONE.getOrElse(chunk, List.of())), "chunk attachment lost");
        NpcContent.SPOTS_DONE.set(chunk, null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuideContent.ALMANAC_GIVEN.set(player, true);
        CompoundTag tag = player.saveWithoutId(new CompoundTag());
        helper.assertTrue(tag.getCompound("neoforge:attachments").contains("raft:almanac_given"), "player attachment is not saved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extended_menu_data(GameTestHelper helper) {
        // the menu types of an extended factory read their opening data from NeoForge's extra menu data
        var type = (net.kasax.raft.platform.ExtendedScreenHandlerType<?, ?>) BuiltInRegistries.MENU.get(Raft.id("makeshift_battery"));
        helper.assertTrue(type != null, "the battery menu is not an extended menu type");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, block("makeshift_battery"));
        var factory = (net.kasax.raft.platform.ExtendedScreenHandlerFactory<?>) helper.getBlockEntity(pos);
        var player = helper.makeMockServerPlayerInLevel();
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        write(buf, type, factory, player);
        var menu = type.create(1, player.getInventory(), buf);
        helper.assertTrue(menu != null && menu.getType() == type, "the battery menu was not made from its data");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <D> void write(net.minecraft.network.RegistryFriendlyByteBuf buf, net.kasax.raft.platform.ExtendedScreenHandlerType<?, ?> type,
                                  net.kasax.raft.platform.ExtendedScreenHandlerFactory<?> factory, net.minecraft.server.level.ServerPlayer player) {
        var codec = (net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, D>) type.getPacketCodec();
        codec.encode(buf, ((net.kasax.raft.platform.ExtendedScreenHandlerFactory<D>) factory).getScreenOpeningData(player));
    }
}
