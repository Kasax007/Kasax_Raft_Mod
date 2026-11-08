package net.kasax.raft.content.food.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.kasax.raft.Raft;
import net.kasax.raft.content.food.block.CookingPotBlockEntity;
import net.kasax.raft.content.food.block.PotRecipes;
import net.kasax.raft.compat.render.SubmitNodeCollector;
import net.kasax.raft.compat.render.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.kasax.raft.compat.render.BlockEntityRenderState;
import net.kasax.raft.compat.render.ItemModelResolver;
import net.kasax.raft.compat.render.ItemStackRenderState;
import net.minecraft.client.renderer.RenderType;
import net.kasax.raft.compat.render.RenderTypes;
import net.kasax.raft.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The cooking pot in the world: a liquid surface whose colour runs from water to the colour of the dish while it
 * cooks, the ingredients bobbing in it and slowly stirred round, and when the dish is done the stew and the dish
 * itself turning above the pot. One textured quad and at most five small items per pot.
 */
public class CookingPotRenderer implements BlockEntityRenderer<CookingPotBlockEntity, CookingPotRenderer.State> {
    private static final ResourceLocation LIQUID = Raft.id("textures/block/cooking_pot_liquid.png");
    private static final float LO = 3.0F / 16F, HI = 13.0F / 16F;
    private static final float SURFACE = 6.5F / 16F;

    private final ItemModelResolver items;

    public CookingPotRenderer(BlockEntityRendererProvider.Context context) {
        this.items = new ItemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] ingredients = {new ItemStackRenderState(), new ItemStackRenderState(),
                new ItemStackRenderState(), new ItemStackRenderState()};
        int ingredientCount;
        final ItemStackRenderState dish = new ItemStackRenderState();
        boolean ready;
        boolean liquid;
        int color;
        float time;
        float boil;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    @Override
    public void extractRenderState(CookingPotBlockEntity pot, State state, float partialTicks, Vec3 camera,
                                   @Nullable Object breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pot, state, partialTicks, camera, breakProgress);
        long seed = pot.getBlockPos().asLong();
        state.time = pot.getLevel() == null ? 0 : (pot.getLevel().getGameTime() % 24000L) + partialTicks;
        state.ready = pot.servings() > 0;
        state.ingredientCount = 0;
        state.boil = 0;
        if (state.ready) {
            items.updateForTopItem(state.dish, pot.dish(), ItemDisplayContext.FIXED, pot.getLevel(), null, (int) seed);
            PotRecipes.Recipe recipe = PotRecipes.byResult(pot.dish());
            state.color = recipe == null ? PotRecipes.MURKY : recipe.color();
            state.liquid = true;
            return;
        }
        state.dish.clear();
        List<ItemStack> in = pot.ingredients();
        for (ItemStack s : in) {
            if (s.isEmpty()) continue;
            items.updateForTopItem(state.ingredients[state.ingredientCount], s, ItemDisplayContext.GROUND, pot.getLevel(),
                    null, (int) seed + state.ingredientCount);
            state.ingredientCount++;
        }
        state.liquid = state.ingredientCount > 0;
        if (!state.liquid) return;
        PotRecipes.Recipe recipe = PotRecipes.match(in);
        if (recipe != null) {
            float f = Math.min(1.0F, pot.progress() / (float) CookingPotBlockEntity.COOK_TIME);
            state.color = lerp(PotRecipes.WATER, recipe.color(), f);
            state.boil = pot.getLevel() != null && CookingPotBlockEntity.heated(pot.getLevel(), pot.getBlockPos()) ? 0.4F + f : 0;
        } else {
            state.color = PotRecipes.candidates(in).isEmpty() ? PotRecipes.MURKY : PotRecipes.WATER;
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.liquid) return;
        float wobble = state.boil * 0.004F * (float) Math.sin(state.time * 0.9F);
        float y = SURFACE + wobble;
        int argb = 0xE6000000 | state.color;
        int light = state.lightCoords;
        RenderType type = RenderTypes.entityTranslucent(LIQUID);
        collector.submitCustomGeometry(pose, type, (p, buffer) -> quad(p, buffer, y, argb, light));

        for (int i = 0; i < state.ingredientCount; i++) {
            // ingredients float half under, stirred slowly round the middle; faster and bouncier when it boils
            float angle = state.time * (1.2F + state.boil * 2.0F) + i * 90.0F;
            float bob = (float) Math.sin((state.time * (0.12F + state.boil * 0.1F)) + i * 1.7F) * (0.012F + state.boil * 0.01F);
            pose.pushPose();
            pose.translate(0.5F, y + bob, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(state.ingredientCount == 1 ? 0.0F : 0.17F, 0.0F, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(70.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(i * 40.0F));
            pose.scale(0.42F, 0.42F, 0.42F);
            state.ingredients[i].submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (state.ready && !state.dish.isEmpty()) {
            // done: the dish turns above the pot
            pose.pushPose();
            pose.translate(0.5F, 0.98F + (float) Math.sin(state.time * 0.08F) * 0.035F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.time * 2.5F));
            pose.scale(0.5F, 0.5F, 0.5F);
            state.dish.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float y, int argb, int light) {
        vertex(pose, buffer, LO, y, LO, 3 / 16F, 3 / 16F, argb, light);
        vertex(pose, buffer, LO, y, HI, 3 / 16F, 13 / 16F, argb, light);
        vertex(pose, buffer, HI, y, HI, 13 / 16F, 13 / 16F, argb, light);
        vertex(pose, buffer, HI, y, LO, 13 / 16F, 3 / 16F, argb, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
                               int argb, int light) {
        buffer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
