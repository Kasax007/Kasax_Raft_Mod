package net.kasax.raft.content.encounters;

import net.kasax.raft.content.BcReg;
import net.minecraft.sounds.SoundEvent;

/** Sound events of the encounters module (sounds.json: tools/content/encounters/data.py). */
public final class EncounterSounds {
    public static SoundEvent SCAVENGER_AMBIENT;
    public static SoundEvent SCAVENGER_HURT;
    public static SoundEvent SCAVENGER_DEATH;
    public static SoundEvent REEF_GUARDIAN_AMBIENT;
    public static SoundEvent REEF_GUARDIAN_HURT;
    public static SoundEvent REEF_GUARDIAN_DEATH;
    public static SoundEvent CAPTAIN_RALLY;
    /** The warning when a guarded structure wakes up (one per kind of guard). */
    public static SoundEvent ALARM_SCAVENGERS;
    public static SoundEvent ALARM_DEEP;
    public static SoundEvent ALARM_CORSAIRS;
    public static SoundEvent ALARM_REEF;
    public static SoundEvent ALARM_FORGE;

    private EncounterSounds() {
    }

    public static void register() {
        SCAVENGER_AMBIENT = BcReg.sound("entity.scavenger.ambient");
        SCAVENGER_HURT = BcReg.sound("entity.scavenger.hurt");
        SCAVENGER_DEATH = BcReg.sound("entity.scavenger.death");
        REEF_GUARDIAN_AMBIENT = BcReg.sound("entity.reef_guardian.ambient");
        REEF_GUARDIAN_HURT = BcReg.sound("entity.reef_guardian.hurt");
        REEF_GUARDIAN_DEATH = BcReg.sound("entity.reef_guardian.death");
        CAPTAIN_RALLY = BcReg.sound("entity.corsair_captain.rally");
        ALARM_SCAVENGERS = BcReg.sound("encounter.scavengers");
        ALARM_DEEP = BcReg.sound("encounter.deep");
        ALARM_CORSAIRS = BcReg.sound("encounter.corsairs");
        ALARM_REEF = BcReg.sound("encounter.reef");
        ALARM_FORGE = BcReg.sound("encounter.forge");
    }
}
