package net.kasax.raft.sound;

import net.kasax.raft.Raft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent Portal_Teleport_Sound = registerSoundEvent("portal_teleport_sound");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Raft.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerSounds() {
        Raft.LOGGER.info("Registering Sounds for " + Raft.MOD_ID);
    }
}
