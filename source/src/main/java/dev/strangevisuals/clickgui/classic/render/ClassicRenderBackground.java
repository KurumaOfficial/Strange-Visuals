package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.localization.GuiLocalization;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import dev.strangevisuals.clickgui.classic.util.ColorUtil;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.classic.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class ClassicRenderBackground {
    private static final Identifier LOGO = StrangeVisuals.id("textures/gui/logo.png");

    private ClassicRenderBackground() {
    }

    public static void renderBackground(DrawContext ctx) {
        float x = ClassicGuiState.x;
        float y = ClassicGuiState.y;
        float width = ClassicGuiState.width;
        float height = ClassicGuiState.height;

        ClassicTheme theme = ClassicThemeManager.getTheme();
        boolean isTranslucent = theme == ClassicTheme.TRANSPARENT_WHITE
            || theme == ClassicTheme.TRANSPARENT_BLACK
            || theme == ClassicTheme.PURPLE
            || theme == ClassicTheme.PINK;

        // Shadow & Background
        RenderUtil.Shadow.draw(ctx, x - 2, y - 2, width, height, 8, 12, 0x40000000);
        int bgAlpha = isTranslucent ? 180 : 255;
        int bgColor = ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), bgAlpha);
        RenderUtil.Round.draw(ctx, x, y, width, height, 8, bgColor);

        // Header logo & text
        int textCol = ColorUtil.getTextColor(1, 1);
        RenderUtil.Image.draw(ctx, LOGO, x + 8, y + 8, 16, 16, ColorUtil.replAlpha(textCol, 204));
        FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, "Strange Visuals", x + 28, y + 15, 8, ColorUtil.replAlpha(textCol, 204));
        FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, "FREE", x + 28, y + 22, 5, ColorUtil.replAlpha(textCol, 127));

        // Search bar at the bottom
        renderSearchBar(ctx, x, y, width, height);
    }

    private static void renderSearchBar(DrawContext ctx, float x, float y, float width, float height) {
        float searchWidth = 90.0F;
        float searchHeight = 20.0F;
        float searchX = x + (width / 2.0F) - (searchWidth / 2.0F);
        float searchY = y + height + 8.0F;

        int alphaValue = ClassicGuiState.searchActive ? 210 : 160;
        int searchBgColor = ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), alphaValue);

        RenderUtil.Round.draw(ctx, searchX, searchY, searchWidth, searchHeight, 4, searchBgColor);
        RenderUtil.Border.draw(ctx, searchX, searchY, searchWidth, searchHeight, 4, 0.5f,
            ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), ClassicGuiState.searchActive ? 180 : 80));

        String searchText = ClassicGuiState.searchQuery.isEmpty()
            ? GuiLocalization.tr("gui.search.placeholder")
            : ClassicGuiState.searchQuery;

        int textColor = ClassicGuiState.searchQuery.isEmpty()
            ? ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), 127)
            : ColorUtil.replAlpha(ColorUtil.getTextColor(1, 1), 255);

        float textY = searchY + (searchHeight - FontDraw.getHeight(FontDraw.FontType.MEDIUM, 6)) / 2.0F
            + FontDraw.getAscent(FontDraw.FontType.MEDIUM, 6);
        FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, searchText, searchX + 8, textY, 6, textColor);

        if (ClassicGuiState.searchActive) {
            boolean cursorVisible = (System.currentTimeMillis() % 1000L) >= 500L;
            if (cursorVisible) {
                float cursorX = searchX + 8 + FontDraw.getWidth(FontDraw.FontType.MEDIUM, searchText, 6) + 1;
                RenderUtil.Rect.draw(ctx, cursorX, searchY + 5.0F, 1.0F, searchHeight - 10.0F, textColor);
            }
        }
    }
}
