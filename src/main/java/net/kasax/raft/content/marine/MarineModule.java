package net.kasax.raft.content.marine;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3 module "marine" (spec 7.2): sea creatures (improved shark with raft bites, hammerhead, whale, manta
 * ray, jellyfish, anglerfish, moray eel), their drops and spawn eggs, the game rule {@code raft:shark_bites} and the
 * CREATURES creative tab icon.
 */
public class MarineModule implements ContentModule {
    @Override
    public String name() {
        return "marine";
    }

    @Override
    public void init() {
        MarineGameRules.register();
        MarineSounds.register();
        MarineEntities.register();
        MarineItems.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.marine.client.MarineClient.init();
    }
}
