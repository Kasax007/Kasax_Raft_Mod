package net.kasax.raft.content.npc.client;

import net.kasax.raft.platform.client.EntityModelLayerRegistry;
import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.NpcCharacter;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Small 3D extras on the head that the player skin layout cannot hold: Tamo's straw hat brim, Okafor's hard hat
 * visor and ridge, Voss's hair bun, Nalu's hibiscus flower. Each is a humanoid model whose only cubes are on the head
 * (so it follows the head), drawn with its own texture {@code textures/entity/npc/<id>_extra.png} (64x32).
 */
public final class NpcAccessories {
    public static final ModelLayerLocation STRAW_HAT = layer("straw_hat");
    public static final ModelLayerLocation HARD_HAT = layer("hard_hat");
    public static final ModelLayerLocation BUN = layer("bun");
    public static final ModelLayerLocation FLOWER = layer("flower");

    private NpcAccessories() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Raft.id("npc"), name);
    }

    static void register() {
        EntityModelLayerRegistry.registerModelLayer(STRAW_HAT, () -> head(h -> h.addOrReplaceChild("brim",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.6F, -7.0F, 14.0F, 1.0F, 14.0F), PartPose.ZERO)));
        EntityModelLayerRegistry.registerModelLayer(HARD_HAT, () -> head(h -> {
            h.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -6.2F, -6.6F, 9.0F, 1.0F, 2.0F), PartPose.ZERO);
            h.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(0, 4).addBox(-0.5F, -9.1F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.ZERO);
        }));
        EntityModelLayerRegistry.registerModelLayer(BUN, () -> head(h -> h.addOrReplaceChild("bun",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -10.0F, 0.5F, 3.0F, 2.0F, 3.0F), PartPose.ZERO)));
        EntityModelLayerRegistry.registerModelLayer(FLOWER, () -> head(h -> h.addOrReplaceChild("flower",
                CubeListBuilder.create().texOffs(0, 0).addBox(4.1F, -6.5F, -1.5F, 1.0F, 2.0F, 2.0F), PartPose.ZERO)));
    }

    /** A humanoid mesh with empty parts except the cubes added to the head. Texture 64x32. */
    private static LayerDefinition head(Consumer<PartDefinition> cubes) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        cubes.accept(head);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** The extra of a character (null: none). */
    static ModelLayerLocation of(NpcCharacter c) {
        return switch (c) {
            case OLD_TAMO -> STRAW_HAT;
            case OKAFOR -> HARD_HAT;
            case DR_VOSS -> BUN;
            case NALU -> FLOWER;
            default -> null;
        };
    }

    static ResourceLocation texture(NpcCharacter c) {
        return Raft.id("textures/entity/npc/" + c.id + "_extra.png");
    }
}
