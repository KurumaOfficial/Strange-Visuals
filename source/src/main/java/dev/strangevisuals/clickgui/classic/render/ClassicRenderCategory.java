package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.clickgui.classic.ClassicCategory;
import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.theme.ClassicTheme;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import dev.strangevisuals.clickgui.classic.util.ColorUtil;
import dev.strangevisuals.clickgui.classic.util.FontDraw;
import dev.strangevisuals.clickgui.classic.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

public final class ClassicRenderCategory {

    private ClassicRenderCategory() {
    }

    public static void renderCategory(DrawContext ctx) {
        float x = ClassicGuiState.x;
        float y = ClassicGuiState.y;

        ClassicTheme theme = ClassicThemeManager.getTheme();
        boolean isTranslucent = theme == ClassicTheme.TRANSPARENT_WHITE || theme == ClassicTheme.TRANSPARENT_BLACK;
        boolean isAccentSoft = theme == ClassicTheme.PURPLE || theme == ClassicTheme.PINK;

        int barBgColor = isTranslucent
            ? ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 127)
            : isAccentSoft
            ? ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 160)
            : ColorUtil.getMainColor(1, 1);

        RenderUtil.Round.draw(ctx, x + 7.0F, y + 30.0F, 195.0F, 12.0F, 4.0F, barBgColor);

        float xGo = 0.0F;
        for (ClassicCategory category : ClassicCategory.values()) {
            float textWidth = FontDraw.getWidth(FontDraw.FontType.MEDIUM, category.getName(), 5);
            if (category == ClassicGuiState.selectedCategory) {
                int pillBg = isTranslucent
                    ? ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1), 127)
                    : isAccentSoft
                    ? ColorUtil.replAlpha(ColorUtil.getBackGroundColor(1, 1), 127)
                    : ColorUtil.getBackGroundColor(1, 1);

                RenderUtil.Border.draw(ctx, x + 10.0F + xGo, y + 31.0F, textWidth + 6.0F, 10.0F, 3.0F, 0.05F, pillBg);
                RenderUtil.Round.draw(ctx, x + 10.0F + xGo, y + 31.0F, textWidth + 6.0F, 10.0F, 3.0F, pillBg);
            }
            FontDraw.drawText(FontDraw.FontType.MEDIUM, ctx, category.getName(), x + 13.5F + xGo, y + 38.0F, 5, ColorUtil.getTextColor(1, 1));
            xGo += 9.0F + textWidth;
        }
    }
}
