package net.kasax.raft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Fabric's default attribute registry: handed to NeoForge in {@link EntityAttributeCreationEvent}. */
public final class FabricDefaultAttributeRegistry {
    private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> ATTRIBUTES = new LinkedHashMap<>();

    private FabricDefaultAttributeRegistry() {
    }

    public static void register(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder builder) {
        register(type, builder.build());
    }

    public static void register(EntityType<? extends LivingEntity> type, AttributeSupplier container) {
        ATTRIBUTES.put(type, container);
    }

    static void onAttributes(EntityAttributeCreationEvent event) {
        ATTRIBUTES.forEach(event::put);
    }
}
