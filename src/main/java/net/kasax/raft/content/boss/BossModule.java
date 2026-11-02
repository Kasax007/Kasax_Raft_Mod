package net.kasax.raft.content.boss;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "boss". See docs/design/beachcraft3.md. */
public class BossModule implements ContentModule {
    @Override
    public String name() {
        return "boss";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.boss.client.BossClient.init();
    }
}
