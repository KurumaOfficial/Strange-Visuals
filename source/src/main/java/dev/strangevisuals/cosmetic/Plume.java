package dev.strangevisuals.cosmetic;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Plume {
    private static final int PRIMARIES = 9;
    private static final int SECONDARIES = 8;
    private static final int COVERTS = 11;
    private static final int VANE_STEPS = 4;

    private static final Matrix4f HERE = new Matrix4f();

    private static final Vector3f spineA = new Vector3f();
    private static final Vector3f spineB = new Vector3f();
    private static final Vector3f shaft = new Vector3f();
    private static final Vector3f chord = new Vector3f();
    private static final Vector3f bow = new Vector3f();
    private static final Vector3f near = new Vector3f();
    private static final Vector3f far = new Vector3f();
    private static final Vector3f spineNormal = new Vector3f();
    private static final Vector3f edgeNormal = new Vector3f();
    private static final Vector3f qa = new Vector3f();
    private static final Vector3f qb = new Vector3f();
    private static final Vector3f qc = new Vector3f();
    private static final Vector3f qd = new Vector3f();

    private static Matrix4f frame = HERE;
    private static int deep;
    private static int bright;
    private static float beat;

    private Plume() {
    }

    public static void render(CosmeticContext ctx, Matrix4f transform, float rootY, float rootOut,
                              float rootBack, float span, float open, float alpha,
                              float sweepBeat, int seed, int bodyColor, int rimColor) {
        frame = transform == null ? HERE : transform;
        deep = bodyColor;
        bright = rimColor;
        beat = sweepBeat;
        if (alpha < 0.01f) {
            return;
        }

        for (int s = -1; s <= 1; s += 2) {
            float rootX = s * rootOut;
            row(ctx, s, span, rootX, rootY, rootBack, alpha, PRIMARIES,
                0.42f, 1.0f, 0.62f, 1.0f, seed + 3);
            row(ctx, s, span, rootX, rootY, rootBack, alpha, SECONDARIES,
                0.14f, 0.5f, 0.34f, 0.78f, seed + 41);
            row(ctx, s, span, rootX, rootY, rootBack, alpha, COVERTS,
                0.0f, 0.34f, 0.15f, 0.55f, seed + 77);
            arm(ctx, s, span, rootX, rootY, rootBack, alpha);
        }
    }

    private static void arm(CosmeticContext ctx, int s, float span, float rootX, float rootY,
                            float rootBack, float alpha) {
        Sweep.begin(9);
        for (int k = 0; k <= 12; k++) {
            float t = k / 12.0f;
            bone(rootX, rootY, rootBack, s, span, t, spineA);
            bone(rootX, rootY, rootBack, s, span, Math.min(1.0f, t + 0.08f), spineB);
            chord.set(spineB).sub(spineA);
            if (chord.lengthSquared() < 1.0e-9f) {
                chord.set(s, 0.0f, 0.0f);
            }
            through(spineA, qa);
            through(spineB, qb);
            float thick = span * 0.028f * (1.0f - t * 0.7f) + span * 0.004f;
            Sweep.ring(ctx, qa, qb.sub(qa), thick, thick,
                Material.argb(Material.mix(bright, deep, t), alpha * (0.5f - t * 0.2f)),
                alpha * (0.35f - t * 0.15f), bright);
        }
    }

    private static void row(CosmeticContext ctx, int s, float span, float rootX, float rootY,
                            float rootBack, float alpha, int count, float from, float to,
                            float shortest, float longest, int seed) {
        for (int k = 0; k < count; k++) {
            float t = from + (to - from) * (k / (float) Math.max(1, count - 1));
            bone(rootX, rootY, rootBack, s, span, t, spineA);
            bone(rootX, rootY, rootBack, s, span, Math.min(1.0f, t + 0.06f), spineB);

            chord.set(spineB).sub(spineA);
            if (chord.lengthSquared() < 1.0e-8f) {
                chord.set(s, 0.0f, 0.0f);
            }
            chord.normalize();

            float u = k / (float) Math.max(1, count - 1);
            float reach = u * (1.0f - 0.35f * u * u) / 0.65f;
            float length = span * (shortest + (longest - shortest) * reach)
                * (0.88f + Material.noise(k * 13 + seed) * 0.24f);

            float lean = (k % 2 == 0 ? 1.0f : -1.0f) * 0.04f;
            shaft.set(s * (0.2f + t * 0.26f) + lean, -(0.44f + t * 0.42f), -(0.72f + t * 0.6f));
            shaft.normalize();
            chord.cross(shaft, bow);
            if (bow.lengthSquared() < 1.0e-8f) {
                bow.set(0.0f, 0.0f, 1.0f);
            } else {
                bow.normalize();
            }

            feather(ctx, length, span * (0.055f + 0.03f * (1.0f - t)),
                alpha * (0.55f + 0.45f * (float) Math.sin((k + 0.5f) / count * Math.PI)));
        }
    }

    private static void feather(CosmeticContext ctx, float length, float width, float alpha) {
        if (alpha < 0.01f) {
            return;
        }
        for (int i = 0; i < VANE_STEPS; i++) {
            float u0 = i / (float) VANE_STEPS;
            float u1 = (i + 1) / (float) VANE_STEPS;
            vaneAt(u0, length, near);
            vaneAt(u1, length, far);
            float w0 = width * profile(u0);
            float w1 = width * profile(u1);
            int rootShade = Material.argb(bright, alpha * (0.5f - u0 * 0.2f));
            int tipShade = Material.argb(bright, alpha * (0.5f - u1 * 0.2f));
            int rootEdge = Material.argb(deep, alpha * (0.42f - u0 * 0.24f));
            int tipEdge = Material.argb(deep, alpha * (0.42f - u1 * 0.24f));

            for (int flip = -1; flip <= 1; flip += 2) {
                through(near, qa);
                qb.set(near).fma(w0 * flip, chord);
                through(qb, qb);
                qc.set(far).fma(w1 * flip, chord);
                through(qc, qc);
                through(far, qd);

                lean(0.0f, spineNormal);
                lean(flip * 0.86f, edgeNormal);
                Material.bodySmooth(ctx, qa, qb, qc, qd,
                    spineNormal, edgeNormal, edgeNormal, spineNormal,
                    rootShade, rootEdge, tipEdge, tipShade);
                Material.rimSmooth(ctx, qa, qb, qc, qd,
                    spineNormal, edgeNormal, edgeNormal, spineNormal, bright, alpha * 0.45f);
            }
        }
    }

    private static void vaneAt(float u, float length, Vector3f out) {
        float curl = (float) Math.sin(u * Math.PI) * length * (0.1f + beat * 0.35f);
        out.set(spineA).fma(length * u, shaft).fma(curl, bow);
    }

    private static void lean(float amount, Vector3f out) {
        out.set(bow).fma(amount, chord).normalize();

        frame.transformDirection(out);
        out.normalize();
    }

    private static void through(Vector3f local, Vector3f out) {
        frame.transformPosition(local.x, local.y, local.z, out);
    }

    private static float profile(float u) {
        return (float) Math.pow(Math.sin(Math.pow(u, 0.62) * Math.PI), 0.75) * (1.0f - u * 0.12f);
    }

    private static void bone(float rootX, float rootY, float rootBack, int s, float span, float t,
                             Vector3f out) {
        float outward = span * (0.06f + 0.94f * (float) Math.sin(t * 1.35));
        float up = span * (0.52f * (float) Math.sin(t * 1.95) + beat * (0.4f + t));
        float back = rootBack - span * (0.05f + 0.3f * t * t);
        out.set(rootX + s * outward, rootY + up, back);
    }
}
