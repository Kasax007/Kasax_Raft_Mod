package net.kasax.raft.content.story.item;

import net.kasax.raft.Raft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** A floating supply barrel from the debris. Use: break it open; the loot ({@code raft:gameplay/supply_barrel}) pops out. */
public class SupplyBarrelItem extends Item {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Raft.id("gameplay/supply_barrel"));

    public SupplyBarrelItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 look = player.getLookAngle();
            Vec3 at = player.getEyePosition().add(look.x * 0.8, -0.4, look.z * 0.8);
            LootTable table = server.getServer().reloadableRegistries().getLootTable(LOOT);
            LootParams params = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
            for (ItemStack loot : table.getRandomItems(params)) {
                ItemEntity drop = new ItemEntity(server, at.x, at.y, at.z, loot,
                        (server.getRandom().nextDouble() - 0.5) * 0.15, 0.2, (server.getRandom().nextDouble() - 0.5) * 0.15);
                drop.setPickUpDelay(8);
                server.addFreshEntity(drop);
            }
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BARREL.defaultBlockState()),
                    at.x, at.y, at.z, 24, 0.25, 0.25, 0.25, 0.1);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 0.5F, 1.4F);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.supply_barrel.0").withStyle(ChatFormatting.GRAY));
    }
}
