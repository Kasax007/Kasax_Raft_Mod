package net.kasax.raft.content.audio.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kasax.raft.Raft;
import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.audio.net.PlacePayload;
import net.kasax.raft.content.building.block.WindTurbineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side of the audio module: receives the player's key place for the {@link MusicDirector}, keeps the sea
 * ambience playing while the player is at the open sea of the raft dimension (with seagulls far away by day) and gives
 * every running wind turbine nearby its rotor sound.
 */
public final class AudioClient {
    private static final Identifier RAFT_DIMENSION = Raft.id("raftdim");
    private static SeaAmbience sea;
    private static long seaStarted;
    private static final List<TurbineSound> TURBINES = new ArrayList<>();
    private static float openSea;

    private AudioClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PlacePayload.TYPE, (payload, context) -> MusicDirector.place = payload.place());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MusicDirector.reset();
            sea = null;
            TURBINES.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(AudioClient::tick);
    }

    /** How much open sea the player hears now (0..1), for tests and the sea loop. */
    public static float openSea() {
        return openSea;
    }

    private static void tick(Minecraft mc) {
        MusicDirector.tick(mc);
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            return;
        }
        boolean raftDim = level.dimension().identifier().equals(RAFT_DIMENSION);
        if (level.getGameTime() % 10 == 0) {
            openSea = raftDim ? measureOpenSea(level, player) : 0.0F;
            turbines(mc, level, player);
        }
        // a new loop when the old one ended, or when the sound engine dropped it (reload of sounds or device)
        if (sea != null && (sea.isStopped() || level.getGameTime() - seaStarted > 100 && !mc.getSoundManager().isActive(sea))) {
            sea = null;
        }
        if (openSea > 0.02F && sea == null) {
            sea = new SeaAmbience();
            seaStarted = level.getGameTime();
            mc.getSoundManager().play(sea);
        }
        if (sea != null) {
            sea.target = openSea;
        }
        // seagulls far away over the water, by day
        if (openSea > 0.4F && !MusicDirector.isNight(level) && level.getRandom().nextInt(1800) == 0) {
            float vol = 0.4F + 0.5F * openSea * level.getRandom().nextFloat();
            mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(AudioSounds.AMBIENT_GULLS,
                    0.9F + level.getRandom().nextFloat() * 0.2F, vol));
        }
    }

    /**
     * 0..1: water at sea level around the player (12 points on a circle), times nearness to the surface, times sky
     * light at the head; 0 with the head under water (the game's own underwater ambience takes over).
     */
    private static float measureOpenSea(ClientLevel level, LocalPlayer player) {
        if (player.isEyeInFluid(FluidTags.WATER)) {
            return 0.0F;
        }
        int sea = level.getSeaLevel();
        double height = player.getEyeY() - sea;
        if (height > 48.0 || height < -24.0) {
            return 0.0F;
        }
        int water = 0;
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            BlockPos p = BlockPos.containing(player.getX() + Math.cos(a) * 10.0, sea - 1, player.getZ() + Math.sin(a) * 10.0);
            if (level.isLoaded(p) && level.getFluidState(p).is(FluidTags.WATER)) {
                water++;
            }
        }
        float around = water / 12.0F;
        float near = (float) Mth.clamp(1.0 - Math.abs(height - 2.0) / 40.0, 0.0, 1.0);
        float sky = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition())) / 15.0F;
        return around * near * (0.25F + 0.75F * sky);
    }

    /** A rotor sound for each wind turbine within 24 blocks that turns (the sound stops itself when it does not). */
    private static void turbines(Minecraft mc, ClientLevel level, LocalPlayer player) {
        TURBINES.removeIf(s -> s.isStopped() || level.getGameTime() - s.started > 100 && !mc.getSoundManager().isActive(s));
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof WindTurbineBlockEntity turbine && turbine.getWind() != WindTurbineBlockEntity.WIND_BLOCKED
                            && be.getBlockPos().distToCenterSqr(player.position()) < 24.0 * 24.0
                            && TURBINES.stream().noneMatch(s -> s.isFor(turbine))) {
                        TurbineSound sound = new TurbineSound(turbine);
                        sound.started = level.getGameTime();
                        TURBINES.add(sound);
                        mc.getSoundManager().play(sound);
                    }
                }
            }
        }
    }
}
