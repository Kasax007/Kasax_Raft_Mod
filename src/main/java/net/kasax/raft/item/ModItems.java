package net.kasax.raft.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.item.custom.EnergyStaffItem;
import net.kasax.raft.item.custom.MetalDetector;
import net.kasax.raft.item.custom.ModArmorItem;
import net.kasax.raft.item.custom.RaftTeleporter;
import net.kasax.raft.sound.ModSounds;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ANCIENT_ENERGY = registerItem("ancient_energy", new Item(new FabricItemSettings()));
    public static final Item ANCIENT_BROKEN_RING = registerItem("ancient_broken_ring", new Item(new FabricItemSettings()));
    public static final Item ANCIENT_CRYSTAL = registerItem("ancient_crystal", new Item(new FabricItemSettings()));

    public static final Item RAW_TITANIUM = registerItem("raw_titanium", new Item(new FabricItemSettings()));
    public static final Item TITANIUM_INGOT = registerItem("titanium_ingot", new Item(new FabricItemSettings()));
    public static final Item TITANIUM_NUGGET = registerItem("titanium_nugget", new Item(new FabricItemSettings()));
    public static final Item PLASTIC = registerItem("plastic", new Item(new FabricItemSettings()));
    public static final Item CIRCUIT_BOARD = registerItem("circuit_board", new Item(new FabricItemSettings()));
    public static final Item TRASH_CUBE = registerItem("trash_cube", new Item(new FabricItemSettings()));


    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet",
            new ModArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new FabricItemSettings()));

    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate",
            new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));

    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings",
            new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));

    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots",
            new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new FabricItemSettings()));


    public static final Item NET = registerItem("net", new Item(new FabricItemSettings().maxCount(1).maxDamage(32)));


    public static final Item SOME_GAME_MUSIC_MUSIC_DISC = registerItem("some_game_music_music_disc",
            new MusicDiscItem(7, ModSounds.Some_Game_Music_Seth, new FabricItemSettings().maxCount(1), 105));
    public static final Item LOFI_BEAT_MUSIC_DISC = registerItem("lofi_beat_music_disc",
            new MusicDiscItem(7, ModSounds.Lofi_Beat_Seth, new FabricItemSettings().maxCount(1), 120));

    public static final Item ENERGY_STAFF = registerItem("energy_staff", new EnergyStaffItem(new FabricItemSettings().maxCount(1).maxDamage(64)));

    public static final Item RAFT_TELEPORTER = registerItem("raft_teleporter", new RaftTeleporter(new FabricItemSettings().maxCount(1).maxDamage(64)));
    public static final Item METAL_DETECTOR = registerItem("metal_detector", new MetalDetector(new FabricItemSettings().maxCount(1).maxDamage(128)));

    //Item layout
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Raft.MOD_ID, name), item);
    }
    public static void registerModItems() {
        Raft.LOGGER.info("Registering Mod Items for " + Raft.MOD_ID);
    }
}
