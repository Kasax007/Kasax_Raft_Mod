package net.kasax.raft.compat.render;

/** 1.21.1 port helper: a renderer that carries layers posed from its render state. */
public interface RenderLayerParent<S extends EntityRenderState, M extends Model<?>> {
    M getModel();

    /** The state of the entity drawn right now (set while the renderer draws). */
    S currentRenderState();
}
