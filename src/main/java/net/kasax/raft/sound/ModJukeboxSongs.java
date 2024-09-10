package net.kasax.raft.sound;

import net.kasax.raft.Raft;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ModJukeboxSongs {
    public static final RegistryKey<JukeboxSong> LOFI_BEAT_SETH = of("lofi_beat_seth");
    public static final RegistryKey<JukeboxSong> SOME_GAME_MUSIC_SETH = of("some_game_music_seth");

    private static RegistryKey<JukeboxSong> of(String id) {
        return RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(Raft.MOD_ID, id));
    }

    private static void register(Registerable<JukeboxSong> registry, RegistryKey<JukeboxSong> key, RegistryEntry.Reference<SoundEvent> soundEvent, int lengthInSeconds, int comparatorOutput) {
        registry.register(key, new JukeboxSong(soundEvent, Text.translatable(Util.createTranslationKey("jukebox_song", key.getValue())), (float)lengthInSeconds, comparatorOutput));
    }

    public static void bootstrap(Registerable<JukeboxSong> registry) {
        register(registry, LOFI_BEAT_SETH, LOFI_BEAT_SETH_EVENT, 120, 7);
        register(registry, SOME_GAME_MUSIC_SETH, SOME_GAME_MUSIC_SETH_EVENT, 105, 7);

    }

    public static final RegistryEntry.Reference<SoundEvent> LOFI_BEAT_SETH_EVENT = registerReference1("music_disc.lofi_beat_seth");
    public static final RegistryEntry.Reference<SoundEvent> SOME_GAME_MUSIC_SETH_EVENT = registerReference1("music_disc.some_game_music_seth");

    private static RegistryEntry.Reference<SoundEvent> registerReference1(String id) {
        return registerReference2(Identifier.of(Raft.MOD_ID, id));
    }

    private static RegistryEntry.Reference<SoundEvent> registerReference2(Identifier id) {
        return registerReference3(id, id);
    }

    private static RegistryEntry.Reference<SoundEvent> registerReference3(Identifier id, Identifier soundId) {
        return Registry.registerReference(Registries.SOUND_EVENT, id, SoundEvent.of(soundId));
    }
}
