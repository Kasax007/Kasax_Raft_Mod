package net.kasax.raft.sound;

import net.kasax.raft.Raft;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent Portal_Teleport_Sound = registerSoundEvent("portal_teleport_sound");
    public static final RegistryKey<JukeboxSong> Some_Game_Music_Seth = registerJukeboxSong("some_game_music_seth");
    public static final RegistryKey<JukeboxSong> Lofi_Beat_Seth = registerJukeboxSong("lofi_beat_seth");


    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(Raft.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static RegistryKey<JukeboxSong> registerJukeboxSong(String name) {
        Identifier id = Identifier.of(Raft.MOD_ID, name);
        return RegistryKey.of(RegistryKeys.JUKEBOX_SONG, id);
    }


    public static void registerSounds() {
        Raft.LOGGER.info("Registering Sounds for " + Raft.MOD_ID);
    }
}
