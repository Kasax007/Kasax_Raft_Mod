package net.kasax.raft.util;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.custom.FurnaceBlock;
import net.kasax.raft.config.FurnaceData;
import net.kasax.raft.item.custom.FurnaceItem;
import net.kasax.raft.Raft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public class FurnaceBlocks {
    public static final List<Block> allFurnaces = new ArrayList<>();
    public static final List<Block> regularFurnaces = new ArrayList<>();

    public static void init() {
        FurnaceData.furnaceData.forEach(FurnaceBlocks::registerFurnace);
    }

    private static void registerFurnace(FurnaceData data) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Raft.id(data.getName()));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                .strength(3.5f)
                .requiresCorrectToolForDrops()
                .lightLevel(createLightLevelFromBlockState(13))
                .setId(blockKey);
        FurnaceBlock furnace = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                new FurnaceBlock(properties, data.getSpeedModifier(), data.getFuelModifier(), data.getDuplicationChance()));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Raft.id(data.getName()));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new FurnaceItem(furnace, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        regularFurnaces.add(furnace);
        allFurnaces.add(furnace);
    }

    public static List<Block> getFurnaces() {
        return allFurnaces;
    }

    private static ToIntFunction<BlockState> createLightLevelFromBlockState(int litLevel) {
        return state -> state.getValue(BlockStateProperties.LIT) ? litLevel : 0;
    }

    public static Block getFurnaceByName(String name) {
        return allFurnaces.stream()
                .filter(furnace -> furnace.getDescriptionId().contains(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Furnace not found: " + name));
    }
}
