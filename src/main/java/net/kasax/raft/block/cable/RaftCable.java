package net.kasax.raft.block.cable;
/*
 * This file is inspired by TechReborn, licensed under the MIT License (MIT).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import net.kasax.raft.Raft;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;

public class RaftCable {
    public enum Cables implements ItemConvertible {
        COPPER_CABLE(128, 12.0, true, RaftEnergyTier.MEDIUM);

        public final String name;
        public final CableBlock block;

        public final int transferRate;
        public final int defaultTransferRate;
        public final double cableThickness;
        public final boolean canKill;
        public final boolean defaultCanKill;
        public final RaftEnergyTier tier;


        Cables(int transferRate, double cableThickness, boolean canKill, RaftEnergyTier tier) {
            name = this.toString().toLowerCase(Locale.ROOT);
            this.transferRate = transferRate;
            this.defaultTransferRate = transferRate;
            this.cableThickness = cableThickness / 2 / 16;
            this.canKill = canKill;
            this.defaultCanKill = canKill;
            this.tier = tier;
            this.block = new CableBlock(this);
            setup(block, name + "_cable");
        }

        public ItemStack getStack() {
            return new ItemStack(block);
        }

        @Override
        public Item asItem() {
            return block.asItem();
        }
    }
    public static <B extends Block> B setup(B block, String name) {
        registerIdent(block, Identifier.of(Raft.MOD_ID, name));
        return block;
    }
    public static void registerIdent(Object object, Identifier identifier){
        final HashMap<Object, Identifier> objIdentMap = new HashMap<>();
        objIdentMap.put(object, identifier);
    }
    public enum RaftEnergyTier {
        MICRO(8, 8),
        LOW(32, 32),
        MEDIUM(128, 128),
        HIGH(512, 512),
        EXTREME(2048, 2048),
        INSANE(8192, 8192),
        INFINITE(Integer.MAX_VALUE, Integer.MAX_VALUE);

        private final int maxInput;
        private final int maxOutput;

        RaftEnergyTier(int maxInput, int maxOutput) {
            this.maxInput = maxInput;
            this.maxOutput = maxOutput;
        }

        public int getMaxInput() {
            return maxInput;
        }

        public int getMaxOutput() {
            return maxOutput;
        }

        public static RaftEnergyTier getTier(long power) {
            for (RaftEnergyTier tier : RaftEnergyTier.values()) {
                if (tier.getMaxInput() >= power) {
                    return tier;
                }
            }
            return RaftEnergyTier.INFINITE;
        }
    }
}
