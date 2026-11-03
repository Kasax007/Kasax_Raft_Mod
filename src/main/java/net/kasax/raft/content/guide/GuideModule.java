package net.kasax.raft.content.guide;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "guide": the Seafarer's Almanac. See docs/design/beachcraft3.md section 15. */
public class GuideModule implements ContentModule {
    @Override
    public String name() {
        return "guide";
    }

    @Override
    public void init() {
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.guide.client.GuideClient.init();
    }
}
