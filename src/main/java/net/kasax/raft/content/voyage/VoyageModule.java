package net.kasax.raft.content.voyage;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "voyage". See docs/design/beachcraft3.md. */
public class VoyageModule implements ContentModule {
    @Override
    public String name() {
        return "voyage";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.voyage.client.VoyageClient.init();
    }
}
