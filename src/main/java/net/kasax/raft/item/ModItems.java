package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.kasax.raft.entity.ModEntities;
import net.kasax.raft.item.custom.*;
import net.kasax.raft.sound.ModJukeboxSongs;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ArmorItem;
import net.kasax.raft.item.custom.DrinkItem;

import java.util.function.Function;

public class ModItems {
    public static final Item ANCIENT_ENERGY = registerItem("ancient_energy", Item::new, new Item.Properties());
    public static final Item ANCIENT_BROKEN_RING = registerItem("ancient_broken_ring",
            p -> new TooltipItem(p, "tooltip.raft.ancient_broken_ring"), new Item.Properties());
    public static final Item ANCIENT_CRYSTAL = registerItem("ancient_crystal",
            p -> new TooltipItem(p, "tooltip.raft.ancient_crystal"), new Item.Properties());

    public static final Item RAW_TITANIUM = registerItem("raw_titanium", Item::new, new Item.Properties());
    public static final Item TITANIUM_INGOT = registerItem("titanium_ingot", Item::new, new Item.Properties());
    public static final Item TITANIUM_NUGGET = registerItem("titanium_nugget", Item::new, new Item.Properties());
    public static final Item PLASTIC = registerItem("plastic", Item::new, new Item.Properties());
    public static final Item CIRCUIT_BOARD = registerItem("circuit_board", Item::new, new Item.Properties());
    public static final Item COCONUT = registerItem("coconut", Item::new, new Item.Properties().food(ModFoodComponents.COCONUT));
    public static final Item PINA_COLADA = registerItem("pina_colada", DrinkItem::new,
            new Item.Properties().food(ModFoodComponents.PINA_COLADA));
    public static final Item TRASH_CUBE = registerItem("trash_cube", TrashCubeItem::new, new Item.Properties());
    public static final Item UNREFINED_OCEAN_GARBAGE = registerItem("unrefined_ocean_garbage", Item::new, new Item.Properties());

    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet",
            p -> new TitaniumArmorItem(ArmorItem.Type.HELMET, p), new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(ModArmorMaterials.TITANIUM_DURABILITY)));
    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate",
            p -> new TitaniumArmorItem(ArmorItem.Type.CHESTPLATE, p), new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(ModArmorMaterials.TITANIUM_DURABILITY)));
    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings",
            p -> new TitaniumArmorItem(ArmorItem.Type.LEGGINGS, p), new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(ModArmorMaterials.TITANIUM_DURABILITY)));
    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots",
            p -> new TitaniumArmorItem(ArmorItem.Type.BOOTS, p), new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(ModArmorMaterials.TITANIUM_DURABILITY)));

    public static final Item NET = registerItem("net", Item::new, new Item.Properties().durability(32));

    public static final Item SOME_GAME_MUSIC_MUSIC_DISC = registerItem("some_game_music_music_disc", Item::new,
            new Item.Properties().stacksTo(1).jukeboxPlayable(ModJukeboxSongs.SOME_GAME_MUSIC_SETH).rarity(Rarity.RARE));
    public static final Item LOFI_BEAT_MUSIC_DISC = registerItem("lofi_beat_music_disc", Item::new,
            new Item.Properties().stacksTo(1).jukeboxPlayable(ModJukeboxSongs.LOFI_BEAT_SETH).rarity(Rarity.RARE));
    public static final Item CRYSTAL_CASTLES_MUSIC_DISC = registerItem("crystal_castles_music_disc", Item::new,
            new Item.Properties().stacksTo(1).jukeboxPlayable(ModJukeboxSongs.CRYSTAL_CASTLES).rarity(Rarity.RARE));

    public static final Item ENERGY_STAFF = registerItem("energy_staff", EnergyStaffItem::new, new Item.Properties().durability(64));
    public static final Item RAFT_TELEPORTER = registerItem("raft_teleporter", RaftTeleporter::new, new Item.Properties().durability(64));
    public static final Item METAL_DETECTOR = registerItem("metal_detector", MetalDetector::new, new Item.Properties().durability(128));

    // ---- raft crafting materials, food and tools ----

    public static final Item ROPE = registerItem("rope", Item::new, new Item.Properties());
    public static final Item SCRAP_METAL = registerItem("scrap_metal", Item::new, new Item.Properties());
    public static final Item SHARK_TOOTH = registerItem("shark_tooth", Item::new, new Item.Properties());
    public static final Item SHARK_MEAT = registerItem("shark_meat", Item::new,
            new Item.Properties().food(ModFoodComponents.SHARK_MEAT));
    public static final Item COOKED_SHARK_MEAT = registerItem("cooked_shark_meat", Item::new,
            new Item.Properties().food(ModFoodComponents.COOKED_SHARK_MEAT));
    public static final Item FRESH_WATER = registerItem("fresh_water", p -> new DrinkItem(p, "tooltip.raft.fresh_water"),
            new Item.Properties().stacksTo(16).food(ModFoodComponents.FRESH_WATER));
    public static final Item COCONUT_MILK = registerItem("coconut_milk", p -> new DrinkItem(p, "tooltip.raft.coconut_milk"),
            new Item.Properties().stacksTo(16).food(ModFoodComponents.COCONUT_MILK));
    public static final Item RECEIVER = registerItem("receiver", ReceiverItem::new, new Item.Properties().stacksTo(1));
    public static final Item PLASTIC_HOOK = registerItem("plastic_hook", PlasticHookItem::new, new Item.Properties().durability(256));
    public static final Item PADDLE = registerItem("paddle", PaddleItem::new, new Item.Properties().durability(128));
    public static final Item SHARK_SPAWN_EGG = registerItem("shark_spawn_egg",
            p -> new SpawnEggItem(ModEntities.SHARK, 0x6E7F8C, 0xD8DEE2, p), new Item.Properties());

    public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Raft.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
    }

    public static void registerModItems() {
        Raft.LOGGER.info("Registering Mod Items for " + Raft.MOD_ID);
    }
}
