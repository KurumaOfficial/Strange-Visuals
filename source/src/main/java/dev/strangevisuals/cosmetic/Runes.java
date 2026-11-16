package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Runes {
    private static final int COUNT = 7;
    private static final float RADIUS = 0.78f;
    private static final float PLATE = 0.13f;
    private static final float THICK = 0.012f;
    private static final int BARS = 4;

    private static final float[] CORNERS = {-1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f};

    private static final Vector3f center = new Vector3f();
    private static final Vector3f across = new Vector3f();
    private static final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f out = new Vector3f();
    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private Runes() {
    }

    public static void render(CosmeticContext ctx) {
        float s = ctx.size;
        float turn = ctx.time * 0.32f;
        int deep = Material.deepen(Material.shift(ctx.color, -8.0f), 0.4f);
        int rim = Material.lift(ctx.color, 0.5f);

        for (int i = 0; i < COUNT; i++) {
            float angle = turn + (float) (Math.PI * 2.0 * i / COUNT);
            float radius = (RADIUS + Material.noise(i * 5 + 1) * 0.16f) * s * (1.0f + ctx.hurt * 0.5f);
            float height = 0.55f + Material.noise(i * 5 + 2) * 0.95f
                + (float) Math.sin(ctx.time * 0.9f + i * 1.7f) * 0.06f;
            float half = PLATE * s * (0.7f + Material.noise(i * 5 + 3) * 0.6f);
            float thick = THICK * s;

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            center.set(cos * radius, height, sin * radius);

            across.set(-sin, 0.0f, cos);
            out.set(cos, 0.0f, sin);

            float alpha = 0.28f + Material.noise(i * 5 + 4) * 0.12f;
            face(ctx, half * 0.88f, thick, Material.argb(deep, alpha));
            face(ctx, half * 0.88f, -thick, Material.argb(deep, alpha));
            border(ctx, half, thick, Material.argb(deep, alpha * 1.4f));
            mark(ctx, i, half, thick, rim);
        }
    }

    private static void face(CosmeticContext ctx, float half, float offset, int color) {
        corner(a, -half, half, offset);
        corner(b, half, half, offset);
        corner(c, half, -half, offset);
        corner(d, -half, -half, offset);
        Material.body(ctx, a, b, c, d, color, color);
    }

    private static void border(CosmeticContext ctx, float half, float thick, int color) {
        float inset = half * 0.88f;
        int edge = Material.argb(Material.lift(color & 0xFFFFFF, 0.2f), (color >>> 24) / 255.0f);
        for (int side = 0; side < 4; side++) {
            float x0 = CORNERS[side * 2];
            float y0 = CORNERS[side * 2 + 1];
            float x1 = CORNERS[(side * 2 + 2) % 8];
            float y1 = CORNERS[(side * 2 + 3) % 8];
            for (int face = -1; face <= 1; face += 2) {
                corner(a, x0 * inset, y0 * inset, thick * face);
                corner(b, x1 * inset, y1 * inset, thick * face);
                corner(c, x1 * half, y1 * half, thick * 0.35f * face);
                corner(d, x0 * half, y0 * half, thick * 0.35f * face);
                Material.body(ctx, a, b, c, d, color, edge);
            }
            corner(a, x0 * half, y0 * half, thick * 0.35f);
            corner(b, x1 * half, y1 * half, thick * 0.35f);
            corner(c, x1 * half, y1 * half, -thick * 0.35f);
            corner(d, x0 * half, y0 * half, -thick * 0.35f);
            Material.body(ctx, a, b, c, d, edge, edge);
        }
    }

    private static void mark(CosmeticContext ctx, int index, float half, float thick, int rim) {
        int pattern = (int) (Material.noise(index * 5 + 7) * 4096.0f);
        float lit = 0.24f + (float) Math.sin(ctx.time * 2.2f + index) * 0.06f;
        float step = half * 2.0f / (BARS + 1);
        float bar = half * 0.07f;

        for (int i = 0; i < BARS; i++) {
            if ((pattern >> i & 1) == 0) {
                continue;
            }
            float y = -half + step * (i + 1);
            float reach = half * (0.34f + (pattern >> (i + 6) & 3) * 0.2f);
            corner(a, -reach, y + bar, thick * 1.3f);
            corner(b, reach, y + bar, thick * 1.3f);
            corner(c, reach, y - bar, thick * 1.3f);
            corner(d, -reach, y - bar, thick * 1.3f);
            Material.light(ctx, a, b, c, d, Material.argb(rim, lit), Material.argb(rim, lit));
        }
    }

    private static void corner(Vector3f target, float x, float y, float offset) {
        target.set(center).fma(x, across).fma(y, up).fma(offset, out);
    }
}
