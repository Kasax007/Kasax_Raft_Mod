package net.kasax.raft.platform.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Fabric's EntityModelLayerRegistry, handed to NeoForge in {@link EntityRenderersEvent.RegisterLayerDefinitions}. */
public final class EntityModelLayerRegistry {
    private static final Map<ModelLayerLocation, TexturedModelDataProvider> LAYERS = new LinkedHashMap<>();

    private EntityModelLayerRegistry() {
    }

    public static void registerModelLayer(ModelLayerLocation modelLayer, TexturedModelDataProvider provider) {
        LAYERS.put(modelLayer, provider);
    }

    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LAYERS.forEach((layer, provider) -> event.registerLayerDefinition(layer, provider::createModelData));
    }

    @FunctionalInterface
    public interface TexturedModelDataProvider {
        LayerDefinition createModelData();
    }
}
