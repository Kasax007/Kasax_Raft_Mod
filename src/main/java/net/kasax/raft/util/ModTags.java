package net.kasax.raft.util;

import net.kasax.raft.Raft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> RAFT_MOD_BLOCK = createTag("raft_mod_block");
        /** Blocks that count as a frame of the raft portal. */
        public static final TagKey<Block> CROP_PLOT_PLANTABLE = createTag("crop_plot_plantable");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Raft.id(name));
        }
    }

    public static class Items {
        public static final TagKey<Item> DRIFTWOOD_LOGS = createTag("driftwood_logs");
        public static final TagKey<Item> PALM_LOGS = createTag("palm_logs");
        public static final TagKey<Item> TITANIUM_ARMOR_REPAIR = createTag("repairs_titanium_armor");
        /** Items the simple grill accepts as fuel. */
        public static final TagKey<Item> GRILL_FUEL = createTag("grill_fuel");
        /** Raw food the grill can cook. */
        public static final TagKey<Item> RAW_FISH = createTag("raw_fish");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Raft.id(name));
        }
    }

    public static class Structures {
        /** Structures the receiver points to. */
        public static final TagKey<Structure> RECEIVER_TARGETS = TagKey.create(Registries.STRUCTURE, Raft.id("receiver_targets"));
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> SCARECROW_SCARES =
                TagKey.create(Registries.ENTITY_TYPE, Raft.id("scarecrow_scares"));
    }
}
