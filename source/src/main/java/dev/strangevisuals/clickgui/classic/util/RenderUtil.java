package dev.strangevisuals.clickgui.classic.util;

import dev.strangevisuals.render.RoundedRectPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RenderUtil {

    private RenderUtil() {
    }

    public static final class Round {
        public static void draw(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
            RoundedRectPipeline.draw(ctx, x, y, width, height, radius, color);
        }
    }

        public static final class Border {
        public static void draw(DrawContext ctx, float x, float y, float width, float height, float radius, float thickness, int color) {
            RoundedRectPipeline.drawOutline(ctx, x, y, width, height, radius, thickness, color);
        }
    }

    public static final class Rect {
        public static void draw(DrawContext ctx, float x, float y, float width, float height, int color) {
            ctx.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
        }
    }

    public static final class Image {
        public static void draw(DrawContext ctx, Identifier identifier, float x, float y, float width, float height, int color) {
            int ix = Math.round(x);
            int iy = Math.round(y);
            int iw = Math.max(1, Math.round(width));
            int ih = Math.max(1, Math.round(height));
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, ix, iy, 0.0f, 0.0f, iw, ih, iw, ih, iw, ih, color);
        }
    }

    public static final class Shadow {
        public static void draw(DrawContext ctx, float x, float y, float width, float height, float radius, float blur, int color) {
            RoundedRectPipeline.draw(ctx, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, color);
        }
    }
}
