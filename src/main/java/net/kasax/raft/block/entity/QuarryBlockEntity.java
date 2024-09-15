package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.util.TickableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class QuarryBlockEntity extends BlockEntity implements TickableBlockEntity {
    private int ticks = 0;
    private BlockPos miningPos = this.pos.down(); // Starting at the block directly below
    private int startX, startZ; // Variables to track X and Z offsets for the 10x10 area
    private boolean miningComplete = false;
    private boolean finished = false;


    private final int MINING_AREA_SIZE = 11; // 11X11 area
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(10_000, 1000, 0) {
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
        this.miningPos = pos.add(startX, -1, startZ); // Start mining from the top of the first column
    }

    private void update() {
        markDirty();
        if(world != null)
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
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
            if (this.world == null || this.world.isClient) {
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
                if (this.miningPos.getY() <= this.world.getBottomY()) {
                    moveToNextColumn();
                    return;
                }

                BlockState state = this.world.getBlockState(this.miningPos);
                while (state.isAir() || state.getHardness(this.world, this.miningPos) < 0 || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) {
                    this.miningPos = this.miningPos.down();
                    state = this.world.getBlockState(this.miningPos);
                    if (this.miningPos.getY() <= this.world.getBottomY()) {
                        moveToNextColumn();
                        return;
                    }
                }

                // Get drops and break the block
                List<ItemStack> drops = new ArrayList<>(state.getDroppedStacks(
                        new LootContextParameterSet.Builder((ServerWorld) this.world)
                                .add(LootContextParameters.TOOL, Items.DIAMOND_PICKAXE.getDefaultStack())
                                .add(LootContextParameters.ORIGIN, this.miningPos.toCenterPos())
                                .addOptional(LootContextParameters.BLOCK_ENTITY, this)));

                this.world.breakBlock(this.miningPos, false);
                this.energyStorage.amount -= 1000;

                Storage<ItemVariant> aboveStorage = findItemStorage((ServerWorld) this.world, this.pos.up());
                if (aboveStorage != null && aboveStorage.supportsInsertion()) {
                    insertDrops(drops, aboveStorage);
                }

                if (!drops.isEmpty()) {
                    spawnDrops(drops, (ServerWorld) this.world, this.pos);
                }

                // Move to the next block in the column
                this.miningPos = this.miningPos.down();
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
        this.miningPos = this.pos.add(startX, -1, startZ);
    }


    // Replaces any water blocks inside the mined area with air
    private void replaceWaterInMinedArea() {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int x = -MINING_AREA_SIZE / 2; x <= MINING_AREA_SIZE / 2; x++) {
            for (int z = -MINING_AREA_SIZE / 2; z <= MINING_AREA_SIZE / 2; z++) {
                for (int y = this.pos.getY(); y >= this.world.getBottomY(); y--) { // Looping top to bottom
                    mutablePos.set(this.pos.getX() + x, y, this.pos.getZ() + z); // Set Y explicitly
                    BlockState blockState = this.world.getBlockState(mutablePos);
                    if (blockState.getBlock() == Blocks.WATER || blockState.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(mutablePos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }



    // Replaces any water blocks on the walls of the hole with glass
    private void replaceWaterOnWalls() {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int outerRadius = MINING_AREA_SIZE / 2 + 1; // Walls just outside the mining area

        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                // Skip blocks inside the 11x11 area; we only want the outer walls
                if (Math.abs(x) <= MINING_AREA_SIZE / 2 && Math.abs(z) <= MINING_AREA_SIZE / 2) {
                    continue;
                }
                // Iterate down from the top of the world to the bottom of the world
                for (int y = this.pos.getY(); y >= this.world.getBottomY(); y--) { // Correctly loop to the bottom
                    //System.out.println(y);
                    //System.out.println("bottom " + this.world.getBottomY());
                    mutablePos.set(this.pos.getX() + x, y, this.pos.getZ() + z); // Set Y explicitly
                    BlockState blockState = this.world.getBlockState(mutablePos);
                    if (blockState.getBlock() == Blocks.WATER || blockState.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(mutablePos, Blocks.GLASS.getDefaultState());
                    }
                }
            }
        }
    }


    private static Storage<ItemVariant> findItemStorage(ServerWorld world, BlockPos pos) {
        return ItemStorage.SIDED.find(world, pos, Direction.DOWN);
    }

    private static void insertDrops(List<ItemStack> drops, Storage<ItemVariant> aboveStorage) {
        for (ItemStack drop : drops) {
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = aboveStorage.insert(ItemVariant.of(drop), drop.getCount(), transaction);
                if (inserted > 0) {
                    drop.decrement((int) inserted);
                    transaction.commit();
                }
            }
        }

        drops.removeIf(ItemStack::isEmpty);
    }

    private static void spawnDrops(List<ItemStack> drops, ServerWorld world, BlockPos pos) {
        for (ItemStack drop : drops) {
            ItemScatterer.spawn(world, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 1.0D, drop);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.ticks = nbt.getInt("Ticks");
        this.miningPos = BlockPos.fromLong(nbt.getLong("MiningPos"));
        if (nbt.contains("Energy", NbtElement.LONG_TYPE)) {
            this.energyStorage.amount = nbt.getLong("Energy");
        }
        if (nbt.contains("StartX")) {
            this.startX = nbt.getInt("StartX");
        }
        if (nbt.contains("StartZ")) {
            this.startZ = nbt.getInt("StartZ");
        }
        if (nbt.contains("finished")) {
            this.finished = nbt.getBoolean("finished");
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Ticks", this.ticks);
        nbt.putLong("MiningPos", this.miningPos.asLong());
        nbt.putLong("Energy", this.energyStorage.amount);
        nbt.putInt("StartX", this.startX);
        nbt.putInt("StartZ", this.startZ);
        nbt.putBoolean("finished", this.finished);
    }

    public BlockPos getMiningPos() {
        return this.miningPos;
    }
}