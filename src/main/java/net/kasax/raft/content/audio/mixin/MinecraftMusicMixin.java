package net.kasax.raft.content.audio.mixin;

import net.kasax.raft.content.audio.client.MusicDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks the music director into the game's music choice and the environment's music volume. */
@Mixin(Minecraft.class)
public abstract class MinecraftMusicMixin {
    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
    private void raft$audio$override(CallbackInfoReturnable<Music> cir) {
        Music music = MusicDirector.override((Minecraft) (Object) this);
        if (music != null) {
            cir.setReturnValue(music);
        }
    }

    @Inject(method = "getSituationalMusic", at = @At("RETURN"), cancellable = true)
    private void raft$audio$dayNight(CallbackInfoReturnable<Music> cir) {
        Music music = cir.getReturnValue();
        Music swapped = MusicDirector.dayNight((Minecraft) (Object) this, music);
        if (swapped != music) {
            cir.setReturnValue(swapped);
        }
    }
}
