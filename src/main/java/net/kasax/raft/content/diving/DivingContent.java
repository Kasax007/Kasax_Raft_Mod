package net.kasax.raft.content.diving;

import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.diving.block.AirCompressorBlock;
import net.kasax.raft.content.diving.block.AirCompressorBlockEntity;
import net.kasax.raft.content.diving.block.AirCompressorMenu;
import net.kasax.raft.content.diving.entity.Harpoon;
import net.kasax.raft.content.diving.item.DivingGearItem;
import net.kasax.raft.content.diving.item.HarpoonGunItem;
import net.kasax.raft.content.diving.item.HarpoonItem;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.kasax.raft.content.diving.item.SeaScooterItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import team.reborn.energy.api.EnergyStorage;

/** Registrations of the diving module (spec 8.2). Items go into the VOYAGE tab after the boats. */
public final class DivingContent {
    private DivingContent() {
    }

    /** Air in an oxygen tank, in ticks. */
    public static final DataComponentType<Integer> STORED_AIR = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Raft.id("stored_air"), DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    static {
        DivingSounds.register();
    }

    // ------------------------------------------------------------------------------------------- armor
    public static final Item DIVING_HELMET = BcReg.item("diving_helmet", p -> new DivingGearItem(p, "tooltip.raft.diving_helmet"),
            armor(DivingGear.DIVING_HELMET, ArmorType.HELMET).rarity(Rarity.UNCOMMON), null);
    public static final OxygenTankItem OXYGEN_TANK = BcReg.item("oxygen_tank", p -> new OxygenTankItem(p, 2_400),
            armor(DivingGear.OXYGEN_TANK, ArmorType.CHESTPLATE).component(STORED_AIR, 2_400), null);
    public static final OxygenTankItem TITANIUM_OXYGEN_TANK = BcReg.item("titanium_oxygen_tank", p -> new OxygenTankItem(p, 7_200),
            armor(DivingGear.TITANIUM_OXYGEN_TANK, ArmorType.CHESTPLATE).component(STORED_AIR, 7_200).rarity(Rarity.RARE), null);
    public static final Item WETSUIT = BcReg.item("wetsuit", p -> new DivingGearItem(p, "tooltip.raft.wetsuit"),
            armor(DivingGear.WETSUIT, ArmorType.LEGGINGS), null);
    public static final Item FLIPPERS = BcReg.item("flippers", p -> new DivingGearItem(p, "tooltip.raft.flippers"),
            armor(DivingGear.FLIPPERS, ArmorType.BOOTS), null);

    // ------------------------------------------------------------------------------------------- tools
    public static final Item HARPOON_ITEM = BcReg.item("harpoon", HarpoonItem::new, new Item.Properties().stacksTo(16), null);
    public static final Item HARPOON_GUN = BcReg.item("harpoon_gun", HarpoonGunItem::new,
            new Item.Properties().durability(384).enchantable(1), null);
    public static final SeaScooterItem SEA_SCOOTER = BcReg.item("sea_scooter", SeaScooterItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(EnergyStorage.ENERGY_COMPONENT, SeaScooterItem.CAPACITY)
                    .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F)), null);

    // ------------------------------------------------------------------------------------------- block
    public static final AirCompressorBlock AIR_COMPRESSOR = BcReg.block("air_compressor", AirCompressorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER).noOcclusion());
    public static final Item AIR_COMPRESSOR_ITEM = BcReg.blockItem("air_compressor", AIR_COMPRESSOR, new Item.Properties(), null,
            "tooltip.raft.air_compressor", "tooltip.raft.air_compressor.1");
    public static final BlockEntityType<AirCompressorBlockEntity> AIR_COMPRESSOR_BE = BcReg.blockEntity("air_compressor",
            AirCompressorBlockEntity::new, AIR_COMPRESSOR);
    public static final MenuType<AirCompressorMenu> AIR_COMPRESSOR_MENU = Registry.register(BuiltInRegistries.MENU,
            Raft.id("air_compressor"), new MenuType<>(AirCompressorMenu::new, FeatureFlags.VANILLA_SET));

    // ------------------------------------------------------------------------------------------- entity
    public static final EntityType<Harpoon> HARPOON = BcReg.entity("harpoon",
            EntityType.Builder.<Harpoon>of(Harpoon::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F)
                    .eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20));

    private static Item.Properties armor(ArmorMaterial material, ArmorType type) {
        return new Item.Properties().humanoidArmor(material, type).attributes(DivingGear.attributes(material, type));
    }

    /** Loads the class (fields register themselves), fills the VOYAGE tab and hooks up energy and dispensers. */
    public static void register() {
        // new tanks come full and new scooters charged (default component values); order: gear, compressor, tools
        BcTabs.add(BcTabs.Tab.VOYAGE, DIVING_HELMET);
        BcTabs.add(BcTabs.Tab.VOYAGE, OXYGEN_TANK);
        BcTabs.add(BcTabs.Tab.VOYAGE, TITANIUM_OXYGEN_TANK);
        BcTabs.add(BcTabs.Tab.VOYAGE, WETSUIT);
        BcTabs.add(BcTabs.Tab.VOYAGE, FLIPPERS);
        BcTabs.add(BcTabs.Tab.VOYAGE, AIR_COMPRESSOR_ITEM);
        BcTabs.add(BcTabs.Tab.VOYAGE, HARPOON_GUN);
        BcTabs.add(BcTabs.Tab.VOYAGE, HARPOON_ITEM);
        BcTabs.add(BcTabs.Tab.VOYAGE, SEA_SCOOTER);
        EnergyStorage.SIDED.registerForBlockEntity(AirCompressorBlockEntity::getEnergyStorage, AIR_COMPRESSOR_BE);
        DispenserBlock.registerBehavior(HARPOON_ITEM, new ProjectileDispenseBehavior(HARPOON_ITEM));
    }
}
