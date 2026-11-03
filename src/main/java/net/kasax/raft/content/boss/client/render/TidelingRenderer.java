package net.kasax.raft.content.boss.client.render;

import net.kasax.raft.Raft;
import net.kasax.raft.content.boss.client.model.TidelingModel;
import net.kasax.raft.content.boss.client.model.TidelingRenderState;
import net.kasax.raft.content.boss.entity.TidelingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renders the tideling; its eyes and side spots glow like the Leviathan's. */
public class TidelingRenderer extends MobRenderer<TidelingEntity, TidelingRenderState, TidelingModel> {
    private static final Identifier TEXTURE = Raft.id("textures/entity/leviathan/tideling.png");
    private static final RenderType GLOW = RenderTypes.eyes(Raft.id("textures/entity/leviathan/tideling_glow.png"));

    public TidelingRenderer(EntityRendererProvider.Context context) {
        super(context, new TidelingModel(context.bakeLayer(TidelingModel.LAYER)), 0.35F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(TidelingRenderState state) {
        return TEXTURE;
    }

    @Override
    public TidelingRenderState createRenderState() {
        return new TidelingRenderState();
    }

    @Override
    public void extractRenderState(TidelingEntity entity, TidelingRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bite = entity.getBite(partialTicks);
        state.onLand = !entity.isInWater();
        state.flopping = entity.isFlopping();
    }
}
