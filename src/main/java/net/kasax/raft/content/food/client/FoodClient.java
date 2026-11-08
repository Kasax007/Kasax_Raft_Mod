package net.kasax.raft.content.food.client;

import net.kasax.raft.content.food.FoodContent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client init of the food module: the cooking pot's contents in the world. */
public final class FoodClient {
    private FoodClient() {
    }

    public static void init() {
        BlockEntityRenderers.register(FoodContent.COOKING_POT_ENTITY, CookingPotRenderer::new);
    }
}
