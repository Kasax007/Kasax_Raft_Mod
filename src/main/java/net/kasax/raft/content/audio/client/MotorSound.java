package net.kasax.raft.content.audio.client;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.voyage.ExpeditionRaft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The outboard motor of a raft (a built raft with a Raft Motor or the Expedition Raft with its engine upgrade): a loop
 * that follows the raft, fades in when the engine starts, rises in pitch and volume with the speed and fades out when
 * it stops. 3.1.0 played a note block bass drum every few ticks.
 */
final class MotorSound extends AbstractTickableSoundInstance {
    private final Entity boat;
    private float level;
    /** Game time when the sound was started (the client renews a sound the engine dropped). */
    long started;

    MotorSound(Entity boat) {
        super(AudioSounds.MOTOR_RUN, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.boat = boat;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.pitch = 0.8F;
        this.x = boat.getX();
        this.y = boat.getY() + 0.5;
        this.z = boat.getZ();
    }

    static boolean running(Entity e) {
        return e instanceof VesselEntity v ? v.isMotorRunning() : e instanceof ExpeditionRaft r && r.isEngineRunning();
    }

    static float speed(Entity e) {
        return e instanceof VesselEntity v ? Math.abs(v.getSpeed()) : (float) e.getDeltaMovement().horizontalDistance();
    }

    boolean isFor(Entity e) {
        return this.boat == e;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (this.boat.isRemoved() || mc.player == null || mc.level != this.boat.level()
                || mc.player.distanceToSqr(this.boat) > 56.0 * 56.0) {
            this.stop();
            return;
        }
        boolean on = running(this.boat);
        this.level += ((on ? 1.0F : 0.0F) - this.level) * (on ? 0.15F : 0.08F);
        float fast = Mth.clamp(speed(this.boat) / 0.45F, 0.0F, 1.0F);
        this.volume = Math.max(0.001F, this.level * (0.55F + 0.25F * fast));
        this.pitch = 0.85F + 0.35F * fast;
        this.x = this.boat.getX();
        this.y = this.boat.getY() + 0.5;
        this.z = this.boat.getZ();
        if (!on && this.level < 0.01F) {
            this.stop();
        }
    }
}
