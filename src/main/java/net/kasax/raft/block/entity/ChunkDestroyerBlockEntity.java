package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.util.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
            if (this.level == null || this.level.isClientSide()) {
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
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();

        for (int y = this.miningPos.getY(); y >= this.level.getMinY(); y--) {
            currentPos.set(this.miningPos.getX(), y, this.miningPos.getZ());

            BlockState state = this.level.getBlockState(currentPos);
            if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.getDestroySpeed(this.level, currentPos) < 0) {
                continue;
            }

            List<ItemStack> blockDrops = new ArrayList<>(state.getDrops(
                    new LootParams.Builder((ServerLevel) this.level)
                            .withParameter(LootContextParams.TOOL, Items.DIAMOND_PICKAXE.getDefaultInstance())
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(currentPos))
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this.level.getBlockEntity(currentPos))));

            this.level.destroyBlock(currentPos, false);
            drops.addAll(blockDrops);
        }

        // Consume energy for the entire column mined
        this.energyStorage.amount -= 100_000;

        // Handle drops
        Storage<ItemVariant> aboveStorage = findItemStorage((ServerLevel) this.level, this.worldPosition.above());
        if (aboveStorage != null && aboveStorage.supportsInsertion()) {
            insertDrops(drops, aboveStorage);
        }
        if (!drops.isEmpty()) {
            spawnDrops(drops, (ServerLevel) this.level, this.worldPosition);
        }

        moveToNextColumn();
    }

    private void moveToNextColumn() {
        miningPos = miningPos.offset(1, 0, 0);

        // Move to the next column when reaching the chunk border
        if (miningPos.getX() >= (chunkX << 4) + 16) {
            miningPos = new BlockPos(chunkX << 4, miningPos.getY(), miningPos.getZ() + 1);
        }

        // If we've finished the chunk, mark as complete
        if (miningPos.getZ() >= (chunkZ << 4) + 16) {
            miningComplete = true;
        }

        // Reset Y position to the top for the next column
        miningPos = new BlockPos(miningPos.getX(), this.worldPosition.getY() - 1, miningPos.getZ());
    }

    private void update() {
        setChanged();
        if(level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyProvider(Direction direction) {
        return this.energyStorage;
    }


    // Replaces any water or lava blocks inside the mined chunk area with air
    private void replaceWaterInMinedArea() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Iterate over all X and Z coordinates within the chunk, and from current Y down to bottom
        for (int x = (chunkX << 4); x < (chunkX << 4) + 16; x++) {
            for (int z = (chunkZ << 4); z < (chunkZ << 4) + 16; z++) {
                for (int y = this.worldPosition.getY(); y >= this.level.getMinY(); y--) { // Loop from current Y to bottom
                    mutablePos.set(x, y, z); // Set the current block position

                    BlockState blockState = this.level.getBlockState(mutablePos);
                    if (!blockState.getFluidState().isEmpty()) {
                        this.level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); // Replace with air
                    }
                }
            }
        }
    }

    // Replaces water/lava blocks just outside the mined chunk area with glass, forming a border
    private void replaceWaterOnWalls() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Define the area just outside the chunk (the border/walls)
        for (int x = (chunkX << 4) - 1; x <= (chunkX << 4) + 16; x++) {
            for (int z = (chunkZ << 4) - 1; z <= (chunkZ << 4) + 16; z++) {
                // Ensure we're outside the chunk to create the walls, not inside the chunk
                if ((x >= (chunkX << 4) && x < (chunkX << 4) + 16) && (z >= (chunkZ << 4) && z < (chunkZ << 4) + 16)) {
                    continue; // Skip the chunk's inner area
                }

                // Iterate from the current Y position down to the world's bottom
                for (int y = this.worldPosition.getY(); y >= this.level.getMinY(); y--) {
                    mutablePos.set(x, y, z); // Set the current block position

                    BlockState blockState = this.level.getBlockState(mutablePos);
                    if (!blockState.getFluidState().isEmpty()) {
                        this.level.setBlock(mutablePos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL); // Replace with glass
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ticks = input.getIntOr("Ticks", 0);
        input.getLong("MiningPos").ifPresent(l -> this.miningPos = BlockPos.of(l));
        this.energyStorage.amount = input.getLongOr("Energy", 0L);
        this.chunkX = input.getIntOr("ChunkX", this.chunkX);
        this.chunkZ = input.getIntOr("ChunkZ", this.chunkZ);
        this.finished = input.getBooleanOr("finished", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Ticks", this.ticks);
        output.putLong("MiningPos", this.miningPos.asLong());
        output.putLong("Energy", this.energyStorage.amount);
        output.putInt("ChunkX", this.chunkX);
        output.putInt("ChunkZ", this.chunkZ);
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
