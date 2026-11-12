package net.kasax.raft.platform.client;

import net.kasax.raft.platform.Event;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Fabric's LivingEntityRenderLayerRegistrationCallback on NeoForge's {@link EntityRenderersEvent.AddLayers}: called for
 * every living entity renderer and every player model, when the renderers are built.
 */
@FunctionalInterface
public interface LivingEntityRenderLayerRegistrationCallback {
    Event<LivingEntityRenderLayerRegistrationCallback> EVENT = new Event<>(() -> {
    });

    void registerRenderLayers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?, ?> entityRenderer,
                              RegistrationHelper registrationHelper, EntityRendererProvider.Context context);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        if (EVENT.listeners().isEmpty()) {
            return;
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> living) {
                fire((EntityType<? extends LivingEntity>) type, living, event.getContext());
            }
        }
        for (var skin : event.getSkins()) {
            EntityRenderer<?, ?> renderer = event.getPlayerRenderer(skin);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> living) {
                fire(net.minecraft.world.entity.EntityTypes.PLAYER, living, event.getContext());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fire(EntityType<? extends LivingEntity> type, LivingEntityRenderer<?, ?, ?> renderer, EntityRendererProvider.Context context) {
        RegistrationHelper helper = new RegistrationHelper() {
            @Override
            public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void register(RenderLayer<S, M> renderLayer) {
                ((LivingEntityRenderer) renderer).addLayer(renderLayer);
            }
        };
        for (LivingEntityRenderLayerRegistrationCallback l : EVENT.listeners()) {
            l.registerRenderLayers(type, renderer, helper, context);
        }
    }

    interface RegistrationHelper {
        <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void register(RenderLayer<S, M> renderLayer);
    }
}
