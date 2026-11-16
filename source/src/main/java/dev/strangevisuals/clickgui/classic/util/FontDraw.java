package dev.strangevisuals.clickgui.classic.util;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.MsdfText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class FontDraw {

    private static final Identifier REGULAR_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier REGULAR_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier BOLD_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier BOLD_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");

    private static GlyphAtlas regular;
    private static GlyphAtlas bold;

    public enum FontType {
        MEDIUM,
        SEMIBOLD
    }

    private FontDraw() {
    }

    private static GlyphAtlas getFont(FontType type) {
        try {
            if (type == FontType.SEMIBOLD) {
                if (bold == null) bold = GlyphAtlas.load(BOLD_DATA, BOLD_ATLAS);
                return bold;
            } else {
                if (regular == null) regular = GlyphAtlas.load(REGULAR_DATA, REGULAR_ATLAS);
                return regular;
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Classic GUI failed to load font {}: {}", type, e.getMessage());
            return null;
        }
    }

    public static void drawText(FontType fontType, DrawContext ctx, String text, float x, float y, int size, int color) {
        drawText(fontType, ctx, text, x, y, size, color, false);
    }

    public static void drawText(FontType fontType, DrawContext ctx, String text, float x, float y, int size, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GlyphAtlas font = getFont(fontType);
        if (font != null && font.canRender(text)) {
            float ptSize = size * 1.05f;
            MsdfText.drawBaseline(ctx, font, text, x, y, ptSize, color);
            return;
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null) {
            return;
        }

        float scale = size <= 0 ? 1.0f : (size / 8.0f);
        float ascent = 7.0f * scale;
        float renderY = y - ascent;

        Matrix3x2fStack matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, renderY);
        matrices.scale(scale, scale);
        ctx.drawText(tr, text, 0, 0, color, shadow);
        matrices.popMatrix();
    }

    public static void drawCenter(FontType fontType, DrawContext ctx, String text, float centerX, float y, int size, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float w = getWidth(fontType, text, size);
        drawText(fontType, ctx, text, centerX - w / 2f, y, size, color, shadow);
    }

    public static float getWidth(FontType fontType, String text, int size) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        GlyphAtlas font = getFont(fontType);
        if (font != null) {
            float ptSize = size * 1.05f;
            return font.width(text, ptSize);
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float baseWidth = tr != null ? tr.getWidth(text) : (text.length() * 6.0f);
        float scale = size <= 0 ? 1.0f : (size / 8.0f);
        return baseWidth * scale;
    }

    public static float getHeight(FontType fontType, int size) {
        float ptSize = size * 1.05f;
        return ptSize * 1.15f;
    }

    public static float getAscent(FontType fontType, int size) {
        GlyphAtlas font = getFont(fontType);
        float ptSize = size * 1.05f;
        return font != null ? font.ascender() * ptSize : 7.0f * (size / 8.0f);
    }
}
