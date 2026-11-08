package net.kasax.raft.content.decor.lootr;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kasax.raft.content.decor.DecorBlocks;
import net.kasax.raft.content.decor.block.DecorChestBlock;
import net.kasax.raft.content.decor.block.DecorChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import noobanidus.mods.lootr.common.api.BuiltInLootrTypes;
import noobanidus.mods.lootr.common.api.data.ILootrInfo;
import noobanidus.mods.lootr.common.api.data.LootrBlockType;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import noobanidus.mods.lootr.common.api.ILootrType;
import noobanidus.mods.lootr.common.api.ILootrBlockEntityConverter;
import org.jetbrains.annotations.Nullable;

/**
 * A Beach Craft crate or treasure chest seen as a Lootr container (only loaded with Lootr). The block entity keeps
 * its loot table and the container's Lootr id; Lootr keeps one inventory per player for that id. Lootr finds this
 * class through the converters below (META-INF/services), which it loads with the ServiceLoader.
 *
 * <p>1.21.1: Lootr 1.11 for Minecraft 1.21.1 has the same design under older names (info provider, converter, info
 * key instead of the newer Lootr's data provider, wrapper and data identifier).
 */
public final class LootrDecorChest implements ILootrBlockEntity {
    private final DecorChestBlockEntity chest;
    private final Set<UUID> clientOpeners = new HashSet<>();
    private boolean clientOpened;
    private String key;

    private LootrDecorChest(DecorChestBlockEntity chest) {
        this.chest = chest;
    }

    /** The one view of this block entity (kept on it, so client flags such as "opened" stay). */
    static LootrDecorChest of(DecorChestBlockEntity chest) {
        Object view = chest.lootrView();
        if (view instanceof LootrDecorChest d) {
            return d;
        }
        LootrDecorChest d = new LootrDecorChest(chest);
        chest.setLootrView(d);
        return d;
    }

    @Override
    public BlockEntity asBlockEntity() {
        return chest;
    }

    @Override
    public ILootrType getInfoNewType() {
        return crate() ? BuiltInLootrTypes.BARREL : BuiltInLootrTypes.CHEST;
    }

    @Override
    public LootrBlockType getInfoBlockType() {
        return crate() ? LootrBlockType.BARREL : LootrBlockType.CHEST;
    }

    private boolean crate() {
        return chest.getBlockState().is(DecorBlocks.WOODEN_CRATE);
    }

    @Override
    public UUID getInfoUUID() {
        return chest.lootrId();
    }

    @Override
    public String getInfoKey() {
        if (key == null) {
            key = ILootrInfo.generateInfoKey(getInfoUUID());
        }
        return key;
    }

    @Override
    public boolean hasBeenOpened() {
        return chest.lootrOpened();
    }

    @Override
    public void setHasBeenOpened(boolean value) {
        chest.setLootrOpened(value);
    }

    @Override
    public boolean isPhysicallyOpen() {
        return chest.getBlockState().hasProperty(DecorChestBlock.OPEN) && chest.getBlockState().getValue(DecorChestBlock.OPEN);
    }

    @Override
    public BlockPos getInfoPos() {
        return chest.getBlockPos();
    }

    @Override
    public ResourceKey<Level> getInfoDimension() {
        return chest.getLevel().dimension();
    }

    @Override
    public int getInfoContainerSize() {
        return chest.getContainerSize();
    }

    @Override
    public @Nullable NonNullList<ItemStack> getInfoReferenceInventory() {
        return null;
    }

    @Override
    public boolean isInfoReferenceInventory() {
        return false;
    }

    @Override
    public ResourceKey<LootTable> getInfoLootTable() {
        return chest.getLootTable();
    }

    @Override
    public long getInfoLootSeed() {
        return chest.getLootTableSeed();
    }

    @Override
    public @Nullable Component getInfoDisplayName() {
        return chest.getDisplayName();
    }

    @Override
    public Level getInfoLevel() {
        return chest.getLevel();
    }

    @Override
    public void markChanged() {
        chest.setChanged();
        markDataChanged();
    }

    @Override
    public int getRandomOffset() {
        return Math.floorMod(chest.getBlockPos().hashCode(), 20);
    }

    @Override
    public @Nullable Set<UUID> getClientOpeners() {
        return clientOpeners;
    }

    @Override
    public boolean isClientOpened() {
        return clientOpened;
    }

    @Override
    public void setClientOpened(boolean opened) {
        clientOpened = opened;
    }

    /** By id, so a wrapper loaded early (Lootr reads its services when it likes) does not start Beach Craft's
     * registration from Lootr's side. */
    private static BlockEntityType<?> type(String name) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("raft", name));
    }

    /** Lootr asks the converter of a block entity type for the Lootr view of its block entities. */
    public abstract static class Wrapper implements ILootrBlockEntityConverter<DecorChestBlockEntity> {
        @Override
        public ILootrBlockEntity apply(DecorChestBlockEntity blockEntity) {
            return blockEntity.getLootTable() != null || blockEntity.hasLootrId() ? of(blockEntity) : null;
        }
    }

    public static final class CrateWrapper extends Wrapper {
        @Override
        public BlockEntityType<?> getBlockEntityType() {
            return type("wooden_crate");
        }
    }

    public static final class TreasureChestWrapper extends Wrapper {
        @Override
        public BlockEntityType<?> getBlockEntityType() {
            return type("treasure_chest");
        }
    }
}
