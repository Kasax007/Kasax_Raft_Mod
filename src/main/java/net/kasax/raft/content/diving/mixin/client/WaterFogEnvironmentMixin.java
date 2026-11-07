package net.kasax.raft.content.diving.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kasax.raft.content.diving.client.DivingClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With the diving helmet on, the water fog starts later and ends much farther away (clear porthole view). */
@Mixin(FogRenderer.class)
public abstract class WaterFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void raft$helmetVision(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thickFog,
                                          float partialTick, CallbackInfo ci) {
        if (camera.getFluidInCamera() != FogType.WATER || !DivingClient.cameraWearsHelmet(camera)) {
            return;
        }
        float baseEnd = RenderSystem.getShaderFogEnd();
        // the helmet needs no time to get used to the water and sees about 2.2x as far
        float end = Math.min(baseEnd * DivingClient.HELMET_FOG_FACTOR, Math.max(renderDistance, baseEnd));
        RenderSystem.setShaderFogStart(Math.max(RenderSystem.getShaderFogStart(), end * 0.1F));
        RenderSystem.setShaderFogEnd(end);
    }
}
