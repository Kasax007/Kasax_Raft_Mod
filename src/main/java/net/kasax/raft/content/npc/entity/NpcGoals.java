package net.kasax.raft.content.npc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Small goals of the people of the sea. */
final class NpcGoals {
    private NpcGoals() {
    }

    /** While a conversation or trade is open: stand still and face the player (sitting NPCs stay seated). */
    static final class Talk extends Goal {
        private final NpcEntity npc;

        Talk(NpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.npc.isTalking();
        }

        @Override
        public void start() {
            this.npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player p = this.npc.talkPartner();
            if (p != null) {
                this.npc.getLookControl().setLookAt(p, 30.0F, 30.0F);
            }
        }
    }

    /** Walks back when the NPC strayed out of its home area (the far-away case teleports, see NpcEntity.aiStep). */
    static final class ReturnHome extends Goal {
        private final NpcEntity npc;
        private int recalc;

        ReturnHome(NpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.npc.hasHome() || this.npc.isTalking()) {
                return false;
            }
            // a little slack beyond the radius: the places of interest at the edge of home are fine
            double r = this.npc.getHomeRadius() + 2.0;
            return this.npc.getHomePosition().distToCenterSqr(this.npc.position()) > r * r;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.npc.isTalking() && this.npc.hasHome()
                    && this.npc.getHomePosition().distToCenterSqr(this.npc.position()) > 4.0;
        }

        @Override
        public void start() {
            this.npc.setSitting(false);
            this.recalc = 0;
        }

        @Override
        public void tick() {
            if (--this.recalc <= 0) {
                this.recalc = 40;
                BlockPos home = this.npc.getHomePosition();
                this.npc.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.6);
            }
        }

        @Override
        public void stop() {
            this.npc.getNavigation().stop();
        }
    }
}
