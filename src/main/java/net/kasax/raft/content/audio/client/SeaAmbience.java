package net.kasax.raft.content.audio.client;

import net.kasax.raft.content.audio.AudioSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The open sea around the player (a seamless stereo loop of waves lapping at the raft, wind over the water). Its
 * volume follows {@link AudioClient}'s estimate of how much open sea there is around the player (water at sea level,
 * height above it, sky above, head not under water) and fades smoothly; it stops itself when it is no longer wanted.
 */
final class SeaAmbience extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 0.85F;
    float target;
    private float level;

    SeaAmbience() {
        super(AudioSounds.AMBIENT_SEA, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.001F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        // about 2 s to fade in or out
        this.level += (this.target - this.level) * 0.03F;
        this.volume = Math.max(0.001F, this.level * MAX_VOLUME);
        if (this.target <= 0.0F && this.level < 0.005F) {
            this.stop();
        }
    }

    void end() {
        this.target = 0.0F;
    }
}
