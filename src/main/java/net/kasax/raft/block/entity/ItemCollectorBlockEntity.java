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
import net.minecraft.world.item.Item;
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
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemCollectorBlockEntity extends BlockEntity implements net.kasax.raft.compat.RemovalSideEffects, MenuProvider, ImplementedInventory {
    public static final BooleanProperty HAS_NET = BooleanProperty.create("has_net");
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

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

    public ItemStack getRenderStack() {
        return this.getItem(OUTPUT_SLOT);
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

    /** Hoppers may only insert nets into the top slot and only take the catch from the bottom slot. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.@org.jetbrains.annotations.Nullable Direction side) {
        return slot == INPUT_SLOT && stack.is(ModItems.NET);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == OUTPUT_SLOT;
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
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        ContainerHelper.saveAllItems(output.tag(), inventory, output.lookup());
        output.putInt("item_collector.progress", progress);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        // loadAllItems only sets non-empty slots: clear first, or an emptied slot keeps its old item on the client
        for (int i = 0; i < inventory.size(); i++) inventory.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.tag(), inventory, input.lookup());
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
        if (isOutputSlotEmptyOrReceivable() && this.hasRecipe(serverLevel)) {
            // the net works faster the faster the raft sails (1x at 1 block/s, up to 4x)
            this.progress += 1 + Math.min(3, (int) (speed * 4));
            setChanged(level, pos, state);
            if (hasCraftingFinished()) {
                this.craftItemKeepInput(serverLevel);
                this.resetProgress();
            }
        }
    }

    /** Puts a caught stack into the output slot if it fits there. */
    private boolean insertCatch(ItemStack stack) {
        ItemStack out = this.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            this.setItem(OUTPUT_SLOT, stack);
            return true;
        }
        if (ItemStack.isSameItemSameComponents(out, stack) && out.getCount() + stack.getCount() <= out.getMaxStackSize()) {
            out.grow(stack.getCount());
            return true;
        }
        return false;
    }

    /**
     * Collects floating ocean debris that the catcher sails through. The items go to the output slot,
     * as long as the slot holds the same item (or nothing) and has room. On a sailing raft the catcher's block is in
     * the raft's shipyard plot: the search is around where that block is in the world.
     */
    private void catchDebris(ServerLevel level, BlockPos pos) {
        Vec3 world = net.kasax.raft.content.vessel.VesselShipyard.toWorld(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (world == null) {
            return;
        }
        AABB area = new AABB(world, world).inflate(2.0, 2.0, 2.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, OceanDebris::isDebris)) {
            ItemStack stack = item.getItem();
            ItemStack out = this.getItem(OUTPUT_SLOT);
            if (out.isEmpty()) {
                this.setItem(OUTPUT_SLOT, stack.copy());
                item.discard();
            } else if (ItemStack.isSameItemSameComponents(out, stack)) {
                int moved = Math.min(out.getMaxStackSize() - out.getCount(), stack.getCount());
                if (moved <= 0) {
                    continue;
                }
                out.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
            } else {
                continue;
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
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(this.inventory.getFirst()), level.registryAccess());

        ItemStack inputStack = this.getItem(INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            // Damage the net in the input slot by 1 (it is removed when it breaks).
            inputStack.hurtAndBreak(1, level, null, item -> {});
            if (inputStack.isEmpty()) {
                this.setItem(INPUT_SLOT, ItemStack.EMPTY);
            }
        }

        // Add the result to the output slot
        this.setItem(OUTPUT_SLOT, result.copyWithCount(this.getItem(OUTPUT_SLOT).getCount() + result.getCount()));
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
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(this.inventory.getFirst()), level.registryAccess());
        return canInsertAmountIntoOutputSlot(result) && canInsertItemIntoOutputSlot(result.getItem());
    }

    private Optional<RecipeHolder<ItemCatchingRecipe>> getCurrentRecipe(ServerLevel level) {
        return this.matchGetter.getRecipeFor(new SingleRecipeInput(this.inventory.getFirst()), level);
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.getItem(OUTPUT_SLOT).getItem() == item || this.getItem(OUTPUT_SLOT).isEmpty();
    }

    private boolean canInsertAmountIntoOutputSlot(ItemStack result) {
        return this.getItem(OUTPUT_SLOT).getCount() + result.getCount() <= getItem(OUTPUT_SLOT).getMaxStackSize();
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.getItem(OUTPUT_SLOT).isEmpty() || this.getItem(OUTPUT_SLOT).getCount() < this.getItem(OUTPUT_SLOT).getMaxStackSize();
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
