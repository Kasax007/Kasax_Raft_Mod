package net.kasax.raft.content.shore;

import net.kasax.raft.content.BcReg;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events of the shore creatures. The sounds.json fragment (tools/content/shore/gen.py) builds them from
 * vanilla recordings with their own pitch and volume, so every creature has a voice of its own.
 */
public final class ShoreSounds {
    public static SoundEvent CRAB_AMBIENT;
    public static SoundEvent CRAB_HURT;
    public static SoundEvent CRAB_DEATH;
    public static SoundEvent CRAB_STEP;
    public static SoundEvent CRAB_PINCH;
    public static SoundEvent CRAB_BURROW;
    public static SoundEvent EMBER_CRAB_AMBIENT;
    public static SoundEvent EMBER_CRAB_HURT;
    public static SoundEvent EMBER_CRAB_DEATH;
    public static SoundEvent SEAGULL_AMBIENT;
    public static SoundEvent SEAGULL_HURT;
    public static SoundEvent SEAGULL_DEATH;
    public static SoundEvent SEAGULL_FLAP;
    public static SoundEvent SEAGULL_PECK;
    public static SoundEvent SEAGULL_STEAL;
    public static SoundEvent PENGUIN_AMBIENT;
    public static SoundEvent PENGUIN_HURT;
    public static SoundEvent PENGUIN_DEATH;
    public static SoundEvent PENGUIN_STEP;
    public static SoundEvent PENGUIN_SLIDE;
    public static SoundEvent SEA_OTTER_AMBIENT;
    public static SoundEvent SEA_OTTER_HURT;
    public static SoundEvent SEA_OTTER_DEATH;
    public static SoundEvent SEA_OTTER_CRACK;
    public static SoundEvent SEA_OTTER_GIFT;
    public static SoundEvent CORSAIR_AMBIENT;

    private ShoreSounds() {
    }

    public static void register() {
        CRAB_AMBIENT = BcReg.sound("entity.crab.ambient");
        CRAB_HURT = BcReg.sound("entity.crab.hurt");
        CRAB_DEATH = BcReg.sound("entity.crab.death");
        CRAB_STEP = BcReg.sound("entity.crab.step");
        CRAB_PINCH = BcReg.sound("entity.crab.pinch");
        CRAB_BURROW = BcReg.sound("entity.crab.burrow");
        EMBER_CRAB_AMBIENT = BcReg.sound("entity.ember_crab.ambient");
        EMBER_CRAB_HURT = BcReg.sound("entity.ember_crab.hurt");
        EMBER_CRAB_DEATH = BcReg.sound("entity.ember_crab.death");
        SEAGULL_AMBIENT = BcReg.sound("entity.seagull.ambient");
        SEAGULL_HURT = BcReg.sound("entity.seagull.hurt");
        SEAGULL_DEATH = BcReg.sound("entity.seagull.death");
        SEAGULL_FLAP = BcReg.sound("entity.seagull.flap");
        SEAGULL_PECK = BcReg.sound("entity.seagull.peck");
        SEAGULL_STEAL = BcReg.sound("entity.seagull.steal");
        PENGUIN_AMBIENT = BcReg.sound("entity.penguin.ambient");
        PENGUIN_HURT = BcReg.sound("entity.penguin.hurt");
        PENGUIN_DEATH = BcReg.sound("entity.penguin.death");
        PENGUIN_STEP = BcReg.sound("entity.penguin.step");
        PENGUIN_SLIDE = BcReg.sound("entity.penguin.slide");
        SEA_OTTER_AMBIENT = BcReg.sound("entity.sea_otter.ambient");
        SEA_OTTER_HURT = BcReg.sound("entity.sea_otter.hurt");
        SEA_OTTER_DEATH = BcReg.sound("entity.sea_otter.death");
        SEA_OTTER_CRACK = BcReg.sound("entity.sea_otter.crack");
        SEA_OTTER_GIFT = BcReg.sound("entity.sea_otter.gift");
        CORSAIR_AMBIENT = BcReg.sound("entity.drowned_corsair.ambient");
    }
}
