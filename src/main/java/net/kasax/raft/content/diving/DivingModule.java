package net.kasax.raft.content.diving;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "diving". See docs/design/beachcraft3.md. */
public class DivingModule implements ContentModule {
    @Override
    public String name() {
        return "diving";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.diving.client.DivingClient.init();
    }
}
