package net.kasax.raft.content.building.client;

import net.kasax.raft.platform.client.EntityModelLayerRegistry;
import net.kasax.raft.content.building.BuildingBlocks;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client-only init of the "building" module: the wind turbine rotor renderer. */
public final class BuildingClient {
    private BuildingClient() {
    }

    public static void init() {
        EntityModelLayerRegistry.registerModelLayer(WindTurbineRotorModel.LAYER, WindTurbineRotorModel::createLayer);
        BlockEntityRenderers.register(BuildingBlocks.WIND_TURBINE_BE, WindTurbineRenderer::new);
    }
}
