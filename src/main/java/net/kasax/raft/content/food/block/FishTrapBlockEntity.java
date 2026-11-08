package net.kasax.raft.content.food.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Catch of a fish trap (9 slots). While the trap is under water with at least 12 water blocks around it, it rolls
 * the loot table {@code raft:gameplay/fish_trap} every 60-120 seconds, twice as often while it holds bait
 * (item tag {@code raft:bait}; one bait is used per catch). It stops when full; hoppers can empty it.
 */
public class FishTrapBlockEntity extends RandomizableContainerBlockEntity {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Raft.id("gameplay/fish_trap"));
    public static final TagKey<Item> BAIT = TagKey.create(Registries.ITEM, Raft.id("bait"));
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int timer = -1;

    public FishTrapBlockEntity(BlockPos pos, BlockState state) {
        super(FoodContent.FISH_TRAP_ENTITY, pos, state);
    }

    public static boolean openWater(Level level, BlockPos pos) {
        if (!level.getFluidState(pos).is(Fluids.WATER)) return false;
        int water = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getFluidState(p).is(Fluids.WATER)) water++;
        }
        return water >= 12;
    }

    private int baitSlot() {
        for (int i = 0; i < items.size(); i++) if (items.get(i).is(BAIT)) return i;
        return -1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FishTrapBlockEntity trap) {
        if (!(level instanceof ServerLevel server)) return;
        if (trap.timer < 0) trap.timer = trap.nextDelay(server);
        if (trap.timer-- > 0) return;
        trap.timer = trap.nextDelay(server);
        if (!openWater(level, pos)) return;
        int bait = trap.baitSlot();
        LootTable table = server.getServer().reloadableRegistries().getLootTable(LOOT);
        List<ItemStack> catchList = table.getRandomItems(new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.CHEST));
        boolean caught = false;
        for (ItemStack s : catchList) caught |= trap.insert(s);
        if (caught && bait >= 0) trap.items.get(bait).shrink(1);
        if (caught) trap.setChanged();
    }

    private int nextDelay(ServerLevel level) {
        int base = 1200 + level.getRandom().nextInt(1200);
        return baitSlot() >= 0 ? base / 2 : base;
    }

    /** Debug/test hook: catch right away on the next tick. */
    public void hurry() {
        timer = 0;
    }

    private boolean insert(ItemStack stack) {
        boolean any = false;
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
                any = true;
            } else if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int n = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(n);
                stack.shrink(n);
                any = true;
            }
        }
        return any;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) ContainerHelper.saveAllItems(output, items);
        output.putInt("timer", timer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, items);
        timer = input.getIntOr("timer", -1);
    }

    @Override
    public int getContainerSize() {
        return 9;
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
    protected Component getDefaultName() {
        return Component.translatable("container.raft.fish_trap");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, this);
    }
}
