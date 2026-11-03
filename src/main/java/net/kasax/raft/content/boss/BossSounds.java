package net.kasax.raft.content.boss;

import net.kasax.raft.content.BcReg;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events of the boss module. They are defined in the module's sounds.json fragment on top of deep-pitched
 * vanilla recordings (so every sound has its own subtitle, e.g. "Leviathan roars").
 */
public final class BossSounds {
    public static SoundEvent LEVIATHAN_AMBIENT;
    public static SoundEvent LEVIATHAN_HURT;
    public static SoundEvent LEVIATHAN_DEATH;
    public static SoundEvent LEVIATHAN_ROAR;
    public static SoundEvent LEVIATHAN_CHARGE;
    public static SoundEvent LEVIATHAN_SPIT;
    public static SoundEvent LEVIATHAN_EMERGE;
    public static SoundEvent LEVIATHAN_DIVE;
    public static SoundEvent LEVIATHAN_SLAM;
    public static SoundEvent LEVIATHAN_FLOP;
    public static SoundEvent LEVIATHAN_WHIRLPOOL;
    public static SoundEvent LEVIATHAN_DEFLECT;
    public static SoundEvent LEVIATHAN_BITE;
    public static SoundEvent LEVIATHAN_BURST;
    public static SoundEvent TIDELING_AMBIENT;
    public static SoundEvent TIDELING_HURT;
    public static SoundEvent TIDELING_DEATH;
    public static SoundEvent TIDELING_FLOP;
    public static SoundEvent TIDELING_BITE;
    public static SoundEvent BRINE_BOLT_HIT;
    public static SoundEvent HEART_AMBIENT;
    public static SoundEvent HEART_ACTIVATE;
    public static SoundEvent TIDEBREAKER_DASH;

    private BossSounds() {
    }

    static void register() {
        LEVIATHAN_AMBIENT = BcReg.sound("entity.leviathan.ambient");
        LEVIATHAN_HURT = BcReg.sound("entity.leviathan.hurt");
        LEVIATHAN_DEATH = BcReg.sound("entity.leviathan.death");
        LEVIATHAN_ROAR = BcReg.sound("entity.leviathan.roar");
        LEVIATHAN_CHARGE = BcReg.sound("entity.leviathan.charge");
        LEVIATHAN_SPIT = BcReg.sound("entity.leviathan.spit");
        LEVIATHAN_EMERGE = BcReg.sound("entity.leviathan.emerge");
        LEVIATHAN_DIVE = BcReg.sound("entity.leviathan.dive");
        LEVIATHAN_SLAM = BcReg.sound("entity.leviathan.slam");
        LEVIATHAN_FLOP = BcReg.sound("entity.leviathan.flop");
        LEVIATHAN_WHIRLPOOL = BcReg.sound("entity.leviathan.whirlpool");
        LEVIATHAN_DEFLECT = BcReg.sound("entity.leviathan.deflect");
        LEVIATHAN_BITE = BcReg.sound("entity.leviathan.bite");
        LEVIATHAN_BURST = BcReg.sound("entity.leviathan.burst");
        TIDELING_AMBIENT = BcReg.sound("entity.tideling.ambient");
        TIDELING_HURT = BcReg.sound("entity.tideling.hurt");
        TIDELING_DEATH = BcReg.sound("entity.tideling.death");
        TIDELING_FLOP = BcReg.sound("entity.tideling.flop");
        TIDELING_BITE = BcReg.sound("entity.tideling.bite");
        BRINE_BOLT_HIT = BcReg.sound("entity.brine_bolt.hit");
        HEART_AMBIENT = BcReg.sound("block.heart_of_the_tide.ambient");
        HEART_ACTIVATE = BcReg.sound("block.heart_of_the_tide.activate");
        TIDEBREAKER_DASH = BcReg.sound("item.tidebreaker.dash");
    }
}
