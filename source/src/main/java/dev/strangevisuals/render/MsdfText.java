package dev.strangevisuals.render;

import net.minecraft.client.gui.DrawContext;

public final class MsdfText {
    private static final float CAP_HEIGHT = 0.70f;
    private static final float ASCENDER = 0.952f;

    private MsdfText() {
    }

    public static float topFor(float centerY, float size) {
        return centerY + CAP_HEIGHT * size * 0.5f - ASCENDER * size;
    }

    public static void drawOn(DrawContext context, GlyphAtlas font, String text, float x,
                              float centerY, float size, int argb) {
        draw(context, font, text, x, topFor(centerY, size), size, argb);
    }

    public static void drawOnExact(DrawContext context, GlyphAtlas font, String text, float x,
                                   float centerY, float size, int argb) {
        drawExact(context, font, text, x, topFor(centerY, size), size, argb);
    }

    public static void drawCenteredOn(DrawContext context, GlyphAtlas font, String text,
                                      float centerX, float centerY, float size, int argb) {
        draw(context, font, text, centerX - font.width(text, size) * 0.5f,
            topFor(centerY, size), size, argb);
    }

    public static void drawRightOn(DrawContext context, GlyphAtlas font, String text,
                                   float rightX, float centerY, float size, int argb) {
        draw(context, font, text, rightX - font.width(text, size), topFor(centerY, size), size, argb);
    }

    public static void draw(DrawContext context, GlyphAtlas font, String text, float x, float y, float size, int argb) {
        write(context, font, text, x, Math.round(y + font.ascender() * size), size, argb);
    }

    public static void drawExact(DrawContext context, GlyphAtlas font, String text, float x, float y, float size, int argb) {
        write(context, font, text, x, y + font.ascender() * size, size, argb);
    }

    public static void drawBaseline(DrawContext context, GlyphAtlas font, String text, float x, float baseline, float size, int argb) {
        write(context, font, text, x, baseline, size, argb);
    }

    private static void write(DrawContext context, GlyphAtlas font, String text, float x, float baseline, float size, int argb) {
        float cursor = x;
        int previous = -1;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            GlyphAtlas.Glyph glyph = font.glyph(codepoint);
            if (glyph != null) {
                cursor += font.kerning(previous, codepoint) * size;
                if (glyph.visible) {
                    SdfPipeline.draw(context, font, glyph,
                        cursor + glyph.planeLeft * size,
                        baseline - glyph.planeTop * size,
                        (glyph.planeRight - glyph.planeLeft) * size,
                        (glyph.planeTop - glyph.planeBottom) * size,
                        argb);
                }
                cursor += glyph.advance * size;
                previous = codepoint;
            }
            offset += Character.charCount(codepoint);
        }
    }

    public static void drawCentered(DrawContext context, GlyphAtlas font, String text, float centerX, float y, float size, int argb) {
        draw(context, font, text, centerX - font.width(text, size) * 0.5f, y, size, argb);
    }

    public static void drawRight(DrawContext context, GlyphAtlas font, String text, float rightX, float y, float size, int argb) {
        draw(context, font, text, rightX - font.width(text, size), y, size, argb);
    }

    public static float width(GlyphAtlas font, String text, float size) {
        return font.width(text, size);
    }
}
