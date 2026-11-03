package net.kasax.raft.content.voyage;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3 module "voyage" (spec 8.1): palm boats, driftwood rafts and the Expedition Raft with its upgrades
 * (sail, engine, anchor, cargo crate, lanterns), its storage menu and the VOYAGE creative tab icon.
 */
public class VoyageModule implements ContentModule {
    @Override
    public String name() {
        return "voyage";
    }

    @Override
    public void init() {
        VoyageContent.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.voyage.client.VoyageClient.init();
    }
}
