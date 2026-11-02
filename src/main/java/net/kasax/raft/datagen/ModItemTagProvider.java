package net.kasax.raft.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider<Item> {

    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, Registries.ITEM, completableFuture);
    }

    public static ResourceKey<Item> key(ItemLike item) {
        return BuiltInRegistries.ITEM.getResourceKey(item.asItem()).orElseThrow();
    }

    private void addAll(TagKey<Item> tag, ItemLike... items) {
        for (ItemLike item : items) {
            tag(tag).add(key(item));
        }
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        addAll(ItemTags.PLANKS, ModBlocks.DRIFTWOOD_PLANKS, ModBlocks.PALM_PLANKS);
        addAll(ItemTags.WOODEN_STAIRS, ModBlocks.DRIFTWOOD_STAIRS, ModBlocks.PALM_STAIRS);
        addAll(ItemTags.WOODEN_SLABS, ModBlocks.DRIFTWOOD_SLAB, ModBlocks.PALM_SLAB);
        addAll(ItemTags.WOODEN_DOORS, ModBlocks.DRIFTWOOD_DOOR, ModBlocks.PALM_DOOR);
        addAll(ItemTags.WOODEN_TRAPDOORS, ModBlocks.DRIFTWOOD_TRAPDOOR, ModBlocks.PALM_TRAPDOOR);
        addAll(ItemTags.WOODEN_BUTTONS, ModBlocks.DRIFTWOOD_BUTTON, ModBlocks.PALM_BUTTON);
        addAll(ItemTags.WOODEN_PRESSURE_PLATES, ModBlocks.DRIFTWOOD_PRESSURE_PLATE, ModBlocks.PALM_PRESSURE_PLATE);
        addAll(ItemTags.WOODEN_FENCES, ModBlocks.DRIFTWOOD_FENCE, ModBlocks.PALM_FENCE);
        addAll(ItemTags.FENCE_GATES, ModBlocks.DRIFTWOOD_FENCE_GATE, ModBlocks.PALM_FENCE_GATE);
        addAll(ItemTags.LEAVES, ModBlocks.DRIFTWOOD_LEAVES, ModBlocks.PALM_LEAVES, ModBlocks.PALM_COCONUT_LEAVES);
        addAll(ItemTags.SAPLINGS, ModBlocks.DRIFTWOOD_SAPLING, ModBlocks.PALM_SAPLING);

        addAll(ItemTags.LOGS_THAT_BURN,
                ModBlocks.DRIFTWOOD_LOG, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_LOG,
                ModBlocks.PALM_LOG, ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_WOOD, ModBlocks.STRIPPED_PALM_LOG);

        addAll(ModTags.Items.DRIFTWOOD_LOGS,
                ModBlocks.DRIFTWOOD_LOG, ModBlocks.STRIPPED_DRIFTWOOD_LOG, ModBlocks.DRIFTWOOD_WOOD, ModBlocks.STRIPPED_DRIFTWOOD_WOOD);

        addAll(ModTags.Items.PALM_LOGS,
                ModBlocks.PALM_LOG, ModBlocks.STRIPPED_PALM_LOG, ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_WOOD);

        addAll(ItemTags.CREEPER_DROP_MUSIC_DISCS,
                ModItems.LOFI_BEAT_MUSIC_DISC, ModItems.CRYSTAL_CASTLES_MUSIC_DISC, ModItems.SOME_GAME_MUSIC_MUSIC_DISC);

        addAll(ItemTags.TRIMMABLE_ARMOR,
                ModItems.TITANIUM_HELMET, ModItems.TITANIUM_CHESTPLATE, ModItems.TITANIUM_LEGGINGS, ModItems.TITANIUM_BOOTS);
        addAll(ItemTags.HEAD_ARMOR, ModItems.TITANIUM_HELMET);
        addAll(ItemTags.CHEST_ARMOR, ModItems.TITANIUM_CHESTPLATE);
        addAll(ItemTags.LEG_ARMOR, ModItems.TITANIUM_LEGGINGS);
        addAll(ItemTags.FOOT_ARMOR, ModItems.TITANIUM_BOOTS);

        addAll(ModTags.Items.TITANIUM_ARMOR_REPAIR, ModItems.TITANIUM_INGOT);

        // Fuel of the simple grill and the water purifier: planks, logs and coal.
        addAll(ModTags.Items.GRILL_FUEL, Items.COAL, Items.CHARCOAL);
        tag(ModTags.Items.GRILL_FUEL).forceAddTag(ItemTags.PLANKS).forceAddTag(ItemTags.LOGS);
        addAll(ItemTags.WOODEN_SLABS, ModBlocks.PALM_THATCH_SLAB);
        addAll(ItemTags.MEAT, ModItems.SHARK_MEAT, ModItems.COOKED_SHARK_MEAT);
        addAll(ItemTags.FISHES, ModItems.SHARK_MEAT, ModItems.COOKED_SHARK_MEAT);
    }
}
