package net.kasax.raft.content.diving.block;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.content.diving.DivingSounds;
import net.kasax.raft.content.diving.item.OxygenTankItem;
import net.kasax.raft.content.diving.item.SeaScooterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Fills the oxygen tank (or charges the sea scooter) in its single slot. Runs on FE from any adjacent energy source
 * (it accepts pushed energy and pulls from neighbours that give it) or by hand: sneak-use with an empty hand or the
 * crank in the GUI adds {@value #CRANK_AIR} air.
 */
public class AirCompressorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT = 0;
    private static final int[] SLOTS = {SLOT};

    public static final long ENERGY_CAPACITY = 20_000;
    public static final long ENERGY_MAX_INPUT = 250;
    /** Air ticks pumped per game tick with power (a 2 min tank fills in 6 s). */
    public static final int AIR_PER_TICK = 20;
    /** FE per pumped air tick (two air ticks per FE). */
    public static final double FE_PER_AIR = 0.5;
    /** FE per tick moved into a sea scooter. */
    public static final long SCOOTER_FE_PER_TICK = 25;
    public static final int CRANK_AIR = 100;
    public static final long CRANK_FE = 40;
    public static final int CRANK_COOLDOWN = 8;

    public static final int KIND_NONE = 0;
    public static final int KIND_TANK = 1;
    public static final int KIND_SCOOTER = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int crankCooldown;
    /** Ticks left of the crank animation in the GUI. */
    private int crankAnim;
    private int working;
    private int hissTimer;
    private double airRemainder;

    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_INPUT, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            ItemStack stack = items.get(SLOT);
            return switch (index) {
                case 0 -> (int) energy.amount;
                case 1 -> (int) ENERGY_CAPACITY;
                case 2 -> stack.getItem() instanceof OxygenTankItem ? OxygenTankItem.getAir(stack)
                        : stack.getItem() instanceof SeaScooterItem ? (int) SimpleEnergyItem.getStoredEnergyUnchecked(stack) : 0;
                case 3 -> stack.getItem() instanceof OxygenTankItem ? OxygenTankItem.capacity(stack)
                        : stack.getItem() instanceof SeaScooterItem ? (int) SeaScooterItem.CAPACITY : 0;
                case 4 -> kind(stack);
                case 5 -> working;
                case 6 -> crankAnim;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy.amount = value;
                case 5 -> working = value;
                case 6 -> crankAnim = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(DivingContent.AIR_COMPRESSOR_BE, pos, state);
    }

    public static int kind(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem) {
            return KIND_TANK;
        }
        return stack.getItem() instanceof SeaScooterItem ? KIND_SCOOTER : KIND_NONE;
    }

    public static boolean accepts(ItemStack stack) {
        return kind(stack) != KIND_NONE;
    }

    public SimpleEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    // ------------------------------------------------------------------------------------------- ticking
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AirCompressorBlockEntity be) {
        if (be.crankCooldown > 0) {
            be.crankCooldown--;
        }
        if (be.crankAnim > 0) {
            be.crankAnim--;
        }
        be.pullEnergy(level, pos);

        boolean working = false;
        ItemStack stack = be.items.get(SLOT);
        if (stack.getItem() instanceof OxygenTankItem && !OxygenTankItem.isFull(stack) && be.energy.amount > 0) {
            int room = OxygenTankItem.capacity(stack) - OxygenTankItem.getAir(stack);
            int affordable = (int) Math.floor(be.energy.amount / FE_PER_AIR);
            int air = Math.min(AIR_PER_TICK, Math.min(room, affordable));
            if (air > 0) {
                OxygenTankItem.fill(stack, air);
                be.airRemainder += air * FE_PER_AIR;
                long cost = (long) Math.floor(be.airRemainder);
                be.airRemainder -= cost;
                be.energy.amount = Math.max(0, be.energy.amount - cost);
                working = true;
            }
        } else if (stack.getItem() instanceof SeaScooterItem scooter && be.energy.amount > 0) {
            long stored = scooter.getStoredEnergy(stack);
            long move = Math.min(SCOOTER_FE_PER_TICK, Math.min(be.energy.amount, SeaScooterItem.CAPACITY - stored));
            if (move > 0) {
                scooter.setStoredEnergy(stack, stored + move);
                be.energy.amount -= move;
                working = true;
            }
        }

        if (working) {
            if (be.hissTimer-- <= 0) {
                be.hissTimer = 40;
                level.playSound(null, pos, DivingSounds.COMPRESSOR_FILL, SoundSource.BLOCKS, 0.35F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }
        } else {
            be.hissTimer = 0;
        }
        be.working = working ? 1 : 0;
        if (state.getValue(AirCompressorBlock.RUNNING) != working) {
            level.setBlock(pos, state.setValue(AirCompressorBlock.RUNNING, working), 3);
        }
        if (working) {
            be.setChanged();
        }
    }

    /** Pulls FE from neighbours that give energy (generators and batteries push on their own anyway). */
    private void pullEnergy(ServerLevel level, BlockPos pos) {
        if (energy.amount >= ENERGY_CAPACITY) {
            return;
        }
        for (Direction direction : Direction.values()) {
            EnergyStorage source = EnergyStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (source == null || !source.supportsExtraction()) {
                continue;
            }
            long room = Math.min(ENERGY_MAX_INPUT, ENERGY_CAPACITY - energy.amount);
            if (room <= 0) {
                return;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long got = source.extract(room, transaction);
                if (got > 0) {
                    energy.amount += got;
                    transaction.commit();
                    setChanged();
                }
            }
        }
    }

    /** One turn of the hand crank. Returns false while the crank is still turning or nothing can be filled. */
    public boolean crank(ServerLevel level) {
        if (crankCooldown > 0) {
            return false;
        }
        ItemStack stack = items.get(SLOT);
        boolean done = false;
        if (stack.getItem() instanceof OxygenTankItem) {
            done = OxygenTankItem.fill(stack, CRANK_AIR) > 0;
        } else if (stack.getItem() instanceof SeaScooterItem scooter) {
            long stored = scooter.getStoredEnergy(stack);
            long add = Math.min(CRANK_FE, SeaScooterItem.CAPACITY - stored);
            if (add > 0) {
                scooter.setStoredEnergy(stack, stored + add);
                done = true;
            }
        }
        crankCooldown = CRANK_COOLDOWN;
        crankAnim = CRANK_COOLDOWN;
        BlockPos pos = getBlockPos();
        level.playSound(null, pos, DivingSounds.COMPRESSOR_CRANK, SoundSource.BLOCKS, 0.8F, done ? 1.0F : 0.8F);
        if (done) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.01);
            setChanged();
        }
        return done;
    }

    // ------------------------------------------------------------------------------------------- container
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.raft.air_compressor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AirCompressorMenu(containerId, inventory, this, data);
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
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return accepts(stack) && items.get(SLOT).isEmpty();
    }

    /** Hoppers take the item out only once it is full. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (stack.getItem() instanceof OxygenTankItem) {
            return OxygenTankItem.isFull(stack);
        }
        return !(stack.getItem() instanceof SeaScooterItem scooter) || scooter.getStoredEnergy(stack) >= SeaScooterItem.CAPACITY;
    }

    /** 0..15 by how full the item in the slot is. */
    public int comparatorSignal() {
        ItemStack stack = items.get(SLOT);
        int max = data.get(3);
        if (max <= 0) {
            return 0;
        }
        return Math.round(15.0F * data.get(2) / max);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.amount = Math.min(ENERGY_CAPACITY, input.getLongOr("Energy", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.amount);
    }
}
