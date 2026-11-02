package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.kasax.raft.util.ModTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public class ModArmorMaterials {
    public static final ResourceKey<EquipmentAsset> TITANIUM_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Raft.id("titanium"));

    /** Durability factor 25 (same as before), protection 3/8/6/3, toughness 2, knockback resistance 0.1. */
    public static final ArmorMaterial TITANIUM = new ArmorMaterial(
            25,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            19,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2f,
            0.1f,
            ModTags.Items.TITANIUM_ARMOR_REPAIR,
            TITANIUM_ASSET);
}
