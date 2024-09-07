package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.kasax.raft.item.custom.*;
import net.kasax.raft.sound.ModSounds;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ANCIENT_ENERGY = registerItem("ancient_energy", new Item(new Item.Settings()));
    public static final Item ANCIENT_BROKEN_RING = registerItem("ancient_broken_ring", new AncientBrokenRingItem(new Item.Settings()));
    public static final Item ANCIENT_CRYSTAL = registerItem("ancient_crystal", new AncientCrystalItem(new Item.Settings()));

    public static final Item RAW_TITANIUM = registerItem("raw_titanium", new Item(new Item.Settings()));
    public static final Item TITANIUM_INGOT = registerItem("titanium_ingot", new Item(new Item.Settings()));
    public static final Item TITANIUM_NUGGET = registerItem("titanium_nugget", new Item(new Item.Settings()));
    public static final Item PLASTIC = registerItem("plastic", new Item(new Item.Settings()));
    public static final Item CIRCUIT_BOARD = registerItem("circuit_board", new Item(new Item.Settings()));
    public static final Item COCONUT = registerItem("coconut", new Item(new Item.Settings().food(ModFoodComponents.COCONUT)));
    public static final Item PINA_COLADA = registerItem("pina_colada", new Item(new Item.Settings().food(ModFoodComponents.PINA_COLADA)));
    public static final Item TRASH_CUBE = registerItem("trash_cube", new TrashCubeItem(new Item.Settings()));
    public static final Item UNREFINED_OCEAN_GARBAGE = registerItem("unrefined_ocean_garbage", new Item(new Item.Settings()));


    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet",
            new TitaniumArmorItem(ModArmorMaterials.TITANIUM.toArmorMaterial(), ArmorItem.Type.HELMET, new Item.Settings()));

    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate",
            new TitaniumArmorItem(ModArmorMaterials.TITANIUM.toArmorMaterial(), ArmorItem.Type.CHESTPLATE, new Item.Settings()));

    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings",
            new TitaniumArmorItem(ModArmorMaterials.TITANIUM.toArmorMaterial(), ArmorItem.Type.LEGGINGS, new Item.Settings()));

    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots",
            new TitaniumArmorItem(ModArmorMaterials.TITANIUM.toArmorMaterial(), ArmorItem.Type.BOOTS, new Item.Settings()));


    public static final Item NET = registerItem("net", new Item(new Item.Settings().maxCount(1).maxDamage(32)));


    public static final Item SOME_GAME_MUSIC_MUSIC_DISC = registerItem("some_game_music_music_disc",
            new Item(new Item.Settings().maxCount(1).jukeboxPlayable(ModSounds.Some_Game_Music_Seth)));
    public static final Item LOFI_BEAT_MUSIC_DISC = registerItem("lofi_beat_music_disc",
            new Item(new Item.Settings().maxCount(1).jukeboxPlayable(ModSounds.Lofi_Beat_Seth)));

    public static final Item ENERGY_STAFF = registerItem("energy_staff", new EnergyStaffItem(new Item.Settings().maxCount(1).maxDamage(64)));

    public static final Item RAFT_TELEPORTER = registerItem("raft_teleporter", new RaftTeleporter(new Item.Settings().maxCount(1).maxDamage(64)));
    public static final Item METAL_DETECTOR = registerItem("metal_detector", new MetalDetector(new Item.Settings().maxCount(1).maxDamage(128)));

    //Item layout
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Raft.MOD_ID, name), item);
    }
    public static void registerModItems() {
        Raft.LOGGER.info("Registering Mod Items for " + Raft.MOD_ID);
    }
}
