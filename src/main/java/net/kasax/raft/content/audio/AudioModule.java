package net.kasax.raft.content.audio;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.kasax.raft.content.ContentModule;
import net.kasax.raft.content.audio.net.PlacePayload;

/**
 * Module "audio": the synthesised sound effects and the music of Beach Craft (sounds, music, ambience; all files and
 * the sound events made by {@code tools/content/audio}, the music by Kevin MacLeod under CC BY 4.0, see
 * CREDITS_MUSIC.txt). Client side: the music director (day/night sea themes, place themes, the Leviathan's theme), the
 * sea ambience around the raft and the wind turbines' hum. See docs/design/sound-assessment.md.
 */
public class AudioModule implements ContentModule {
    @Override
    public String name() {
        return "audio";
    }

    @Override
    public void init() {
        AudioSounds.register();
        AudioMusic.register();
        PayloadTypeRegistry.playS2C().register(PlacePayload.TYPE, PlacePayload.CODEC);
        AudioPlaces.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.audio.client.AudioClient.init();
    }
}
