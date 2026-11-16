package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Spires {
    private static final int OUTER_COUNT = 9;
    private static final int INNER_COUNT = 9;
    private static final float RADIUS = 0.3f;
    private static final float HEIGHT = 0.3f;
    private static final float BASE = 0.05f;
    private static final int HORN_SIDES = 11;
    private static final int HORN_STEPS = 6;

    private static final Vector3f tip = new Vector3f();
    private static final Vector3f[] foot = {new Vector3f(), new Vector3f(), new Vector3f()};

    private Spires() {
    }

    public static void render(CosmeticContext ctx) {
        float y = ctx.height + 0.06f;
        ring(ctx, y, OUTER_COUNT, RADIUS, HEIGHT * 0.72f, 0.0f, 16.0f, 0.72f);
        ring(ctx, y + 0.015f, INNER_COUNT, RADIUS * 0.68f, HEIGHT, 0.5f, 7.0f, 1.0f);
    }

    private static void ring(CosmeticContext ctx, float y, int count, float radius, float height,
                             float offset, float lean, float weight) {
        float s = ctx.size;
        int deep = Material.deepen(Material.shift(ctx.color, -14.0f), 0.36f);
        int point = Material.shift(ctx.color, 20.0f);
        int rim = Material.lift(ctx.color, 0.48f);
        float step = (float) (Math.PI * 2.0 / count);

        for (int i = 0; i < count; i++) {
            float angle = (i + offset) * step;

            float breathe = (float) Math.sin(ctx.time * 1.1f + i * 1.37f) * 0.1f;
            float outward = (float) Math.toRadians(lean) * (1.0f + breathe);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            Sweep.begin(HORN_SIDES);
            float footRadius = radius * s;
            float tall = height * s * (1.0f + breathe * 0.22f);
            float reach = footRadius + (float) Math.sin(outward) * tall;
            tip.set(cos * reach, y + (float) Math.cos(outward) * tall, sin * reach);

            foot[0].set(cos * footRadius, y, sin * footRadius);
            foot[1].set(tip).sub(foot[0]);
            float wide = BASE * s * 1.35f;
            for (int step2 = 0; step2 <= HORN_STEPS; step2++) {
                float t = step2 / (float) HORN_STEPS;

                float across = wide * (float) Math.pow(1.0f - t, 0.72) * (1.0f - t * 0.05f);
                foot[2].set(foot[0]).fma(t, foot[1]);
                Sweep.ring(ctx, foot[2], foot[1], across, across,
                    Material.argb(Material.mix(deep, point, t * t), (0.34f - t * 0.26f) * weight),
                    (0.18f + t * 0.16f) * weight, rim);
            }
        }
    }
}
