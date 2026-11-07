package net.kasax.raft.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Logs every sound the client starts while a clip is recorded ({@code rec}; the test client has no audio device):
 * which sound file, when (seconds of game time since the clip started), volume, pitch, position, attenuation and the
 * camera position. Written to {@code <clip>/sounds.jsonl} plus {@code <clip>/shot.json} (frames per second of game
 * time), the format the video tools mix the real game sounds from (same as the Dreamcraft film harness).
 */
public final class FilmSound {
    private static Writer out;
    private static long startTick;

    private FilmSound() {
    }

    /** @param gameTime the client level's game time when the clip starts (read on the client thread) */
    public static synchronized void start(Path dir, double fps, long gameTime) {
        stop();
        try {
            Files.createDirectories(dir);
            // realtime: recorded with the world held while each frame is read back, so a clip second is a game second
            // (clips without it ran three times faster than the game: two ticks per frame at 30 fps)
            Files.writeString(dir.resolve("shot.json"), String.format(Locale.ROOT, "{\"fps\": %s, \"realtime\": true}%n", fps));
            out = Files.newBufferedWriter(dir.resolve("sounds.jsonl"), StandardCharsets.UTF_8);
            startTick = gameTime;
        } catch (IOException e) {
            BcScript.LOG.warn("[bctest] sound log: {}", e.toString());
            out = null;
        }
    }

    public static synchronized void stop() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
            out = null;
        }
    }

    public static synchronized void log(SoundManager manager, SoundInstance s) {
        if (out == null) return;
        try {
            if (s.resolve(manager) == null) return;
            Sound sound = s.getSound();
            if (sound == null) return;
            Minecraft mc = Minecraft.getInstance();
            double t = mc.level == null ? 0 : (mc.level.getGameTime() - startTick) / 20.0;
            Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
            out.write(String.format(Locale.ROOT,
                    "{\"t\": %.3f, \"event\": \"%s\", \"file\": \"%s\", \"volume\": %.3f, \"pitch\": %.3f, \"x\": %.2f, \"y\": %.2f, \"z\": %.2f, "
                            + "\"attenuation\": \"%s\", \"relative\": %s, \"distance\": %d, \"source\": \"%s\", \"cam_x\": %.2f, \"cam_y\": %.2f, \"cam_z\": %.2f}\n",
                    t, s.getLocation(), sound.getPath(), s.getVolume(), s.getPitch(), s.getX(), s.getY(), s.getZ(),
                    s.getAttenuation().name(), s.isRelative(), sound.getAttenuationDistance(), s.getSource().getName(), cam.x, cam.y, cam.z));
            out.flush();
        } catch (Exception e) {
            BcScript.LOG.warn("[bctest] sound log entry failed: {}", e.toString());
        }
    }
}
