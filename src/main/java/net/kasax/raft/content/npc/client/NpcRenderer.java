package net.kasax.raft.content.npc.client;

import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.NpcCharacter;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Renders a person of the sea with their own skin ({@code textures/entity/npc/<id>.png}, player skin layout). */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, NpcRenderState, NpcModel> {
    private final Identifier texture;

    public NpcRenderer(EntityRendererProvider.Context context, NpcCharacter character) {
        super(context, new NpcModel(context.bakeLayer(character.slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), character.slim), 0.5F);
        this.texture = Raft.id("textures/entity/npc/" + character.id + ".png");
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(NpcEntity entity, NpcRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = this.texture;
        state.sitting = entity.isSitting();
        if (state.sitting) {
            state.isPassenger = true;
        }
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState state) {
        return state.texture != null ? state.texture : this.texture;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(NpcEntity mob, HumanoidArm arm) {
        ItemStack held = mob.getItemHeldByArm(arm);
        if (held.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (mob.isUsingItem() && mob.getUsedItemHand().asArm(mob.getMainArm()) == arm && held.is(Items.SPYGLASS)) {
            return HumanoidModel.ArmPose.SPYGLASS;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public Vec3 getRenderOffset(NpcRenderState state) {
        // sitting on the ground: the legs stick out forward, so the body sits lower
        return state.sitting ? new Vec3(0.0, -0.62, 0.0) : super.getRenderOffset(state);
    }

    @Override
    protected boolean shouldShowName(NpcEntity entity, double distanceToCameraSq) {
        if (entity.hasCustomName() && entity.isCustomNameVisible()) {
            return super.shouldShowName(entity, distanceToCameraSq);
        }
        // the name shows when the player looks at the NPC from up close
        return distanceToCameraSq < 100.0 && entity == this.entityRenderDispatcher.crosshairPickEntity
                && super.shouldShowName(entity, distanceToCameraSq);
    }
}
