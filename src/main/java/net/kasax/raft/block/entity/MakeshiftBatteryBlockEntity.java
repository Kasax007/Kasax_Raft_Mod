package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.Raft;
import net.kasax.raft.screen.MakeshiftBatteryScreenHandler;
import net.kasax.raft.util.BlockPosPayload;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class MakeshiftBatteryBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPosPayload> {

    public static final Text TITLE = Text.translatable("container.raft.makeshift_battery");

    public MakeshiftBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAKESHIT_BATTERY_BLOCK_ENTITY, pos, state);
    }

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(1_000_000, 10_000, 10_000) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();

            update();
        }
    };

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Energy", NbtElement.LONG_TYPE)) {
            this.energyStorage.amount = nbt.getLong("Energy");
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("Energy", this.energyStorage.amount);
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyProvider(Direction direction) {
        return this.energyStorage;
    }

    @Override
    public BlockPosPayload getScreenOpeningData(ServerPlayerEntity player) {
        return new BlockPosPayload(this.pos);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MakeshiftBatteryScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return TITLE;
    }


    public void tick(World world, BlockPos pos, BlockState state) {
        if(this.world == null || this.world.isClient)
            return;
        update();

    }

    private void update() {
        markDirty();
        if(world != null)
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        System.out.println(this.energyStorage.amount);
    }
}
