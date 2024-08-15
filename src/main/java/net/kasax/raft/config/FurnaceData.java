package net.kasax.raft.config;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.Raft;

import java.util.Arrays;
import java.util.List;

public class FurnaceData {
    private final String name;
    private final double speedModifier;
    private final double fuelModifier;
    private final int duplicationChance;

    public FurnaceData(String name, double speedModifier, double fuelModifier, int duplicationChance) {
        this.name = name;
        this.speedModifier = speedModifier;
        this.fuelModifier = fuelModifier;
        this.duplicationChance = duplicationChance;
    }

    public static List<FurnaceData> furnaceData = Arrays.asList(
            FurnaceData.of("brick", 1.5f, 1.5f, 0),
            //FurnaceData.of("iron", 2, 2, 0),
            //FurnaceData.of("gold", 3f, 2, 0),
            //FurnaceData.of("diamond", 3.5f, 3f, 0),
            //FurnaceData.of("obsidian", 4f, 2f, 0),
            //FurnaceData.of("nether", 4.5f, 3.5f, 0),
            //FurnaceData.of(//"emerald", 8f, 8f, 33),
            //FurnaceData.of("end", 12f, 10f, 66),
            FurnaceData.of("titanium", 32f, 16f, 100)
    );

    public String getName() {
        return String.format("%s_furnace", name);
    }

    public String getID() {
        return String.format("%s:%s_furnace", Raft.MOD_ID, name);
    }

    public double getSpeedModifier() {
        return speedModifier;
    }

    public double getFuelModifier() {
        return fuelModifier;
    }

    public int getDuplicationChance() {
        return duplicationChance;
    }

    public static FurnaceData of(String name, double speedModifier, double fuelModifier, int duplicationChance) {
        return new FurnaceData(name, speedModifier, fuelModifier, duplicationChance);
    }
}
