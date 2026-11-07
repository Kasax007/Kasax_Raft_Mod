package net.kasax.raft.compat.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** 1.21.1 port helper: an entity model posed from a render state (see {@link Model}). */
public abstract class EntityModel<S extends EntityRenderState> extends Model<S> {
    public static final float MODEL_Y_OFFSET = -1.501F;

    protected EntityModel(ModelPart root) {
        this(root, RenderType::entityCutout);
    }

    protected EntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(root, renderType);
    }
}
