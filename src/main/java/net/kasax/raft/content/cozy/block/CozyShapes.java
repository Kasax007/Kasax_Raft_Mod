package net.kasax.raft.content.cozy.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Shapes of the Beach Craft 3.3 blocks (cozy, food and gear modules), from the generated *ShapeData classes. Food
 * shapes are added by name lookup so the cozy module does not depend on the food module's classes.
 */
public final class CozyShapes {
    private static final Map<String, double[][]> BOXES = new HashMap<>();

    static {
        CozyShapeData.fill(BOXES);
        try {
            Class.forName("net.kasax.raft.content.food.block.FoodShapeData").getMethod("fill", Map.class).invoke(null, BOXES);
        } catch (ReflectiveOperationException ignored) {
            // module not present
        }
    }

    private CozyShapes() {
    }

    public static VoxelShape shape(String name) {
        double[][] boxes = BOXES.get(name);
        if (boxes == null) throw new IllegalArgumentException("Unknown cozy shape " + name);
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            shape = Shapes.or(shape, Block.box(Math.max(0, b[0]), Math.max(0, b[1]), Math.max(0, b[2]),
                    Math.min(16, b[3]), Math.min(16, b[4]), Math.min(16, b[5])));
        }
        return shape.optimize();
    }
}
