package net.kasax.raft.content.decor.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** The item of a buoy: used on the water surface it puts the buoy into the top water block, floating. */
public class BuoyItem extends BlockItem {
    public BuoyItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        BlockHitResult blockHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (fluidHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = fluidHit.getBlockPos();
            boolean water = level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource();
            boolean closerThanBlock = blockHit.getType() != HitResult.Type.BLOCK
                    || blockHit.getLocation().distanceToSqr(player.getEyePosition()) >= fluidHit.getLocation().distanceToSqr(player.getEyePosition()) - 0.01;
            if (water && closerThanBlock) {
                // place into the water block itself (it is replaceable), so the buoy floats waterlogged
                return super.useOn(new UseOnContext(player, hand, fluidHit));
            }
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return super.useOn(new UseOnContext(player, hand, blockHit));
        }
        return InteractionResult.PASS;
    }
}
