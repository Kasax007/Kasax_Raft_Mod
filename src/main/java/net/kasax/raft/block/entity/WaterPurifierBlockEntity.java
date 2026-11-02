package net.kasax.raft.block.entity;

import net.kasax.raft.block.custom.WaterPurifierBlock;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.screen.WaterPurifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Turns water bottles into fresh water bottles (a water bucket gives three) while it is fuelled.
 * Slots: 0 input, 1 fuel, 2 output.
 */
public class WaterPurifierBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    private static final int[] SLOTS_UP = {SLOT_INPUT};
    private static final int[] SLOTS_DOWN = {SLOT_OUTPUT, SLOT_FUEL};
    private static final int[] SLOTS_SIDE = {SLOT_FUEL, SLOT_INPUT};

    public static final int BOTTLE_TIME = 200;
    public static final int BUCKET_TIME = 400;

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int burnTime;
    private int burnTotal;
    private int progress;
    private int progressTotal;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTotal;
                case 2 -> progress;
                case 3 -> progressTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnTotal = value;
                case 2 -> progress = value;
                case 3 -> progressTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public WaterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PURIFIER_BLOCK_ENTITY, pos, state);
    }

    // ---- what the purifier understands ----

    public static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    public static boolean isWaterInput(ItemStack stack) {
        return isWaterBottle(stack) || stack.is(Items.WATER_BUCKET);
    }

    public static int fuelValue(ItemStack stack) {
        return SimpleGrillBlockEntity.fuelValue(stack);
    }

    private static int bottlesFor(ItemStack input) {
        return input.is(Items.WATER_BUCKET) ? 3 : 1;
    }

    private boolean canProcess() {
        ItemStack input = items.get(SLOT_INPUT);
        if (!isWaterInput(input)) {
            return false;
        }
        ItemStack out = items.get(SLOT_OUTPUT);
        int gain = bottlesFor(input);
        if (out.isEmpty()) {
            return gain <= ModItems.FRESH_WATER.getDefaultMaxStackSize();
        }
        return out.is(ModItems.FRESH_WATER) && out.getCount() + gain <= out.getMaxStackSize();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, WaterPurifierBlockEntity be) {
        boolean wasLit = be.burnTime > 0;
        boolean changed = false;
        if (be.burnTime > 0) {
            be.burnTime--;
        }

        boolean canProcess = be.canProcess();
        if (canProcess && be.burnTime <= 0) {
            ItemStack fuel = be.items.get(SLOT_FUEL);
            int value = fuelValue(fuel);
            if (value > 0) {
                be.burnTime = value;
                be.burnTotal = value;
                fuel.shrink(1);
                changed = true;
            }
        }

        if (canProcess && be.burnTime > 0) {
            ItemStack input = be.items.get(SLOT_INPUT);
            be.progressTotal = input.is(Items.WATER_BUCKET) ? BUCKET_TIME : BOTTLE_TIME;
            be.progress++;
            changed = true;
            if (be.progress >= be.progressTotal) {
                be.progress = 0;
                int gain = bottlesFor(input);
                if (input.is(Items.WATER_BUCKET)) {
                    be.items.set(SLOT_INPUT, new ItemStack(Items.BUCKET));
                } else {
                    input.shrink(1);
                }
                ItemStack out = be.items.get(SLOT_OUTPUT);
                if (out.isEmpty()) {
                    be.items.set(SLOT_OUTPUT, new ItemStack(ModItems.FRESH_WATER, gain));
                } else {
                    out.grow(gain);
                }
            }
        } else if (be.progress > 0) {
            be.progress = Math.max(0, be.progress - 2);
            changed = true;
        }

        boolean lit = be.burnTime > 0;
        if (wasLit != lit) {
            state = state.setValue(WaterPurifierBlock.LIT, lit);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    // ---- container plumbing ----

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.raft.water_purifier");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WaterPurifierMenu(containerId, inventory, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> isWaterInput(stack);
            case SLOT_FUEL -> fuelValue(stack) > 0;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : side == Direction.UP ? SLOTS_UP : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT || (slot == SLOT_INPUT && stack.is(Items.BUCKET));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("BurnTime", 0);
        burnTotal = input.getIntOr("BurnTotal", 0);
        progress = input.getIntOr("Progress", 0);
        progressTotal = input.getIntOr("ProgressTotal", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnTotal", burnTotal);
        output.putInt("Progress", progress);
        output.putInt("ProgressTotal", progressTotal);
    }
}
