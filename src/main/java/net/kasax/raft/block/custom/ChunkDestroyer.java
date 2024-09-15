package net.kasax.raft.block.custom;

import net.kasax.raft.block.entity.ChunkDestroyerBlockEntity;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.kasax.raft.block.entity.QuarryBlockEntity;
import net.kasax.raft.util.TickableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkDestroyer extends Block implements BlockEntityProvider {
    public ChunkDestroyer(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH)); // Default to NORTH
    }
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.raft.chunk_destroyer.tooltip1").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.raft.chunk_destroyer.tooltip2").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHUNK_DESTROYER_BLOCK_ENTITY.instantiate(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(!world.isClient) {
            if (world.getBlockEntity(pos) instanceof ChunkDestroyerBlockEntity blockEntity) {
                player.sendMessage(Text.of("Mining at: " + blockEntity.getMiningPos().toString()), true);
            }
        }

        return ActionResult.success(world.isClient);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return TickableBlockEntity.getTicker(world);
    }
}
