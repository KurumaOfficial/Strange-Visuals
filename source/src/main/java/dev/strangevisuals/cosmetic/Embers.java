package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Embers {
    private static final int COUNT = 30;

    private static final float LIFE = 2.6f;
    private static final float RISE = 2.1f;
    private static final float SIZE = 0.022f;

    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private Embers() {
    }

    public static void render(CosmeticContext ctx) {
        float drive = ctx.drive();
        int warm = Material.shift(ctx.color, -20.0f);
        int rim = Material.lift(ctx.color, 0.55f);

        for (int i = 0; i < COUNT; i++) {
            float offset = Material.noise(i * 11 + 1);
            float pace = 0.75f + Material.noise(i * 11 + 2) * 0.6f;
            float t = ((ctx.time * pace / LIFE) + offset) % 1.0f;

            float angle = Material.noise(i * 11 + 3) * (float) (Math.PI * 2.0);
            float radius = (0.18f + Material.noise(i * 11 + 4) * 0.42f) * (0.6f + t * 0.7f);
            float sway = (float) Math.sin(ctx.time * 1.6f + offset * 9.0f) * 0.09f * t;

            float x = (float) Math.cos(angle) * radius + sway;
            float z = (float) Math.sin(angle) * radius - drive * t * 0.55f;
            float y = t * RISE * ctx.size + 0.08f;

            float alpha = Material.smoothstep(0.0f, 0.12f, t) * (1.0f - Material.smoothstep(0.45f, 1.0f, t));
            if (alpha < 0.02f) {
                continue;
            }
            float size = SIZE * ctx.size * (1.0f + Material.noise(i * 11 + 5) * 1.4f) * (1.0f - t * 0.45f);
            float stretch = size * (2.2f + drive * 2.0f);

            a.set(x - size, y + stretch, z);
            b.set(x + size, y + stretch, z);
            c.set(x + size * 0.35f, y - stretch * 0.6f, z);
            d.set(x - size * 0.35f, y - stretch * 0.6f, z);

            Material.body(ctx, a, b, c, d,
                Material.argb(warm, alpha * 0.3f), Material.argb(warm, 0.0f));
            Material.light(ctx, a, b, c, d,
                Material.argb(rim, alpha * 0.4f), Material.argb(rim, 0.0f));
        }
    }
}
