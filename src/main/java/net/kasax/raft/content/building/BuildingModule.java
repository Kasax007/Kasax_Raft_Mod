package net.kasax.raft.content.building;

import net.kasax.raft.content.ContentModule;

/** Beach Craft 3 module "building": building blocks and Expedition tech (docs/design/beachcraft3.md, section 5). */
public class BuildingModule implements ContentModule {
    @Override
    public String name() {
        return "building";
    }

    @Override
    public void init() {
        BuildingBlocks.init();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.building.client.BuildingClient.init();
    }
}
