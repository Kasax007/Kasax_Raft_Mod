package net.kasax.raft.content.shore;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "shore". See docs/design/beachcraft3.md. */
public class ShoreModule implements ContentModule {
    @Override
    public String name() {
        return "shore";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.shore.client.ShoreClient.init();
    }
}
