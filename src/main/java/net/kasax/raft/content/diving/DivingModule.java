package net.kasax.raft.content.diving;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "diving" (spec 8.2): diving gear, oxygen, air compressor, harpoon gun, sea scooter. */
public class DivingModule implements ContentModule {
    @Override
    public String name() {
        return "diving";
    }

    @Override
    public void init() {
        DivingContent.register();
        DivingEvents.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.diving.client.DivingClient.init();
    }
}
