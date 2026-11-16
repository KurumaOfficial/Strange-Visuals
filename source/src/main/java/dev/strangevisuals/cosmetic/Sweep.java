package dev.strangevisuals.cosmetic;

import org.joml.Vector3f;

public final class Sweep {
    private static final int MAX_SIDES = 24;

    private static final Vector3f[] wasAt = new Vector3f[MAX_SIDES];
    private static final Vector3f[] wasNormal = new Vector3f[MAX_SIDES];
    private static final Vector3f[] nowAt = new Vector3f[MAX_SIDES];
    private static final Vector3f[] nowNormal = new Vector3f[MAX_SIDES];

    private static final Vector3f right = new Vector3f();
    private static final Vector3f up = new Vector3f();
    private static final Vector3f heading = new Vector3f();
    private static final Vector3f scratch = new Vector3f();
    private static final Vector3f last = new Vector3f();

    private static int sides;
    private static boolean started;
    private static int wasColor;
    private static float wasRim;

    static {
        for (int i = 0; i < MAX_SIDES; i++) {
            wasAt[i] = new Vector3f();
            wasNormal[i] = new Vector3f();
            nowAt[i] = new Vector3f();
            nowNormal[i] = new Vector3f();
        }
    }

    private Sweep() {
    }

    public static void begin(int wanted) {
        sides = Math.max(3, Math.min(MAX_SIDES, wanted));
        started = false;
    }

    public static void ringAcross(CosmeticContext ctx, Vector3f centre, Vector3f forward,
                                  Vector3f across, float acrossRight, float acrossUp, int color,
                                  float rimStrength, int rimColor) {
        heading.set(forward);
        if (heading.lengthSquared() < 1.0e-9f) {
            heading.set(0.0f, 0.0f, -1.0f);
        }
        heading.normalize();
        scratch.set(across).fma(-across.dot(heading), heading);
        if (scratch.lengthSquared() < 1.0e-9f) {
            ring(ctx, centre, forward, acrossRight, acrossUp, color, rimStrength, rimColor);
            return;
        }
        right.set(scratch).normalize();
        right.cross(heading, up);
        up.normalize();
        build(ctx, centre, acrossRight, acrossUp, color, rimStrength, rimColor);
    }

    public static void ring(CosmeticContext ctx, Vector3f centre, Vector3f forward,
                            float acrossRight, float acrossUp, int color, float rimStrength,
                            int rimColor) {
        heading.set(forward);
        if (heading.lengthSquared() < 1.0e-9f) {
            heading.set(0.0f, 0.0f, -1.0f);
        }
        heading.normalize();

        float widen = 1.0f;
        if (started) {
            float turn = last.dot(heading);
            scratch.set(last).add(heading);
            if (scratch.lengthSquared() > 1.0e-8f) {
                last.set(heading);
                heading.set(scratch).normalize();
                widen = 1.0f / Math.max(0.55f, (float) Math.sqrt(Math.max(0.0, (1.0 + turn) * 0.5)));
            } else {
                last.set(heading);
            }
        } else {
            last.set(heading);
        }
        acrossRight *= widen;
        acrossUp *= widen;

        if (!started) {
            up.set(0.0f, 1.0f, 0.0f);
            if (Math.abs(heading.y) > 0.97f) {
                up.set(0.0f, 0.0f, 1.0f);
            }
            heading.cross(up, right);
            right.normalize();
            right.cross(heading, up);
            up.normalize();
        } else {
            scratch.set(right).fma(-right.dot(heading), heading);
            if (scratch.lengthSquared() < 1.0e-9f) {
                scratch.set(up).fma(-up.dot(heading), heading);
            }
            right.set(scratch).normalize();
            right.cross(heading, up);
            up.normalize();
        }

        build(ctx, centre, acrossRight, acrossUp, color, rimStrength, rimColor);
    }

    private static void build(CosmeticContext ctx, Vector3f centre, float acrossRight,
                              float acrossUp, int color, float rimStrength, int rimColor) {
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2.0 * i / sides;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            nowAt[i].set(centre).fma(cos * acrossRight, right).fma(sin * acrossUp, up);

            nowNormal[i].set(0.0f, 0.0f, 0.0f)
                .fma(cos * Math.max(acrossUp, 1.0e-4f), right)
                .fma(sin * Math.max(acrossRight, 1.0e-4f), up)
                .normalize();
        }

        if (started) {
            for (int i = 0; i < sides; i++) {
                int next = (i + 1) % sides;
                Material.bodySmooth(ctx, wasAt[i], wasAt[next], nowAt[next], nowAt[i],
                    wasNormal[i], wasNormal[next], nowNormal[next], nowNormal[i],
                    wasColor, wasColor, color, color);
                if (rimStrength > 0.0f || wasRim > 0.0f) {
                    Material.rimSmooth(ctx, wasAt[i], wasAt[next], nowAt[next], nowAt[i],
                        wasNormal[i], wasNormal[next], nowNormal[next], nowNormal[i],
                        rimColor, (wasRim + rimStrength) * 0.5f);
                }
            }
        }

        for (int i = 0; i < sides; i++) {
            wasAt[i].set(nowAt[i]);
            wasNormal[i].set(nowNormal[i]);
        }
        wasColor = color;
        wasRim = rimStrength;
        started = true;
    }

    public static void cap(CosmeticContext ctx, Vector3f centre, int color) {
        if (!started) {
            return;
        }
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            Material.bodySmooth(ctx, wasAt[i], wasAt[next], centre, centre,
                wasNormal[i], wasNormal[next], heading, heading, color, color, color, color);
        }
    }
}
