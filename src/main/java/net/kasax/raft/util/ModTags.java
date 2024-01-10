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
            return TagKey.of(RegistryKeys.BLOCK, new Identifier(Raft.MOD_ID, name));
        }
    }

    public static class Items {


        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, new Identifier(Raft.MOD_ID, name));
        }
    }
}
