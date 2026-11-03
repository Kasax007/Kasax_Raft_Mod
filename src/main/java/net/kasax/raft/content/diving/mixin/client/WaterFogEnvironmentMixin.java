package net.kasax.raft.content.diving.mixin.client;

import net.kasax.raft.content.diving.client.DivingClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With the diving helmet on, the water fog starts later and ends much farther away (clear porthole view). */
@Mixin(WaterFogEnvironment.class)
public abstract class WaterFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private void raft$helmetVision(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker,
                                   CallbackInfo ci) {
        if (!DivingClient.cameraWearsHelmet(camera)) {
            return;
        }
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        float baseEnd = camera.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_END_DISTANCE, partialTicks);
        // the helmet needs no time to get used to the water and sees about 2.2x as far
        float end = Math.min(Math.max(baseEnd * DivingClient.HELMET_FOG_FACTOR, fog.environmentalEnd), Math.max(renderDistance, baseEnd));
        fog.environmentalStart = Math.max(fog.environmentalStart, end * 0.1F);
        fog.environmentalEnd = end;
        fog.skyEnd = end;
        fog.cloudEnd = end;
    }
}
