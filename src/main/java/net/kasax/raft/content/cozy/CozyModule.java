package net.kasax.raft.content.cozy;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3.3 module "cozy": furniture, lights, plants, wall decor, music, building blocks and comfort. */
public class CozyModule implements ContentModule {
    @Override
    public String name() {
        return "cozy";
    }

    @Override
    public void init() {
        CozyContent.init();
        CozyComfort.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.cozy.client.CozyClient.init();
    }
}
