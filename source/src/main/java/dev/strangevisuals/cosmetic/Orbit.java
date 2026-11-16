package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Orbit {
    private static final int HOOPS = 3;
    private static final int SEGMENTS = 54;
    private static final int HOOP_SIDES = 8;
    private static final float RADIUS = 0.42f;
    private static final float BAND = 0.022f;
    private static final float THICK = 0.01f;

    private static final float[] TILT = {18.0f, 74.0f, 118.0f};
    private static final float[] LEAN = {0.0f, 40.0f, 95.0f};
    private static final float[] PACE = {0.55f, -0.38f, 0.27f};

    private static final Vector3f axisX = new Vector3f();
    private static final Vector3f axisY = new Vector3f();
    private static final Vector3f at = new Vector3f();
    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();

    private Orbit() {
    }

    public static void render(CosmeticContext ctx) {
        float y = ctx.height + 0.3f + (float) Math.sin(ctx.time * 1.2f) * 0.02f;
        for (int hoop = 0; hoop < HOOPS; hoop++) {
            hoop(ctx, hoop, y);
        }
    }

    private static void hoop(CosmeticContext ctx, int index, float centerY) {
        float tilt = (float) Math.toRadians(TILT[index]);
        float lean = (float) Math.toRadians(LEAN[index]);
        float radius = RADIUS * ctx.size * (1.0f - index * 0.11f);
        float band = BAND * ctx.size;
        float thick = THICK * ctx.size;
        float turn = ctx.time * PACE[index];

        axisX.set((float) Math.cos(lean), 0.0f, (float) Math.sin(lean));
        axisY.set((float) (-Math.sin(lean) * Math.sin(tilt)), (float) Math.cos(tilt),
            (float) (Math.cos(lean) * Math.sin(tilt)));

        int deep = Material.deepen(Material.shift(ctx.color, -10.0f), 0.3f);
        int rim = Material.lift(ctx.color, 0.5f);
        float step = (float) (Math.PI * 2.0 / SEGMENTS);
        float bead = wrap(ctx.time * (1.4f + index * 0.35f));

        Sweep.begin(HOOP_SIDES);
        for (int i = 0; i <= SEGMENTS; i++) {
            float here = turn + i * step;
            float lit = beadLight(here - turn, bead);
            plot(a, here, radius, centerY, 0.0f);
            plot(b, here + step, radius, centerY, 0.0f);
            b.sub(a, c);
            Sweep.ring(ctx, a, c, band, thick * 1.6f,
                Material.argb(Material.mix(deep, rim, lit * 0.6f), 0.24f + lit * 0.34f),
                0.22f + lit * 0.4f, rim);
        }
    }

    private static float beadLight(float angle, float bead) {
        float behind = wrap(bead - angle);
        if (behind > 0.8f) {
            return 0.0f;
        }
        float fade = 1.0f - behind / 0.8f;
        return fade * fade;
    }

    private static float wrap(float radians) {
        float full = (float) (Math.PI * 2.0);
        float value = radians % full;
        return value < 0.0f ? value + full : value;
    }

    private static void plot(Vector3f out, float angle, float radius, float centerY, float offset) {
        float cos = (float) Math.cos(angle) * radius;
        float sin = (float) Math.sin(angle) * radius;
        at.set(0.0f, centerY, 0.0f).fma(cos, axisX).fma(sin, axisY);

        out.set(axisX).cross(axisY).normalize().mul(offset).add(at);
    }
}
