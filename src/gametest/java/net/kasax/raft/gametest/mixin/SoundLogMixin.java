package net.kasax.raft.gametest.mixin;

import net.kasax.raft.gametest.FilmSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every sound the game starts goes through SoundManager.play: log it while a clip is recorded (there is no audio device). */
@Mixin(SoundManager.class)
public class SoundLogMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void bctest$log(SoundInstance instance, CallbackInfo cir) {
        FilmSound.log((SoundManager) (Object) this, instance);
    }
}
