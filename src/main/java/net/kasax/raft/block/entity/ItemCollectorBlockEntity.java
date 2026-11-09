package net.kasax.raft.block.entity;

import net.kasax.raft.block.custom.ItemCatcher;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.recipe.ItemCatchingRecipe;
import net.kasax.raft.recipe.ModRecipes;
import net.kasax.raft.screen.ItemCollectorScreenHandler;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.kasax.raft.world.OceanDebris;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ItemCollectorBlockEntity extends BlockEntity implements MenuProvider, ImplementedInventory {
    public static final BooleanProperty HAS_NET = BooleanProperty.create("has_net");

    public static final int INPUT_SLOT = 0;
    /**
     * 3.5: nine slots for the catch (slots 1 to 9; before, slot 1 was the only one). A catcher saved by an older version
     * keeps its catch: its old output slot 1 is the first of the nine.
     */
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int OUTPUT_SLOTS = 9;
    public static final int SIZE = FIRST_OUTPUT_SLOT + OUTPUT_SLOTS;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    protected final ContainerData propertyDelegate;
    private int progress = 0;
    private int maxProgress = 576;

    private final RecipeManager.CachedCheck<SingleRecipeInput, ItemCatchingRecipe> matchGetter =
            RecipeManager.createCheck(ModRecipes.ITEM_CATCHING_TYPE);

    public ItemCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_COLLECTOR_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> ItemCollectorBlockEntity.this.progress;
                    case 1 -> ItemCollectorBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> ItemCollectorBlockEntity.this.progress = value;
                    case 1 -> ItemCollectorBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    /** The newest kind of catch, drawn above the catcher: the last filled of the nine slots. */
    public ItemStack getRenderStack() {
        for (int slot = SIZE - 1; slot >= FIRST_OUTPUT_SLOT; slot--) {
            if (!this.getItem(slot).isEmpty()) {
                return this.getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot < SIZE;
    }

    public void updateBlockState() {
        Level level = this.level;
        if (level == null || level.isClientSide()) return;
        BlockState currentState = level.getBlockState(this.worldPosition);
        if (!currentState.hasProperty(HAS_NET)) return;
        BlockState newState = currentState.setValue(HAS_NET, this.hasNetInInputSlot());
        if (currentState != newState) {
            level.setBlock(this.worldPosition, newState, 3);
        }
    }

    /** Hoppers may only insert nets into the top slot and only take the catch from the nine slots below. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.@org.jspecify.annotations.Nullable Direction side) {
        return slot == INPUT_SLOT && stack.is(ModItems.NET);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return isOutputSlot(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && stack.is(ModItems.NET);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("raft.item_collector_display_name");
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, inventory);
        output.putInt("item_collector.progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // loadAllItems only sets non-empty slots: clear first, or an emptied slot keeps its old item on the client
        for (int i = 0; i < inventory.size(); i++) inventory.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, inventory);
        progress = input.getIntOr("item_collector.progress", 0);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateBlockState();
        // the caught item is drawn on top of the catcher: tell the clients when the slots change (GUI, hopper)
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }

    public boolean hasNetInInputSlot() {
        return this.getItem(INPUT_SLOT).is(ModItems.NET);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ItemCollectorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    /** Below this speed (blocks per tick) the net catches nothing: an item catcher works while its raft sails. */
    public static final float MIN_SPEED = 0.05F;
    /** Chance per tick and block-per-tick of speed to net a piece of flotsam out of the sea (about one a minute at 6 blocks/s). */
    public static final float CATCH_PER_SPEED = 0.0025F;
    /** Shortest time between two catches. */
    public static final int CATCH_COOLDOWN = 200;
    private int cooldown;

    /**
     * Speed of the raft this catcher sails on (a sailing raft built by the player: the catcher is in its shipyard
     * plot), 0 when it stands in the world, the raft holds still or its anchor is down.
     */
    private static float raftSpeed(Level level, BlockPos pos) {
        if (!net.kasax.raft.content.vessel.VesselShipyard.maybePlot(pos.getX(), pos.getZ())) {
            return 0.0F;
        }
        var vessel = net.kasax.raft.content.vessel.VesselShipyard.vesselAt(level, pos);
        if (vessel == null || vessel.isDocked() || vessel.structure() == null || vessel.structure().anchorDown()) {
            return 0.0F;
        }
        return Math.abs(vessel.getSpeed());
    }

    /**
     * 3.2: the item catcher only works while its raft is moving (a raft you built and set sail with): the faster it
     * sails, the more it catches. It picks up flotsam it sails through, now and then nets a piece out of the sea
     * (cooldown {@link #CATCH_COOLDOWN}), and its net slowly fills with unrefined ocean garbage (the item catching
     * recipe, faster at speed). Standing still, anchored or on land it catches nothing.
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        float speed = raftSpeed(level, pos);
        if (speed < MIN_SPEED) {
            if (this.progress != 0) {
                this.resetProgress();
                setChanged(level, pos, state);
            }
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (level.getGameTime() % 5 == 0) {
            catchDebris(serverLevel, pos);
        }
        if (this.cooldown == 0 && this.hasNetInInputSlot() && serverLevel.getRandom().nextFloat() < speed * CATCH_PER_SPEED) {
            ItemStack caught = OceanDebris.randomDebris(serverLevel, serverLevel.getRandom());
            if (!caught.isEmpty() && this.insertCatch(caught)) {
                this.cooldown = CATCH_COOLDOWN;
                level.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.4F, 1.2F);
                setChanged(level, pos, state);
            }
        }
        if (this.hasRecipe(serverLevel)) {
            // the net works faster the faster the raft sails (1x at 1 block/s, up to 4x)
            this.progress += 1 + Math.min(3, (int) (speed * 4));
            setChanged(level, pos, state);
            if (hasCraftingFinished()) {
                this.craftItemKeepInput(serverLevel);
                this.resetProgress();
            }
        }
    }

    /** How many of the stack fit into the nine output slots (onto the same item, then into empty slots). */
    private int roomFor(ItemStack stack) {
        int room = 0;
        for (int slot = FIRST_OUTPUT_SLOT; slot < SIZE && room < stack.getCount(); slot++) {
            ItemStack out = this.getItem(slot);
            if (out.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(out, stack)) {
                room += Math.max(0, out.getMaxStackSize() - out.getCount());
            }
        }
        return Math.min(room, stack.getCount());
    }

    /**
     * Puts as much of the stack as fits into the output slots: first onto stacks of the same item, then into the first
     * empty slot. Shrinks the given stack by what went in and returns that number; tells the clients when it changed.
     */
    private int addToOutputs(ItemStack stack) {
        int before = stack.getCount();
        for (int slot = FIRST_OUTPUT_SLOT; slot < SIZE && !stack.isEmpty(); slot++) {
            ItemStack out = this.getItem(slot);
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, stack)) {
                int moved = Math.min(stack.getCount(), out.getMaxStackSize() - out.getCount());
                if (moved > 0) {
                    out.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = FIRST_OUTPUT_SLOT; slot < SIZE && !stack.isEmpty(); slot++) {
            if (this.getItem(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.getItems().set(slot, stack.split(moved));
            }
        }
        int added = before - stack.getCount();
        if (added > 0) {
            setChanged();
        }
        return added;
    }

    /** Puts a caught stack into the output slots if all of it fits there. */
    private boolean insertCatch(ItemStack stack) {
        if (roomFor(stack) < stack.getCount()) {
            return false;
        }
        addToOutputs(stack.copy());
        return true;
    }

    /**
     * Collects floating ocean debris that the catcher sails through. The items go to the nine output slots: onto the
     * same item while it has room, else into an empty slot. On a sailing raft the catcher's block is in the raft's
     * shipyard plot: the search is around where that block is in the world.
     */
    private void catchDebris(ServerLevel level, BlockPos pos) {
        Vec3 world = net.kasax.raft.content.vessel.VesselShipyard.toWorld(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (world == null) {
            return;
        }
        AABB area = new AABB(world, world).inflate(2.0, 2.0, 2.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, OceanDebris::isDebris)) {
            ItemStack stack = item.getItem().copy();
            if (addToOutputs(stack) <= 0) {
                continue;
            }
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 1.2F);
            setChanged(level, pos, getBlockState());
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private void craftItemKeepInput(ServerLevel level) {
        Optional<RecipeHolder<ItemCatchingRecipe>> recipe = getCurrentRecipe(level);
        if (recipe.isEmpty()) return;
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(this.inventory.getFirst()));

        ItemStack inputStack = this.getItem(INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            // Damage the net in the input slot by 1 (it is removed when it breaks).
            inputStack.hurtAndBreak(1, level, null, item -> {});
            if (inputStack.isEmpty()) {
                this.setItem(INPUT_SLOT, ItemStack.EMPTY);
            }
        }

        // Add the result to the output slots
        addToOutputs(result.copy());
        updateBlockState();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean hasCraftingFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftProgress() {
        progress++;
    }

    private boolean hasRecipe(ServerLevel level) {
        Optional<RecipeHolder<ItemCatchingRecipe>> recipe = getCurrentRecipe(level);
        if (recipe.isEmpty()) return false;
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(this.inventory.getFirst()));
        return !result.isEmpty() && roomFor(result) >= result.getCount();
    }

    private Optional<RecipeHolder<ItemCatchingRecipe>> getCurrentRecipe(ServerLevel level) {
        return this.matchGetter.getRecipeFor(new SingleRecipeInput(this.inventory.getFirst()), level);
    }

    //Sync for client renderer of item
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
