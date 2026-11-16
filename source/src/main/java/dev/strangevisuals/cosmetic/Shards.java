package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Shards {
    private static final int COUNT = 20;
    private static final float SIZE = 0.05f;
    private static final int FACES = 6;
    private static final int WAIST = 5;

    private static final float RECOIL = 0.75f;

    private static final Vector3f center = new Vector3f();
    private static final Vector3f along = new Vector3f();
    private static final Vector3f across = new Vector3f();
    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private Shards() {
    }

    public static void render(CosmeticContext ctx) {
        int deep = Material.deepen(ctx.color, 0.25f);
        int rim = Material.lift(ctx.color, 0.5f);

        for (int i = 0; i < COUNT; i++) {
            float phase = Material.noise(i * 7 + 1) * (float) (Math.PI * 2.0);
            float pace = 0.3f + Material.noise(i * 7 + 2) * 0.52f;
            float tilt = (Material.noise(i * 7 + 3) - 0.5f) * 0.95f;
            float radius = 0.5f + Material.noise(i * 7 + 4) * 0.32f + ctx.hurt * RECOIL;
            float height = 0.5f + Material.noise(i * 7 + 5) * 1.05f;
            float size = SIZE * ctx.size * (0.5f + Material.noise(i * 7 + 6) * 1.0f);

            float angle = ctx.time * pace + phase;
            float drift = (float) Math.sin(ctx.time * 0.8f + phase) * 0.07f;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            center.set(cos * radius, height + drift + sin * tilt * radius, sin * radius);

            along.set(-sin, tilt * 0.8f, cos).normalize();
            across.set(0.0f, 1.0f, 0.0f).cross(along).normalize();

            float spin = ctx.time * (0.8f + pace) + phase;
            float wide = size * (1.7f + (float) Math.sin(spin) * 0.55f);
            float thin = size * 0.3f;
            float alpha = 0.16f + Material.noise(i * 7 + 7) * 0.16f + ctx.hurt * 0.3f;

            a.set(center).fma(-wide, along);
            b.set(center).fma(wide, along);
            b.sub(a, along);
            Sweep.begin(FACES);
            for (int step = 0; step <= WAIST; step++) {
                float t = step / (float) WAIST;
                float girth = thin * (float) Math.sin(Math.pow(t, 0.8) * Math.PI) * 1.15f;
                c.set(a).fma(t, along);
                Sweep.ringAcross(ctx, c, along, across, girth, girth * 0.55f,
                    Material.argb(Material.mix(deep, rim, t * t), alpha * (0.4f + t * 0.5f)),
                    alpha * (1.0f + t), rim);
            }
        }
    }
}
