package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.screen.MakeshiftBatteryScreenHandler;
import net.kasax.raft.util.BlockPosPayload;
import net.kasax.raft.util.EnergySyncPayload;
import net.kasax.raft.util.StoredEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class MakeshiftBatteryBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPosPayload> {

    public static final Component TITLE = Component.translatable("container.raft.makeshift_battery");
    public static final long CAPACITY = 1_000_000;

    public MakeshiftBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAKESHIFT_BATTERY_BLOCK_ENTITY, pos, state);
    }

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, 10_000, 10_000) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();

            update();
        }
    };

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStorage.amount = input.getLongOr("Energy", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", this.energyStorage.amount);
    }

    // 3.4.2: the battery keeps its energy when it is broken (see StoredEnergy)
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.energyStorage.amount = StoredEnergy.fromItem(components, this.energyStorage.getCapacity());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        StoredEnergy.toItem(components, this.energyStorage.amount);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        StoredEnergy.removeFromTag(output);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyProvider(Direction direction) {
        return this.energyStorage;
    }

    @Override
    public BlockPosPayload getScreenOpeningData(ServerPlayer player) {
        return new BlockPosPayload(this.worldPosition);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new MakeshiftBatteryScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        update();

        for (Direction direction : Direction.values()) {
            // cables pull from the battery themselves, and two batteries side by side would only push the same energy back and forth
            var neighbour = this.level.getBlockEntity(this.worldPosition.relative(direction));
            if (neighbour instanceof MakeshiftBatteryBlockEntity || neighbour instanceof net.kasax.raft.block.cable.CableBlockEntity) {
                continue;
            }
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

    private long lastSyncedEnergy = -1;
    private int ticksSinceSync;

    /** Saves and syncs the energy to the clients: at most 4 times a second and only when it changed. */
    private void update() {
        if (++ticksSinceSync < 5 || this.energyStorage.amount == lastSyncedEnergy) {
            return;
        }
        ticksSinceSync = 0;
        lastSyncedEnergy = this.energyStorage.amount;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            sendEnergySync(serverLevel, this.worldPosition, this.energyStorage.amount, this.energyStorage.getCapacity());
        }
    }

    public static void sendEnergySync(ServerLevel level, BlockPos pos, long energy, long maxEnergy) {
        EnergySyncPayload payload = new EnergySyncPayload(pos, energy, maxEnergy);
        // Send the packet to all players tracking this block position
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
