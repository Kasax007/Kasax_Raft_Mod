package net.kasax.raft.content.marine;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "marine". See docs/design/beachcraft3.md. */
public class MarineModule implements ContentModule {
    @Override
    public String name() {
        return "marine";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.marine.client.MarineClient.init();
    }
}
