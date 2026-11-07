package net.kasax.raft.content.diving;

import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Map;

/** Armor materials of the diving gear and small helpers to ask what an entity is wearing. */
public final class DivingGear {
    private DivingGear() {
    }

    /** A piece of diving gear: slot, armor points, durability factor, enchantability and toughness. */
    public record Material(String name, EquipmentSlot slot, int defense, int durability, int enchantability, float toughness,
                           TagKey<Item> repair) {
        public net.kasax.raft.compat.GearItem.Gear gear() {
            return new net.kasax.raft.compat.GearItem.Gear(this.slot, this.enchantability, this.repair, DivingSounds.EQUIP_DIVING_GEAR);
        }

        /** Durability like vanilla armor: the slot's base durability times the factor. */
        public int maxDamage() {
            int base = switch (this.slot) {
                case HEAD -> 11;
                case CHEST -> 16;
                case LEGS -> 15;
                default -> 13;
            };
            return base * this.durability;
        }
    }

    public static final Material DIVING_HELMET = material("diving_helmet", EquipmentSlot.HEAD, 2, 15, 12, 0f);
    public static final Material OXYGEN_TANK = material("oxygen_tank", EquipmentSlot.CHEST, 3, 12, 9, 0f);
    public static final Material TITANIUM_OXYGEN_TANK = material("titanium_oxygen_tank", EquipmentSlot.CHEST, 6, 25, 15, 0f);
    public static final Material WETSUIT = material("wetsuit", EquipmentSlot.LEGS, 2, 10, 15, 0f);
    public static final Material FLIPPERS = material("flippers", EquipmentSlot.FEET, 1, 10, 15, 0f);

    /** Land penalty of the flippers (added while the wearer is out of water, see DivingEvents). */
    public static final ResourceLocation FLIPPERS_ON_LAND = Raft.id("flippers_on_land");

    private static Material material(String name, EquipmentSlot slot, int defense, int durability, int enchantability,
                                     float toughness) {
        TagKey<Item> repair = TagKey.create(Registries.ITEM, Raft.id("repairs_" + name));
        return new Material(name, slot, defense, durability, enchantability, toughness, repair);
    }

    /** Armor attributes of the material plus the gear's own bonus (wetsuit: mining, flippers: swimming). */
    public static ItemAttributeModifiers attributes(Material material) {
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(material.slot());
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(Raft.id("armor." + material.name()), material.defense(),
                        AttributeModifier.Operation.ADD_VALUE), group)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(Raft.id("armor." + material.name()), material.toughness(),
                        AttributeModifier.Operation.ADD_VALUE), group)
                .build();
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
