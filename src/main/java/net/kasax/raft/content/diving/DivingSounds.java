package net.kasax.raft.content.diving;

import net.kasax.raft.content.BcReg;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/** Sound events of the diving module (files: vanilla sounds re-tuned in the module's sounds.json fragment). */
public final class DivingSounds {
    private DivingSounds() {
    }

    public static final Holder<SoundEvent> EQUIP_DIVING_GEAR = BcReg.soundHolder("item.armor.equip_diving_gear");
    public static final SoundEvent HARPOON_GUN_FIRE = BcReg.sound("item.harpoon_gun.fire");
    public static final SoundEvent HARPOON_GUN_RELOAD = BcReg.sound("item.harpoon_gun.reload");
    public static final SoundEvent HARPOON_HIT = BcReg.sound("entity.harpoon.hit");
    public static final SoundEvent HARPOON_HIT_GROUND = BcReg.sound("entity.harpoon.hit_ground");
    public static final SoundEvent COMPRESSOR_CRANK = BcReg.sound("block.air_compressor.crank");
    public static final SoundEvent COMPRESSOR_FILL = BcReg.sound("block.air_compressor.fill");
    public static final SoundEvent SEA_SCOOTER_RUN = BcReg.sound("item.sea_scooter.run");
    public static final SoundEvent TANK_REFILL = BcReg.sound("item.oxygen_tank.refill");
    public static final SoundEvent TANK_LOW = BcReg.sound("item.oxygen_tank.low");

    static void register() {
        // loading the class registers the events above
    }
}
