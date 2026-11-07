package net.kasax.raft.content.shore;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3 module "shore": crabs, ember crabs, seagulls, penguins, sea otters and the drowned corsairs,
 * plus their items (crab claws, ember shell, cutlass, spawn eggs). See docs/design/beachcraft3.md, section 7.1.
 */
public class ShoreModule implements ContentModule {
    @Override
    public String name() {
        return "shore";
    }

    @Override
    public void init() {
        ShoreSounds.register();
        ShoreEntities.register();
        ShoreItems.register();
        SeagullSky.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.shore.client.ShoreClient.init();
    }
}
