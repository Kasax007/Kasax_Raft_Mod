package net.kasax.raft.mixin;
//Inspired by Fabric Furnaces Mod by Draylar
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import net.kasax.raft.block.entity.FurnaceEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceEntityMixin extends BlockEntity {

    @Unique private static final Random ff_random = new Random();
    @Unique @Nullable private static AbstractFurnaceBlockEntity ff_entityContext = null;

    private AbstractFurnaceEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "tick",
            at = @At("HEAD"))
    private static void resetCraftingContext(World world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ff_entityContext = blockEntity;
    }

    @Inject(
            method = "craftRecipe",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V", shift = At.Shift.AFTER))
    private static void doubleIncrementOutput(DynamicRegistryManager registryManager, RecipeEntry<?> recipe, DefaultedList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if(ff_entityContext instanceof FurnaceEntity) {
            boolean doubled = ff_random.nextInt(100) < ((FurnaceEntity) ff_entityContext).getDuplicationChance();
            if(doubled) {
                slots.get(2).increment(1);
            }
        }
    }

    @Inject(
            method = "craftRecipe",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;", ordinal = 0, shift = At.Shift.AFTER))
    private static void doubleSetOutput(DynamicRegistryManager registryManager, RecipeEntry<?> recipe, DefaultedList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if(ff_entityContext instanceof FurnaceEntity) {
            boolean doubled = ff_random.nextInt(100) < ((FurnaceEntity) ff_entityContext).getDuplicationChance();
            if(doubled) {
                slots.get(2).increment(1);
            }
        }
    }

    @Inject(
            method = "getCookTime",
            at = @At("RETURN"), cancellable = true)
    private static void modifyCookTime(World world, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Integer> cir) {
        Integer original = cir.getReturnValue();

        if(ff_entityContext instanceof FurnaceEntity) {
            cir.setReturnValue((int) (original / ((FurnaceEntity) ff_entityContext).getSpeedModifier()));
        }
    }
}