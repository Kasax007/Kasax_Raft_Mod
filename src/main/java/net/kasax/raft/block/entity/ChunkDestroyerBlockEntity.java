package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.util.StoredEnergy;
import net.kasax.raft.util.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
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
    public static final long CAPACITY = 500_000;
    /** FE for a column in which at least one block is mined. Columns with nothing to mine are free (3.4.2). */
    public static final long ENERGY_PER_COLUMN = 100_000;
    /** Ticks between two mined columns. */
    public static final int TICKS_PER_COLUMN = 200;
    /** Empty columns passed over per tick: they take no mining time, but a whole empty chunk is not checked at once. */
    private static final int EMPTY_COLUMNS_PER_TICK = 16;

    private int ticks = 0;
    private BlockPos miningPos;
    private int chunkX, chunkZ; // Chunk coordinates
    private boolean miningComplete = false;
    private boolean finished = false;
    /** The current column was found to hold something to mine (not saved: it is checked again after loading). */
    private boolean columnHasBlocks = false;

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, 100_000, 0) {
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

    /**
     * Mines one column every {@link #TICKS_PER_COLUMN} ticks for {@link #ENERGY_PER_COLUMN} FE. Columns with nothing to
     * mine (only air, water, lava or unbreakable blocks) are passed over at once and cost nothing, also when the
     * machine is out of energy, so empty parts of the chunk never hold it up.
     */
    @Override
    public void tick() {
        if (finished || this.level == null || this.level.isClientSide()) {
            return;
        }
        if (miningComplete) {
            finished = true; // Stop further actions once complete
            replaceWaterInMinedArea();
            replaceWaterOnWalls();
            setChanged();
            return;
        }

        int skipped = 0;
        while (!columnHasBlocks) {
            if (columnHasSomethingToMine()) {
                columnHasBlocks = true;
            } else {
                moveToNextColumn();
                setChanged();
                if (miningComplete || ++skipped >= EMPTY_COLUMNS_PER_TICK) {
                    return;
                }
            }
        }

        // Check if enough energy to mine a column
        if (this.energyStorage.amount < ENERGY_PER_COLUMN) {
            return; // Not enough energy
        }

        if (this.ticks++ % TICKS_PER_COLUMN == 0) {
            mineColumn();
        }
    }

    /** Air, water, lava (bubble columns too) and unbreakable blocks such as bedrock are left as they are. */
    private boolean canMine(BlockState state, BlockPos pos) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && !state.is(Blocks.BUBBLE_COLUMN)
                && state.getDestroySpeed(this.level, pos) >= 0;
    }

    private boolean columnHasSomethingToMine() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = this.miningPos.getY(); y >= this.level.getMinY(); y--) {
            pos.set(this.miningPos.getX(), y, this.miningPos.getZ());
            if (canMine(this.level.getBlockState(pos), pos)) {
                return true;
            }
        }
        return false;
    }

    private void mineColumn() {
        List<ItemStack> drops = new ArrayList<>();
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        int mined = 0;

        for (int y = this.miningPos.getY(); y >= this.level.getMinY(); y--) {
            currentPos.set(this.miningPos.getX(), y, this.miningPos.getZ());

            BlockState state = this.level.getBlockState(currentPos);
            if (!canMine(state, currentPos)) {
                continue;
            }

            List<ItemStack> blockDrops = new ArrayList<>(state.getDrops(
                    new LootParams.Builder((ServerLevel) this.level)
                            .withParameter(LootContextParams.TOOL, Items.DIAMOND_PICKAXE.getDefaultInstance())
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(currentPos))
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this.level.getBlockEntity(currentPos))));

            if (this.level.destroyBlock(currentPos, false)) {
                mined++;
                drops.addAll(blockDrops);
            }
        }

        // Only a column in which something was mined costs energy (it may have been dug out since it was checked)
        if (mined > 0) {
            this.energyStorage.amount -= ENERGY_PER_COLUMN;
            setChanged();
        }

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
        columnHasBlocks = false;
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

    // 3.4.2: the chunk destroyer keeps its energy when it is broken (see StoredEnergy)
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
        // past the last column: the mining is done (the water clean-up was still to come when it was saved)
        this.miningComplete = this.miningPos.getZ() >= (this.chunkZ << 4) + 16;
        this.columnHasBlocks = false;
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
