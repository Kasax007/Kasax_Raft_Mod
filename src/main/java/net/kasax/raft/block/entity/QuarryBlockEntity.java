package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.util.StoredEnergy;
import net.kasax.raft.util.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class QuarryBlockEntity extends BlockEntity implements TickableBlockEntity {
    public static final long CAPACITY = 10_000;
    private int ticks = 0;
    private BlockPos miningPos = this.worldPosition.below(); // Starting at the block directly below
    private int startX, startZ; // Variables to track X and Z offsets for the 10x10 area
    private boolean miningComplete = false;
    private boolean finished = false;


    private final int MINING_AREA_SIZE = 11; // 11X11 area
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, 1000, 0) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            update();
        }
    };

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_BLOCK_ENTITY, pos, state);
        this.startX = -MINING_AREA_SIZE / 2; // Start from the center of the 10x10 grid
        this.startZ = -MINING_AREA_SIZE / 2;
        this.miningPos = pos.offset(startX, -1, startZ); // Start mining from the top of the first column
    }

    private void update() {
        setChanged();
        if(level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // 3.4.2: the quarry keeps its energy when it is broken (see StoredEnergy)
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        this.energyStorage.amount = StoredEnergy.fromItem(components.get(StoredEnergy.COMPONENT), this.energyStorage.getCapacity());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        StoredEnergy.toItem(components, this.energyStorage.amount);
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        StoredEnergy.removeFromTag(tag);
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyProvider(Direction direction) {
        return this.energyStorage;
    }

    @Override
    public void tick() {
        if (!finished) {
            if (this.level == null || this.level.isClientSide()) {
                return;
            }

            // Check if Quarry has enough energy to mine
            if (this.energyStorage.amount < 1000) {
                return; // Not enough energy, do nothing
            }

            // If we've completed the entire 10x10 area, proceed to the water replacement step
            if (miningComplete) {
                replaceWaterInMinedArea();
                replaceWaterOnWalls();
                finished = true; // Once water is replaced, stop further actions
                return;
            }

            // Continue mining logic every second (20 ticks)
            if (this.ticks++ % 20 == 0) {
                // If we hit the bottom of the world, move to the next column
                if (this.miningPos.getY() <= this.level.getMinBuildHeight()) {
                    moveToNextColumn();
                    return;
                }

                BlockState state = this.level.getBlockState(this.miningPos);
                while (state.isAir() || state.getDestroySpeed(this.level, this.miningPos) < 0 || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                    this.miningPos = this.miningPos.below();
                    state = this.level.getBlockState(this.miningPos);
                    if (this.miningPos.getY() <= this.level.getMinBuildHeight()) {
                        moveToNextColumn();
                        return;
                    }
                }

                // Get drops and break the block
                List<ItemStack> drops = new ArrayList<>(state.getDrops(
                        new LootParams.Builder((ServerLevel) this.level)
                                .withParameter(LootContextParams.TOOL, Items.DIAMOND_PICKAXE.getDefaultInstance())
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.miningPos))
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this.level.getBlockEntity(this.miningPos))));

                this.level.destroyBlock(this.miningPos, false);
                this.energyStorage.amount -= 1000;
                this.setChanged();

                Storage<ItemVariant> aboveStorage = findItemStorage((ServerLevel) this.level, this.worldPosition.above());
                if (aboveStorage != null && aboveStorage.supportsInsertion()) {
                    insertDrops(drops, aboveStorage);
                }

                if (!drops.isEmpty()) {
                    spawnDrops(drops, (ServerLevel) this.level, this.worldPosition);
                }

                // Move to the next block in the column
                this.miningPos = this.miningPos.below();
            }
        }
    }

    private void moveToNextColumn() {
        // Move to the next position in the 11x11 grid
        startX++;

        // If we've reached the end of a row, reset startX and increment startZ
        if (startX > MINING_AREA_SIZE / 2) {
            startX = -MINING_AREA_SIZE / 2;
            startZ++;
        }

        // If we've completed the entire 11x11 area, stop mining
        if (startZ > MINING_AREA_SIZE / 2) {
            miningComplete = true;
            return; // Stop mining once the area is complete
        }

        // Reset Y position to the top of the world for the next column
        this.miningPos = this.worldPosition.offset(startX, -1, startZ);
    }


    // Replaces any water blocks inside the mined area with air
    private void replaceWaterInMinedArea() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -MINING_AREA_SIZE / 2; x <= MINING_AREA_SIZE / 2; x++) {
            for (int z = -MINING_AREA_SIZE / 2; z <= MINING_AREA_SIZE / 2; z++) {
                for (int y = this.worldPosition.getY(); y >= this.level.getMinBuildHeight(); y--) { // Looping top to bottom
                    mutablePos.set(this.worldPosition.getX() + x, y, this.worldPosition.getZ() + z); // Set Y explicitly
                    BlockState blockState = this.level.getBlockState(mutablePos);
                    if (!blockState.getFluidState().isEmpty()) {
                        this.level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }



    // Replaces any water blocks on the walls of the hole with glass
    private void replaceWaterOnWalls() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int outerRadius = MINING_AREA_SIZE / 2 + 1; // Walls just outside the mining area

        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                // Skip blocks inside the 11x11 area; we only want the outer walls
                if (Math.abs(x) <= MINING_AREA_SIZE / 2 && Math.abs(z) <= MINING_AREA_SIZE / 2) {
                    continue;
                }
                // Iterate down from the top of the world to the bottom of the world
                for (int y = this.worldPosition.getY(); y >= this.level.getMinBuildHeight(); y--) { // Correctly loop to the bottom
                    //System.out.println(y);
                    //System.out.println("bottom " + this.level.getMinBuildHeight());
                    mutablePos.set(this.worldPosition.getX() + x, y, this.worldPosition.getZ() + z); // Set Y explicitly
                    BlockState blockState = this.level.getBlockState(mutablePos);
                    if (!blockState.getFluidState().isEmpty()) {
                        this.level.setBlock(mutablePos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }


    private static Storage<ItemVariant> findItemStorage(ServerLevel world, BlockPos pos) {
        return ItemStorage.SIDED.find(world, pos, Direction.DOWN);
    }

    private static void insertDrops(List<ItemStack> drops, Storage<ItemVariant> aboveStorage) {
        for (ItemStack drop : drops) {
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = aboveStorage.insert(ItemVariant.of(drop), drop.getCount(), transaction);
                if (inserted > 0) {
                    drop.shrink((int) inserted);
                    transaction.commit();
                }
            }
        }

        drops.removeIf(ItemStack::isEmpty);
    }

    private static void spawnDrops(List<ItemStack> drops, ServerLevel world, BlockPos pos) {
        for (ItemStack drop : drops) {
            Containers.dropItemStack(world, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, drop);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        this.ticks = input.getIntOr("Ticks", 0);
        input.getLong("MiningPos").ifPresent(l -> this.miningPos = BlockPos.of(l));
        this.energyStorage.amount = input.getLongOr("Energy", 0L);
        this.startX = input.getIntOr("StartX", this.startX);
        this.startZ = input.getIntOr("StartZ", this.startZ);
        this.finished = input.getBooleanOr("finished", false);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        output.putInt("Ticks", this.ticks);
        output.putLong("MiningPos", this.miningPos.asLong());
        output.putLong("Energy", this.energyStorage.amount);
        output.putInt("StartX", this.startX);
        output.putInt("StartZ", this.startZ);
        output.putBoolean("finished", this.finished);
    }

    public BlockPos getMiningPos() {
        return this.miningPos;
    }

    /** Stored energy, for the status message. */
    public long getEnergyAmount() {
        return this.energyStorage.amount;
    }
}
