package net.kasax.raft.content.story;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "story". See docs/design/beachcraft3.md. */
public class StoryModule implements ContentModule {
    @Override
    public String name() {
        return "story";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.story.client.StoryClient.init();
    }
}
