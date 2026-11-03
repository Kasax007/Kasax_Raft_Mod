package net.kasax.raft.content.diving;

import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Armor materials of the diving gear and small helpers to ask what an entity is wearing. */
public final class DivingGear {
    private DivingGear() {
    }

    public static final ArmorMaterial DIVING_HELMET = material("diving_helmet", ArmorType.HELMET, 2, 15, 12, 0f);
    public static final ArmorMaterial OXYGEN_TANK = material("oxygen_tank", ArmorType.CHESTPLATE, 3, 12, 9, 0f);
    public static final ArmorMaterial TITANIUM_OXYGEN_TANK = material("titanium_oxygen_tank", ArmorType.CHESTPLATE, 6, 25, 15, 0f);
    public static final ArmorMaterial WETSUIT = material("wetsuit", ArmorType.LEGGINGS, 2, 10, 15, 0f);
    public static final ArmorMaterial FLIPPERS = material("flippers", ArmorType.BOOTS, 1, 10, 15, 0f);

    /** Land penalty of the flippers (added while the wearer is out of water, see DivingEvents). */
    public static final Identifier FLIPPERS_ON_LAND = Raft.id("flippers_on_land");

    private static ArmorMaterial material(String name, ArmorType type, int defense, int durability, int enchantability,
                                          float toughness) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Raft.id(name));
        TagKey<Item> repair = TagKey.create(Registries.ITEM, Raft.id("repairs_" + name));
        return new ArmorMaterial(durability, Map.of(type, defense), enchantability, DivingSounds.EQUIP_DIVING_GEAR,
                toughness, 0f, repair, asset);
    }

    /** Armor attributes of the material plus the gear's own bonus (wetsuit: mining, flippers: swimming). */
    public static ItemAttributeModifiers attributes(ArmorMaterial material, ArmorType type) {
        ItemAttributeModifiers modifiers = material.createAttributes(type);
        if (material == WETSUIT) {
            // submerged_mining_speed has the base 0.2: +200 % of the base = x3
            modifiers = modifiers.withModifierAdded(Attributes.SUBMERGED_MINING_SPEED,
                    new AttributeModifier(Raft.id("wetsuit_mining"), 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.LEGS);
        } else if (material == FLIPPERS) {
            modifiers = modifiers.withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY,
                    new AttributeModifier(Raft.id("flippers_swimming"), 0.6, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.FEET);
        }
        return modifiers;
    }

    // ------------------------------------------------------------------------------------------- queries
    public static boolean wearsHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(DivingContent.DIVING_HELMET);
    }

    /** True if the entity wears a diving helmet with a lamp fitted. */
    public static boolean wearsLampHelmet(LivingEntity entity) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(DivingContent.DIVING_HELMET) && head.has(DivingContent.DIVING_LAMP);
    }

    /** The oxygen tank worn on the chest, or an empty stack. */
    public static ItemStack tank(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof OxygenTankItem ? chest : ItemStack.EMPTY;
    }

    public static boolean wearsWetsuit(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.LEGS).is(DivingContent.WETSUIT);
    }

    public static boolean wearsFlippers(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(DivingContent.FLIPPERS);
    }

    /** Helmet, any tank, wetsuit and flippers. */
    public static boolean fullSet(LivingEntity entity) {
        return wearsHelmet(entity) && !tank(entity).isEmpty() && wearsWetsuit(entity) && wearsFlippers(entity);
    }

    /** True if the entity breathes from its tank right now (helmet on, tank with air left). */
    public static boolean breathesFromTank(LivingEntity entity) {
        if (!wearsHelmet(entity)) {
            return false;
        }
        ItemStack tank = tank(entity);
        return !tank.isEmpty() && OxygenTankItem.getAir(tank) > 0;
    }
}
