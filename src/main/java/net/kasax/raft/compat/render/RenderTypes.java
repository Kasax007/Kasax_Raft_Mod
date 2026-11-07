package net.kasax.raft.compat.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 1.21.1 port helper: the newer game's render type factory names, mapped to 1.21.1's {@link RenderType}. */
public final class RenderTypes {
    private RenderTypes() {
    }

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }

    public static RenderType dragonRays() {
        return RenderType.dragonRays();
    }

    public static RenderType linesTranslucent() {
        return RenderType.lines();
    }

    public static RenderType waterMask() {
        return RenderType.waterMask();
    }
}
