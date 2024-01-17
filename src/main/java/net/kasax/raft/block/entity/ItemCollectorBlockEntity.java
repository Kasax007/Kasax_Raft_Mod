package net.kasax.raft.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.kasax.raft.Raft;
import net.kasax.raft.block.custom.ItemCatcher;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.screen.ItemCollectorScreenHandler;
import net.kasax.raft.world.biome.ModBiomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

public class ItemCollectorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory {
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
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt("item_collector.progress");
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
        ItemStack result = new ItemStack(ModItems.SOME_GAME_MUSIC_MUSIC_DISC);

        this.setStack(OUTPUT_SLOT, new ItemStack(result.getItem(), getStack(OUTPUT_SLOT).getCount() + result.getCount()));
    }

    private boolean hasCraftingFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        ItemStack result = new ItemStack(ModItems.SOME_GAME_MUSIC_MUSIC_DISC);
        boolean hasInput = getStack(INPUT_SLOT).getItem() == ModItems.ANCIENT_CRYSTAL;

        return hasInput && canInsertAmountIntoOutputSlot(result) && canInsertItemIntoOutputSlot(result.getItem());
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
}
