package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Seraph {
    private static final int PINIONS = 11;
    private static final int PINION_SIDES = 9;
    private static final int PINION_STEPS = 7;
    private static final int SPAR_STEPS = 10;

    private static final float ROOT_X = 0.12f;
    private static final float ROOT_Y = 1.3f;
    private static final float ROOT_Z = -0.1f;

    private static final float SPAR_LENGTH = 0.95f;
    private static final float SPAR_RISE = 0.5f;
    private static final float PINION_LENGTH = 1.05f;

    private static final Vector3f hinge = new Vector3f();
    private static final Vector3f tip = new Vector3f();
    private static final Vector3f a = new Vector3f();
    private static final Vector3f b = new Vector3f();
    private static final Vector3f c = new Vector3f();
    private static final Vector3f d = new Vector3f();
    private static final Vector3f spineTop = new Vector3f();
    private static final Vector3f spineTip = new Vector3f();

    private Seraph() {
    }

    public static void render(CosmeticContext ctx) {
        float drive = ctx.drive();
        float beat = 0.055f + drive * 0.12f + ctx.hurt * 0.14f;

        for (int side = -1; side <= 1; side += 2) {
            spar(ctx, side, drive);
            for (int i = 0; i < PINIONS; i++) {
                pinion(ctx, side, i, beat, drive);
            }
        }
    }

    private static void spar(CosmeticContext ctx, int side, float drive) {
        int rim = Material.lift(ctx.color, 0.5f);
        int deep = Material.deepen(Material.shift(ctx.color, -10.0f), 0.34f);
        Sweep.begin(9);
        for (int i = 0; i <= SPAR_STEPS; i++) {
            float share = i / (float) SPAR_STEPS;
            hingeAt(hinge, ctx, side, share, drive);
            hingeAt(tip, ctx, side, Math.min(1.0f, share + 0.08f), drive);
            float thin = (0.026f - share * 0.014f) * ctx.size;
            Sweep.ring(ctx, hinge, tip.sub(hinge), thin, thin,
                Material.argb(Material.mix(deep, rim, share * 0.5f), 0.38f - share * 0.16f),
                0.3f - share * 0.12f, rim);
        }
    }

    private static void pinion(CosmeticContext ctx, int side, int index, float beat, float drive) {
        float share = index / (float) (PINIONS - 1);
        hingeAt(hinge, ctx, side, share, drive);

        float wave = (float) Math.sin(ctx.time * 2.6f - share * 2.4f + (side > 0 ? 0.0f : 0.9f));
        float droop = (float) Math.toRadians(28.0f + share * 34.0f) + wave * beat;
        float length = PINION_LENGTH * ctx.size * (0.55f + 0.45f * (float) Math.sin(Math.PI * (0.2f + share * 0.75f)));
        float half = (0.055f - share * 0.022f) * ctx.size;

        float reachX = (float) Math.sin(droop) * length * 0.35f;
        float reachY = -(float) Math.cos(droop) * length;
        float reachZ = -length * 0.24f;
        tip.set(hinge).add(reachX * side, reachY, reachZ);

        int deep = Material.deepen(Material.shift(ctx.color, -10.0f), 0.3f);
        int cool = Material.shift(ctx.color, 24.0f);
        int rim = Material.lift(ctx.color, 0.45f);

        b.set(tip).sub(hinge);
        c.set(side, 0.0f, 0.35f * side).normalize();
        Sweep.begin(PINION_SIDES);
        for (int step = 0; step <= PINION_STEPS; step++) {
            float t = step / (float) PINION_STEPS;

            float across = half * (float) Math.pow(1.0f - t, 0.55f) * (0.75f + 0.25f
                * (float) Math.sin(Math.PI * Math.min(1.0, t * 1.6)));
            a.set(hinge).fma(t, b);
            Sweep.ringAcross(ctx, a, b, c, across, across * 0.34f,
                Material.argb(Material.mix(deep, cool, t * t), 0.34f - t * 0.26f),
                0.26f - t * 0.14f, rim);
        }

        spineTop.set(hinge).fma(half * 0.9f, c);
        spineTip.set(tip).fma(half * 0.2f, c);
        d.set(0.0f, half * 0.22f, 0.0f);
        a.set(spineTop).add(d);
        b.set(spineTop).sub(d);
        Material.lightQuad(ctx, a, b, spineTip, spineTip,
            Material.argb(rim, 0.24f), Material.argb(rim, 0.24f),
            Material.argb(rim, 0.05f), Material.argb(rim, 0.05f));
    }

    private static void hingeAt(Vector3f out, CosmeticContext ctx, int side, float share, float drive) {
        float open = 1.0f + drive * 0.14f;
        float reach = SPAR_LENGTH * ctx.size * share * open;
        float rise = SPAR_RISE * ctx.size * (float) Math.sin(share * Math.PI * 0.62) * open;
        out.set((ROOT_X + reach) * side, ROOT_Y + rise, ROOT_Z - reach * 0.3f);
    }
}
