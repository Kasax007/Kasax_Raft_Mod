package net.kasax.raft.content.boss.client.model;

import net.kasax.raft.Raft;
import net.kasax.raft.content.boss.entity.LeviathanBody;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Leviathan: a horned sea-serpent head with a hinged jaw full of teeth, swept-back frills and long barbels; six body
 * segments (three heavy ones with side fins, three slimmer ones) and a tail with a tall fan fluke. The first segment
 * still wears the broken golden shackle of the Tidewardens. Every root part is placed in world space by the renderer
 * (the body follows the head's path), so the parts are posed from the render state instead of a fixed skeleton.
 * Texture 256x256 (+ an emissive layer for eyes, spots and runes).
 */
public class LeviathanModel extends EntityModel<LeviathanRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Raft.id("leviathan"), "main");

    private final ModelPart[] parts = new ModelPart[LeviathanRenderState.PARTS];
    private final ModelPart jaw;
    private final ModelPart frillLeft;
    private final ModelPart frillRight;
    private final ModelPart barbelLeft;
    private final ModelPart barbelRight;
    private final ModelPart[] fins = new ModelPart[LeviathanBody.SEGMENTS];
    private final ModelPart[] sideFinsLeft = new ModelPart[3];
    private final ModelPart[] sideFinsRight = new ModelPart[3];
    private final ModelPart chain;
    private final ModelPart fluke;
    private final ModelPart flukeLeft;
    private final ModelPart flukeRight;
    private final ModelPart tailFin;

    public LeviathanModel(ModelPart root) {
        super(root);
        this.parts[0] = root.getChild("head");
        for (int i = 0; i < LeviathanBody.SEGMENTS; i++) {
            this.parts[i + 1] = root.getChild("seg" + i);
            this.fins[i] = this.parts[i + 1].getChild("fin");
        }
        this.parts[7] = root.getChild("tail");
        ModelPart head = this.parts[0];
        this.jaw = head.getChild("jaw");
        this.frillLeft = head.getChild("frill_left");
        this.frillRight = head.getChild("frill_right");
        this.barbelLeft = head.getChild("barbel_left");
        this.barbelRight = head.getChild("barbel_right");
        for (int i = 0; i < 3; i++) {
            this.sideFinsLeft[i] = this.parts[i + 1].getChild("left_fin");
            this.sideFinsRight[i] = this.parts[i + 1].getChild("right_fin");
        }
        this.chain = this.parts[1].getChild("collar").getChild("chain");
        ModelPart tail = this.parts[7];
        this.fluke = tail.getChild("fluke");
        this.flukeLeft = tail.getChild("fluke_left");
        this.flukeRight = tail.getChild("fluke_right");
        this.tailFin = tail.getChild("fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ------------------------------------------------------------------ head (pivot = centre of the hitbox)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 54).addBox(-15.0F, -11.0F, -14.0F, 30.0F, 18.0F, 28.0F)
                .texOffs(162, 142).addBox(-11.0F, -8.0F, -30.0F, 22.0F, 12.0F, 16.0F)
                .texOffs(122, 100).addBox(-1.0F, -20.0F, -10.0F, 2.0F, 9.0F, 26.0F)
                .texOffs(100, 175).addBox(8.0F, -14.0F, -14.0F, 7.0F, 4.0F, 16.0F)
                .texOffs(100, 175).mirror().addBox(-15.0F, -14.0F, -14.0F, 7.0F, 4.0F, 16.0F).mirror(false)
                .texOffs(124, 201).addBox(15.0F, -7.0F, -11.0F, 1.0F, 4.0F, 6.0F)
                .texOffs(124, 201).mirror().addBox(-16.0F, -7.0F, -11.0F, 1.0F, 4.0F, 6.0F).mirror(false)
                .texOffs(178, 175).addBox(-9.0F, 4.0F, -29.0F, 18.0F, 2.0F, 14.0F)
                .texOffs(226, 201).addBox(6.0F, 4.0F, -29.5F, 2.0F, 5.0F, 2.0F)
                .texOffs(226, 201).mirror().addBox(-8.0F, 4.0F, -29.5F, 2.0F, 5.0F, 2.0F).mirror(false),
                PartPose.ZERO);
        // swept-back horns in two pieces, curling upwards at the tips
        PartDefinition hornLeft = head.addOrReplaceChild("horn_left", CubeListBuilder.create()
                .texOffs(56, 175).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 10.0F),
                PartPose.offsetAndRotation(10.0F, -11.0F, 2.0F, 0.3F, 0.25F, 0.0F));
        hornLeft.addOrReplaceChild("horn_left_tip", CubeListBuilder.create()
                .texOffs(56, 189).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 9.0F, 0.55F, 0.1F, 0.0F));
        PartDefinition hornRight = head.addOrReplaceChild("horn_right", CubeListBuilder.create()
                .texOffs(56, 175).mirror().addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 10.0F),
                PartPose.offsetAndRotation(-10.0F, -11.0F, 2.0F, 0.3F, -0.25F, 0.0F));
        hornRight.addOrReplaceChild("horn_right_tip", CubeListBuilder.create()
                .texOffs(56, 189).mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 9.0F, 0.55F, -0.1F, 0.0F));
        head.addOrReplaceChild("frill_left", CubeListBuilder.create()
                .texOffs(88, 100).addBox(0.0F, -10.0F, 0.0F, 1.0F, 20.0F, 16.0F),
                PartPose.offsetAndRotation(14.5F, -1.0F, 6.0F, 0.0F, 0.55F, 0.0F));
        head.addOrReplaceChild("frill_right", CubeListBuilder.create()
                .texOffs(88, 100).mirror().addBox(-1.0F, -10.0F, 0.0F, 1.0F, 20.0F, 16.0F),
                PartPose.offsetAndRotation(-14.5F, -1.0F, 6.0F, 0.0F, -0.55F, 0.0F));
        head.addOrReplaceChild("barbel_left", CubeListBuilder.create()
                .texOffs(52, 201).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(10.0F, 2.0F, -27.0F, 0.35F, 0.0F, -0.25F));
        head.addOrReplaceChild("barbel_right", CubeListBuilder.create()
                .texOffs(52, 201).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(-10.0F, 2.0F, -27.0F, 0.35F, 0.0F, 0.25F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(116, 54).addBox(-12.0F, 0.0F, -38.0F, 24.0F, 7.0F, 38.0F)
                .texOffs(0, 142).addBox(-10.0F, -2.0F, -37.0F, 20.0F, 2.0F, 31.0F)
                .texOffs(96, 201).addBox(-3.0F, 7.0F, -36.0F, 6.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 5.0F, 10.0F));

        // ------------------------------------------------------------------ body segments (pivot = front joint)
        for (int i = 0; i < LeviathanBody.SEGMENTS; i++) {
            PartDefinition seg;
            if (i < 3) {
                seg = root.addOrReplaceChild("seg" + i, CubeListBuilder.create()
                        .texOffs(46, 0).addBox(-13.0F, -11.0F, -2.0F, 26.0F, 22.0F, 26.0F), PartPose.ZERO);
                seg.addOrReplaceChild("fin", CubeListBuilder.create()
                        .texOffs(102, 142).addBox(-1.0F, -9.0F, 0.0F, 2.0F, 9.0F, 20.0F),
                        PartPose.offset(0.0F, -10.0F, 2.0F));
                seg.addOrReplaceChild("left_fin", CubeListBuilder.create()
                        .texOffs(0, 201).addBox(0.0F, 0.0F, 0.0F, 14.0F, 1.0F, 12.0F),
                        PartPose.offsetAndRotation(12.5F, 5.0F, 3.0F, 0.0F, -0.3F, 0.5F));
                seg.addOrReplaceChild("right_fin", CubeListBuilder.create()
                        .texOffs(0, 201).mirror().addBox(-14.0F, 0.0F, 0.0F, 14.0F, 1.0F, 12.0F),
                        PartPose.offsetAndRotation(-12.5F, 5.0F, 3.0F, 0.0F, 0.3F, -0.5F));
            } else {
                seg = root.addOrReplaceChild("seg" + i, CubeListBuilder.create()
                        .texOffs(0, 100).addBox(-10.0F, -9.0F, -2.0F, 20.0F, 18.0F, 24.0F), PartPose.ZERO);
                seg.addOrReplaceChild("fin", CubeListBuilder.create()
                        .texOffs(0, 175).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 8.0F, 18.0F),
                        PartPose.offset(0.0F, -8.0F, 2.0F));
            }
            if (i == 0) {
                // the broken golden shackle of the Tidewardens
                PartDefinition collar = seg.addOrReplaceChild("collar", CubeListBuilder.create()
                        .texOffs(40, 175).addBox(-14.0F, -9.0F, 6.0F, 3.0F, 21.0F, 5.0F)
                        .texOffs(146, 142).addBox(11.0F, -12.0F, 6.0F, 3.0F, 24.0F, 5.0F)
                        .texOffs(146, 201).addBox(-11.0F, 9.0F, 6.0F, 22.0F, 3.0F, 5.0F)
                        .texOffs(200, 201).addBox(3.0F, -12.0F, 6.0F, 8.0F, 3.0F, 5.0F), PartPose.ZERO);
                collar.addOrReplaceChild("chain", CubeListBuilder.create()
                        .texOffs(138, 201).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                        PartPose.offset(-12.5F, -9.0F, 8.5F));
            }
        }

        // ------------------------------------------------------------------ tail
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(178, 100).addBox(-7.0F, -6.0F, -2.0F, 14.0F, 12.0F, 22.0F), PartPose.ZERO);
        tail.addOrReplaceChild("fin", CubeListBuilder.create()
                .texOffs(146, 175).addBox(-1.0F, -6.0F, 0.0F, 2.0F, 6.0F, 14.0F),
                PartPose.offset(0.0F, -5.0F, 1.0F));
        tail.addOrReplaceChild("fluke", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -16.0F, 0.0F, 1.0F, 32.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 17.0F));
        tail.addOrReplaceChild("fluke_left", CubeListBuilder.create()
                .texOffs(56, 201).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 10.0F),
                PartPose.offsetAndRotation(6.0F, 2.0F, 12.0F, 0.0F, -0.45F, 0.2F));
        tail.addOrReplaceChild("fluke_right", CubeListBuilder.create()
                .texOffs(56, 201).mirror().addBox(-10.0F, 0.0F, 0.0F, 10.0F, 1.0F, 10.0F),
                PartPose.offsetAndRotation(-6.0F, 2.0F, 12.0F, 0.0F, 0.45F, -0.2F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(LeviathanRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        for (int i = 0; i < LeviathanRenderState.PARTS; i++) {
            ModelPart p = this.parts[i];
            p.x = -state.px[i] * 16.0F;
            p.y = -state.py[i] * 16.0F;
            p.z = state.pz[i] * 16.0F;
            p.yRot = state.yaw[i] * Mth.DEG_TO_RAD + Mth.PI;
            p.xRot = state.pitch[i] * Mth.DEG_TO_RAD;
            if (i >= 1 && i <= LeviathanBody.SEGMENTS) {
                float s = LeviathanBody.SEGMENT_SCALE[i - 1];
                p.xScale = s;
                p.yScale = s;
                p.zScale = s;
            }
        }
        float swim = state.inWater ? 1.0F : 0.35F;
        float pace = 0.16F + state.speed * 0.5F;

        this.jaw.xRot = state.jaw * 0.8F;
        float frill = Mth.sin(t * 0.12F) * 0.12F * swim + state.jaw * 0.35F;
        this.frillLeft.yRot += frill;
        this.frillRight.yRot -= frill;
        this.barbelLeft.xRot += Mth.sin(t * 0.09F) * 0.25F * swim;
        this.barbelRight.xRot += Mth.sin(t * 0.09F + 1.3F) * 0.25F * swim;
        this.barbelLeft.zRot += Mth.sin(t * 0.07F) * 0.12F;
        this.barbelRight.zRot -= Mth.sin(t * 0.07F + 0.8F) * 0.12F;

        for (int i = 0; i < LeviathanBody.SEGMENTS; i++) {
            this.fins[i].xRot = Mth.sin(t * pace - i * 0.7F) * 0.08F * swim;
            this.fins[i].zRot = state.bend[i + 1] * Mth.DEG_TO_RAD * 0.4F;
        }
        for (int i = 0; i < 3; i++) {
            float flap = Mth.sin(t * 0.22F + i * 1.1F) * 0.32F * swim;
            this.sideFinsLeft[i].zRot += flap;
            this.sideFinsRight[i].zRot -= flap;
            this.sideFinsLeft[i].yRot += Mth.sin(t * 0.22F + i * 1.1F + 1.2F) * 0.12F * swim;
            this.sideFinsRight[i].yRot -= Mth.sin(t * 0.22F + i * 1.1F + 1.2F) * 0.12F * swim;
        }
        this.chain.zRot = Mth.sin(t * 0.1F) * 0.25F;
        this.chain.xRot = Mth.sin(t * 0.13F + 0.5F) * 0.2F;

        float sweep = Mth.sin(t * pace * 1.3F) * (0.22F + state.speed * 0.8F) * swim;
        this.fluke.yRot = sweep + state.bend[7] * Mth.DEG_TO_RAD * 0.5F;
        this.tailFin.zRot = sweep * 0.3F;
        float tf = Mth.sin(t * pace * 1.3F + 0.8F) * 0.25F * swim;
        this.flukeLeft.zRot += tf;
        this.flukeRight.zRot -= tf;
        if (!state.inWater && state.state == 5) {
            // stranded: the tail slaps the floor
            this.fluke.yRot += Mth.sin(t * 0.6F) * 0.5F;
        }
    }
}
