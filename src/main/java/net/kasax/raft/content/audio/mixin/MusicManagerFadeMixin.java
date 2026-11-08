package net.kasax.raft.content.audio.mixin;

import net.kasax.raft.content.audio.client.MusicDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 has no music volume hook (Minecraft.getMusicVolume of 26.3): the Leviathan's music that MusicDirector
 * silences outside the fight is stopped here instead.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerFadeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"))
    private void raft$audio$silence(CallbackInfo ci) {
        if (MusicDirector.musicVolume(this.minecraft, 1.0F) <= 0.0F) {
            this.stopPlaying();
        }
    }
}
