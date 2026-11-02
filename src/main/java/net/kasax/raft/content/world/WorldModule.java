package net.kasax.raft.content.world;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "world". See docs/design/beachcraft3.md. */
public class WorldModule implements ContentModule {
    @Override
    public String name() {
        return "world";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.world.client.WorldClient.init();
    }
}
