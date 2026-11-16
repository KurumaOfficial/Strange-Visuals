package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Kagune {
    private static final int BLADES = 8;
    private static final int LAMINAE = 2;
    private static final int SEGMENTS = 20;
    private static final int BLADE_SIDES = 12;

    private static final float THICKNESS = 0.19f;

    private static final float ROOT_X = 0.115f;
    private static final float ROOT_Y = 1.26f;
    private static final float ROOT_Z = -0.11f;

    private static final float FIRST = -20.0f;
    private static final float LAST = 102.0f;

    private static final float CURL = 30.0f;
    private static final float LENGTH = 1.5f;
    private static final float WIDTH = 0.27f;

    private static final float SWEEP = 0.5f;

    private static final Vector3f spine = new Vector3f();
    private static final Vector3f lastSpine = new Vector3f();
    private static final Vector3f across = new Vector3f();

    private static final Vector3f pfl = new Vector3f();
    private static final Vector3f pfr = new Vector3f();
    private static final Vector3f pbl = new Vector3f();
    private static final Vector3f pbr = new Vector3f();
    private static final Vector3f nfl = new Vector3f();
    private static final Vector3f nfr = new Vector3f();
    private static final Vector3f nbl = new Vector3f();
    private static final Vector3f nbr = new Vector3f();
    private static final Vector3f ra = new Vector3f();
    private static final Vector3f rb = new Vector3f();
    private static final Vector3f rc = new Vector3f();
    private static final Vector3f rd = new Vector3f();

    private Kagune() {
    }

    public static void render(CosmeticContext ctx) {
        float drive = ctx.drive();

        float open = 1.0f + drive * 0.16f + ctx.hurt * 0.22f;
        float flutter = 0.03f + drive * 0.08f + ctx.hurt * 0.12f;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < BLADES; i++) {
                for (int lamina = 0; lamina < LAMINAE; lamina++) {
                    blade(ctx, side, i, lamina, open, flutter);
                }
            }
        }
    }

    private static void blade(CosmeticContext ctx, int side, int index, int lamina,
                              float open, float flutter) {
        float share = index / (float) (BLADES - 1);

        float shrink = 1.0f - lamina * 0.15f;
        float lean = (float) Math.toRadians((lamina - 1) * 4.5f);

        float base = (float) Math.toRadians((FIRST + (LAST - FIRST) * share) * open) + lean;
        float length = LENGTH * ctx.size * shrink
            * (0.56f + 0.44f * (float) Math.sin(Math.PI * (0.24f + share * 0.7f)));
        float width = WIDTH * ctx.size * shrink * (0.62f + 0.38f * (float) Math.sin(Math.PI * share));
        float depth = ROOT_Z - (0.03f * index + 0.022f * lamina) * ctx.size;
        float phase = ctx.time * 2.3f + index * 0.8f + lamina * 0.4f + (side > 0 ? 0.0f : 1.9f);
        int seed = index * 13 + lamina * 5 + (side > 0 ? 0 : 101);

        int root = Material.deepen(Material.shift(ctx.color, -16.0f), 0.42f);
        int tip = Material.shift(ctx.color, 22.0f);
        int rim = Material.lift(ctx.color, 0.4f);
        float fade = 1.0f - lamina * 0.22f;

        Sweep.begin(BLADE_SIDES);
        boolean first = true;
        for (int seg = 0; seg <= SEGMENTS; seg++) {
            float t = seg / (float) SEGMENTS;
            point(spine, side, ctx.size, base, t, length, depth, phase, flutter);
            if (first) {
                perpendicular(across, (float) Math.sin(base) * side, (float) Math.cos(base));
            } else {
                perpendicular(across, spine.x - lastSpine.x, spine.y - lastSpine.y);
            }

            float lead = leading(t) * width;
            float trail = trailing(t, seed) * width;

            float thick = (lead + trail) * THICKNESS + 0.004f * ctx.size;

            nfl.set(spine).fma((lead - trail) * 0.5f, across);
            nfr.set(spine).sub(lastSpine);
            if (first) {
                nfr.set((float) Math.sin(base) * side, (float) Math.cos(base), 0.0f);
            }
            float half = (lead + trail) * 0.5f;
            float shade = 1.0f - t;
            Sweep.ringAcross(ctx, nfl, nfr, across, half, thick,
                membrane(root, tip, shade, fade), 0.22f * shade * fade + 0.05f, rim);

            nbl.set(spine).fma(lead, across);
            if (!first) {
                float near = 1.0f - (seg - 1) / (float) SEGMENTS;
                float far = 1.0f - t;
                ra.set(pbl).add(0.0f, 0.0f, thick * 0.6f);
                rb.set(pbl).add(0.0f, 0.0f, -thick * 0.6f);
                rc.set(nbl).add(0.0f, 0.0f, -thick * 0.6f);
                rd.set(nbl).add(0.0f, 0.0f, thick * 0.6f);
                Material.light(ctx, ra, rb, rc, rd,
                    Material.argb(rim, near * near * 0.24f * fade),
                    Material.argb(rim, far * far * 0.24f * fade));
                if (lamina == 0) {
                    fibre(ctx, near, far, rim, 0.3f);
                    fibre(ctx, near, far, rim, -0.24f);
                }
            }

            pfl.set(spine).fma(lead, across);
            pfr.set(spine).fma(-trail, across);
            pbl.set(nbl);
            lastSpine.set(spine);
            first = false;
        }
    }

    private static int darker(int argb) {
        int alpha = Math.min(255, Math.round((argb >>> 24) * 1.5f));
        return alpha << 24 | Material.deepen(argb & 0xFFFFFF, 0.2f);
    }

    private static void edge(CosmeticContext ctx, float near, float far, int rim, float fade) {
        strip(0.0f, 0.1f);
        Material.light(ctx, ra, rb, rc, rd,
            Material.argb(rim, near * near * 0.22f * fade),
            Material.argb(rim, far * far * 0.22f * fade));
    }

    private static void fibre(CosmeticContext ctx, float near, float far, int rim, float at) {
        strip(0.5f + at, 0.5f + at + 0.05f);
        Material.light(ctx, ra, rb, rc, rd,
            Material.argb(rim, near * 0.08f), Material.argb(rim, far * far * 0.08f));
    }

    private static void strip(float from, float to) {
        pfl.lerp(pfr, from, ra);
        pfl.lerp(pfr, to, rb);
        nfl.lerp(nfr, to, rc);
        nfl.lerp(nfr, from, rd);
    }

    private static int membrane(int root, int tip, float along, float fade) {
        int rgb = Material.mix(tip, root, along * along);
        return Material.argb(rgb, (0.04f + along * along * 0.34f) * fade);
    }

    private static void point(Vector3f out, int side, float size, float base, float t, float length,
                              float depth, float phase, float flutter) {
        float angle = base + (float) Math.toRadians(CURL) * t * t;
        float reach = length * t;
        float wave = (float) Math.sin(phase - t * 3.3f) * flutter * t * t;

        float x = ROOT_X + (float) Math.sin(angle) * reach + wave * 0.45f;
        float y = ROOT_Y + (float) Math.cos(angle) * reach + wave;
        float z = depth - SWEEP * size * t * t;
        out.set(x * side, y, z);
    }

    private static float leading(float t) {
        return Material.smoothstep(0.0f, 0.14f, t) * (float) Math.pow(1.0f - t, 0.6) * 0.55f;
    }

    private static float trailing(float t, int seed) {
        float wobble = 0.68f + 0.32f * (float) Math.sin(t * 11.0f + Material.noise(seed) * 6.3f);
        return leading(t) * wobble * 1.25f;
    }

    private static void perpendicular(Vector3f out, float dx, float dy) {
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001f) {
            out.set(1.0f, 0.0f, 0.0f);
            return;
        }
        out.set(-dy / len, dx / len, 0.0f);
    }
}
