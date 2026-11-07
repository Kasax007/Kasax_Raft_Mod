package net.kasax.raft.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Function;

/**
 * 1.21.1 port helper: a model posed from a render state, as in the newer game. Every frame starts from the parts'
 * initial poses ({@link #setupAnim(Object)} resets them), then the subclass poses the parts. It is a 1.21.1
 * entity model underneath, so vanilla renderers and layers can draw it.
 */
public abstract class Model<S> extends net.minecraft.client.model.EntityModel<Entity> {
    protected final ModelPart root;
    private final Function<ResourceLocation, RenderType> renderTypeFn;
    /** The state the next {@link #setupAnim(Entity, float, float, float, float, float)} call poses from. */
    public S pendingState;

    public Model(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
        this.renderTypeFn = renderType;
    }

    public final ModelPart root() {
        return this.root;
    }

    public final Function<ResourceLocation, RenderType> renderTypeFunction() {
        return this.renderTypeFn;
    }

    public final List<ModelPart> allParts() {
        return this.root.getAllParts().toList();
    }

    public void setupAnim(S state) {
        this.resetPose();
    }

    public final void resetPose() {
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.pendingState != null) this.setupAnim(this.pendingState);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.root.render(poseStack, buffer, light, overlay, color);
    }

    /** A model without state. */
    public static class Simple extends Model<net.minecraft.util.Unit> {
        public Simple(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
            super(root, renderType);
        }
    }
}
