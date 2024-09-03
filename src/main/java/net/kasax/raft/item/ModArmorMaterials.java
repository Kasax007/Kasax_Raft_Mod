package net.kasax.raft.item;

import net.kasax.raft.Raft;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum ModArmorMaterials {
    TITANIUM("titanium", 25, new int[] {3, 8, 6, 3}, 19,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 2f, 0.1f, () -> Ingredient.ofItems(ModItems.TITANIUM_INGOT));

    private final String name;
    private final int durabilityMultiplier;
    private final Map<ArmorItem.Type, Integer> protectionAmounts;
    private final int enchantability;
    private final RegistryEntry<SoundEvent> equipSound;
    private final float toughness;
    private final float knockbackResistence;
    private final Supplier<Ingredient> repairIngredient;

    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmountsArray, int enchantability,
                      RegistryEntry<SoundEvent> equipSound, float toughness, float knockbackResistence,
                      Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = new EnumMap<>(ArmorItem.Type.class);
        this.protectionAmounts.put(ArmorItem.Type.HELMET, protectionAmountsArray[0]);
        this.protectionAmounts.put(ArmorItem.Type.CHESTPLATE, protectionAmountsArray[1]);
        this.protectionAmounts.put(ArmorItem.Type.LEGGINGS, protectionAmountsArray[2]);
        this.protectionAmounts.put(ArmorItem.Type.BOOTS, protectionAmountsArray[3]);
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistence = knockbackResistence;
        this.repairIngredient = repairIngredient;
    }

    public ArmorMaterial toArmorMaterial() {
        return new ArmorMaterial(protectionAmounts, enchantability, equipSound, repairIngredient, List.of(), toughness, knockbackResistence);
    }

    public String getName() {
        return Raft.MOD_ID + ":" + this.name;
    }
}
