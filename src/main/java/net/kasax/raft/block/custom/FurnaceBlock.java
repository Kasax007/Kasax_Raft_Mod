package net.kasax.raft.block.custom;

//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.entity.FurnaceEntity;
import net.kasax.raft.util.FurnaceEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FurnaceBlock extends AbstractFurnaceBlock {
    protected final double speed;
    protected final double fuel;
    protected final int dupe;

    public FurnaceBlock(Properties properties, double speedMultiplier, double fuelMultiplier, int dupeChance100) {
        super(properties);
        this.speed = speedMultiplier;
        this.fuel = fuelMultiplier;
        this.dupe = dupeChance100;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, FurnaceEntities.RAFT_FURNACE,
                (lvl, pos, st, be) -> FurnaceEntity.tick((net.minecraft.server.level.ServerLevel) lvl, pos, st, be));
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FurnaceEntity) {
            player.openMenu((MenuProvider) blockEntity);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY();
            double z = pos.getZ() + 0.5D;

            if (random.nextDouble() < 0.1D) {
                level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            Direction direction = state.getValue(FACING);
            Direction.Axis axis = direction.getAxis();

            double side = random.nextDouble() * 0.6D - 0.3D;
            double dx = axis == Direction.Axis.X ? direction.getStepX() * 0.52D : side;
            double dy = random.nextDouble() * 6.0D / 16.0D;
            double dz = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52D : side;

            level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0D, 0.0D, 0.0D);
        }
    }

    public double getSpeedModifier() {
        return speed;
    }

    public double getFuelModifier() {
        return fuel;
    }

    public int getDuplicationChance() {
        return dupe;
    }
}
