package net.kasax.raft.content.voyage.client;

import net.kasax.raft.content.voyage.RaftUpgrade;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Model of the Expedition Raft (geometry in {@link ExpeditionRaftGeometry}). Upgrades switch parts on; the raft rocks
 * on the waves, the sail fills with speed and is furled at anchor, the propeller spins, the rudder follows the helm,
 * the anchor drops on its rope, lanterns swing and the oars row when the raft is paddled.
 */
public class ExpeditionRaftModel extends EntityModel<ExpeditionRaftRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(net.kasax.raft.Raft.id("expedition_raft"), "main");
    public static final ModelLayerLocation WATER_MASK = new ModelLayerLocation(net.kasax.raft.Raft.id("expedition_raft"), "water_mask");

    /** Rope length (model px) below the davit when the anchor is raised / dropped. */
    private static final float ROPE_UP = 2.0F;
    private static final float ROPE_DOWN = 44.0F;

    private final ModelPart rig;
    private final ModelPart sail;
    private final ModelPart flag;
    private final ModelPart rigging;
    private final ModelPart engine;
    private final ModelPart propeller;
    private final ModelPart anchorGear;
    private final ModelPart anchorRope;
    private final ModelPart anchor;
    private final ModelPart crates;
    private final ModelPart lanterns;
    private final ModelPart[] lamps = new ModelPart[4];
    private final ModelPart rudder;
    private final ModelPart leftOar;
    private final ModelPart rightOar;

    public ExpeditionRaftModel(ModelPart root) {
        super(root);
        this.rig = root.getChild("rig");
        this.sail = this.rig.getChild("sail");
        this.flag = this.rig.getChild("flag");
        this.rigging = root.getChild("rigging");
        this.engine = root.getChild("engine");
        this.propeller = this.engine.getChild("propeller");
        this.anchorGear = root.getChild("anchor_gear");
        this.anchorRope = root.getChild("anchor_rope");
        this.anchor = root.getChild("anchor");
        this.crates = root.getChild("crates");
        this.lanterns = root.getChild("lanterns");
        for (int i = 0; i < this.lamps.length; i++) {
            this.lamps[i] = this.lanterns.getChild("lantern_" + i);
        }
        this.rudder = root.getChild("rudder");
        this.leftOar = root.getChild("left_oar");
        this.rightOar = root.getChild("right_oar");
    }

    @Override
    public void setupAnim(ExpeditionRaftRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        int up = state.upgrades;
        boolean hasSail = RaftUpgrade.SAIL.in(up);
        boolean hasAnchor = RaftUpgrade.ANCHOR.in(up);
        this.rig.visible = hasSail;
        this.rigging.visible = hasSail;
        this.engine.visible = RaftUpgrade.ENGINE.in(up);
        this.anchorGear.visible = hasAnchor;
        this.anchorRope.visible = hasAnchor;
        this.anchor.visible = hasAnchor;
        this.crates.visible = RaftUpgrade.CRATE.in(up);
        this.lanterns.visible = RaftUpgrade.LANTERN.in(up);

        // gentle rocking on the swell
        if (state.inWater && !state.isUnderWater) {
            this.root().xRot = Mth.sin(t * 0.055F) * 0.014F;
            this.root().zRot = Mth.sin(t * 0.042F + 1.3F) * 0.02F;
        }

        // sail: billows towards the bow with speed, flutters, is furled up to the yard at anchor
        float fill = state.sailFill;
        this.sail.xRot = -(0.03F + 0.24F * fill) + Mth.sin(t * 0.45F) * 0.012F * (0.4F + fill);
        this.sail.yScale = 1.0F - 0.7F * state.anchorDrop;
        this.flag.yRot = -1.05F - 0.5F * fill + Mth.sin(t * (0.25F + 0.3F * fill)) * (0.18F + 0.12F * fill);
        this.flag.zRot = 0.55F * (1.0F - fill);

        // helm
        this.rudder.yRot = Mth.clamp(-state.turn * 0.09F, -0.6F, 0.6F);

        // engine
        this.propeller.zRot = state.propeller;
        if (state.engineRunning) {
            this.engine.y += Mth.sin(t * 2.9F) * 0.1F;
            this.engine.zRot = Mth.sin(t * 3.7F) * 0.006F;
        }

        // anchor
        float length = Mth.lerp(state.anchorDrop, ROPE_UP, ROPE_DOWN);
        this.anchorRope.yScale = length;
        this.anchor.y += length;
        this.anchor.zRot = Mth.sin(t * 0.07F) * 0.05F * (1.0F - state.anchorDrop);
        this.anchor.xRot = Mth.sin(t * 0.05F + 0.7F) * 0.04F;

        // lanterns swing on their arms
        for (int i = 0; i < this.lamps.length; i++) {
            this.lamps[i].zRot = Mth.sin(t * 0.08F + i * 1.7F) * 0.07F;
            this.lamps[i].xRot = Mth.sin(t * 0.06F + i * 0.9F) * 0.05F;
        }

        // oars: row like the vanilla boat paddles, otherwise stowed along the sides
        if (state.rowing) {
            this.animateOar(this.leftOar, state.rowingTimeLeft, false);
            this.animateOar(this.rightOar, state.rowingTimeRight, true);
        } else {
            this.leftOar.yRot = -1.32F;
            this.leftOar.zRot = 0.1F;
            this.rightOar.yRot = Mth.PI + 1.32F;
            this.rightOar.zRot = -0.1F;
        }
    }

    private void animateOar(ModelPart oar, float time, boolean right) {
        float dip = Mth.clampedLerp((Mth.sin(-time) + 1.0F) / 2.0F, 0.55F, 0.08F);
        float sweep = Mth.clampedLerp((Mth.sin(-time + 1.0F) + 1.0F) / 2.0F, -0.65F, 0.65F);
        if (right) {
            oar.yRot = Mth.PI - sweep;
            oar.zRot = -dip;
        } else {
            oar.yRot = sweep;
            oar.zRot = dip;
        }
    }
}
