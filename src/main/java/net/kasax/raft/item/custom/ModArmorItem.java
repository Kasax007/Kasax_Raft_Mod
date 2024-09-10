package net.kasax.raft.item.custom;

import net.kasax.raft.item.ModArmorMaterials;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ModArmorItem extends ArmorItem {

    // Modify the map to store a list of StatusEffectInstances for each ArmorMaterial
    private static final Map<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>> MATERIAL_TO_EFFECT_MAP =
            (new ImmutableMap.Builder<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>>())
                    .put(ModArmorMaterials.TITANIUM.toArmorMaterial(), Arrays.asList(
                            new StatusEffectInstance(StatusEffects.WATER_BREATHING, 400, 1, false, false, true),
                            new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 400, 1, false, false, true),
                            new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 400, 1, false, false, true)
                    ))
                    .build();

    public ModArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(!world.isClient()) {
            if(entity instanceof PlayerEntity player && hasFullSuitOfArmorOn(player)) {
                evaluateArmorEffects(player);
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    private void evaluateArmorEffects(PlayerEntity player) {
        for (Map.Entry<RegistryEntry<ArmorMaterial>, List<StatusEffectInstance>> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
            RegistryEntry<ArmorMaterial> mapArmorMaterial = entry.getKey();
            List<StatusEffectInstance> mapStatusEffects = entry.getValue();
            if(hasCorrectArmorOn(mapArmorMaterial.value(), player)) {
                addStatusEffectsForMaterial(player, mapArmorMaterial.value(), mapStatusEffects);

            }
        }
    }

    // New method to handle multiple effects
    private void addStatusEffectsForMaterial(PlayerEntity player, ArmorMaterial mapArmorMaterial, List<StatusEffectInstance> mapStatusEffects) {
        for (StatusEffectInstance effect : mapStatusEffects) {
            boolean hasPlayerEffect = player.hasStatusEffect(effect.getEffectType());

            if(hasCorrectArmorOn(mapArmorMaterial, player) && !hasPlayerEffect) {
                player.addStatusEffect(new StatusEffectInstance(effect));
            }
        }
    }

    private boolean hasFullSuitOfArmorOn(PlayerEntity player) {
        ItemStack boots = player.getInventory().getArmorStack(0);
        ItemStack leggings = player.getInventory().getArmorStack(1);
        ItemStack breastplate = player.getInventory().getArmorStack(2);
        ItemStack helmet = player.getInventory().getArmorStack(3);

        return !helmet.isEmpty() && !breastplate.isEmpty()
                && !leggings.isEmpty() && !boots.isEmpty();
    }

    private boolean hasCorrectArmorOn(ArmorMaterial material, PlayerEntity player) {
        for (ItemStack armorStack: player.getInventory().armor) {
            if(!(armorStack.getItem() instanceof ArmorItem)) {
                return false;
            }
        }

        ArmorItem boots = ((ArmorItem)player.getInventory().getArmorStack(0).getItem());
        ArmorItem leggings = ((ArmorItem)player.getInventory().getArmorStack(1).getItem());
        ArmorItem breastplate = ((ArmorItem)player.getInventory().getArmorStack(2).getItem());
        ArmorItem helmet = ((ArmorItem)player.getInventory().getArmorStack(3).getItem());

        return removeLayers(helmet.getMaterial().value().toString()).equals(removeLayers(material.toString())) &&
                removeLayers(breastplate.getMaterial().value().toString()).equals(removeLayers(material.toString())) &&
                removeLayers(leggings.getMaterial().value().toString()).equals(removeLayers(material.toString())) &&
                removeLayers(boots.getMaterial().value().toString()).equals(removeLayers(material.toString()));
    }
    // Helper function to remove the layers part of the string
    private String removeLayers(String materialString) {
        return materialString.replaceAll(", layers=\\[.*?]", "");
    }
}
