package net.kasax.raft.util;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.kasax.raft.Raft;
import net.kasax.raft.block.custom.FurnaceBlock;
import net.kasax.raft.config.FurnaceData;
import net.kasax.raft.item.custom.FurnaceItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static net.minecraft.block.Blocks.STONE;

public class FurnaceBlocks {
    public static final List<Block> allFurnaces = new ArrayList<>();
    public static final List<Block> regularFurnaces = new ArrayList<>();

    public static void init() {
        FurnaceData.furnaceData.forEach(FurnaceBlocks::registerFurnace);
    }

    private static void registerFurnace(FurnaceData data) {
        FurnaceBlock baseFurnace = register(data.getName(), new FurnaceBlock(FabricBlockSettings.copyOf(STONE).hardness(3.5f).luminance(createLightLevelFromBlockState(13)), data.getSpeedModifier(), data.getFuelModifier(), data.getDuplicationChance()));
        Registry.register(Registries.ITEM, data.getID(), new FurnaceItem(baseFurnace, new Item.Settings()));
        regularFurnaces.add(baseFurnace);
        allFurnaces.add(baseFurnace);
    }

    private static FurnaceBlock register(String name, FurnaceBlock block) {
        return Registry.register(Registries.BLOCK, new Identifier(Raft.MOD_ID, name), block);
    }

    public static List<Block> getFurnaces() {
        return allFurnaces;
    }

    private static ToIntFunction<BlockState> createLightLevelFromBlockState(int litLevel) {
        return (blockState) -> (Boolean)blockState.get(Properties.LIT) ? litLevel : 0;
    }
    public static Block getFurnaceByName(String name) {
        return allFurnaces.stream()
                .filter(furnace -> furnace.getTranslationKey().contains(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Furnace not found: " + name));
    }
}
