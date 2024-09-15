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

public class ChunkDestroyerBlockEntity extends BlockEntity implements TickableBlockEntity {
    private int ticks = 0;
    private BlockPos miningPos;
    private int chunkX, chunkZ; // Chunk coordinates
    private boolean miningComplete = false;
    private boolean finished = false;

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(500_000, 100_000, 0) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            update();
        }
    };

    public ChunkDestroyerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHUNK_DESTROYER_BLOCK_ENTITY, pos, state);
        this.chunkX = pos.getX() >> 4; // Get chunk X coordinate
        this.chunkZ = pos.getZ() >> 4; // Get chunk Z coordinate
        this.miningPos = new BlockPos(chunkX << 4, pos.getY() - 1, chunkZ << 4); // Start at the top of the first column
    }

    @Override
    public void tick() {
        if (!finished) {
            if (this.world == null || this.world.isClient) {
                return;
            }

            // Check if enough energy to mine a column
            if (this.energyStorage.amount < 100_000) {
                return; // Not enough energy
            }

            if (miningComplete) {
                finished = true; // Stop further actions once complete
                replaceWaterInMinedArea();
                replaceWaterOnWalls();
                return;
            }

            if (this.ticks++ % 200 == 0) { // Mining every second
                mineColumn();
            }
        }
    }

    private void mineColumn() {
        List<ItemStack> drops = new ArrayList<>();
        BlockPos.Mutable currentPos = new BlockPos.Mutable();

        for (int y = this.miningPos.getY(); y >= this.world.getBottomY(); y--) {
            currentPos.set(this.miningPos.getX(), y, this.miningPos.getZ());

            BlockState state = this.world.getBlockState(currentPos);
            if (state.isAir() || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA || state.getHardness(this.world, this.miningPos) < 0) {
                continue;
            }

            List<ItemStack> blockDrops = new ArrayList<>(state.getDroppedStacks(
                    new LootContextParameterSet.Builder((ServerWorld) this.world)
                            .add(LootContextParameters.TOOL, Items.DIAMOND_PICKAXE.getDefaultStack())
                            .add(LootContextParameters.ORIGIN, currentPos.toCenterPos())
                            .addOptional(LootContextParameters.BLOCK_ENTITY, this)));

            this.world.breakBlock(currentPos, false);
            drops.addAll(blockDrops);
        }

        // Consume energy for the entire column mined
        this.energyStorage.amount -= 100_000;

        // Handle drops
        Storage<ItemVariant> aboveStorage = findItemStorage((ServerWorld) this.world, this.pos.up());
        if (aboveStorage != null && aboveStorage.supportsInsertion()) {
            insertDrops(drops, aboveStorage);
        }
        if (!drops.isEmpty()) {
            spawnDrops(drops, (ServerWorld) this.world, this.pos);
        }

        moveToNextColumn();
    }

    private void moveToNextColumn() {
        miningPos = miningPos.add(1, 0, 0);

        // Move to the next column when reaching the chunk border
        if (miningPos.getX() >= (chunkX << 4) + 16) {
            miningPos = new BlockPos(chunkX << 4, miningPos.getY(), miningPos.getZ() + 1);
        }

        // If we've finished the chunk, mark as complete
        if (miningPos.getZ() >= (chunkZ << 4) + 16) {
            miningComplete = true;
        }

        // Reset Y position to the top for the next column
        miningPos = new BlockPos(miningPos.getX(), this.pos.getY() - 1, miningPos.getZ());
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


    // Replaces any water or lava blocks inside the mined chunk area with air
    private void replaceWaterInMinedArea() {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // Iterate over all X and Z coordinates within the chunk, and from current Y down to bottom
        for (int x = (chunkX << 4); x < (chunkX << 4) + 16; x++) {
            for (int z = (chunkZ << 4); z < (chunkZ << 4) + 16; z++) {
                for (int y = this.pos.getY(); y >= this.world.getBottomY(); y--) { // Loop from current Y to bottom
                    mutablePos.set(x, y, z); // Set the current block position

                    BlockState blockState = this.world.getBlockState(mutablePos);
                    if (blockState.getBlock() == Blocks.WATER || blockState.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(mutablePos, Blocks.AIR.getDefaultState()); // Replace with air
                    }
                }
            }
        }
    }

    // Replaces water/lava blocks just outside the mined chunk area with glass, forming a border
    private void replaceWaterOnWalls() {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // Define the area just outside the chunk (the border/walls)
        for (int x = (chunkX << 4) - 1; x <= (chunkX << 4) + 16; x++) {
            for (int z = (chunkZ << 4) - 1; z <= (chunkZ << 4) + 16; z++) {
                // Ensure we're outside the chunk to create the walls, not inside the chunk
                if ((x >= (chunkX << 4) && x < (chunkX << 4) + 16) && (z >= (chunkZ << 4) && z < (chunkZ << 4) + 16)) {
                    continue; // Skip the chunk's inner area
                }

                // Iterate from the current Y position down to the world's bottom
                for (int y = this.pos.getY(); y >= this.world.getBottomY(); y--) {
                    mutablePos.set(x, y, z); // Set the current block position

                    BlockState blockState = this.world.getBlockState(mutablePos);
                    if (blockState.getBlock() == Blocks.WATER || blockState.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(mutablePos, Blocks.GLASS.getDefaultState()); // Replace with glass
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
        if (nbt.contains("ChunkX")) {
            this.chunkX = nbt.getInt("ChunkX");
        }
        if (nbt.contains("ChunkZ")) {
            this.chunkZ = nbt.getInt("ChunkZ");
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
        nbt.putInt("ChunkX", this.chunkX);
        nbt.putInt("ChunkZ", this.chunkZ);
        nbt.putBoolean("finished", this.finished);
    }

    public BlockPos getMiningPos() {
        return this.miningPos;
    }
}