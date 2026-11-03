package net.kasax.raft.content.decor;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "decor": decoration blocks, seats and the ship cannon (docs/design/beachcraft3.md, section 6). */
public class DecorModule implements ContentModule {
    @Override
    public String name() {
        return "decor";
    }

    @Override
    public void init() {
        DecorBlocks.init();
        DecorEntities.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.decor.client.DecorClient.init();
    }
}
