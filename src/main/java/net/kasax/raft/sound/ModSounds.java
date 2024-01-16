package net.kasax.raft.sound;

import net.kasax.raft.Raft;
import net.minecraft.client.sound.Sound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent Portal_Teleport_Sound = registerSoundEvent("portal_teleport_sound");
    public static final SoundEvent Some_Game_Music_Seth = registerSoundEvent("some_game_music_seth");
    public static final SoundEvent Lofi_Beat_Seth = registerSoundEvent("lofi_beat_seth");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(Raft.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void registerSounds() {
        Raft.LOGGER.info("Registering Sounds for " + Raft.MOD_ID);
    }
}
