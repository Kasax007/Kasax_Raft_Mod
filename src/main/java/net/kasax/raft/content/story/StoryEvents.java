package net.kasax.raft.content.story;

import net.kasax.raft.platform.ServerEntityEvents;
import net.minecraft.world.entity.item.ItemEntity;

/** World events of the story module. */
public final class StoryEvents {
    private StoryEvents() {
    }

    static void register() {
        // Tide Sigils and the Tidewarden Key never despawn when dropped: losing one would block the story.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && isRelic(item) && !isFakeCopy(item)) {
                item.setUnlimitedLifetime();
            }
        });
    }

    /**
     * /give spawns a display copy next to the player (ItemEntity.makeFakeItem: never picked up, age one tick before
     * despawn) after the real item went into the inventory. It must vanish as usual, or it would float there forever.
     */
    private static boolean isFakeCopy(ItemEntity item) {
        return item.getAge() >= 5999 && item.hasPickUpDelay();
    }

    private static boolean isRelic(ItemEntity item) {
        var stack = item.getItem();
        return stack.is(StoryContent.CORAL_SIGIL) || stack.is(StoryContent.PEARL_SIGIL) || stack.is(StoryContent.STORM_SIGIL)
                || stack.is(StoryContent.EMBER_SIGIL) || stack.is(StoryContent.TIDEWARDEN_KEY);
    }
}
