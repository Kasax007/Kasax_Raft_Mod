package net.kasax.raft.content;

import net.kasax.raft.Raft;
import net.kasax.raft.item.custom.TooltipBlockItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Registration helpers shared by all Beach Craft 3 modules. Everything is registered in the {@code raft} namespace. */
public final class BcReg {
    private BcReg() {
    }

    public static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Raft.id(name));
    }

    public static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Raft.id(name));
    }

    /** A block without item (wall variants, crops, potted plants, technical blocks). */
    public static <T extends Block> T block(String name, Function<BlockBehaviour.Properties, T> factory,
                                            BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = blockKey(name);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    /** A block plus its plain block item, added to the given creative tab (tab may be null). */
    public static <T extends Block> T blockWithItem(String name, Function<BlockBehaviour.Properties, T> factory,
                                                    BlockBehaviour.Properties properties, BcTabs.Tab tab, String... tooltips) {
        T block = block(name, factory, properties);
        blockItem(name, block, new Item.Properties(), tab, tooltips);
        return block;
    }

    /** The block item of a block; tooltips are lang keys shown in grey under the name. */
    public static Item blockItem(String name, Block block, Item.Properties properties, BcTabs.Tab tab, String... tooltips) {
        ResourceKey<Item> key = itemKey(name);
        properties = properties.setId(key).useBlockDescriptionPrefix();
        Item item = tooltips.length == 0 ? new BlockItem(block, properties) : new TooltipBlockItem(block, properties, tooltips);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        if (tab != null) BcTabs.add(tab, item);
        return item;
    }

    /** A block item made by a custom factory (double blocks, wall/standing items...). */
    public static <T extends Item> T blockItem(String name, BiFunction<Block, Item.Properties, T> factory, Block block,
                                               Item.Properties properties, BcTabs.Tab tab) {
        ResourceKey<Item> key = itemKey(name);
        T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(block, properties.setId(key).useBlockDescriptionPrefix()));
        if (tab != null) BcTabs.add(tab, item);
        return item;
    }

    public static <T extends Item> T item(String name, Function<Item.Properties, T> factory, Item.Properties properties,
                                          BcTabs.Tab tab) {
        ResourceKey<Item> key = itemKey(name);
        T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
        if (tab != null) BcTabs.add(tab, item);
        return item;
    }

    public static Item item(String name, BcTabs.Tab tab) {
        return item(name, Item::new, new Item.Properties(), tab);
    }

    public static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Raft.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(String name,
                                                                        BlockEntityType.BlockEntitySupplier<T> factory,
                                                                        Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Raft.id(name), new BlockEntityType<>(factory, Set.of(blocks)));
    }

    /** A sound event {@code raft:<name>}; add it to the module's {@code assets/raft/sounds.json} fragment. */
    public static SoundEvent sound(String name) {
        Identifier id = Raft.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static Holder.Reference<SoundEvent> soundHolder(String name) {
        Identifier id = Raft.id(name);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
