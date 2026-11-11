package net.kasax.raft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Z-fighting fix for Beach Craft's entity and block entity models, applied once when a layer is baked: two faces of
 * different cubes that lie in the same plane (in the model's rest pose), face the same way, overlap
 * and show different pixels there flicker in the game. The smaller of the two faces (a band, a fin, a decal on a body)
 * is lifted by {@link #LIFT} pixels along its normal, so it is always drawn on top. Found with the bctest command
 * {@code dumpmodels} and tools/bctest/zfight_entities.py.
 */
public final class ModelZFix {
    /** How far the smaller face is lifted, in model pixels (1/16 block before the model's own scale). */
    public static final float LIFT = 0.04F;

    private record Face(String path, ModelPart.Cube cube, ModelPart.Polygon poly, Vector3f[] world, Vector3f normal) {
    }

    private ModelZFix() {
    }

    /** Lifts the smaller face of every z-fighting pair in the baked model; returns how many faces were lifted. */
    public static int fix(ModelPart root) {
        List<Face> faces = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            Matrix4f m = pose.pose();
            for (ModelPart.Polygon poly : ((net.kasax.raft.compat.mixin.CubeAccess) cube).raft$polygons()) {
                ModelPart.Vertex[] v = poly.vertices;
                if (v.length != 4) {
                    continue;
                }
                Vector3f[] w = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    w[i] = m.transformPosition(v[i].pos.x() / 16F, v[i].pos.y() / 16F, v[i].pos.z() / 16F, new Vector3f()).mul(16F);
                }
                Vector3f n = new Vector3f(w[1]).sub(w[0]).cross(new Vector3f(w[2]).sub(w[0]));
                if (n.lengthSquared() < 1e-12F) {
                    continue;
                }
                faces.add(new Face(path, cube, poly, w, n.normalize()));
            }
        });
        Map<ModelPart.Polygon, Boolean> lifted = new IdentityHashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            Face a = faces.get(i);
            for (int k = i + 1; k < faces.size(); k++) {
                Face b = faces.get(k);
                if (a.cube == b.cube || a.normal.dot(b.normal) < 0.9999F
                        || Math.abs(new Vector3f(b.world[0]).sub(a.world[0]).dot(a.normal)) > 0.01F) {
                    continue;
                }
                if (overlap(a, b) < 0.05F || sameUv(a, b)) {
                    continue;
                }
                Face small = area(a) <= area(b) ? a : b;
                if (lifted.put(small.poly, Boolean.TRUE) == null) {
                    lift(small.poly);
                }
            }
        }
        return lifted.size();
    }

    private static void lift(ModelPart.Polygon poly) {
        // (the vertex positions are shared between the faces of a cube: every lifted vertex gets its own copy)
        ModelPart.Vertex[] v = poly.vertices;
        float nx = poly.normal.x() * LIFT, ny = poly.normal.y() * LIFT, nz = poly.normal.z() * LIFT;
        for (int i = 0; i < v.length; i++) {
            v[i] = new ModelPart.Vertex(v[i].pos.x() + nx, v[i].pos.y() + ny, v[i].pos.z() + nz, v[i].u, v[i].v);
        }
    }

    private static float area(Face f) {
        return new Vector3f(f.world[1]).sub(f.world[0]).cross(new Vector3f(f.world[3]).sub(f.world[0])).length();
    }

    /** Overlap area of two coplanar quads (both rectangles in the plane), in square pixels. */
    private static float overlap(Face a, Face b) {
        Vector3f ax = new Vector3f(a.world[1]).sub(a.world[0]).normalize();
        Vector3f ay = new Vector3f(a.normal).cross(ax);
        float[] ra = range(a.world, ax, ay), rb = range(b.world, ax, ay);
        float w = Math.min(ra[1], rb[1]) - Math.max(ra[0], rb[0]);
        float h = Math.min(ra[3], rb[3]) - Math.max(ra[2], rb[2]);
        return w > 0 && h > 0 ? w * h : 0;
    }

    private static float[] range(Vector3f[] w, Vector3f ax, Vector3f ay) {
        float[] r = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Vector3f p : w) {
            float x = p.dot(ax), y = p.dot(ay);
            r[0] = Math.min(r[0], x);
            r[1] = Math.max(r[1], x);
            r[2] = Math.min(r[2], y);
            r[3] = Math.max(r[3], y);
        }
        return r;
    }

    /** Both faces map the overlap to the same texture pixels (then nothing flickers): compared at b's corners. */
    private static boolean sameUv(Face a, Face b) {
        for (int i = 0; i < 4; i++) {
            float[] ua = uvAt(a, b.world[i]);
            if (ua == null) {
                return false;
            }
            ModelPart.Vertex vb = b.poly.vertices[i];
            if (Math.abs(ua[0] - vb.u) > 1e-4F || Math.abs(ua[1] - vb.v) > 1e-4F) {
                return false;
            }
        }
        return true;
    }

    private static float[] uvAt(Face f, Vector3f q) {
        Vector3f e1 = new Vector3f(f.world[1]).sub(f.world[0]);
        Vector3f e2 = new Vector3f(f.world[3]).sub(f.world[0]);
        Vector3f d = new Vector3f(q).sub(f.world[0]);
        float s = d.dot(e1) / e1.lengthSquared(), t = d.dot(e2) / e2.lengthSquared();
        ModelPart.Vertex[] v = f.poly.vertices;
        return new float[]{v[0].u + s * (v[1].u - v[0].u) + t * (v[3].u - v[0].u),
                v[0].v + s * (v[1].v - v[0].v) + t * (v[3].v - v[0].v)};
    }
}
