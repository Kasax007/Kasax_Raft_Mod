package net.kasax.raft.content.vessel;

import net.kasax.raft.content.ContentModule;

/**
 * Module "vessel" (docs/design/raft-building.md): rafts built from blocks that sail as one piece. A helm turns the
 * connected raft foundations and everything on them into a {@link VesselEntity} whose blocks keep working in a shipyard
 * plot; sails, oars and motors on board give it power; it becomes blocks again when a player drops anchor.
 */
public class VesselModule implements ContentModule {
    @Override
    public String name() {
        return "vessel";
    }

    @Override
    public void init() {
        VesselContent.register();
        VesselShipyard.init();
        VesselEvents.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.vessel.client.VesselClient.init();
    }
}
