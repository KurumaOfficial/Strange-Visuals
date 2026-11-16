package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Tendrils {
    private static final int LIMBS = 4;
    private static final int SEGMENTS = 30;
    private static final int SIDES = 13;

    private static final float ROOT_Y = 1.24f;
    private static final float ROOT_Z = -0.12f;
    private static final float LENGTH = 2.3f;
    private static final float GIRTH = 0.085f;

    private static final Vector3f spine = new Vector3f();
    private static final Vector3f lastSpine = new Vector3f();
    private static final Vector3f forward = new Vector3f();
    private static final Vector3f barbUp = new Vector3f();
    private static final Vector3f barbAim = new Vector3f();
    private static final Vector3f barbAt = new Vector3f();

    private static final float[] barbs = new float[(SEGMENTS + 1) * 8];
    private static int barbCount;

    private Tendrils() {
    }

    public static void render(CosmeticContext ctx) {
        float drive = ctx.drive();
        for (int i = 0; i < LIMBS; i++) {
            limb(ctx, i, drive);
        }
    }

    private static void limb(CosmeticContext ctx, int index, float drive) {
        int side = index < 2 ? -1 : 1;
        boolean inner = index == 1 || index == 2;
        float length = LENGTH * ctx.size * (inner ? 0.78f : 1.0f);
        float girth = GIRTH * ctx.size * (inner ? 0.82f : 1.0f);
        float spread = (float) Math.toRadians(inner ? 22.0f : 46.0f);
        float phase = ctx.time * 1.35f + index * 1.9f;
        float lash = 0.09f + drive * 0.13f + ctx.hurt * 0.26f;

        int root = Material.deepen(Material.shift(ctx.color, -18.0f), 0.45f);
        int end = Material.shift(ctx.color, 26.0f);
        int rim = Material.lift(ctx.color, 0.45f);

        Sweep.begin(SIDES);
        for (int seg = 0; seg <= SEGMENTS; seg++) {
            float t = seg / (float) SEGMENTS;
            path(spine, side, spread, t, length, phase, lash, inner);
            if (seg == 0) {
                forward.set((float) Math.sin(spread) * side, 0.35f, -1.0f).normalize();
            } else {
                forward.set(spine).sub(lastSpine);
                if (forward.lengthSquared() < 1.0e-9f) {
                    forward.set(0.0f, 0.0f, -1.0f);
                }
                forward.normalize();
            }

            float rib = 1.0f + 0.11f * (float) Math.cos(t * SEGMENTS * 0.62f);
            float radius = girth * taper(t) * rib;
            float shade = 1.0f - t;
            Sweep.ring(ctx, spine, forward, radius, radius * 0.82f,
                Material.argb(Material.mix(end, root, shade * shade), 0.1f + shade * 0.4f),
                0.3f * shade + 0.1f, rim);

            if (seg > 0 && seg % 3 == 0 && t > 0.12f && t < 0.94f) {
                note(radius, t, seg, index);
            }
            lastSpine.set(spine);
        }
        barbs(ctx, end, rim);
    }

    private static void path(Vector3f out, int side, float spread, float t, float length,
                             float phase, float lash, boolean inner) {
        float reach = length * t;
        float rise = (inner ? 0.55f : 0.32f) * length * (float) Math.sin(t * Math.PI * 0.72);

        float slow = (float) Math.sin(phase - t * 1.6f) * lash * reach;
        float fast = (float) Math.sin(phase * 2.7f - t * 4.4f) * lash * 0.45f * reach * t;

        float x = ((float) Math.sin(spread) * reach + slow) * side;
        float y = ROOT_Y + rise + fast;
        float z = ROOT_Z - (float) Math.cos(spread) * reach * 0.78f + slow * 0.4f;
        out.set(x, y, z);
    }

    private static void note(float radius, float t, int seg, int index) {
        if (barbCount + 8 > barbs.length) {
            return;
        }

        barbUp.set(0.0f, 1.0f, 0.0f).fma(-forward.y, forward);
        if (barbUp.lengthSquared() < 1.0e-8f) {
            barbUp.set(1.0f, 0.0f, 0.0f);
        }
        barbUp.normalize();

        float length = radius * (2.4f + Material.noise(seg * 7 + index * 31) * 1.6f)
            * (float) Math.sin(Math.pow(t, 0.7) * Math.PI);
        barbs[barbCount++] = spine.x + barbUp.x * radius * 0.8f;
        barbs[barbCount++] = spine.y + barbUp.y * radius * 0.8f;
        barbs[barbCount++] = spine.z + barbUp.z * radius * 0.8f;

        barbs[barbCount++] = barbUp.x * 0.78f - forward.x * 0.62f;
        barbs[barbCount++] = barbUp.y * 0.78f - forward.y * 0.62f;
        barbs[barbCount++] = barbUp.z * 0.78f - forward.z * 0.62f;
        barbs[barbCount++] = length;
        barbs[barbCount++] = radius;
    }

    private static void barbs(CosmeticContext ctx, int end, int rim) {
        for (int i = 0; i + 8 <= barbCount; i += 8) {
            float length = barbs[i + 6];
            float radius = barbs[i + 7];
            if (length < 1.0e-4f) {
                continue;
            }
            barbAim.set(barbs[i + 3], barbs[i + 4], barbs[i + 5]);
            if (barbAim.lengthSquared() < 1.0e-8f) {
                continue;
            }
            barbAim.normalize();
            Sweep.begin(7);
            for (int step = 0; step <= 3; step++) {
                float u = step / 3.0f;
                float across = radius * 0.55f * (float) Math.pow(1.0f - u, 0.7);
                barbAt.set(barbs[i], barbs[i + 1], barbs[i + 2]).fma(length * u, barbAim);
                Sweep.ring(ctx, barbAt, barbAim, across, across,
                    Material.argb(end, 0.34f - u * 0.26f), 0.3f - u * 0.1f, rim);
            }
        }
        barbCount = 0;
    }

    private static float taper(float t) {
        return Material.smoothstep(0.0f, 0.1f, t) * (float) Math.pow(1.0f - t, 0.75) * 1.25f;
    }
}
