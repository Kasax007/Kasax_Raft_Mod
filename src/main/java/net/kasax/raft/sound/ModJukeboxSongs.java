package net.kasax.raft.sound;

import net.kasax.raft.Raft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

/**
 * Keys of the three music disc songs (the song definitions themselves are data pack files under
 * data/raft/jukebox_song) and the sound events they play.
 */
public class ModJukeboxSongs {
    public static final ResourceKey<JukeboxSong> LOFI_BEAT_SETH = of("lofi_beat_seth");
    public static final ResourceKey<JukeboxSong> CRYSTAL_CASTLES = of("crystal_castles");
    public static final ResourceKey<JukeboxSong> SOME_GAME_MUSIC_SETH = of("some_game_music_seth");

    private static ResourceKey<JukeboxSong> of(String id) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, Raft.id(id));
    }

    public static final Holder.Reference<SoundEvent> LOFI_BEAT_SETH_EVENT = registerSound("music_disc.lofi_beat_seth");
    public static final Holder.Reference<SoundEvent> CRYSTAL_CASTLES_EVENT = registerSound("music_disc.crystal_castles");
    public static final Holder.Reference<SoundEvent> SOME_GAME_MUSIC_SETH_EVENT = registerSound("music_disc.some_game_music_seth");

    private static Holder.Reference<SoundEvent> registerSound(String id) {
        Identifier identifier = Raft.id(id);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }
}
