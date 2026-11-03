package net.kasax.raft.content.guide;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.guide.item.AlmanacItem;
import net.minecraft.world.item.Item;

/** Items and data of the guide module. */
public final class GuideContent {
    private GuideContent() {
    }

    /** The Seafarer's Almanac: the in-game guide book (spec 15). */
    public static Item SEAFARERS_ALMANAC;

    /** Set once a player got the free almanac on their first visit to the raft dimension (kept through death). */
    public static final AttachmentType<Boolean> ALMANAC_GIVEN = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .copyOnDeath()
            .buildAndRegister(Raft.id("almanac_given"));

    static void init() {
        SEAFARERS_ALMANAC = BcReg.item("seafarers_almanac", AlmanacItem::new, new Item.Properties().stacksTo(1), BcTabs.Tab.STORY);
    }
}
