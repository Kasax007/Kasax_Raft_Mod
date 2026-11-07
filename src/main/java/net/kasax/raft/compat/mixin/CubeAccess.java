package net.kasax.raft.compat.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.1 port: the faces of a model cube (public in the newer game), for ModelZFix. */
@Mixin(ModelPart.Cube.class)
public interface CubeAccess {
    @Accessor("polygons")
    ModelPart.Polygon[] raft$polygons();
}
