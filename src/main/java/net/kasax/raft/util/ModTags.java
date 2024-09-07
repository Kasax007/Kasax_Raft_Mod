package net.kasax.raft.util;

import net.kasax.raft.Raft;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> RAFT_MOD_BLOCK =
                createTag("raft_mod_block");

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(Raft.MOD_ID, name));
        }
    }

    public static class Items {

        public static final TagKey<Item> DRIFTWOOD_LOGS =
                createTag("driftwood_logs");
        public static final TagKey<Item> PALM_LOGS =
                createTag("driftwood_logs");
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(Raft.MOD_ID, name));
        }
    }
}
