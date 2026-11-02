package net.kasax.raft.content.world;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3 module "world": terrain and biomes of the raft dimension (data files), natural blocks and items
 * ({@link WorldBlocks}) and world generation feature types ({@link WorldFeatures}). See docs/design/beachcraft3.md
 * section 4.
 */
public class WorldModule implements ContentModule {
    @Override
    public String name() {
        return "world";
    }

    @Override
    public void init() {
        WorldBlocks.init();
        WorldFeatures.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.world.client.WorldClient.init();
    }
}
