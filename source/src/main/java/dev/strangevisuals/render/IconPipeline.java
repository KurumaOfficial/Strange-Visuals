package dev.strangevisuals.render;

import net.minecraft.client.gui.DrawContext;

public final class IconPipeline {
    private IconPipeline() {
    }

    public static void init() {
    }

    public static void draw(DrawContext context, GlyphAtlas atlas, int codepoint, float centerX, float centerY, float size, int argb) {
        GlyphAtlas.Glyph glyph = atlas.glyph(codepoint);
        if (glyph == null || !glyph.visible) {
            return;
        }
        float width = glyph.aspect >= 1.0f ? size : size * glyph.aspect;
        float height = glyph.aspect >= 1.0f ? size / glyph.aspect : size;
        SdfPipeline.draw(context, atlas, glyph, centerX - width * 0.5f, centerY - height * 0.5f, width, height, argb);
    }
}
