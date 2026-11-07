package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.kasax.raft.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModArmorMaterials {
    /** Durability factor 25 (same as before), protection 3/8/6/3, toughness 2, knockback resistance 0.1. */
    public static final int TITANIUM_DURABILITY = 25;

    public static final Holder<ArmorMaterial> TITANIUM = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Raft.id("titanium"),
            new ArmorMaterial(defense(3, 8, 6, 3), 19, SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModTags.Items.TITANIUM_ARMOR_REPAIR),
                    List.of(new ArmorMaterial.Layer(Raft.id("titanium"))), 2f, 0.1f));

    public static Map<ArmorItem.Type, Integer> defense(int helmet, int chest, int legs, int boots) {
        Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.BODY, chest);
        return map;
    }
}
