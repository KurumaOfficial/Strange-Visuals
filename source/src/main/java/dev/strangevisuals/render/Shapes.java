package dev.strangevisuals.render;

import net.minecraft.client.gui.DrawContext;

public final class Shapes {
    private Shapes() {
    }

    public static void circle(DrawContext context, float centerX, float centerY, float radius, int argb) {
        int top = Math.round(centerY - radius);
        int bottom = Math.round(centerY + radius);
        for (int y = top; y < bottom; y++) {
            float dy = (y + 0.5f) - centerY;
            float remaining = radius * radius - dy * dy;
            if (remaining <= 0.0f) {
                continue;
            }
            float half = (float) Math.sqrt(remaining);
            int left = Math.round(centerX - half);
            int right = Math.round(centerX + half);
            context.fill(left, y, right, y + 1, argb);
        }
    }

    public static void ring(DrawContext context, float centerX, float centerY, float radius, float thickness, int argb) {
        float inner = radius - thickness;
        int top = Math.round(centerY - radius);
        int bottom = Math.round(centerY + radius);
        for (int y = top; y < bottom; y++) {
            float dy = (y + 0.5f) - centerY;
            float outerSpan = radius * radius - dy * dy;
            if (outerSpan <= 0.0f) {
                continue;
            }
            float outerHalf = (float) Math.sqrt(outerSpan);
            float innerSpan = inner * inner - dy * dy;
            if (innerSpan <= 0.0f) {
                int left = Math.round(centerX - outerHalf);
                int right = Math.round(centerX + outerHalf);
                context.fill(left, y, right, y + 1, argb);
                continue;
            }
            float innerHalf = (float) Math.sqrt(innerSpan);
            int leftOuter = Math.round(centerX - outerHalf);
            int leftInner = Math.round(centerX - innerHalf);
            int rightInner = Math.round(centerX + innerHalf);
            int rightOuter = Math.round(centerX + outerHalf);
            context.fill(leftOuter, y, leftInner, y + 1, argb);
            context.fill(rightInner, y, rightOuter, y + 1, argb);
        }
    }
}
