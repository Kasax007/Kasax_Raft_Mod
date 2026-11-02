package net.kasax.raft.content.marine;

import net.kasax.raft.content.BcReg;
import net.minecraft.sounds.SoundEvent;

/** Custom sounds of the marine module (synthesised whale song and blowhole spout; see assets/raft/sounds/marine). */
public final class MarineSounds {
    public static SoundEvent WHALE_SONG;
    public static SoundEvent WHALE_SPOUT;

    private MarineSounds() {
    }

    static void register() {
        WHALE_SONG = BcReg.sound("entity.whale.song");
        WHALE_SPOUT = BcReg.sound("entity.whale.spout");
    }
}
