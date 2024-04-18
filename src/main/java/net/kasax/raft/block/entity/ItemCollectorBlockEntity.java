package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.block.custom.ItemCatcher;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.recipe.ItemCatchingRecipe;
import net.kasax.raft.screen.ItemCollectorScreenHandler;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemCollectorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory {
    public static final Property<Integer> HAS_NET = IntProperty.of("has_net", 0, 1);
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 576;

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
        int hasNet = this.hasNetInInputSlot();
        if (world != null && pos != null) {
            BlockState currentState = world.getBlockState(pos);
            try {
                BlockState newState = currentState.with(HAS_NET, hasNet);
                if (currentState != newState) {
                    world.setBlockState(pos, newState);
                }
            }
            catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);

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
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("item_collector.progress", progress);
        nbt.putInt("item_collector.net", hasNetInInputSlot());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt("item_collector.progress");
        updateBlockState();
    }
    @Override
    public void markDirty() {
        //world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        super.markDirty();
        updateBlockState();
    }

    public int hasNetInInputSlot() {
        if (this.getStack(INPUT_SLOT).getItem() == ModItems.NET) {
            return 1;
        }
        return 0;
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

        String biomeString = world.getBiome(pos).toString();
        String targetBiomeString = ModBiomes.RAFT_OCEAN_BIOME.toString();

        String biomeKey = getBiomeKeyFromBiomeString(biomeString);
        String targetBiomeKey = getBiomeKeyFromBiomeString(targetBiomeString);

        //Raft.LOGGER.info("Biome biome is " + biomeKey + " Should match " + targetBiomeKey + " Waterlogged " + isWaterlogged + " If statement " + biomeKey.equals(targetBiomeKey));
        if (isWaterlogged && biomeKey.equals(targetBiomeKey)) {
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

    private String getBiomeKeyFromBiomeString(String biomeString) {
        int start = biomeString.indexOf('[');
        int end = biomeString.indexOf(']');
        return biomeString.substring(start + 1, end);
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private void craftItemKeepInput() {
        Optional<ItemCatchingRecipe> recipe = getCurrentRecipe();
        ItemStack inputStack = getStack(INPUT_SLOT);
        ItemStack result = recipe.get().getOutput(getWorld().getRegistryManager());

        if (!inputStack.isEmpty()) {
            // Damage the item in the input slot by 1
            inputStack.damage(1, world.random, null);

            // Check if the item is completely damaged, and if so, replace it with an empty ItemStack
            if (inputStack.getDamage() >= inputStack.getMaxDamage()) {
                setStack(INPUT_SLOT, ItemStack.EMPTY);
            } else {
                setStack(INPUT_SLOT, inputStack);
            }
        }

        // Add the result to the output slot
        setStack(OUTPUT_SLOT, new ItemStack(result.getItem(), getStack(OUTPUT_SLOT).getCount() + result.getCount()));
    }


    private boolean hasCraftingFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        Optional<ItemCatchingRecipe> recipe = getCurrentRecipe();

        if(recipe.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.get().getOutput(getWorld().getRegistryManager());

        return recipe.isPresent() && canInsertAmountIntoOutputSlot(result) && canInsertItemIntoOutputSlot(result.getItem());
    }

    private Optional<ItemCatchingRecipe> getCurrentRecipe() {
        SimpleInventory inv = new SimpleInventory(this.size());
        for(int i = 0; i < this.size(); i++) {
            inv.setStack(i, this.getStack(i));
        }

        return getWorld().getRecipeManager().getFirstMatch(ItemCatchingRecipe.Type.INSTANCE, inv, getWorld());
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
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
