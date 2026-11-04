package net.kasax.raft.content.audio;

import net.kasax.raft.content.BcReg;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events of the audio module (the synthesised sounds in assets/raft/sounds/sfx, the music in sounds/music; made
 * by {@code tools/content/audio}). Events that already existed (Leviathan, crabs, seagulls, diving gear) stay in their
 * modules and only point at the new files.
 */
public final class AudioSounds {
    // creatures
    public static SoundEvent SHARK_AMBIENT;
    public static SoundEvent SHARK_HURT;
    public static SoundEvent SHARK_DEATH;
    public static SoundEvent SHARK_BITE;
    public static SoundEvent SHARK_GNAW;
    public static SoundEvent SHARK_RAFT_BREAK;
    public static SoundEvent WHALE_HURT;
    public static SoundEvent WHALE_DEATH;
    public static SoundEvent MANTA_HURT;
    public static SoundEvent MANTA_DEATH;
    public static SoundEvent JELLYFISH_STING;
    public static SoundEvent JELLYFISH_HURT;
    public static SoundEvent JELLYFISH_DEATH;
    public static SoundEvent ANGLERFISH_AMBIENT;
    public static SoundEvent ANGLERFISH_BITE;
    public static SoundEvent ANGLERFISH_HURT;
    public static SoundEvent ANGLERFISH_DEATH;
    public static SoundEvent MORAY_BITE;
    // blocks and items
    public static SoundEvent PURIFIER_BOIL;
    public static SoundEvent GRILL_SIZZLE;
    public static SoundEvent WIND_TURBINE_SPIN;
    public static SoundEvent CANNON_FIRE;
    public static SoundEvent TERMINAL_USE;
    public static SoundEvent CONTROL_PANEL_BEEP;
    public static SoundEvent HAMMOCK_CREAK;
    public static SoundEvent PLASTIC_HOOK_REEL;
    // story
    public static SoundEvent ALTAR_INSERT;
    public static SoundEvent ALTAR_FORGE;
    public static SoundEvent SEAL_OPEN;
    public static SoundEvent SEAL_LOCKED;
    public static SoundEvent LEVIATHAN_SUMMON;
    // ambience
    public static SoundEvent AMBIENT_SEA;
    public static SoundEvent AMBIENT_GULLS;
    public static Holder.Reference<SoundEvent> AMBIENT_DEEP;
    // music (holders: a Music record needs one)
    public static Holder.Reference<SoundEvent> MUSIC_SEA_DAY;
    public static Holder.Reference<SoundEvent> MUSIC_SEA_NIGHT;
    public static Holder.Reference<SoundEvent> MUSIC_PORT_SOLACE;
    public static Holder.Reference<SoundEvent> MUSIC_SALTMERE;
    public static Holder.Reference<SoundEvent> MUSIC_ABYSS;
    public static Holder.Reference<SoundEvent> MUSIC_SANCTUM;
    public static Holder.Reference<SoundEvent> MUSIC_HELIX;
    public static Holder.Reference<SoundEvent> MUSIC_LIGHTHOUSE;
    public static Holder.Reference<SoundEvent> MUSIC_LEVIATHAN;

    private AudioSounds() {
    }

    static void register() {
        SHARK_AMBIENT = BcReg.sound("entity.shark.ambient");
        SHARK_HURT = BcReg.sound("entity.shark.hurt");
        SHARK_DEATH = BcReg.sound("entity.shark.death");
        SHARK_BITE = BcReg.sound("entity.shark.bite");
        SHARK_GNAW = BcReg.sound("entity.shark.gnaw");
        SHARK_RAFT_BREAK = BcReg.sound("entity.shark.raft_break");
        WHALE_HURT = BcReg.sound("entity.whale.hurt");
        WHALE_DEATH = BcReg.sound("entity.whale.death");
        MANTA_HURT = BcReg.sound("entity.manta_ray.hurt");
        MANTA_DEATH = BcReg.sound("entity.manta_ray.death");
        JELLYFISH_STING = BcReg.sound("entity.jellyfish.sting");
        JELLYFISH_HURT = BcReg.sound("entity.jellyfish.hurt");
        JELLYFISH_DEATH = BcReg.sound("entity.jellyfish.death");
        ANGLERFISH_AMBIENT = BcReg.sound("entity.anglerfish.ambient");
        ANGLERFISH_BITE = BcReg.sound("entity.anglerfish.bite");
        ANGLERFISH_HURT = BcReg.sound("entity.anglerfish.hurt");
        ANGLERFISH_DEATH = BcReg.sound("entity.anglerfish.death");
        MORAY_BITE = BcReg.sound("entity.moray_eel.bite");
        PURIFIER_BOIL = BcReg.sound("block.water_purifier.boil");
        GRILL_SIZZLE = BcReg.sound("block.grill.sizzle");
        WIND_TURBINE_SPIN = BcReg.sound("block.wind_turbine.spin");
        CANNON_FIRE = BcReg.sound("block.ship_cannon.fire");
        TERMINAL_USE = BcReg.sound("block.research_terminal.use");
        CONTROL_PANEL_BEEP = BcReg.sound("block.control_panel.beep");
        HAMMOCK_CREAK = BcReg.sound("block.hammock.creak");
        PLASTIC_HOOK_REEL = BcReg.sound("item.plastic_hook.reel");
        ALTAR_INSERT = BcReg.sound("block.tidewarden_altar.insert");
        ALTAR_FORGE = BcReg.sound("block.tidewarden_altar.forge");
        SEAL_OPEN = BcReg.sound("block.tide_seal.open");
        SEAL_LOCKED = BcReg.sound("block.tide_seal.locked");
        LEVIATHAN_SUMMON = BcReg.sound("block.leviathan_altar.summon");
        AMBIENT_SEA = BcReg.sound("ambient.raft.sea");
        AMBIENT_GULLS = BcReg.sound("ambient.raft.gulls");
        AMBIENT_DEEP = BcReg.soundHolder("ambient.raft.deep");
        MUSIC_SEA_DAY = BcReg.soundHolder("music.raft.sea_day");
        MUSIC_SEA_NIGHT = BcReg.soundHolder("music.raft.sea_night");
        MUSIC_PORT_SOLACE = BcReg.soundHolder("music.raft.port_solace");
        MUSIC_SALTMERE = BcReg.soundHolder("music.raft.saltmere");
        MUSIC_ABYSS = BcReg.soundHolder("music.raft.abyss");
        MUSIC_SANCTUM = BcReg.soundHolder("music.raft.sanctum");
        MUSIC_HELIX = BcReg.soundHolder("music.raft.helix");
        MUSIC_LIGHTHOUSE = BcReg.soundHolder("music.raft.lighthouse");
        MUSIC_LEVIATHAN = BcReg.soundHolder("music.raft.leviathan");
    }
}
