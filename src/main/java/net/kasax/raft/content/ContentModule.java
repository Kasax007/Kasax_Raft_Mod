package net.kasax.raft.content;

/**
 * One self-contained content area of Beach Craft 3 (world, decor, creatures, story, ...).
 *
 * <p>Each module owns its package {@code net.kasax.raft.content.<module>}, its resource folder
 * {@code src/main/modules/<module>/} (merged into the jar by the {@code mergeResources} Gradle task) and its mixin
 * config {@code raft-<module>.mixins.json}. Client-only code lives in {@code net.kasax.raft.content.<module>.client}
 * and is only reached through {@link #initClient()}.
 */
public interface ContentModule {
    /** Module name, equal to the package and resource folder name. */
    String name();

    /** Common init: registries (blocks, items, entities, sounds, block entities, menus, payloads), events. */
    default void init() {
    }

    /** Client init: renderers, model layers, screens, HUD, key bindings, client payload handlers. */
    default void initClient() {
    }
}
