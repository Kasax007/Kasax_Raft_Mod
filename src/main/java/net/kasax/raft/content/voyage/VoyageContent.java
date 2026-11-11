package net.kasax.raft.content.voyage;

import com.mojang.serialization.Codec;
import net.kasax.raft.platform.ExtendedScreenHandlerType;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.EnumMap;
import java.util.Map;
import net.kasax.raft.compat.ItemProps;

/** Registrations of the voyage module (spec 8.1): palm/driftwood boats and the Expedition Raft with its upgrades. */
public final class VoyageContent {
    private VoyageContent() {
    }

    /** Upgrades stored on an Expedition Raft item (bit mask of {@link RaftUpgrade}). */
    public static final DataComponentType<Integer> RAFT_UPGRADES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            net.kasax.raft.Raft.id("raft_upgrades"), DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, RaftUpgrade.ALL))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // ------------------------------------------------------------------------------------------- entities
    public static final EntityType<ModBoats.PalmBoat> PALM_BOAT = BcReg.entity("palm_boat",
            EntityType.Builder.<ModBoats.PalmBoat>of(ModBoats.PalmBoat::new, MobCategory.MISC).sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final EntityType<ModBoats.PalmChestBoat> PALM_CHEST_BOAT = BcReg.entity("palm_chest_boat",
            EntityType.Builder.<ModBoats.PalmChestBoat>of(ModBoats.PalmChestBoat::new, MobCategory.MISC).sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final EntityType<ModBoats.DriftwoodRaft> DRIFTWOOD_RAFT = BcReg.entity("driftwood_raft",
            EntityType.Builder.<ModBoats.DriftwoodRaft>of(ModBoats.DriftwoodRaft::new, MobCategory.MISC).sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final EntityType<ModBoats.DriftwoodChestRaft> DRIFTWOOD_CHEST_RAFT = BcReg.entity("driftwood_chest_raft",
            EntityType.Builder.<ModBoats.DriftwoodChestRaft>of(ModBoats.DriftwoodChestRaft::new, MobCategory.MISC).sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final EntityType<ExpeditionRaft> EXPEDITION_RAFT = BcReg.entity("expedition_raft",
            EntityType.Builder.<ExpeditionRaft>of(ExpeditionRaft::new, MobCategory.MISC).sized(3.0F, 0.6F).eyeHeight(0.6F).clientTrackingRange(10));

    // ------------------------------------------------------------------------------------------- items
    public static final Item EXPEDITION_RAFT_ITEM = BcReg.item("expedition_raft", ExpeditionRaftItem::new,
            new Item.Properties().stacksTo(1).component(RAFT_UPGRADES, 0), BcTabs.Tab.VOYAGE);
    public static final Map<RaftUpgrade, Item> UPGRADE_ITEMS = new EnumMap<>(RaftUpgrade.class);

    static {
        for (RaftUpgrade upgrade : RaftUpgrade.values()) {
            UPGRADE_ITEMS.put(upgrade, BcReg.item(upgrade.itemId, p -> new RaftUpgradeItem(p, upgrade),
                    new Item.Properties().stacksTo(16), BcTabs.Tab.VOYAGE));
        }
    }

    public static final Item PALM_BOAT_ITEM = boatItem("palm_boat", PALM_BOAT);
    public static final Item PALM_CHEST_BOAT_ITEM = boatItem("palm_chest_boat", PALM_CHEST_BOAT);
    public static final Item DRIFTWOOD_RAFT_ITEM = boatItem("driftwood_raft", DRIFTWOOD_RAFT);
    public static final Item DRIFTWOOD_CHEST_RAFT_ITEM = boatItem("driftwood_chest_raft", DRIFTWOOD_CHEST_RAFT);

    // ------------------------------------------------------------------------------------------- menu
    public static final MenuType<ExpeditionRaftMenu> EXPEDITION_RAFT_MENU = Registry.register(BuiltInRegistries.MENU,
            net.kasax.raft.Raft.id("expedition_raft"), new ExtendedScreenHandlerType<>(ExpeditionRaftMenu::new, ByteBufCodecs.VAR_INT));

    private static Item boatItem(String name, EntityType<? extends net.minecraft.world.entity.vehicle.Boat> type) {
        Item item = BcReg.item(name, p -> new ModBoatItem(type, p),
                ItemProps.cookingFuel(new Item.Properties(), ItemProps.COOKING_TIME_BOATS).stacksTo(1), BcTabs.Tab.VOYAGE);
        DispenserBlock.registerBehavior(item, new ModBoatDispenseBehavior(type));
        return item;
    }

    public static Item upgradeItem(RaftUpgrade upgrade) {
        return UPGRADE_ITEMS.get(upgrade);
    }

    /** Loads the class (all fields above register themselves) and sets the VOYAGE tab icon. */
    public static void register() {
        BcTabs.icon(BcTabs.Tab.VOYAGE, () -> {
            ItemStack stack = new ItemStack(EXPEDITION_RAFT_ITEM);
            stack.set(RAFT_UPGRADES, RaftUpgrade.SAIL.bit());
            return stack;
        });
    }
}
