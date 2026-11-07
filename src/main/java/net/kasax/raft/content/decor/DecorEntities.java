package net.kasax.raft.content.decor;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.decor.entity.CannonballEntity;
import net.kasax.raft.content.decor.entity.SeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entities of the decor module: the shared invisible seat and the cannonball. */
public final class DecorEntities {
    private DecorEntities() {
    }

    public static final EntityType<SeatEntity> SEAT = BcReg.entity("seat",
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.1F).passengerAttachments(0.0F).noSummon()
                    .clientTrackingRange(10).updateInterval(20));

    public static final EntityType<CannonballEntity> CANNONBALL = BcReg.entity("cannonball",
            EntityType.Builder.<CannonballEntity>of(CannonballEntity::new, MobCategory.MISC)
                    .sized(0.375F, 0.375F).clientTrackingRange(8).updateInterval(10));

    public static void init() {
    }
}
