package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Halo {
    private static final int BLADES = 22;
    private static final int INNER_BLADES = 13;
    private static final float INNER = 0.16f;
    private static final float OUTER = 0.47f;

    private static final float DROP = 0.19f;
    private static final float VEIL = 0.12f;
    private static final float SPIN = 0.4f;
    private static final int RUNNERS = 2;

    private static final int AROUND = 64;
    private static final int PROFILE = 7;

    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();
    private static final Vector3f na = new Vector3f();
    private static final Vector3f nb = new Vector3f();
    private static final Vector3f edge0 = new Vector3f();
    private static final Vector3f edge1 = new Vector3f();

    private Halo() {
    }

    public static void render(CosmeticContext ctx) {
        float lift = ctx.player.isSneaking() ? 0.17f : 0.25f;
        float bob = (float) Math.sin(ctx.time * 1.35f) * 0.02f;
        float y = ctx.height + lift + bob;
        float spin = ctx.time * SPIN;

        brim(ctx, y, spin, BLADES, INNER, OUTER, DROP, 1.0f);

        brim(ctx, y - 0.035f, -spin * 1.45f, INNER_BLADES, INNER * 0.55f, OUTER * 0.58f,
            DROP * 0.5f, 0.55f);
        core(ctx, y, spin);
    }

    private static void brim(CosmeticContext ctx, float y, float spin, int ribCount,
                             float innerRadius, float outerRadius, float drop, float weight) {
        float s = ctx.size;
        int deep = Material.deepen(Material.shift(ctx.color, -12.0f), 0.35f);
        int tip = Material.shift(ctx.color, 18.0f);
        int rim = Material.lift(ctx.color, 0.45f);
        float inner = innerRadius * s;
        float outer = outerRadius * s;
        float fall = drop * s;
        float veil = VEIL * s;

        for (int step = 0; step < PROFILE; step++) {
            float t0 = step / (float) PROFILE;
            float t1 = (step + 1) / (float) PROFILE;
            profileAt(t0, inner, outer, fall, veil, edge0);
            profileAt(t1, inner, outer, fall, veil, edge1);

            float dr = edge1.x - edge0.x;
            float dy = edge1.y - edge0.y;
            float len = (float) Math.sqrt(dr * dr + dy * dy);
            if (len < 1.0e-6f) {
                continue;
            }
            float nr = dy / len;
            float ny = -dr / len;

            for (int i = 0; i < AROUND; i++) {
                float from = spin + (float) (Math.PI * 2.0) * i / AROUND;
                float to = spin + (float) (Math.PI * 2.0) * (i + 1) / AROUND;
                float lit = Math.max(runnerLight(from - spin, ctx.time),
                    runnerLight(to - spin, ctx.time));

                plot(a, from, edge0.x, y + edge0.y);
                plot(b, to, edge0.x, y + edge0.y);
                plot(c, to, edge1.x, y + edge1.y);
                plot(d, from, edge1.x, y + edge1.y);
                turned(from, nr, ny, na);
                turned(to, nr, ny, nb);

                int near = Material.argb(Material.mix(deep, tip, t0),
                    (0.3f - t0 * 0.2f + lit * 0.16f) * weight);
                int far = Material.argb(Material.mix(deep, tip, t1),
                    (0.3f - t1 * 0.2f + lit * 0.16f) * weight);
                Material.bodySmooth(ctx, a, b, c, d, na, nb, nb, na, near, near, far, far);
                Material.rimSmooth(ctx, a, b, c, d, na, nb, nb, na, rim,
                    (0.2f + lit * 0.5f) * weight);
            }
        }

        for (int i = 0; i < ribCount; i++) {
            float angle = spin + (float) (Math.PI * 2.0) * i / ribCount;
            float lit = runnerLight(angle - spin, ctx.time);
            for (int step = 0; step < PROFILE; step++) {
                profileAt(step / (float) PROFILE, inner, outer, fall, veil, edge0);
                profileAt((step + 1) / (float) PROFILE, inner, outer, fall, veil, edge1);

                float thin = 0.005f * s * (1.0f + lit);
                float spread0 = thin / Math.max(edge0.x, 0.02f);
                float spread1 = thin / Math.max(edge1.x, 0.02f);
                plot(a, angle - spread0, edge0.x, y + edge0.y + thin * 0.4f);
                plot(b, angle + spread0, edge0.x, y + edge0.y + thin * 0.4f);
                plot(c, angle + spread1, edge1.x, y + edge1.y + thin * 0.4f);
                plot(d, angle - spread1, edge1.x, y + edge1.y + thin * 0.4f);
                Material.light(ctx, a, b, c, d,
                    Material.argb(rim, (0.1f + lit * 0.3f) * weight),
                    Material.argb(rim, (0.06f + lit * 0.22f) * weight));
            }
        }
    }

    private static void profileAt(float t, float inner, float outer, float fall, float veil,
                                  Vector3f out) {
        if (t <= 0.7f) {
            float u = t / 0.7f;
            out.set(inner + (outer - inner) * u, -fall * (float) Math.pow(u, 1.7), 0.0f);
        } else {
            float u = (t - 0.7f) / 0.3f;
            out.set(outer * (1.0f + 0.03f * u), -fall - veil * u * (1.3f - u * 0.3f), 0.0f);
        }
    }

    private static void turned(float angle, float nr, float ny, Vector3f out) {
        out.set((float) Math.cos(angle) * nr, ny, (float) Math.sin(angle) * nr).normalize();
    }

    private static void core(CosmeticContext ctx, float y, float spin) {
        float s = ctx.size;
        float outer = INNER * s;
        float inner = outer * 0.7f;
        float pulse = 0.24f + (float) Math.sin(ctx.time * 2.1f) * 0.04f;
        int rim = Material.lift(ctx.color, 0.5f);

        int segments = BLADES * 2;
        float step = (float) (Math.PI * 2.0 / segments);
        for (int i = 0; i < segments; i++) {
            float from = spin + i * step;
            float to = from + step;
            plot(a, from, inner, y);
            plot(b, to, inner, y);
            plot(c, to, outer, y);
            plot(d, from, outer, y);
            Material.light(ctx, a, b, c, d,
                Material.argb(rim, pulse * 0.35f), Material.argb(rim, pulse));
        }
    }

    private static float runnerLight(float angle, float time) {
        float best = 0.0f;
        for (int i = 0; i < RUNNERS; i++) {
            float head = time * 1.15f + (float) (Math.PI * 2.0) * i / RUNNERS;
            float behind = wrap(head - angle);
            if (behind > 1.05f) {
                continue;
            }
            float fade = 1.0f - behind / 1.05f;
            best = Math.max(best, fade * fade);
        }
        return best;
    }

    private static float wrap(float radians) {
        float full = (float) (Math.PI * 2.0);
        float value = radians % full;
        return value < 0.0f ? value + full : value;
    }

    private static void plot(Vector3f out, float angle, float radius, float y) {
        out.set((float) Math.cos(angle) * radius, y, (float) Math.sin(angle) * radius);
    }
}
