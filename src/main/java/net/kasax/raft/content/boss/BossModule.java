package net.kasax.raft.content.boss;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3 module "boss" (spec 10): the Leviathan with its tidelings and brine bolts, and the rewards
 * (leviathan_scale, tidebreaker, heart_of_the_tide, leviathan_trophy). The fight itself is summoned by the story
 * module's leviathan_altar.
 */
public class BossModule implements ContentModule {
    @Override
    public String name() {
        return "boss";
    }

    @Override
    public void init() {
        BossSounds.register();
        BossEntities.register();
        BossBlocks.register();
        BossItems.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.boss.client.BossClient.init();
    }
}
