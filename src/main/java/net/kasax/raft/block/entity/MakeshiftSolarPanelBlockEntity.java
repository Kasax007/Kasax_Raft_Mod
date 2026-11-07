package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class MakeshiftSolarPanelBlockEntity extends BlockEntity {
    public MakeshiftSolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAKESHIFT_SOLAR_PANEL_BLOCK_ENTITY, pos, state);
    }

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(100_000, 0, 1_000) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();

            update();
        }
    };

    public long getEnergy() {
        return this.energyStorage.getAmount();
    }

    public long getMaxEnergy() {
        return this.energyStorage.getCapacity();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        boolean inOverworld = level.dimension() == Level.OVERWORLD;
        boolean inRaftDim = level.dimension() == ModDimensions.RAFTDIM_LEVEL_KEY;
        boolean skyVisible = level.canSeeSky(pos.above());

        // Energy only by day, in the overworld and over the Endless Sea (it has day and night now).
        if (skyVisible && (inOverworld || inRaftDim) && level.isDay()) {
            if (energyStorage.amount < energyStorage.getCapacity()) {
                energyStorage.amount = Mth.clamp(energyStorage.amount + 10, 0, energyStorage.getCapacity());
                update();
            }
        }

        for (Direction direction : Direction.values()) {
            EnergyStorage storage = EnergyStorage.SIDED.find(this.level, this.worldPosition.relative(direction), direction.getOpposite());
            if (storage != null && storage.supportsInsertion()) {
                try (Transaction transaction = Transaction.openOuter()) {
                    long insertable;
                    try (Transaction simulateTransaction = transaction.openNested()) {
                        insertable = storage.insert(Long.MAX_VALUE, simulateTransaction);
                    }

                    long extracted = this.energyStorage.extract(insertable, transaction);
                    long inserted = storage.insert(extracted, transaction);
                    if (extracted == inserted)
                        transaction.commit();
                }
            }
        }
    }

    private int ticksSinceSync;

    /** Saves every tick, but tells the clients only once a second (it charges every tick in daylight). */
    private void update() {
        setChanged();
        if (++ticksSinceSync >= 20 && level instanceof ServerLevel serverLevel) {
            ticksSinceSync = 0;
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        energyStorage.amount = input.getLongOr("Energy", 0L);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        output.putLong("Energy", this.energyStorage.amount);
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyProvider(Direction direction) {
        return this.energyStorage;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
