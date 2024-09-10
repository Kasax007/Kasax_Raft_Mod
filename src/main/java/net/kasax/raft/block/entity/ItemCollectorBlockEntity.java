package net.kasax.raft.block.entity;

import net.kasax.raft.block.custom.ItemCatcher;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.recipe.ItemCatchingRecipe;
import net.kasax.raft.screen.ItemCollectorScreenHandler;
import net.kasax.raft.util.BlockPosPayload;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemCollectorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    public static final BooleanProperty HAS_NET = BooleanProperty.of("has_net");
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 576;

    private final RecipeManager.MatchGetter<RecipeInput, ItemCatchingRecipe> matchGetter = RecipeManager.createCachedMatchGetter(ItemCatchingRecipe.Type.INSTANCE);

    public ItemCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_COLLECTOR_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
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
            public int size() {
                return 2;
            }
        };
        updateBlockState();
    }

    public ItemStack getRenderStack() {
        return this.getStack(OUTPUT_SLOT);
    }

    public void updateBlockState() {
        if (this.world == null || this.pos == null) return;
        if (this.world.isClient()) return;
        boolean hasNet = this.hasNetInInputSlot();
        BlockState currentState = this.world.getBlockState(this.pos);
        BlockState newState = currentState.with(HAS_NET, hasNet);
        if (currentState != newState) {
            this.world.setBlockState(this.pos, newState);
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("item_collector_display_name");
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("item_collector.progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("item_collector.progress");
        updateBlockState();
    }
    @Override
    public void markDirty() {
        //world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        super.markDirty();
        updateBlockState();
    }

    public boolean hasNetInInputSlot() {
        return this.getStack(INPUT_SLOT).isOf(ModItems.NET);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemCollectorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if(world.isClient()) {
            return;
        }
        boolean isWaterlogged = state.get(ItemCatcher.WATERLOGGED);

        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);

        if (isWaterlogged && biomeEntry.matchesKey(ModBiomes.RAFT_OCEAN_BIOME)) {
            if (isOutputSlotEmptyOrReceivable()) {
                if (this.hasRecipe()) {
                    this.increaseCraftProgress();
                    markDirty(world, pos, state);

                    if (hasCraftingFinished()) {
                        this.craftItemKeepInput();
                        this.resetProgress();
                    }
                } else {
                    this.resetProgress();
                }
            } else {
                this.resetProgress();
                markDirty(world, pos, state);
            }
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private void craftItemKeepInput() {
        if (!(this.world instanceof ServerWorld serverWorld)) return;
        Optional<RecipeEntry<ItemCatchingRecipe>> recipe = getCurrentRecipe();
        ItemStack inputStack = this.getStack(INPUT_SLOT);
        ItemStack result = recipe.get().value().getResult(null).getItem().getDefaultStack();

        if (!inputStack.isEmpty()) {
            // Damage the item in the input slot by 1
            inputStack.damage(1, serverWorld, null, item -> {});

            // Check if the item is completely damaged, and if so, replace it with an empty ItemStack
            if (inputStack.getDamage() >= inputStack.getMaxDamage()) {
                this.setStack(INPUT_SLOT, ItemStack.EMPTY);
            } else {
                this.setStack(INPUT_SLOT, inputStack);
            }
        }

        // Add the result to the output slot
        this.setStack(OUTPUT_SLOT, new ItemStack(result.getItem(), this.getStack(OUTPUT_SLOT).getCount() + result.getCount()));
    }


    private boolean hasCraftingFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        Optional<RecipeEntry<ItemCatchingRecipe>> recipe = getCurrentRecipe();

        return recipe.isPresent() && canInsertAmountIntoOutputSlot(recipe.get().value().getResult(null))
                && canInsertItemIntoOutputSlot(recipe.get().value().getResult(null).getItem());
    }

    private Optional<RecipeEntry<ItemCatchingRecipe>> getCurrentRecipe() {
        return this.matchGetter.getFirstMatch(new SingleStackRecipeInput(this.inventory.getFirst()), this.getWorld());
    }


    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.getStack(OUTPUT_SLOT).getItem() == item || this.getStack(OUTPUT_SLOT).isEmpty();
    }

    private boolean canInsertAmountIntoOutputSlot(ItemStack result) {
        return this.getStack(OUTPUT_SLOT).getCount() + result.getCount() <= getStack(OUTPUT_SLOT).getMaxCount();
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.getStack(OUTPUT_SLOT).isEmpty() || this.getStack(OUTPUT_SLOT).getCount() < this.getStack(OUTPUT_SLOT).getMaxCount();
    }

    //Sync for client renderer of item
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}