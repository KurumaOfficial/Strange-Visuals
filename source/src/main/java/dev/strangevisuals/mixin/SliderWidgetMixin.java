package dev.strangevisuals.mixin;

import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.render.WidgetStylePipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {
    @Shadow protected double value;

    protected SliderWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$customSlider(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            return;
        }
        Screen screen = client.currentScreen;
        if (screen instanceof dev.strangevisuals.ui.editor.MainMenuEditorScreen
                || screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen
                || screen instanceof net.minecraft.client.gui.screen.ChatScreen
                || screen instanceof net.minecraft.client.gui.screen.TitleScreen) {
            return;
        }

        // Authentic Vanilla Theme: custom slider track & thumb across all screens
        if (dev.strangevisuals.ui.style.StyleManager.getStyle() == dev.strangevisuals.ui.style.MenuStyle.VANILLA) {
            boolean isHovered = isSelected() || isHovered();
            dev.strangevisuals.render.VanillaStylePipeline.renderSlider(context, getX(), getY(), getWidth(), getHeight(), value, getMessage(), this.active, isHovered);
            ci.cancel();
            return;
        }

        // Modern Beta Theme:
        if (screen instanceof GameMenuScreen && !dev.strangevisuals.ui.editor.MainMenuConfig.get().customPauseScreen) {
            return;
        }

        if ((screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                || screen instanceof PackScreen || screen instanceof SelectWorldScreen)
                && !dev.strangevisuals.ui.editor.MainMenuConfig.get().customSettingsScreen) {
            return;
        }

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean isHovered = isSelected() || isHovered() || (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h);
        float hover = WidgetStylePipeline.updateHover(this, isHovered, delta);

            // 1. Deep obsidian glass track
            int fillAlpha = Math.round(150.0f + hover * 50.0f);
            RoundedRectPipeline.draw(context, x, y, w, h, (fillAlpha << 24) | 0x0E0F14);

            // 2. Subtle ambient outline
            int borderRgb = WidgetStylePipeline.mix(0xFFFFFF, NeonEdge.cool(), hover);
            int borderAlpha = Math.round(20.0f + hover * 75.0f);
            RoundedRectPipeline.drawOutline(context, x, y, w, h, (borderAlpha << 24) | (borderRgb & 0xFFFFFF));

            // 3. Active cyan fill bar
            float fillW = (float) (w * Math.max(0.0, Math.min(1.0, value)));
            if (fillW > 4.0f) {
                RoundedRectPipeline.draw(context, x + 1.0f, y + 1.0f, fillW - 2.0f, h - 2.0f,
                    (Math.round(45.0f + hover * 35.0f) << 24) | (NeonEdge.cool() & 0xFFFFFF));
            }

            // 4. Thumb handle
            float thumbW = 8.0f;
            float thumbX = (float) (x + (w - thumbW) * Math.max(0.0, Math.min(1.0, value)));
            int thumbColor = WidgetStylePipeline.mix(0x1E2638, 0xFFFFFF, hover);
            RoundedRectPipeline.draw(context, thumbX, y + 2.0f, thumbW, h - 4.0f, 0xEE000000 | thumbColor);
            RoundedRectPipeline.drawOutline(context, thumbX, y + 2.0f, thumbW, h - 4.0f, NeonEdge.cool());

            // 5. Centered label
            int textRgb = this.active ? WidgetStylePipeline.mix(0xD7DCE6, 0xFFFFFF, hover) : 0x657185;
            WidgetStylePipeline.drawCenteredLabel(context, getMessage(), x, y, w, h, 0xFF000000 | textRgb);

            ci.cancel();
    }
}
