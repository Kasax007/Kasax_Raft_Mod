package net.kasax.raft.content.audio.client;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.building.block.WindTurbineBlock;
import net.kasax.raft.content.building.block.WindTurbineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;

/** The rotor of a wind turbine sweeping through the wind (a loop at the rotor, louder and higher the faster it turns). */
final class TurbineSound extends AbstractTickableSoundInstance {
    private final WindTurbineBlockEntity turbine;
    private float speed;
    /** Game time when the sound was started (the client renews a sound the engine dropped). */
    long started;

    TurbineSound(WindTurbineBlockEntity turbine) {
        super(AudioSounds.WIND_TURBINE_SPIN, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.turbine = turbine;
        this.looping = true;
        this.delay = 0;
        Direction facing = turbine.getBlockState().hasProperty(WindTurbineBlock.FACING)
                ? turbine.getBlockState().getValue(WindTurbineBlock.FACING) : Direction.NORTH;
        this.x = turbine.getBlockPos().getX() + 0.5 + facing.getStepX() * 0.8;
        this.y = turbine.getBlockPos().getY() + 0.5;
        this.z = turbine.getBlockPos().getZ() + 0.5 + facing.getStepZ() * 0.8;
        this.volume = 0.001F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    boolean isFor(WindTurbineBlockEntity be) {
        return this.turbine == be;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (this.turbine.isRemoved() || mc.player == null || mc.level != this.turbine.getLevel()
                || mc.player.distanceToSqr(this.x, this.y, this.z) > 48.0 * 48.0) {
            this.stop();
            return;
        }
        byte wind = this.turbine.getWind();
        float target = wind == WindTurbineBlockEntity.WIND_RUNNING ? 1.0F : wind == WindTurbineBlockEntity.WIND_TOO_LOW ? 0.3F : 0.0F;
        // the rotor speeds up and slows down over a few seconds, like the model
        this.speed += (target - this.speed) * 0.04F;
        this.volume = Math.max(0.001F, this.speed * 0.9F);
        this.pitch = 0.7F + 0.3F * this.speed;
        if (target == 0.0F && this.speed < 0.01F) {
            this.stop();
        }
    }
}
