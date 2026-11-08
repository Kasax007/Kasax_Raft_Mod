package net.kasax.raft.content.food.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contents of a cooking pot: up to four ingredients (one item each), cooking progress and the finished dish with its
 * servings. Cooking needs heat below the pot (block tag {@code raft:pot_heat_sources}; campfires and braziers only
 * while lit) and takes 10 seconds.
 *
 * <p>Forgiving by design: the pot only takes an ingredient that still fits a dish together with what is already in
 * it (see {@link PotRecipes#canAdd}); everything else is refused with a message on the action bar. After every
 * change the action bar says what the contents can become and what is missing. Sneak-use with an empty hand gives
 * all ingredients back (breaking the pot drops them too).
 *
 * <p>The contents are sent to the client (block entity update), which runs the same cooking timer for the in-world
 * view: ingredients bobbing in the liquid, a surface that turns from water to the dish's colour, bubbles and steam
 * (CookingPotRenderer, {@link #clientTick}).
 */
public class CookingPotBlockEntity extends BlockEntity implements net.kasax.raft.compat.RemovalSideEffects {
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
            if (pot.progress > 0) {
                pot.progress = Math.max(0, pot.progress - 2);
                pot.setChanged();
            }
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
            level.playSound(null, pos, FoodContent.COOKING_POT_DONE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8F, 0.9F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        8, 0.25, 0.15, 0.25, 0.0);
            }
            pot.update();
        }
        pot.setChanged();
    }

    /**
     * Client side: the same cooking timer as the server (for the colour of the surface) and the particles: bubbles
     * and steam while it cooks, gentle steam when the dish is ready, grey smoke over a mix that makes nothing.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity pot) {
        RandomSource random = level.getRandom();
        boolean heat = heated(level, pos);
        double x = pos.getX() + 0.5, y = pos.getY() + 7.2 / 16.0, z = pos.getZ() + 0.5;
        if (pot.servings > 0) {
            if (random.nextInt(heat ? 6 : 14) == 0) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.25,
                        z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
            return;
        }
        if (pot.count() == 0) return;
        PotRecipes.Recipe recipe = PotRecipes.match(pot.ingredients);
        if (recipe != null && heat) {
            if (pot.progress < COOK_TIME - 1) pot.progress++;
            float f = pot.progress / (float) COOK_TIME;
            // bubbles get busier as the dish gets done
            if (random.nextFloat() < 0.25F + 0.6F * f) {
                level.addParticle(ParticleTypes.BUBBLE_POP, x + (random.nextDouble() - 0.5) * 0.55, y + 0.02,
                        z + (random.nextDouble() - 0.5) * 0.55, 0, 0.03, 0);
            }
            if (random.nextFloat() < 0.08F + 0.25F * f) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.2,
                        z + (random.nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
            }
        } else {
            if (pot.progress > 0) pot.progress = Math.max(0, pot.progress - 2);
            if (heat && PotRecipes.candidates(pot.ingredients).isEmpty() && random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0.03, 0);
            }
        }
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
                player.displayClientMessage(Component.translatable("message.raft.pot.ready", dish.getHoverName(), servings), true);
                return true;
            }
            stack.consume(1, player);
            ItemStack serving = dish.copy();
            player.getInventory().placeItemBackInInventory(serving);
            level.playSound(null, getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 0.8F);
            if (--servings <= 0) {
                dish = ItemStack.EMPTY;
                servings = 0;
            }
            update();
            return true;
        }
        if (stack.is(Items.BOWL)) {
            status(player);
            return true;
        }
        if (!PotRecipes.isIngredient(stack)) {
            // not part of any dish: refuse it (and do not fall through to the empty-hand status message)
            player.displayClientMessage(Component.translatable("message.raft.pot.not_ingredient", stack.getHoverName()), true);
            return true;
        }
        if (count() >= 4) {
            player.displayClientMessage(Component.translatable("message.raft.pot.full"), true);
            return true;
        }
        if (!PotRecipes.canAdd(ingredients, stack)) {
            player.displayClientMessage(Component.translatable("message.raft.pot.no_fit", stack.getHoverName()), true);
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
            player.displayClientMessage(Component.translatable("message.raft.pot.ready", dish.getHoverName(), servings), true);
            return;
        }
        if (player.isShiftKeyDown() && count() > 0) {
            for (int i = 0; i < 4; i++) {
                if (!ingredients.get(i).isEmpty()) {
                    player.getInventory().placeItemBackInInventory(ingredients.get(i));
                    ingredients.set(i, ItemStack.EMPTY);
                }
            }
            progress = 0;
            player.displayClientMessage(Component.translatable("message.raft.pot.taken"), true);
            if (getLevel() != null) {
                getLevel().playSound(null, getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.0F);
            }
            update();
            return;
        }
        status(player);
    }

    /** What the pot holds and what it can become, on the action bar. */
    private void status(Player player) {
        player.displayClientMessage(status(), true);
    }

    public Component status() {
        if (servings > 0) return Component.translatable("message.raft.pot.ready", dish.getHoverName(), servings);
        if (count() == 0) return Component.translatable("message.raft.pot.empty");
        PotRecipes.Recipe recipe = PotRecipes.match(ingredients);
        Level level = getLevel();
        if (recipe != null) {
            Component name = new ItemStack(recipe.result().get()).getHoverName();
            if (level != null && !heated(level, getBlockPos())) return Component.translatable("message.raft.pot.no_heat", name);
            return Component.translatable("message.raft.pot.recipe", name);
        }
        List<PotRecipes.Recipe> candidates = PotRecipes.candidates(ingredients);
        if (candidates.isEmpty()) {
            List<Component> names = new ArrayList<>();
            for (ItemStack s : ingredients) if (!s.isEmpty()) names.add(s.getHoverName());
            return Component.translatable("message.raft.pot.unknown", PotRecipes.list(names));
        }
        if (candidates.size() == 1) {
            List<Component> missing = new ArrayList<>();
            for (PotRecipes.Need n : PotRecipes.missing(candidates.getFirst(), ingredients)) missing.add(n.label().get());
            return Component.translatable("message.raft.pot.needs",
                    new ItemStack(candidates.getFirst().result().get()).getHoverName(), PotRecipes.list(missing));
        }
        List<Component> dishes = new ArrayList<>();
        for (PotRecipes.Recipe r : candidates) dishes.add(new ItemStack(r.result().get()).getHoverName());
        return Component.translatable("message.raft.pot.could", PotRecipes.list(dishes));
    }

    private void update() {
        setChanged();
        Level level = getLevel();
        if (level == null) return;
        BlockState state = getBlockState();
        CookingPotBlock.Contents c = servings > 0 ? CookingPotBlock.Contents.READY
                : count() > 0 ? CookingPotBlock.Contents.COOKING : CookingPotBlock.Contents.EMPTY;
        if (state.getValue(CookingPotBlock.CONTENTS) != c) {
            state = state.setValue(CookingPotBlock.CONTENTS, c);
            level.setBlock(getBlockPos(), state, Block.UPDATE_ALL);
        }
        level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    public List<ItemStack> ingredients() {
        return ingredients;
    }

    public ItemStack dish() {
        return dish;
    }

    public int servings() {
        return servings;
    }

    public int progress() {
        return progress;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(tag121, reg121);
        ContainerHelper.saveAllItems(tag121, ingredients, false, reg121);
        if (!dish.isEmpty()) output.store("dish", ItemStack.CODEC, dish);
        output.putInt("servings", servings);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(tag121, reg121);
        ingredients.replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag121, ingredients, reg121);
        dish = input.read("dish", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        servings = input.getIntOr("servings", 0);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
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
}
