package net.kasax.raft.content.audio;

import net.minecraft.sounds.Music;
import org.jetbrains.annotations.Nullable;

/**
 * The music of the raft dimension. The sea themes and the trench theme come from the biomes' and the dimension's
 * {@code minecraft:audio/background_music} attribute (data); the client swaps the day theme for the night theme after
 * dusk, plays a place's theme inside Port Solace, Saltmere, Helix Station, the Old Lighthouse and the Tidewarden
 * temples (the server tells it where the player is, see {@link AudioPlaces}) and the Leviathan's theme while its boss
 * bar is shown.
 */
public final class AudioMusic {
    /** Delays in ticks: the sea themes come back after 2..6 minutes of silence, a place's theme within 0.5..2 minutes. */
    public static final int SEA_MIN_DELAY = 2400;
    public static final int SEA_MAX_DELAY = 7200;
    public static final int PLACE_MIN_DELAY = 600;
    public static final int PLACE_MAX_DELAY = 2400;

    public static Music SEA_DAY;
    public static Music SEA_NIGHT;
    public static Music PORT_SOLACE;
    public static Music SALTMERE;
    public static Music SANCTUM;
    public static Music HELIX;
    public static Music LIGHTHOUSE;
    /** The boss fight: replaces whatever plays at once and loops without a pause. */
    public static Music LEVIATHAN;

    private AudioMusic() {
    }

    static void register() {
        SEA_DAY = new Music(AudioSounds.MUSIC_SEA_DAY, SEA_MIN_DELAY, SEA_MAX_DELAY, false);
        SEA_NIGHT = new Music(AudioSounds.MUSIC_SEA_NIGHT, SEA_MIN_DELAY, SEA_MAX_DELAY, false);
        PORT_SOLACE = new Music(AudioSounds.MUSIC_PORT_SOLACE, PLACE_MIN_DELAY, PLACE_MAX_DELAY, false);
        SALTMERE = new Music(AudioSounds.MUSIC_SALTMERE, PLACE_MIN_DELAY, PLACE_MAX_DELAY, false);
        SANCTUM = new Music(AudioSounds.MUSIC_SANCTUM, PLACE_MIN_DELAY, PLACE_MAX_DELAY, false);
        HELIX = new Music(AudioSounds.MUSIC_HELIX, PLACE_MIN_DELAY, PLACE_MAX_DELAY, false);
        LIGHTHOUSE = new Music(AudioSounds.MUSIC_LIGHTHOUSE, PLACE_MIN_DELAY, PLACE_MAX_DELAY, false);
        LEVIATHAN = new Music(AudioSounds.MUSIC_LEVIATHAN, 0, 0, true);
    }

    /** The theme of a place id sent by the server ({@link AudioPlaces}), or null. */
    public static @Nullable Music forPlace(String place) {
        return switch (place) {
            case AudioPlaces.PORT_SOLACE -> PORT_SOLACE;
            case AudioPlaces.SALTMERE -> SALTMERE;
            case AudioPlaces.SANCTUM, AudioPlaces.CORAL_TEMPLE -> SANCTUM;
            case AudioPlaces.HELIX -> HELIX;
            case AudioPlaces.LIGHTHOUSE -> LIGHTHOUSE;
            default -> null;
        };
    }
}
