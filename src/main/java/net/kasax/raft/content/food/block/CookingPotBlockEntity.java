package net.kasax.raft.content.food.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contents of a cooking pot: up to four ingredients (one item each), cooking progress and the finished dish with its
 * servings. Cooking needs heat below the pot (block tag {@code raft:pot_heat_sources}; campfires and braziers only
 * while lit) and takes 10 seconds.
 */
public class CookingPotBlockEntity extends BlockEntity {
    public static final int COOK_TIME = 200;
    public static final TagKey<net.minecraft.world.level.block.Block> HEAT =
            TagKey.create(Registries.BLOCK, Raft.id("pot_heat_sources"));

    private final NonNullList<ItemStack> ingredients = NonNullList.withSize(4, ItemStack.EMPTY);
    private ItemStack dish = ItemStack.EMPTY;
    private int servings;
    private int progress;

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(FoodContent.COOKING_POT_ENTITY, pos, state);
    }

    public static boolean heated(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(HEAT)) return false;
        return !below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity pot) {
        if (pot.servings > 0) return;
        PotRecipes.Recipe recipe = PotRecipes.match(pot.ingredients);
        if (recipe == null || !heated(level, pos)) {
            if (pot.progress > 0) pot.progress = Math.max(0, pot.progress - 2);
            return;
        }
        if (++pot.progress % 40 == 0) {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.2F);
        }
        if (pot.progress >= COOK_TIME) {
            pot.progress = 0;
            pot.dish = new ItemStack(recipe.result().get());
            pot.servings = recipe.servings();
            pot.ingredients.replaceAll(s -> ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.3F);
            pot.update();
        }
        pot.setChanged();
    }

    private int count() {
        int n = 0;
        for (ItemStack s : ingredients) if (!s.isEmpty()) n++;
        return n;
    }

    /** Right-click with an item: a bowl serves the dish, an ingredient goes in. Returns true if handled. */
    public boolean useItem(ItemStack stack, Player player) {
        Level level = getLevel();
        if (level == null) return false;
        if (servings > 0) {
            if (!stack.is(Items.BOWL)) {
                player.sendOverlayMessage(Component.translatable("message.raft.pot.ready", dish.getHoverName(), servings));
                return true;
            }
            stack.consume(1, player);
            ItemStack serving = dish.copy();
            player.getInventory().placeItemBackInInventory(serving, net.minecraft.util.Prediction.SERVER_ONLY);
            level.playSound(null, getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 0.8F);
            if (--servings <= 0) {
                dish = ItemStack.EMPTY;
                servings = 0;
            }
            update();
            return true;
        }
        if (!PotRecipes.isIngredient(stack)) {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.not_ingredient"));
            return false;
        }
        if (count() >= 4) {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.full"));
            return true;
        }
        for (int i = 0; i < 4; i++) {
            if (ingredients.get(i).isEmpty()) {
                ingredients.set(i, stack.copyWithCount(1));
                break;
            }
        }
        stack.consume(1, player);
        progress = 0;
        level.playSound(null, getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.4F);
        status(player);
        update();
        return true;
    }

    public void useEmptyHand(Player player) {
        if (servings > 0) {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.ready", dish.getHoverName(), servings));
            return;
        }
        if (player.isShiftKeyDown()) {
            for (int i = 0; i < 4; i++) {
                if (!ingredients.get(i).isEmpty()) {
                    player.getInventory().placeItemBackInInventory(ingredients.get(i), net.minecraft.util.Prediction.SERVER_ONLY);
                    ingredients.set(i, ItemStack.EMPTY);
                }
            }
            progress = 0;
            update();
            return;
        }
        status(player);
    }

    private void status(Player player) {
        if (count() == 0) {
            player.sendOverlayMessage(Component.translatable("block.raft.cooking_pot"));
            return;
        }
        MutableComponent list = Component.empty();
        boolean first = true;
        for (ItemStack s : ingredients) {
            if (s.isEmpty()) continue;
            if (!first) list.append(", ");
            list.append(s.getHoverName());
            first = false;
        }
        PotRecipes.Recipe recipe = PotRecipes.match(ingredients);
        if (recipe != null && getLevel() != null && !heated(getLevel(), getBlockPos())) {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.no_heat"));
        } else if (recipe != null) {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.recipe",
                    new ItemStack(recipe.result().get()).getHoverName()));
        } else {
            player.sendOverlayMessage(Component.translatable("message.raft.pot.added", list));
        }
    }

    private void update() {
        setChanged();
        Level level = getLevel();
        if (level == null) return;
        BlockState state = getBlockState();
        CookingPotBlock.Contents c = servings > 0 ? CookingPotBlock.Contents.READY
                : count() > 0 ? CookingPotBlock.Contents.COOKING : CookingPotBlock.Contents.EMPTY;
        if (state.getValue(CookingPotBlock.CONTENTS) != c) {
            level.setBlock(getBlockPos(), state.setValue(CookingPotBlock.CONTENTS, c), Block.UPDATE_ALL);
        }
    }

    public ItemStack dish() {
        return dish;
    }

    public int servings() {
        return servings;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, ingredients, false);
        if (!dish.isEmpty()) output.store("dish", ItemStack.CODEC, dish);
        output.putInt("servings", servings);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ingredients.replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, ingredients);
        dish = input.read("dish", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        servings = input.getIntOr("servings", 0);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        Level level = getLevel();
        if (level instanceof ServerLevel) {
            for (ItemStack s : ingredients) {
                if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
        }
    }

    static Item unused() {
        return Items.AIR;
    }
}
