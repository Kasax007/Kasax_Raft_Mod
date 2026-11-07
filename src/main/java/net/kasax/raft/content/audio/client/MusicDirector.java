package net.kasax.raft.content.audio.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.audio.AudioMusic;
import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.audio.mixin.BossHealthOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Picks the music of the raft dimension on the client (called from {@code Minecraft.getSituationalMusic} by
 * MinecraftMusicMixin): the Leviathan's theme while its boss bar is shown, a place's theme inside a key place (sent by
 * the server), otherwise the biome's music from the environment attributes, where the sea's day theme becomes the
 * night theme after dusk. When the fight ends the boss theme fades out through the vanilla music fade.
 */
public final class MusicDirector {
    private static final ResourceLocation LEVIATHAN = Raft.id("leviathan");
    private static final String LEVIATHAN_NAME = "entity.raft.leviathan";
    static String place = "";
    private static boolean fight;

    private MusicDirector() {
    }

    /** Music that replaces the vanilla choice, or null to let the game decide (screens keep their own music). */
    public static @Nullable Music override(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Screen screen = mc.screen;
        if (screen != null && screen.getBackgroundMusic() != null) {
            return null;
        }
        if (fight) {
            return AudioMusic.LEVIATHAN;
        }
        return AudioMusic.forPlace(place);
    }

    /** The sea's day theme turns into the night theme between dusk and dawn. */
    public static @Nullable Music dayNight(Minecraft mc, @Nullable Music music) {
        if (music != null && mc.level != null && music.getEvent().is(AudioSounds.MUSIC_SEA_DAY.key()) && isNight(mc.level)) {
            return AudioMusic.SEA_NIGHT;
        }
        return music;
    }

    /** Music volume of the environment: 0 while the boss theme still plays after the fight, so it fades out. */
    public static float musicVolume(Minecraft mc, float volume) {
        if (!fight && mc.getMusicManager().isPlayingMusic(AudioMusic.LEVIATHAN)) {
            return 0.0F;
        }
        return volume;
    }

    public static boolean isNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= 12800L && time < 23200L;
    }

    public static boolean inFight() {
        return fight;
    }

    public static String place() {
        return place;
    }

    static void tick(Minecraft mc) {
        fight = mc.level != null && leviathanBarShown(mc);
    }

    static void reset() {
        place = "";
        fight = false;
    }

    /** True while a boss bar of the Leviathan is on screen (the bar's id is the entity's UUID). */
    private static boolean leviathanBarShown(Minecraft mc) {
        // 1.21.1: boss bars have their own ids, so the bar is known by the Leviathan's name
        for (LerpingBossEvent event : ((BossHealthOverlayAccessor) mc.gui.getBossOverlay()).raft$events().values()) {
            if (event.getName().getContents() instanceof TranslatableContents name && LEVIATHAN_NAME.equals(name.getKey())) {
                return true;
            }
        }
        return false;
    }
}
