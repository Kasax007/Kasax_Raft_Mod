/**
 * The NeoForge loader layer of Beach Craft.
 *
 * <p>Beach Craft is written for Fabric first; the NeoForge port keeps the mod code unchanged and only swaps the
 * Fabric API for these classes. They have the shape (class, method and constant names) of the Fabric API and Team
 * Reborn Energy calls the mod makes, but are implemented with NeoForge: registration events, the event bus, payload
 * handlers, capabilities (energy and items), attachments and biome modifiers. A merge from the Fabric branch then
 * mostly needs its imports pointed here ({@code net.fabricmc.fabric.api.**} becomes {@code net.kasax.raft.platform},
 * client parts {@code net.kasax.raft.platform.client}, transfer {@code .transfer}, Team Reborn Energy {@code .energy}).
 *
 * <p>{@link net.kasax.raft.platform.NeoPlatform} runs the mod's common and client init inside the first
 * {@code RegisterEvent}, while every registry is open, the same order Fabric uses (all registrations, then client
 * setup); what NeoForge wants in later events (attributes, payloads, capabilities, renderers, screens, layers) is
 * recorded and handed over when that event comes.
 */
package net.kasax.raft.platform;
