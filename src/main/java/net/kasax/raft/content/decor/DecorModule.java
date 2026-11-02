package net.kasax.raft.content.decor;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "decor". See docs/design/beachcraft3.md. */
public class DecorModule implements ContentModule {
    @Override
    public String name() {
        return "decor";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.decor.client.DecorClient.init();
    }
}
