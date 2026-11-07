package net.kasax.raft.content.voyage;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The palm boats and driftwood rafts (1.21.1 port). 1.21.1 knows only its own boat woods, so each boat is a small
 * subclass that drops its own item; the rafts use the bamboo raft's shape and seat positions.
 */
public final class ModBoats {
    private ModBoats() {
    }

    public static class PalmBoat extends Boat {
        public PalmBoat(EntityType<? extends Boat> type, Level level) {
            super(type, level);
        }

        @Override
        public Item getDropItem() {
            return VoyageContent.PALM_BOAT_ITEM;
        }
    }

    public static class PalmChestBoat extends ChestBoat {
        public PalmChestBoat(EntityType<? extends Boat> type, Level level) {
            super(type, level);
        }

        @Override
        public Item getDropItem() {
            return VoyageContent.PALM_CHEST_BOAT_ITEM;
        }
    }

    public static class DriftwoodRaft extends Boat {
        public DriftwoodRaft(EntityType<? extends Boat> type, Level level) {
            super(type, level);
            this.setVariant(Boat.Type.BAMBOO);
        }

        @Override
        public Item getDropItem() {
            return VoyageContent.DRIFTWOOD_RAFT_ITEM;
        }
    }

    public static class DriftwoodChestRaft extends ChestBoat {
        public DriftwoodChestRaft(EntityType<? extends Boat> type, Level level) {
            super(type, level);
            this.setVariant(Boat.Type.BAMBOO);
        }

        @Override
        public Item getDropItem() {
            return VoyageContent.DRIFTWOOD_CHEST_RAFT_ITEM;
        }
    }
}
