package net.kasax.raft.content.story;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.item.ItemEntity;

/** World events of the story module. */
public final class StoryEvents {
    private StoryEvents() {
    }

    static void register() {
        // Tide Sigils and the Tidewarden Key never despawn when dropped: losing one would block the story.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && isRelic(item)) {
                item.setUnlimitedLifetime();
            }
        });
    }

    private static boolean isRelic(ItemEntity item) {
        var stack = item.getItem();
        return stack.is(StoryContent.CORAL_SIGIL) || stack.is(StoryContent.PEARL_SIGIL) || stack.is(StoryContent.STORM_SIGIL)
                || stack.is(StoryContent.EMBER_SIGIL) || stack.is(StoryContent.TIDEWARDEN_KEY);
    }
}
