package net.kasax.raft.block.entity;

import net.kasax.raft.block.custom.SimpleGrillBlock;
import net.kasax.raft.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * The simple grill cooks raw food like a campfire, but in two thirds of the time (50 % faster).
 * It burns planks, logs and coal.
 */
public class SimpleGrillBlockEntity extends BlockEntity implements Clearable {
    public static final int SLOTS = 2;
    /** Cooking takes this share of the campfire time: 2/3 equals a 50 % higher speed. */
    private static final float TIME_FACTOR = 2.0f / 3.0f;
    private static final int MAX_BURN_TIME = 12000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[SLOTS];
    private final int[] cookingTime = new int[SLOTS];
    private int burnTime = 0;

    public SimpleGrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_GRILL_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public int getBurnTime() {
        return burnTime;
    }

    /** Burn time of a fuel item in ticks, 0 if the grill does not accept it. */
    public static int fuelValue(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModTags.Items.GRILL_FUEL)) {
            return 0;
        }
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 1600 : 300;
    }

    public boolean addFuel(ItemStack stack, boolean consume) {
        int value = fuelValue(stack);
        if (value <= 0 || burnTime + value > MAX_BURN_TIME) {
            return false;
        }
        burnTime += value;
        if (consume) {
            stack.shrink(1);
        }
        updateLit();
        setChanged();
        return true;
    }

    private void updateLit() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        boolean lit = burnTime > 0;
        if (state.hasProperty(SimpleGrillBlock.LIT) && state.getValue(SimpleGrillBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(SimpleGrillBlock.LIT, lit), 3);
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SimpleGrillBlockEntity grill,
                                  RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes) {
        boolean changed = false;
        if (grill.burnTime > 0) {
            grill.burnTime--;
            changed = true;
            if (grill.burnTime == 0) {
                grill.updateLit();
            }
        }
        boolean lit = grill.burnTime > 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = grill.items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (lit) {
                grill.cookingProgress[slot]++;
                changed = true;
                if (grill.cookingProgress[slot] >= grill.cookingTime[slot]) {
                    SingleRecipeInput input = new SingleRecipeInput(stack);
                    ItemStack result = recipes.getRecipeFor(input, level).map(r -> r.value().assemble(input)).orElse(stack);
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, result);
                    grill.items.set(slot, ItemStack.EMPTY);
                    grill.cookingProgress[slot] = 0;
                    level.sendBlockUpdated(pos, state, state, 3);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
                }
            } else if (grill.cookingProgress[slot] > 0) {
                grill.cookingProgress[slot] = Mth.clamp(grill.cookingProgress[slot] - 2, 0, grill.cookingTime[slot]);
                changed = true;
            }
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /** Puts one raw food item on the grill. Returns false if the item is not cookable or the grill is full. */
    public boolean placeFood(ServerLevel level, @Nullable Player player, ItemStack stack, RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.get(slot).isEmpty()) {
                Optional<RecipeHolder<CampfireCookingRecipe>> recipe = recipes.getRecipeFor(new SingleRecipeInput(stack), level);
                if (recipe.isEmpty()) {
                    return false;
                }
                this.cookingTime[slot] = Math.max(1, Math.round(recipe.get().value().cookingTime() * TIME_FACTOR));
                this.cookingProgress[slot] = 0;
                this.items.set(slot, stack.consumeAndReturn(1, player));
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.gameEvent(GameEvent.BLOCK_CHANGE, worldPosition, GameEvent.Context.of(player, getBlockState()));
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Takes the last placed item back. */
    public ItemStack takeLast(Level level) {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
                cookingProgress[slot] = 0;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> createRecipeCheck() {
        return RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        input.getIntArray("CookingTimes").ifPresentOrElse(
                times -> System.arraycopy(times, 0, cookingProgress, 0, Math.min(cookingProgress.length, times.length)),
                () -> Arrays.fill(cookingProgress, 0));
        input.getIntArray("CookingTotalTimes").ifPresentOrElse(
                times -> System.arraycopy(times, 0, cookingTime, 0, Math.min(cookingTime.length, times.length)),
                () -> Arrays.fill(cookingTime, 0));
        burnTime = input.getIntOr("BurnTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        output.putIntArray("CookingTimes", cookingProgress);
        output.putIntArray("CookingTotalTimes", cookingTime);
        output.putInt("BurnTime", burnTime);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, items);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
