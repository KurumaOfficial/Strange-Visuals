package dev.strangevisuals.clickgui.classic.render;

import dev.strangevisuals.clickgui.classic.ClassicGuiState;
import dev.strangevisuals.clickgui.classic.theme.ClassicThemeManager;
import net.minecraft.client.gui.DrawContext;

public final class ClassicGuiRender {

    private ClassicGuiRender() {
    }

    public static void render(DrawContext ctx, double mouseX, double mouseY, float deltaTicks) {
        ClassicThemeManager.update();

        ClassicRenderBackground.renderBackground(ctx);
        ClassicRenderCategory.renderCategory(ctx);
        ClassicRenderModule.renderModule(ctx, mouseX, mouseY);
        ClassicRenderTheme.renderTheme(ctx, mouseX, mouseY);
        ClassicRenderTheme.renderLanguageSwitch(ctx, mouseX, mouseY);
    }
}
