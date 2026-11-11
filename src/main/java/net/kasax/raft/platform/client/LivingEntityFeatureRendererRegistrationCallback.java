package net.kasax.raft.platform.client;

import net.kasax.raft.platform.Event;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Fabric's LivingEntityFeatureRendererRegistrationCallback on NeoForge's {@link EntityRenderersEvent.AddLayers}: called
 * for every living entity renderer and every player model, when the renderers are built.
 */
@FunctionalInterface
public interface LivingEntityFeatureRendererRegistrationCallback {
    Event<LivingEntityFeatureRendererRegistrationCallback> EVENT = new Event<>(() -> {
    });

    void registerRenderers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?> entityRenderer,
                           RegistrationHelper registrationHelper, EntityRendererProvider.Context context);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        if (EVENT.listeners().isEmpty()) {
            return;
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                fire((EntityType<? extends LivingEntity>) type, living, event.getContext());
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                fire(EntityType.PLAYER, living, event.getContext());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fire(EntityType<? extends LivingEntity> type, LivingEntityRenderer<?, ?> renderer, EntityRendererProvider.Context context) {
        RegistrationHelper helper = new RegistrationHelper() {
            @Override
            public <T extends LivingEntity> void register(RenderLayer<T, ? extends EntityModel<T>> featureRenderer) {
                ((LivingEntityRenderer) renderer).addLayer(featureRenderer);
            }
        };
        for (LivingEntityFeatureRendererRegistrationCallback l : EVENT.listeners()) {
            l.registerRenderers(type, renderer, helper, context);
        }
    }

    interface RegistrationHelper {
        <T extends LivingEntity> void register(RenderLayer<T, ? extends EntityModel<T>> featureRenderer);
    }
}
