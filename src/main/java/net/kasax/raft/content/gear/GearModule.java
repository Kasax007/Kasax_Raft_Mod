package net.kasax.raft.content.gear;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3.3 module "gear": weapons, armour sets, the wreck compass, the captain's spyglass and fishing bait. */
public class GearModule implements ContentModule {
    @Override
    public String name() {
        return "gear";
    }

    @Override
    public void init() {
        GearContent.init();
        GearEvents.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.gear.client.GearClient.init();
    }
}
